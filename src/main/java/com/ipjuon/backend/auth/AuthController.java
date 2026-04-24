package com.ipjuon.backend.auth;

import com.ipjuon.backend.vendor.Vendor;
import com.ipjuon.backend.vendor.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    private final VendorRepository vendorRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(VendorRepository vendorRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.vendorRepository = vendorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        // 관리자 로그인
        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            log.info("[로그인 성공] 관리자: {}", username);
            String token = jwtUtil.generate(username, "admin", null);
            return Map.of("success", true, "role", "admin", "token", token);
        }

        // 은행 계정 로그인 (vendor_accounts 테이블 조회)
        Optional<Vendor> vendorOpt = vendorRepository.findByLoginId(username);
        if (vendorOpt.isPresent()) {
            Vendor vendor = vendorOpt.get();
            String vt = vendor.getVendorType();
            boolean isBankAccount = "은행".equals(vt) || "은행상담사".equals(vt);
            boolean active = "active".equals(vendor.getStatus())
                    && (vendor.getIsActive() == null || Boolean.TRUE.equals(vendor.getIsActive()));
            if (isBankAccount
                    && passwordEncoder.matches(password, vendor.getPassword())
                    && active) {
                // role 결정: vendor.role 우선, 없으면 vendorType 기반 폴백 (legacy 호환)
                String bankRole = vendor.getRole();
                if (bankRole == null || bankRole.isEmpty()) {
                    bankRole = "은행상담사".equals(vt) ? "bank_consultant" : "bank_manager";
                }
                log.info("[로그인 성공] 은행: {} ({}) role={}", vendor.getVendorName(), username, bankRole);
                String token = jwtUtil.generate(username, "bank", vendor.getVendorName(), bankRole);
                Map<String, Object> resp = new java.util.HashMap<>();
                resp.put("success", true);
                resp.put("role", "bank");
                resp.put("bank_role", bankRole);
                resp.put("bank_name", vendor.getVendorName());
                resp.put("display_name", vendor.getBankManager());
                resp.put("must_change_password", Boolean.TRUE.equals(vendor.getMustChangePassword()));
                resp.put("token", token);
                return resp;
            }
            if (!active) {
                log.warn("[로그인 실패] 비활성 계정: {}", username);
            } else {
                log.warn("[로그인 실패] 비밀번호 불일치: {}", username);
            }
        } else {
            log.warn("[로그인 실패] 존재하지 않는 계정: {}", username);
        }

        return Map.of("success", false, "message", "아이디 또는 비밀번호가 올바르지 않습니다");
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> body) {
        String loginId     = body.get("loginId");
        String currentPw   = body.get("currentPassword");
        String newPw       = body.get("newPassword");

        if (loginId == null || currentPw == null || newPw == null)
            return Map.of("success", false, "message", "필수 항목이 누락되었습니다");

        if (newPw.length() < 6)
            return Map.of("success", false, "message", "새 비밀번호는 6자 이상이어야 합니다");

        return vendorRepository.findByLoginId(loginId)
                .map(vendor -> {
                    if (!passwordEncoder.matches(currentPw, vendor.getPassword())) {
                        log.warn("[비밀번호 변경 실패] 현재 비밀번호 불일치: {}", loginId);
                        return Map.<String, Object>of("success", false, "message", "현재 비밀번호가 올바르지 않습니다");
                    }
                    vendor.setPassword(passwordEncoder.encode(newPw));
                    vendor.setMustChangePassword(false); // 최초 변경 완료
                    vendorRepository.save(vendor);
                    log.info("[비밀번호 변경 성공] {}", loginId);
                    return Map.<String, Object>of("success", true, "message", "비밀번호가 변경되었습니다");
                })
                .orElse(Map.of("success", false, "message", "계정을 찾을 수 없습니다"));
    }

    // 팀장이 자기 팀 상담사 비번 초기화 (loginId_2024! 패턴으로 리셋 + must_change_password 플래그)
    @PostMapping("/reset-consultant-password")
    public Map<String, Object> resetConsultantPassword(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody Map<String, String> body
    ) {
        String managerLoginId = (String) request.getAttribute("auth.loginId");
        String targetLoginId  = body.get("targetLoginId");

        if (managerLoginId == null) return Map.of("success", false, "message", "인증 필요");
        if (targetLoginId == null || targetLoginId.isEmpty())
            return Map.of("success", false, "message", "대상 계정이 누락되었습니다");

        Vendor manager = vendorRepository.findByLoginId(managerLoginId).orElse(null);
        if (manager == null) return Map.of("success", false, "message", "팀장 계정 없음");

        // 권한: bank_manager만 허용
        String mRole = manager.getRole();
        if (mRole == null) mRole = "은행상담사".equals(manager.getVendorType()) ? "bank_consultant" : "bank_manager";
        if (!"bank_manager".equals(mRole))
            return Map.of("success", false, "message", "권한이 없습니다");

        Vendor target = vendorRepository.findByLoginId(targetLoginId).orElse(null);
        if (target == null) return Map.of("success", false, "message", "대상 계정 없음");

        // 본인 팀(parent_vendor_id == manager.id) 인지 확인
        if (!manager.getId().equals(target.getParentVendorId()))
            return Map.of("success", false, "message", "본인 팀의 상담사가 아닙니다");

        String tempPw = targetLoginId + "_2024!";
        target.setPassword(passwordEncoder.encode(tempPw));
        target.setMustChangePassword(true);
        vendorRepository.save(target);
        log.info("[비번 초기화] {} -> {} (by {})", targetLoginId, tempPw, managerLoginId);
        return Map.of(
            "success", true,
            "message", "비밀번호가 초기화되었습니다",
            "temp_password", tempPw
        );
    }
}
