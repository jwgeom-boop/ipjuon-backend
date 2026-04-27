package com.ipjuon.backend.bankprofile;

import com.ipjuon.backend.vendor.Vendor;
import com.ipjuon.backend.vendor.VendorRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class BankProfileController {

    private static final Logger log = LoggerFactory.getLogger(BankProfileController.class);

    private final BankProfileRepository profileRepo;
    private final VendorRepository vendorRepo;

    public BankProfileController(BankProfileRepository profileRepo, VendorRepository vendorRepo) {
        this.profileRepo = profileRepo;
        this.vendorRepo = vendorRepo;
    }

    // ========================================
    // 1. B2C 입주민용 — 인증 불필요
    // ========================================

    /** 은행 카드 목록 (입주민 앱 메인) — 인사글 미리보기 + 마감 여부만 노출 */
    @GetMapping("/api/b2c/banks")
    public List<Map<String, Object>> b2cBankList() {
        return profileRepo.findAllOrdered().stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("bank_name", p.getBank_name());
                    m.put("greeting_preview", preview(p.getGreeting(), 80));
                    m.put("is_closed", p.getIs_closed());
                    m.put("closing_message", p.getClosing_message());
                    m.put("business_hours", p.getBusiness_hours());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** 은행 상세 (동의서 작성 후만 풀 콘텐츠 — 동의서 ID 헤더 검증은 프론트 신뢰 모델) */
    @GetMapping("/api/b2c/banks/{bankName}")
    public ResponseEntity<Map<String, Object>> b2cBankDetail(@PathVariable String bankName) {
        return profileRepo.findByBank_name(bankName)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("bank_name", p.getBank_name());
                    m.put("greeting", p.getGreeting());
                    m.put("products", p.getProducts());
                    m.put("business_hours", p.getBusiness_hours());
                    m.put("notice", p.getNotice());
                    m.put("is_closed", p.getIs_closed());
                    m.put("closing_message", p.getClosing_message());
                    m.put("contact_phone", p.getContact_phone());
                    m.put("contact_email", p.getContact_email());
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================================
    // 2. v4 팀장/상담사용 — 본인 은행만
    // ========================================

    /** 본인 은행 프로필 조회 */
    @GetMapping("/api/v4/bank-profile")
    public ResponseEntity<BankProfile> getMyBankProfile(HttpServletRequest req) {
        Vendor v = currentVendor(req);
        if (v == null) return ResponseEntity.status(401).build();
        BankProfile p = profileRepo.findByBank_name(v.getVendorName())
                .orElseGet(() -> {
                    BankProfile np = new BankProfile();
                    np.setBank_name(v.getVendorName());
                    np.setIs_closed(false);
                    return profileRepo.save(np);
                });
        return ResponseEntity.ok(p);
    }

    /** 본인 은행 프로필 수정 */
    @PutMapping("/api/v4/bank-profile")
    @Transactional
    public ResponseEntity<BankProfile> updateMyBankProfile(HttpServletRequest req,
                                                           @RequestBody Map<String, Object> body) {
        Vendor v = currentVendor(req);
        if (v == null) return ResponseEntity.status(401).build();

        BankProfile p = profileRepo.findByBank_name(v.getVendorName()).orElseGet(BankProfile::new);
        p.setBank_name(v.getVendorName());
        if (body.containsKey("greeting"))         p.setGreeting(asString(body.get("greeting")));
        if (body.containsKey("products"))         p.setProducts(asString(body.get("products")));
        if (body.containsKey("business_hours"))   p.setBusiness_hours(asString(body.get("business_hours")));
        if (body.containsKey("notice"))           p.setNotice(asString(body.get("notice")));
        if (body.containsKey("is_closed"))        p.setIs_closed(asBoolean(body.get("is_closed")));
        if (body.containsKey("closing_message"))  p.setClosing_message(asString(body.get("closing_message")));
        if (body.containsKey("contact_phone"))    p.setContact_phone(asString(body.get("contact_phone")));
        if (body.containsKey("contact_email"))    p.setContact_email(asString(body.get("contact_email")));
        p.setUpdatedBy(v.getLoginId());
        p.setUpdatedByRole(resolveBankRole(v));

        BankProfile saved = profileRepo.save(p);
        log.info("[은행 프로필 수정] bank={}, by={}", saved.getBank_name(), saved.getUpdatedBy());
        return ResponseEntity.ok(saved);
    }

    // ========================================
    // 3. 관리자용 — 모든 은행 프로필 (감독)
    // ========================================
    @GetMapping("/api/admin/bank-profiles")
    public List<BankProfile> listAllForAdmin(HttpServletRequest req) {
        if (!"admin".equals(req.getAttribute("auth.role"))) {
            return Collections.emptyList();
        }
        return profileRepo.findAllOrdered();
    }

    // === helpers ===
    private Vendor currentVendor(HttpServletRequest req) {
        String loginId = (String) req.getAttribute("auth.loginId");
        if (loginId == null) return null;
        return vendorRepo.findByLoginId(loginId).orElse(null);
    }

    private String resolveBankRole(Vendor v) {
        if (v == null) return null;
        if (v.getRole() != null && !v.getRole().isEmpty()) return v.getRole();
        return "은행상담사".equals(v.getVendorType()) ? "bank_consultant" : "bank_manager";
    }

    private static String preview(String s, int max) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }

    private static String asString(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Boolean asBoolean(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean) return (Boolean) o;
        String s = o.toString().trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "y".equals(s);
    }
}
