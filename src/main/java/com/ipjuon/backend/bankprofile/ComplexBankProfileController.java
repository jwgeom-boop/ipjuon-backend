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

/**
 * 단지×은행 프로필 CRUD.
 *
 * - 입주민 앱 (b2c): GET /api/b2c/complex-banks/{complex}/{bank}
 *     단지별 데이터 우선 → 없으면 BankProfile (글로벌) fallback
 * - 팀장/상담사 (v4): GET/POST/PUT/DELETE /api/v4/my-bank/complex-profiles/...
 *     본인 은행의 단지별 프로필만 관리
 */
@RestController
@CrossOrigin(origins = "*")
public class ComplexBankProfileController {

    private static final Logger log = LoggerFactory.getLogger(ComplexBankProfileController.class);

    private final ComplexBankProfileRepository repo;
    private final BankProfileRepository globalRepo;
    private final VendorRepository vendorRepo;

    public ComplexBankProfileController(ComplexBankProfileRepository repo,
                                        BankProfileRepository globalRepo,
                                        VendorRepository vendorRepo) {
        this.repo = repo;
        this.globalRepo = globalRepo;
        this.vendorRepo = vendorRepo;
    }

    // ========================================
    // 1. B2C 입주민 앱 — 단지별 우선 + 글로벌 fallback
    // ========================================
    @GetMapping("/api/b2c/complex-banks/{complexName}/{bankName}")
    public ResponseEntity<Map<String, Object>> b2cComplexBankDetail(@PathVariable String complexName,
                                                                     @PathVariable String bankName) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 단지×은행 조합 우선
        Optional<ComplexBankProfile> complexOpt = repo.findByComplexAndBank(complexName, bankName);
        if (complexOpt.isPresent()) {
            ComplexBankProfile p = complexOpt.get();
            result.put("source", "complex");      // 데이터 출처 표시 (디버깅/UI 용)
            result.put("bank_name", p.getBank_name());
            result.put("complex_name", p.getComplex_name());
            result.put("branch_name", p.getBranch_name());
            result.put("greeting", p.getGreeting());
            result.put("products", p.getProducts());
            result.put("business_hours", p.getBusiness_hours());
            result.put("notice", p.getNotice());
            result.put("is_closed", p.getIs_closed());
            result.put("closing_message", p.getClosing_message());
            result.put("contact_phone", p.getContact_phone());
            result.put("contact_email", p.getContact_email());
            return ResponseEntity.ok(result);
        }

        // 단지별 없으면 글로벌 BankProfile
        Optional<BankProfile> globalOpt = globalRepo.findByBank_name(bankName);
        if (globalOpt.isPresent()) {
            BankProfile p = globalOpt.get();
            result.put("source", "global");      // 글로벌 fallback
            result.put("bank_name", p.getBank_name());
            result.put("complex_name", complexName);
            result.put("branch_name", null);
            result.put("greeting", p.getGreeting());
            result.put("products", p.getProducts());
            result.put("business_hours", p.getBusiness_hours());
            result.put("notice", p.getNotice());
            result.put("is_closed", p.getIs_closed());
            result.put("closing_message", p.getClosing_message());
            result.put("contact_phone", p.getContact_phone());
            result.put("contact_email", p.getContact_email());
            return ResponseEntity.ok(result);
        }

        // 둘 다 없으면 404
        return ResponseEntity.notFound().build();
    }

    // ========================================
    // 2. v4 팀장/상담사 — 본인 은행 단지별 프로필 관리
    // ========================================

    /** 본인 은행의 모든 단지별 프로필 목록 */
    @GetMapping("/api/v4/my-bank/complex-profiles")
    public List<ComplexBankProfile> listMyBankComplexProfiles(HttpServletRequest req) {
        Vendor v = currentVendor(req);
        if (v == null) return Collections.emptyList();
        return repo.findAllByBank(v.getVendorName());
    }

    /** 단건 */
    @GetMapping("/api/v4/my-bank/complex-profiles/{id}")
    public ResponseEntity<ComplexBankProfile> getOne(HttpServletRequest req, @PathVariable UUID id) {
        Vendor v = currentVendor(req);
        if (v == null) return ResponseEntity.status(401).build();
        return repo.findById(id)
                .filter(p -> v.getVendorName().equals(p.getBank_name()))   // 본인 은행 보호
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 신규 등록 */
    @PostMapping("/api/v4/my-bank/complex-profiles")
    @Transactional
    public ResponseEntity<ComplexBankProfile> create(HttpServletRequest req,
                                                      @RequestBody Map<String, Object> body) {
        Vendor v = currentVendor(req);
        if (v == null) return ResponseEntity.status(401).build();

        String complexName = asString(body.get("complex_name"));
        if (complexName == null) {
            return ResponseEntity.badRequest().build();
        }

        // 이미 같은 (complex_name, bank_name) 조합 있으면 거부 (UNIQUE 제약)
        if (repo.findByComplexAndBank(complexName, v.getVendorName()).isPresent()) {
            return ResponseEntity.status(409).build();
        }

        ComplexBankProfile p = new ComplexBankProfile();
        p.setComplex_name(complexName);
        p.setBank_name(v.getVendorName());        // 본인 은행 강제
        applyFields(p, body);
        applyMeta(p, v);

        ComplexBankProfile saved = repo.save(p);
        log.info("[단지×은행 프로필 신규] complex={}, bank={}, by={}",
                saved.getComplex_name(), saved.getBank_name(), saved.getUpdatedBy());
        return ResponseEntity.ok(saved);
    }

    /** 수정 */
    @PutMapping("/api/v4/my-bank/complex-profiles/{id}")
    @Transactional
    public ResponseEntity<ComplexBankProfile> update(HttpServletRequest req,
                                                      @PathVariable UUID id,
                                                      @RequestBody Map<String, Object> body) {
        Vendor v = currentVendor(req);
        if (v == null) return ResponseEntity.status(401).build();

        ComplexBankProfile p = repo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        if (!v.getVendorName().equals(p.getBank_name())) {
            return ResponseEntity.status(403).build();      // 다른 은행 데이터 수정 금지
        }

        applyFields(p, body);
        applyMeta(p, v);

        ComplexBankProfile saved = repo.save(p);
        log.info("[단지×은행 프로필 수정] complex={}, bank={}, by={}",
                saved.getComplex_name(), saved.getBank_name(), saved.getUpdatedBy());
        return ResponseEntity.ok(saved);
    }

    /** 삭제 (팀장만) */
    @DeleteMapping("/api/v4/my-bank/complex-profiles/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> delete(HttpServletRequest req, @PathVariable UUID id) {
        Vendor v = currentVendor(req);
        if (v == null) return ResponseEntity.status(401).build();
        String role = resolveBankRole(v);
        if (!"bank_manager".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "삭제 권한 없음 (팀장만)"));
        }

        ComplexBankProfile p = repo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        if (!v.getVendorName().equals(p.getBank_name())) {
            return ResponseEntity.status(403).body(Map.of("error", "다른 은행 데이터 삭제 불가"));
        }

        repo.deleteById(id);
        log.info("[단지×은행 프로필 삭제] id={}, complex={}, bank={}",
                id, p.getComplex_name(), p.getBank_name());
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // === helpers ===
    private Vendor currentVendor(HttpServletRequest req) {
        String loginId = (String) req.getAttribute("auth.loginId");
        if (loginId == null) return null;
        return vendorRepo.findByLoginId(loginId).orElse(null);
    }

    private String resolveBankRole(Vendor v) {
        if (v.getRole() != null && !v.getRole().isEmpty()) return v.getRole();
        return "은행상담사".equals(v.getVendorType()) ? "bank_consultant" : "bank_manager";
    }

    private void applyFields(ComplexBankProfile p, Map<String, Object> body) {
        if (body.containsKey("branch_name"))      p.setBranch_name(asString(body.get("branch_name")));
        if (body.containsKey("greeting"))         p.setGreeting(asString(body.get("greeting")));
        if (body.containsKey("products"))         p.setProducts(asString(body.get("products")));
        if (body.containsKey("business_hours"))   p.setBusiness_hours(asString(body.get("business_hours")));
        if (body.containsKey("notice"))           p.setNotice(asString(body.get("notice")));
        if (body.containsKey("is_closed"))        p.setIs_closed(asBoolean(body.get("is_closed")));
        if (body.containsKey("closing_message"))  p.setClosing_message(asString(body.get("closing_message")));
        if (body.containsKey("contact_phone"))    p.setContact_phone(asString(body.get("contact_phone")));
        if (body.containsKey("contact_email"))    p.setContact_email(asString(body.get("contact_email")));
    }

    private void applyMeta(ComplexBankProfile p, Vendor v) {
        p.setUpdatedBy(v.getLoginId());
        p.setUpdatedByRole(resolveBankRole(v));
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
