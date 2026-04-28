package com.ipjuon.backend.b2c;

import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.consultation.ConsultationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /** 전화번호 정규화 — 010-1234-5678 / 01012345678 / 010 1234 5678 모두 동일 처리 */
    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }
}
