package com.ipjuon.backend.b2c;

import com.ipjuon.backend.complex.ComplexTemplate;
import com.ipjuon.backend.complex.ComplexTemplateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 입주민 앱(B2C) — 단지 정보 조회.
 * 단지 안내글, 납부방법, 관리사무소, 분양/옵션 계좌 등 입주민에게 도움 되는 정보 노출.
 */
@RestController
@RequestMapping("/api/b2c/complex")
@CrossOrigin(origins = "*")
public class B2cComplexController {

    private final ComplexTemplateRepository repo;

    public B2cComplexController(ComplexTemplateRepository repo) {
        this.repo = repo;
    }

    /** 단지 목록 (이름만) */
    @GetMapping("/list")
    public List<Map<String, Object>> list() {
        return repo.findAllByOrderByComplex_nameAsc().stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("complex_name", t.getComplex_name());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** 단지 상세 정보 — 입주민 앱에서 표시 */
    @GetMapping
    public ResponseEntity<?> detail(@RequestParam(required = false) String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name 필수"));
        }
        return repo.findByComplex_name(name)
                .map(t -> ResponseEntity.ok((Object) toDto(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    private static Map<String, Object> toDto(ComplexTemplate t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("complex_name", t.getComplex_name());

        // 1. 관리비 예치금
        Map<String, Object> mgmt = new LinkedHashMap<>();
        mgmt.put("bank", t.getMgmt_fee_bank());
        mgmt.put("account", t.getMgmt_fee_account());
        mgmt.put("holder", t.getMgmt_fee_holder());
        mgmt.put("timing", t.getMgmt_fee_timing());
        m.put("mgmt_fee", mgmt);

        // 2. 관리사무소
        Map<String, Object> office = new LinkedHashMap<>();
        office.put("location", t.getMgmt_office_location());
        office.put("phone", t.getMgmt_office_phone());
        office.put("fax", t.getMgmt_office_fax());
        office.put("open_date", t.getMgmt_office_open_date());
        m.put("mgmt_office", office);

        // 3. 납부방법
        m.put("payment_methods", t.getPayment_methods());
        m.put("payment_notes", t.getPayment_notes());

        // 4-7. 분양/옵션 대금
        Map<String, Object> general = new LinkedHashMap<>();
        general.put("balance_note", t.getGeneral_balance_note());
        general.put("balance_holder", t.getGeneral_balance_holder());
        general.put("option_bank", t.getGeneral_option_bank());
        general.put("option_account", t.getGeneral_option_account());
        general.put("option_holder", t.getGeneral_option_holder());
        m.put("general", general);

        Map<String, Object> union = new LinkedHashMap<>();
        union.put("balance_note", t.getUnion_balance_note());
        union.put("balance_holder", t.getUnion_balance_holder());
        union.put("option_bank", t.getUnion_option_bank());
        union.put("option_account", t.getUnion_option_account());
        union.put("option_holder", t.getUnion_option_holder());
        m.put("union", union);

        // 8-9. 중도금대출 / 분양대금 조회
        m.put("middle_loan_note", t.getMiddle_loan_note());
        m.put("sale_price_inquiry_url", t.getSale_price_inquiry_url());

        // 10. 정책
        m.put("stamp_duty", t.getStamp_duty());
        return m;
    }
}
