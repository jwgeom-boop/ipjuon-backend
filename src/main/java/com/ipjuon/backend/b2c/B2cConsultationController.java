package com.ipjuon.backend.b2c;

import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.consultation.ConsultationRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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

    /** 입주민 액션 발생 시 마지막 액션 시각·종류 기록 (상담사 알림함용) */
    private static void markResidentAction(ConsultationRequest r, String type) {
        r.setResident_last_action_at(OffsetDateTime.now());
        r.setResident_last_action_type(type);
    }

    /** 본인 상담건 목록 — 진행단계 우선 정렬 */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@RequestParam String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null) {
            return ResponseEntity.badRequest().body(List.of());
        }

        List<ConsultationRequest> rows = repo.findByResidentPhone(normalized);

        // 정렬용: 진행이 가장 많이 된 단계 = 가장 중요 → 최상단
        // executing(자서·실행 진행) > result(가심사 결과) > consulting(상담·심사) > apply(신청)
        // done/cancel 은 별도 섹션이라 사실상 마지막
        Map<String, Integer> displayOrder = Map.of(
                "executing", 1,
                "result", 2,
                "consulting", 3,
                "apply", 4,
                "done", 5,
                "cancel", 6
        );

        // 중복 제거용: 같은 은행이면 가장 진행된 단계 1건만 (done > executing > result > consulting > apply > cancel)
        // 동의서 제출이 여러 번 일어나거나 시드 데이터로 같은 은행 다건 존재 시 정리.
        Map<String, Integer> advancementOrder = Map.of(
                "done", 6,
                "executing", 5,
                "result", 4,
                "consulting", 3,
                "apply", 2,
                "cancel", 1
        );

        Map<String, ConsultationRequest> dedupedByBank = new LinkedHashMap<>();
        for (ConsultationRequest r : rows) {
            String bank = r.getVendor_name();
            if (bank == null) continue;
            ConsultationRequest existing = dedupedByBank.get(bank);
            if (existing == null) {
                dedupedByBank.put(bank, r);
                continue;
            }
            int newRank = advancementOrder.getOrDefault(B2cConsultationDto.mapStage(r.getLoan_status()), 0);
            int oldRank = advancementOrder.getOrDefault(B2cConsultationDto.mapStage(existing.getLoan_status()), 0);
            if (newRank > oldRank) {
                dedupedByBank.put(bank, r);
            } else if (newRank == oldRank) {
                // 같은 단계면 더 최근 stage_changed_at / created_at 우선
                OffsetDateTime newTs = r.getStage_changed_at() != null ? r.getStage_changed_at() : r.getCreatedAt();
                OffsetDateTime oldTs = existing.getStage_changed_at() != null ? existing.getStage_changed_at() : existing.getCreatedAt();
                if (newTs != null && oldTs != null && newTs.isAfter(oldTs)) {
                    dedupedByBank.put(bank, r);
                }
            }
        }

        List<Map<String, Object>> result = dedupedByBank.values().stream()
                .map(B2cConsultationDto::toListItem)
                .sorted(Comparator.comparingInt(m ->
                        displayOrder.getOrDefault((String) m.get("stage"), 99)))
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
            markResidentAction(r, "accept");
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
            markResidentAction(r, "cancel");
            repo.save(r);
            log.info("[B2C] 취소 요청 id={} phone={} reason={}", id, normalized, reason);
            return ResponseEntity.ok(B2cConsultationDto.toDetail(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * 중도금 이자 보고 — 입주민이 실행일 당일 은행에서 확인 후 앱으로 전달.
     * 조건:
     *   - executing 단계
     *   - 실행일 D-1 ~ D+0 (실행일 미설정 시 거부)
     *   - 상담사가 settle_middle_interest 확정 전까지만 수정 가능
     */
    @PostMapping("/{id}/report-middle-interest")
    @Transactional
    public ResponseEntity<?> reportMiddleInterest(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String normalized = normalizePhone((String) body.get("phone"));
        if (normalized == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone 필수"));
        }
        Object amountObj = body.get("amount");
        Long amount;
        try {
            amount = amountObj == null ? null : Long.parseLong(amountObj.toString().replaceAll("\\D", ""));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "amount 형식 오류"));
        }
        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "유효한 금액이 필요합니다"));
        }

        return repo.findById(id).<ResponseEntity<?>>map(r -> {
            if (!normalized.equals(normalizePhone(r.getResident_phone()))) {
                return ResponseEntity.status(403).body(Map.of("error", "본인 상담건이 아닙니다"));
            }
            if (!"executing".equals(r.getLoan_status())) {
                return ResponseEntity.status(409).body(Map.of("error", "자서·실행 단계에서만 보고 가능합니다"));
            }
            if (r.getExecution_date() == null) {
                return ResponseEntity.status(409).body(Map.of("error", "실행일이 아직 정해지지 않았습니다"));
            }
            // 실행일 D-1 ~ D+0 (시간대 무관 단순 비교)
            long daysToExecution = ChronoUnit.DAYS.between(LocalDate.now(), r.getExecution_date());
            if (daysToExecution > 1 || daysToExecution < 0) {
                return ResponseEntity.status(409).body(Map.of("error",
                        "실행일 전날부터 당일까지만 보고 가능합니다 (D" + (daysToExecution >= 0 ? "-" : "+") + Math.abs(daysToExecution) + ")"));
            }
            // 이미 상담사가 확정한 경우 수정 거부
            if (r.getSettle_middle_interest() != null && r.getSettle_middle_interest() > 0) {
                return ResponseEntity.status(409).body(Map.of("error", "상담사가 이미 확정한 건은 수정할 수 없습니다"));
            }

            r.setReported_middle_interest(amount);
            r.setReported_middle_interest_at(OffsetDateTime.now());
            markResidentAction(r, "report_middle_interest");
            repo.save(r);
            log.info("[B2C] 중도금이자 보고 id={} amount={} phone={}", id, amount, normalized);

            // 상담사 통보 채널은 다음 차수 — 백엔드 저장만 하고 사이트 인박스는 후속
            return ResponseEntity.ok(B2cConsultationDto.toDetail(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * [v2] 자서 캘린더 — 입주민이 (date, time, location) 선택.
     */
    @PostMapping("/{id}/select-signing-calendar")
    @Transactional
    public ResponseEntity<?> selectSigningCalendar(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String normalized = normalizePhone((String) body.get("phone"));
        if (normalized == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone 필수"));
        }
        String dateStr = (String) body.get("date");
        String timeStr = (String) body.get("time");
        String location = (String) body.get("location");
        if (dateStr == null || timeStr == null || location == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "date, time, location 필수"));
        }
        return repo.findById(id).<ResponseEntity<?>>map(r -> {
            if (!normalized.equals(normalizePhone(r.getResident_phone()))) {
                return ResponseEntity.status(403).body(Map.of("error", "본인 상담건이 아닙니다"));
            }
            if (r.getSigning_confirmed_at() != null) {
                return ResponseEntity.status(409).body(Map.of("error", "이미 확정된 일정입니다"));
            }
            try {
                r.setSigning_selected_date(LocalDate.parse(dateStr));
                r.setSigning_selected_time(timeStr);
                r.setSigning_selected_location_str(location);
                r.setSigning_selected_at(OffsetDateTime.now());
                markResidentAction(r, "signing_select");
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "날짜 형식 오류"));
            }
            repo.save(r);
            log.info("[B2C] 자서 캘린더 선택 id={} date={} time={} location={}", id, dateStr, timeStr, location);
            return ResponseEntity.ok(B2cConsultationDto.toDetail(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * [Legacy] 자서 일정 슬롯 선택 — 구 모델 호환 유지.
     */
    @PostMapping("/{id}/select-signing-slot")
    @Transactional
    public ResponseEntity<?> selectSigningSlot(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String normalized = normalizePhone((String) body.get("phone"));
        if (normalized == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone 필수"));
        }
        Object idxObj = body.get("slot_index");
        Integer slotIndex;
        try {
            slotIndex = idxObj == null ? null : Integer.parseInt(idxObj.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "slot_index 형식 오류"));
        }
        if (slotIndex == null || slotIndex < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "유효한 slot_index 필요"));
        }

        return repo.findById(id).<ResponseEntity<?>>map(r -> {
            if (!normalized.equals(normalizePhone(r.getResident_phone()))) {
                return ResponseEntity.status(403).body(Map.of("error", "본인 상담건이 아닙니다"));
            }
            if (r.getSigning_offered_slots() == null || r.getSigning_offered_slots().isBlank()) {
                return ResponseEntity.status(409).body(Map.of("error", "아직 일정이 제시되지 않았습니다"));
            }
            if (r.getSigning_confirmed_at() != null) {
                return ResponseEntity.status(409).body(Map.of("error", "이미 확정된 일정입니다"));
            }
            r.setSigning_selected_slot_index(slotIndex);
            r.setSigning_selected_at(OffsetDateTime.now());
            repo.save(r);
            log.info("[B2C] 자서 슬롯 선택 id={} idx={}", id, slotIndex);
            return ResponseEntity.ok(B2cConsultationDto.toDetail(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * 입주민 준비서류 체크리스트 업데이트 — 클라이언트가 체크된 doc id 전체 배열 전송.
     */
    @PostMapping("/{id}/document-checks")
    @Transactional
    public ResponseEntity<?> updateDocChecks(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String normalized = normalizePhone((String) body.get("phone"));
        if (normalized == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone 필수"));
        }
        Object checksObj = body.get("checks");
        if (!(checksObj instanceof List<?>)) {
            return ResponseEntity.badRequest().body(Map.of("error", "checks 배열 필수"));
        }
        List<?> checks = (List<?>) checksObj;
        String csv = checks.stream()
                .filter(o -> o != null && !o.toString().isBlank())
                .map(Object::toString)
                .collect(Collectors.joining(","));

        return repo.findById(id).<ResponseEntity<?>>map(r -> {
            if (!normalized.equals(normalizePhone(r.getResident_phone()))) {
                return ResponseEntity.status(403).body(Map.of("error", "본인 상담건이 아닙니다"));
            }
            r.setResident_doc_checks(csv);
            r.setResident_doc_checks_at(OffsetDateTime.now());
            markResidentAction(r, "doc_checks");
            repo.save(r);
            log.info("[B2C] 준비서류 체크 업데이트 id={} count={}", id, checks.size());
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
