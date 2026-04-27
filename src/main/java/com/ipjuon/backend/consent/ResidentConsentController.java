package com.ipjuon.backend.consent;

import com.ipjuon.backend.bankprofile.BankProfile;
import com.ipjuon.backend.bankprofile.BankProfileRepository;
import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.consultation.ConsultationRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 입주민 동의서 제출 endpoint.
 * 동의 시점에 마감 안 된 모든 은행에 ConsultationRequest(loan_status=apply) 를 자동 생성.
 * → 각 은행 팀장/상담사 인박스에 신규 건으로 자동 노출됨.
 */
@RestController
@RequestMapping("/api/b2c/consents")
@CrossOrigin(origins = "*")
public class ResidentConsentController {

    private static final Logger log = LoggerFactory.getLogger(ResidentConsentController.class);

    private final ResidentConsentRepository consentRepo;
    private final BankProfileRepository profileRepo;
    private final ConsultationRepository consultationRepo;

    public ResidentConsentController(ResidentConsentRepository consentRepo,
                                     BankProfileRepository profileRepo,
                                     ConsultationRepository consultationRepo) {
        this.consentRepo = consentRepo;
        this.profileRepo = profileRepo;
        this.consultationRepo = consultationRepo;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> submit(HttpServletRequest req,
                                                       @RequestBody Map<String, Object> body) {
        String name = asString(body.get("resident_name"));
        String phone = asString(body.get("resident_phone"));
        if (phone == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "연락처 필수"));
        }
        // 이름이 비어 있으면 "(미상)" — 입주민 앱에서 회원정보 미입력 상태로 동의만 한 경우.
        if (name == null) name = "(미상)";

        // 1) 동의서 기록 저장
        ResidentConsent c = new ResidentConsent();
        c.setResident_name(name);
        c.setResident_phone(phone);
        c.setResident_complex(asString(body.get("resident_complex")));
        c.setDong(asString(body.get("dong")));
        c.setHo(asString(body.get("ho")));
        c.setApt_type(asString(body.get("apt_type")));
        c.setTerms_version(asString(body.get("terms_version")) != null ? asString(body.get("terms_version")) : "v1.0");
        c.setUserAgent(req.getHeader("User-Agent"));
        Object inviteIdObj = body.get("invite_id");
        if (inviteIdObj != null) {
            try { c.setInvite_id(UUID.fromString(inviteIdObj.toString())); } catch (Exception ignore) {}
        }

        // 2) 마감 안 된 은행만 분배 대상
        List<BankProfile> openBanks = profileRepo.findAllOrdered().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIs_closed()))
                .collect(Collectors.toList());

        // 3) 각 은행에 ConsultationRequest 자동 생성 (loan_status=apply)
        List<UUID> createdConsultationIds = new ArrayList<>();
        List<String> deliveredBanks = new ArrayList<>();
        // 동의서 자동 분배는 assignee_vendor_id / manager 표시명 모두 비워둠 →
        // 팀장이 인박스에서 직접 상담사 배정. 팀장 vendor 조회 불필요
        // (vendor_name 으로 팀장1+상담사3=4명 → NonUniqueResultException 발생함).
        for (BankProfile bp : openBanks) {
            ConsultationRequest r = new ConsultationRequest();
            r.setResident_name(name);
            r.setResident_phone(phone);
            r.setVendor_name(bp.getBank_name());
            r.setVendor_type("bank");
            r.setComplex_name(c.getResident_complex());
            r.setDong(c.getDong());
            r.setHo(c.getHo());
            r.setApt_type(c.getApt_type());
            r.setLoan_status("apply");
            r.setStage_changed_at(OffsetDateTime.now());
            r.setReceive_date(LocalDate.now());
            r.setStatus("대기중");
            r.setMemo("입주민 앱 동의서 제출 — 자동 분배");
            r.setSpecial_notes("CONSENT");

            ConsultationRequest saved = consultationRepo.save(r);
            createdConsultationIds.add(saved.getId());
            deliveredBanks.add(bp.getBank_name());
        }

        c.setDistributed_count(createdConsultationIds.size());
        c.setDistributed_banks(String.join(",", deliveredBanks));
        ResidentConsent savedConsent = consentRepo.save(c);

        log.info("[입주민 동의서] consent_id={}, resident={}({}), 분배={}/{}건",
                savedConsent.getId(), name, phone, createdConsultationIds.size(),
                profileRepo.count());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consent_id", savedConsent.getId());
        result.put("distributed_count", createdConsultationIds.size());
        result.put("distributed_banks", deliveredBanks);
        result.put("message", deliveredBanks.size() + "개 은행에서 곧 연락드립니다");
        return ResponseEntity.ok(result);
    }

    /** 동의서 조회 (입주민 앱이 새로고침 시 동의 여부 확인용) */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable UUID id) {
        return consentRepo.findById(id)
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("resident_name", c.getResident_name());
                    m.put("consented_at", c.getConsentedAt());
                    m.put("distributed_count", c.getDistributed_count());
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private static String asString(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
