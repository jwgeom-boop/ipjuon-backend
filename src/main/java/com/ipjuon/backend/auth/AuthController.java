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
            if ("은행".equals(vendor.getVendorType())
                    && passwordEncoder.matches(password, vendor.getPassword())
                    && "active".equals(vendor.getStatus())) {
                log.info("[로그인 성공] 은행: {} ({})", vendor.getVendorName(), username);
                String token = jwtUtil.generate(username, "bank", vendor.getVendorName());
                return Map.of(
                    "success", true,
                    "role", "bank",
                    "bank_name", vendor.getVendorName(),
                    "token", token
                );
            }
            if (!"active".equals(vendor.getStatus())) {
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
                    vendorRepository.save(vendor);
                    log.info("[비밀번호 변경 성공] {}", loginId);
                    return Map.<String, Object>of("success", true, "message", "비밀번호가 변경되었습니다");
                })
                .orElse(Map.of("success", false, "message", "계정을 찾을 수 없습니다"));
    }
}
