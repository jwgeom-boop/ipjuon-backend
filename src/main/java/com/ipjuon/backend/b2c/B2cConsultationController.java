package com.ipjuon.backend.b2c;

import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.consultation.ConsultationRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 입주민 앱(B2C) — 본인 상담건 조회 API.
 * 인증: 전화번호 기반 (MVP). 모든 응답은 B2cConsultationDto 로 마스킹된 필드만 노출.
 */
@RestController
@RequestMapping("/api/b2c/consultations")
@CrossOrigin(origins = "*")
public class B2cConsultationController {

    private static final Logger log = LoggerFactory.getLogger(B2cConsultationController.class);

    private final ConsultationRepository repo;

    public B2cConsultationController(ConsultationRepository repo) {
        this.repo = repo;
    }

    /** 본인 상담건 목록 — 진행단계 우선 정렬 */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@RequestParam String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null) {
            return ResponseEntity.badRequest().body(List.of());
        }

        List<ConsultationRequest> rows = repo.findByResidentPhone(normalized);

        // 진행단계 우선 정렬: result(가심사) > consulting > apply > executing > done > cancel
        // (가장 액션이 필요한 단계가 위로)
        Map<String, Integer> stageOrder = Map.of(
                "result", 1,
                "consulting", 2,
                "apply", 3,
                "executing", 4,
                "done", 5,
                "cancel", 6
        );

        List<Map<String, Object>> result = rows.stream()
                .map(B2cConsultationDto::toListItem)
                .sorted(Comparator.comparingInt(m ->
                        stageOrder.getOrDefault((String) m.get("stage"), 99)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /** 본인 상담건 상세 (소유자 검증: phone 일치) */
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable UUID id, @RequestParam String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone 필수"));
        }

        return repo.findById(id)
                .map(r -> {
                    if (!normalized.equals(normalizePhone(r.getResident_phone()))) {
                        log.warn("[B2C] 소유자 불일치 시도 id={} requested_phone={}", id, normalized);
                        return ResponseEntity.status(403).body((Object) Map.of("error", "본인 상담건이 아닙니다"));
                    }
                    return ResponseEntity.ok((Object) B2cConsultationDto.toDetail(r));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 가심사 결과 수용 — 입주민이 승인 금액·금리에 동의.
     * loan_status 변경은 안 함 (상담사가 다음 단계로 이동시킴), customer_accepted_at 만 기록.
     */
    @PostMapping("/{id}/accept")
    @Transactional
    public ResponseEntity<?> accept(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String normalized = normalizePhone(body.get("phone"));
        if (normalized == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone 필수"));
        }
        return repo.findById(id).<ResponseEntity<?>>map(r -> {
            if (!normalized.equals(normalizePhone(r.getResident_phone()))) {
                return ResponseEntity.status(403).body(Map.of("error", "본인 상담건이 아닙니다"));
            }
            if (!"result".equals(r.getLoan_status())) {
                return ResponseEntity.status(409).body(Map.of("error", "가심사 결과 단계에서만 수용 가능합니다"));
            }
            r.setCustomer_accepted_at(OffsetDateTime.now());
            repo.save(r);
            log.info("[B2C] 가심사 수용 id={} phone={}", id, normalized);
            return ResponseEntity.ok(B2cConsultationDto.toDetail(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * 취소 요청 — 입주민 측 취소 의사. 즉시 cancel 이 아니라 cancel_requested 로 두어
     * 상담사가 사이트에서 확인 후 최종 cancel 처리.
     */
    @PostMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String normalized = normalizePhone(body.get("phone"));
        if (normalized == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone 필수"));
        }
        String reason = body.getOrDefault("reason", "고객 요청");
        return repo.findById(id).<ResponseEntity<?>>map(r -> {
            if (!normalized.equals(normalizePhone(r.getResident_phone()))) {
                return ResponseEntity.status(403).body(Map.of("error", "본인 상담건이 아닙니다"));
            }
            if ("done".equals(r.getLoan_status())) {
                return ResponseEntity.status(409).body(Map.of("error", "이미 실행 완료된 건은 취소할 수 없습니다"));
            }
            if ("cancel".equals(r.getLoan_status()) || "cancel_requested".equals(r.getLoan_status())) {
                return ResponseEntity.status(409).body(Map.of("error", "이미 취소 요청된 건입니다"));
            }
            r.setLoan_status("cancel_requested");
            r.setCanceled_reason(reason);
            r.setStage_changed_at(OffsetDateTime.now());
            repo.save(r);
            log.info("[B2C] 취소 요청 id={} phone={} reason={}", id, normalized, reason);
            return ResponseEntity.ok(B2cConsultationDto.toDetail(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** 전화번호 정규화 — 010-1234-5678 / 01012345678 / 010 1234 5678 모두 동일 처리 */
    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }
}
