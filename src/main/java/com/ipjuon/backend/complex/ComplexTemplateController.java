package com.ipjuon.backend.complex;

import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.consultation.ConsultationRequest;
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

/**
 * 단지 템플릿 CRUD + 자동 채움(resolve) + 일괄 적용(apply-batch).
 * 권한: 관리자 / bank_manager / bank_consultant 모두 CRUD 가능 (삭제는 admin/bank_manager만).
 * 마지막 수정자(loginId/role/bank)는 항상 기록되어 충돌 추적 가능.
 */
@RestController
@RequestMapping("/api/v4/complex-templates")
@CrossOrigin(origins = "*")
public class ComplexTemplateController {

    private static final Logger log = LoggerFactory.getLogger(ComplexTemplateController.class);

    private final ComplexTemplateRepository templateRepo;
    private final ComplexTemplateAptFeeRepository feeRepo;
    private final ConsultationRepository consultationRepo;
    private final VendorRepository vendorRepo;

    public ComplexTemplateController(ComplexTemplateRepository templateRepo,
                                      ComplexTemplateAptFeeRepository feeRepo,
                                      ConsultationRepository consultationRepo,
                                      VendorRepository vendorRepo) {
        this.templateRepo = templateRepo;
        this.feeRepo = feeRepo;
        this.consultationRepo = consultationRepo;
        this.vendorRepo = vendorRepo;
    }

    // ========================================
    // 1. 목록 조회
    // ========================================
    @GetMapping
    public List<Map<String, Object>> list() {
        return templateRepo.findAllByOrderByComplex_nameAsc().stream()
                .map(t -> {
                    Map<String, Object> m = toMap(t);
                    int feeCount = feeRepo.findByTemplateId(t.getId()).size();
                    m.put("apt_fee_count", feeCount);
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ========================================
    // 2. 단건 조회 (평형별 금액 포함)
    // ========================================
    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable UUID id) {
        ComplexTemplate t = templateRepo.findById(id).orElseThrow();
        Map<String, Object> m = toMap(t);
        m.put("apt_fees", feeRepo.findByTemplateId(t.getId()));
        return m;
    }

    // ========================================
    // 3. 단지명으로 조회
    // ========================================
    @GetMapping("/by-name/{complexName}")
    public ResponseEntity<Map<String, Object>> getByName(@PathVariable String complexName) {
        return templateRepo.findByComplex_name(complexName)
                .map(t -> {
                    Map<String, Object> m = toMap(t);
                    m.put("apt_fees", feeRepo.findByTemplateId(t.getId()));
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================================
    // 4. 신규 등록 (template + apt_fees 한 번에)
    // ========================================
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> create(HttpServletRequest req,
                                                       @RequestBody Map<String, Object> body) {
        Vendor v = currentVendor(req);
        if (v == null && !isAdmin(req)) return ResponseEntity.status(401).build();

        ComplexTemplate t = new ComplexTemplate();
        applyTemplateFields(t, body);
        applyMeta(t, req, v, true);

        ComplexTemplate saved = templateRepo.save(t);

        // 평형별 금액 동시 저장
        replaceAptFees(saved.getId(), body.get("apt_fees"));

        log.info("[단지 템플릿 등록] complex={}, by={}", saved.getComplex_name(), saved.getCreatedBy());
        Map<String, Object> result = toMap(saved);
        result.put("apt_fees", feeRepo.findByTemplateId(saved.getId()));
        return ResponseEntity.ok(result);
    }

    // ========================================
    // 5. 수정 (template + apt_fees)
    // ========================================
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> update(HttpServletRequest req,
                                                      @PathVariable UUID id,
                                                      @RequestBody Map<String, Object> body) {
        Vendor v = currentVendor(req);
        if (v == null && !isAdmin(req)) return ResponseEntity.status(401).build();

        ComplexTemplate t = templateRepo.findById(id).orElseThrow();
        applyTemplateFields(t, body);
        applyMeta(t, req, v, false);

        ComplexTemplate saved = templateRepo.save(t);

        if (body.containsKey("apt_fees")) {
            replaceAptFees(saved.getId(), body.get("apt_fees"));
        }

        log.info("[단지 템플릿 수정] complex={}, by={}", saved.getComplex_name(), saved.getUpdatedBy());
        Map<String, Object> result = toMap(saved);
        result.put("apt_fees", feeRepo.findByTemplateId(saved.getId()));
        return ResponseEntity.ok(result);
    }

    // ========================================
    // 6. 삭제 (admin / bank_manager만)
    // ========================================
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> delete(HttpServletRequest req, @PathVariable UUID id) {
        if (!isAdmin(req)) {
            Vendor v = currentVendor(req);
            String role = resolveBankRole(v);
            if (!"bank_manager".equals(role)) {
                return ResponseEntity.status(403).body(Map.of("error", "삭제 권한이 없습니다 (관리자/팀장만)"));
            }
        }
        feeRepo.deleteByTemplateId(id);
        templateRepo.deleteById(id);
        log.info("[단지 템플릿 삭제] id={}", id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    // ========================================
    // 7. resolve — 세대 등록 시 자동 채움용
    //   GET /api/v4/complex-templates/by-name/{complexName}/resolve?aptType=84A
    // ========================================
    @GetMapping("/by-name/{complexName}/resolve")
    public ResponseEntity<Map<String, Object>> resolve(@PathVariable String complexName,
                                                        @RequestParam(required = false) String aptType) {
        return templateRepo.findByComplex_name(complexName)
                .map(t -> {
                    Map<String, Object> m = toMap(t);
                    m.put("apt_fees", feeRepo.findByTemplateId(t.getId()));

                    // 평형 매칭 시 해당 평형의 관리비 예치금 별도 필드로 노출
                    if (aptType != null && !aptType.isEmpty()) {
                        feeRepo.findByTemplateIdAndAptType(t.getId(), aptType).ifPresent(f -> {
                            m.put("matched_apt_type", f.getApt_type());
                            m.put("matched_mgmt_fee_amount", f.getMgmt_fee_amount());
                        });
                    }

                    // 프론트 prefill용 매핑 힌트 (ConsultationRequest 필드명 → 값)
                    Map<String, Object> prefill = buildPrefillMap(t,
                            m.get("matched_mgmt_fee_amount") instanceof Long
                                ? (Long) m.get("matched_mgmt_fee_amount") : null);
                    m.put("prefill", prefill);

                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================================
    // 8. apply-batch — 단지에 속한 진행중 세대들에 일괄 반영
    // ========================================
    @PostMapping("/{id}/apply-batch")
    @Transactional
    public Map<String, Object> applyBatch(HttpServletRequest req,
                                          @PathVariable UUID id,
                                          @RequestBody(required = false) Map<String, Object> body) {
        ComplexTemplate t = templateRepo.findById(id).orElseThrow();
        List<ComplexTemplateAptFee> fees = feeRepo.findByTemplateId(id);
        Map<String, Long> feeByApt = fees.stream().collect(
                Collectors.toMap(ComplexTemplateAptFee::getApt_type,
                                 ComplexTemplateAptFee::getMgmt_fee_amount,
                                 (a, b) -> a));

        // 옵션
        @SuppressWarnings("unchecked")
        List<String> fieldsFilter = body != null && body.get("fields") instanceof List
                ? (List<String>) body.get("fields") : Collections.emptyList();
        @SuppressWarnings("unchecked")
        List<String> excludeStatus = body != null && body.get("exclude_status") instanceof List
                ? (List<String>) body.get("exclude_status")
                : Arrays.asList("done", "cancel");

        // 같은 단지의 ConsultationRequest 전부 조회 (consultation 테이블에 complex_name 컬럼 있음)
        List<ConsultationRequest> matched = consultationRepo.findAll().stream()
                .filter(c -> t.getComplex_name() != null && t.getComplex_name().equals(c.getComplex_name()))
                .filter(c -> !excludeStatus.contains(c.getLoan_status()))
                .collect(Collectors.toList());

        int updatedCount = 0;
        for (ConsultationRequest c : matched) {
            boolean changed = false;

            // 인지대
            if (shouldApply(fieldsFilter, "stamp_duty") && t.getStamp_duty() != null) {
                c.setSettle_stamp_duty(t.getStamp_duty());
                changed = true;
            }
            // 관리비 예치금 계좌
            if (shouldApply(fieldsFilter, "mgmt_fee_account") && t.getMgmt_fee_account() != null) {
                c.setSettle_mgmt_account(t.getMgmt_fee_account());
                changed = true;
            }
            // 평형별 관리비 예치금 금액 (apt_type 매칭 시)
            if (shouldApply(fieldsFilter, "mgmt_fee_amount") && c.getApt_type() != null) {
                Long amount = feeByApt.get(c.getApt_type());
                if (amount != null) {
                    c.setSettle_mgmt_fee(amount);
                    changed = true;
                }
            }
            // 일반 옵션대금 계좌 (분양 division 일반인 경우)
            if (shouldApply(fieldsFilter, "general_option_account")
                    && t.getGeneral_option_account() != null
                    && (c.getDivision() == null || "일반".equals(c.getDivision()))) {
                c.setOption_account(t.getGeneral_option_account());
                changed = true;
            }
            // 조합 옵션대금 계좌 (조합인 경우)
            if (shouldApply(fieldsFilter, "union_option_account")
                    && t.getUnion_option_account() != null
                    && "조합".equals(c.getDivision())) {
                c.setOption_account(t.getUnion_option_account());
                changed = true;
            }

            if (changed) {
                consultationRepo.save(c);
                updatedCount++;
            }
        }

        log.info("[단지 일괄 적용] complex={}, matched={}, updated={}",
                t.getComplex_name(), matched.size(), updatedCount);

        Map<String, Object> result = new HashMap<>();
        result.put("complex_name", t.getComplex_name());
        result.put("matched_count", matched.size());
        result.put("updated_count", updatedCount);
        result.put("excluded_status", excludeStatus);
        return result;
    }

    // ========================================
    // === 헬퍼 ===
    // ========================================

    private Vendor currentVendor(HttpServletRequest req) {
        String loginId = (String) req.getAttribute("auth.loginId");
        if (loginId == null) return null;
        return vendorRepo.findByLoginId(loginId).orElse(null);
    }

    private boolean isAdmin(HttpServletRequest req) {
        Object role = req.getAttribute("auth.role");
        return "admin".equals(role);
    }

    private String resolveBankRole(Vendor v) {
        if (v == null) return null;
        if (v.getRole() != null && !v.getRole().isEmpty()) return v.getRole();
        return "은행상담사".equals(v.getVendorType()) ? "bank_consultant" : "bank_manager";
    }

    private void applyMeta(ComplexTemplate t, HttpServletRequest req, Vendor v, boolean isCreate) {
        String loginId = v != null ? v.getLoginId() : (String) req.getAttribute("auth.loginId");
        String role = isAdmin(req) ? "admin" : resolveBankRole(v);
        String bank = v != null ? v.getVendorName() : null;

        if (isCreate) {
            t.setCreatedBy(loginId);
        }
        t.setUpdatedBy(loginId);
        t.setUpdatedByRole(role);
        t.setUpdatedByBank(bank);
    }

    @Transactional
    protected void replaceAptFees(UUID templateId, Object aptFeesObj) {
        if (!(aptFeesObj instanceof List)) return;
        feeRepo.deleteByTemplateId(templateId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) aptFeesObj;
        int order = 0;
        for (Map<String, Object> row : rows) {
            String aptType = asString(row.get("apt_type"));
            Long amount = asLong(row.get("mgmt_fee_amount"));
            if (aptType == null || amount == null) continue;

            ComplexTemplateAptFee f = new ComplexTemplateAptFee();
            f.setTemplate_id(templateId);
            f.setApt_type(aptType);
            f.setMgmt_fee_amount(amount);
            Integer displayOrder = asInt(row.get("display_order"));
            f.setDisplay_order(displayOrder != null ? displayOrder : order);
            feeRepo.save(f);
            order++;
        }
    }

    private boolean shouldApply(List<String> fieldsFilter, String fieldName) {
        return fieldsFilter == null || fieldsFilter.isEmpty() || fieldsFilter.contains(fieldName);
    }

    private Map<String, Object> buildPrefillMap(ComplexTemplate t, Long matchedFee) {
        Map<String, Object> p = new HashMap<>();
        if (t.getMgmt_fee_account() != null) p.put("settle_mgmt_account", t.getMgmt_fee_account());
        if (t.getStamp_duty() != null)       p.put("settle_stamp_duty",   t.getStamp_duty());
        if (matchedFee != null)              p.put("settle_mgmt_fee",     matchedFee);
        if (t.getGeneral_option_account() != null)
            p.put("option_account_general", t.getGeneral_option_account());
        if (t.getUnion_option_account() != null)
            p.put("option_account_union",   t.getUnion_option_account());
        return p;
    }

    private Map<String, Object> toMap(ComplexTemplate t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("complex_name", t.getComplex_name());
        m.put("mgmt_fee_bank", t.getMgmt_fee_bank());
        m.put("mgmt_fee_account", t.getMgmt_fee_account());
        m.put("mgmt_fee_holder", t.getMgmt_fee_holder());
        m.put("mgmt_fee_timing", t.getMgmt_fee_timing());
        m.put("mgmt_office_location", t.getMgmt_office_location());
        m.put("mgmt_office_phone", t.getMgmt_office_phone());
        m.put("mgmt_office_fax", t.getMgmt_office_fax());
        m.put("mgmt_office_open_date", t.getMgmt_office_open_date());
        m.put("payment_methods", t.getPayment_methods());
        m.put("payment_notes", t.getPayment_notes());
        m.put("general_balance_note", t.getGeneral_balance_note());
        m.put("general_balance_holder", t.getGeneral_balance_holder());
        m.put("general_option_bank", t.getGeneral_option_bank());
        m.put("general_option_account", t.getGeneral_option_account());
        m.put("general_option_holder", t.getGeneral_option_holder());
        m.put("union_balance_note", t.getUnion_balance_note());
        m.put("union_balance_holder", t.getUnion_balance_holder());
        m.put("union_option_bank", t.getUnion_option_bank());
        m.put("union_option_account", t.getUnion_option_account());
        m.put("union_option_holder", t.getUnion_option_holder());
        m.put("middle_loan_note", t.getMiddle_loan_note());
        m.put("sale_price_inquiry_url", t.getSale_price_inquiry_url());
        m.put("stamp_duty", t.getStamp_duty());
        m.put("guarantee_fee_rate", t.getGuarantee_fee_rate());
        m.put("middle_loan_rate", t.getMiddle_loan_rate());
        m.put("created_at", t.getCreatedAt());
        m.put("updated_at", t.getUpdatedAt());
        m.put("created_by", t.getCreatedBy());
        m.put("updated_by", t.getUpdatedBy());
        m.put("updated_by_role", t.getUpdatedByRole());
        m.put("updated_by_bank", t.getUpdatedByBank());
        return m;
    }

    private void applyTemplateFields(ComplexTemplate t, Map<String, Object> body) {
        if (body.containsKey("complex_name"))         t.setComplex_name(asString(body.get("complex_name")));
        if (body.containsKey("mgmt_fee_bank"))        t.setMgmt_fee_bank(asString(body.get("mgmt_fee_bank")));
        if (body.containsKey("mgmt_fee_account"))     t.setMgmt_fee_account(asString(body.get("mgmt_fee_account")));
        if (body.containsKey("mgmt_fee_holder"))      t.setMgmt_fee_holder(asString(body.get("mgmt_fee_holder")));
        if (body.containsKey("mgmt_fee_timing"))      t.setMgmt_fee_timing(asString(body.get("mgmt_fee_timing")));
        if (body.containsKey("mgmt_office_location")) t.setMgmt_office_location(asString(body.get("mgmt_office_location")));
        if (body.containsKey("mgmt_office_phone"))    t.setMgmt_office_phone(asString(body.get("mgmt_office_phone")));
        if (body.containsKey("mgmt_office_fax"))      t.setMgmt_office_fax(asString(body.get("mgmt_office_fax")));
        if (body.containsKey("mgmt_office_open_date"))t.setMgmt_office_open_date(asString(body.get("mgmt_office_open_date")));
        if (body.containsKey("payment_methods"))      t.setPayment_methods(asString(body.get("payment_methods")));
        if (body.containsKey("payment_notes"))        t.setPayment_notes(asString(body.get("payment_notes")));
        if (body.containsKey("general_balance_note")) t.setGeneral_balance_note(asString(body.get("general_balance_note")));
        if (body.containsKey("general_balance_holder")) t.setGeneral_balance_holder(asString(body.get("general_balance_holder")));
        if (body.containsKey("general_option_bank"))  t.setGeneral_option_bank(asString(body.get("general_option_bank")));
        if (body.containsKey("general_option_account")) t.setGeneral_option_account(asString(body.get("general_option_account")));
        if (body.containsKey("general_option_holder"))t.setGeneral_option_holder(asString(body.get("general_option_holder")));
        if (body.containsKey("union_balance_note"))   t.setUnion_balance_note(asString(body.get("union_balance_note")));
        if (body.containsKey("union_balance_holder")) t.setUnion_balance_holder(asString(body.get("union_balance_holder")));
        if (body.containsKey("union_option_bank"))    t.setUnion_option_bank(asString(body.get("union_option_bank")));
        if (body.containsKey("union_option_account")) t.setUnion_option_account(asString(body.get("union_option_account")));
        if (body.containsKey("union_option_holder"))  t.setUnion_option_holder(asString(body.get("union_option_holder")));
        if (body.containsKey("middle_loan_note"))     t.setMiddle_loan_note(asString(body.get("middle_loan_note")));
        if (body.containsKey("sale_price_inquiry_url"))t.setSale_price_inquiry_url(asString(body.get("sale_price_inquiry_url")));
        if (body.containsKey("stamp_duty"))           t.setStamp_duty(asLong(body.get("stamp_duty")));
        if (body.containsKey("guarantee_fee_rate"))   t.setGuarantee_fee_rate(asBigDecimal(body.get("guarantee_fee_rate")));
        if (body.containsKey("middle_loan_rate"))     t.setMiddle_loan_rate(asBigDecimal(body.get("middle_loan_rate")));
    }

    private static String asString(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        String s = o.toString().replaceAll("[^0-9-]", "");
        if (s.isEmpty()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }

    private static Integer asInt(Object o) {
        Long l = asLong(o);
        return l == null ? null : l.intValue();
    }

    private static java.math.BigDecimal asBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return new java.math.BigDecimal(o.toString());
        try { return new java.math.BigDecimal(o.toString().trim()); } catch (Exception e) { return null; }
    }
}
