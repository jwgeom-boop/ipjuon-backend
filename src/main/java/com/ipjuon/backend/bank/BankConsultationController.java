package com.ipjuon.backend.bank;

import com.ipjuon.backend.b2c.MessagesHelper;
import com.ipjuon.backend.consultation.ConsultationRequest;
import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.vendor.Vendor;
import com.ipjuon.backend.vendor.VendorRepository;
import com.ipjuon.backend.webpush.WebPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bank")
@CrossOrigin(origins = "*")
public class BankConsultationController {

    private static final Logger log = LoggerFactory.getLogger(BankConsultationController.class);

    private final ConsultationRepository repository;
    private final BankExcelExportService excelService;
    private final VendorRepository vendorRepository;
    private final WebPushService webPushService;

    @Value("${complex.name}")
    private String complexName;

    @Value("${complex.full-name}")
    private String complexFullName;

    @Value("${complex.approval-no}")
    private String approvalNo;

    @Value("${complex.total-limit}")
    private long totalLimit;

    public BankConsultationController(ConsultationRepository repository,
                                       BankExcelExportService excelService,
                                       VendorRepository vendorRepository,
                                       WebPushService webPushService) {
        this.repository = repository;
        this.excelService = excelService;
        this.vendorRepository = vendorRepository;
        this.webPushService = webPushService;
    }

    /** loan_status 전환 시 입주민 앱으로 Web Push 발송 */
    private void sendStageChangePush(ConsultationRequest c, String oldStatus, String newStatus) {
        if (c.getResident_phone() == null) return;
        String bank = c.getVendor_name() != null ? c.getVendor_name() : "은행";
        String url = "/my/consultations/" + c.getId();

        String title;
        String body;
        switch (String.valueOf(newStatus)) {
            case "result":
                title = "🎉 " + bank + " 가심사 결과 도착";
                body = c.getApproved_amount() != null
                        ? "승인 " + formatEok(c.getApproved_amount())
                            + (c.getApproved_rate() != null ? " · " + c.getApproved_rate() : "")
                        : "탭하여 결과 확인";
                break;
            case "executing":
                title = "✍️ " + bank + " 자서·실행 진행";
                body = c.getSigning_date() != null
                        ? "자서일 " + c.getSigning_date()
                        : "일정 협의 시작";
                break;
            case "done":
                title = "✅ " + bank + " 대출 실행 완료";
                body = c.getExecution_date() != null
                        ? c.getExecution_date() + " 실행"
                        : "실행이 완료되었습니다";
                break;
            case "cancel":
                title = "❌ " + bank + " 상담 취소";
                body = c.getCanceled_reason() != null ? c.getCanceled_reason() : "상담이 취소되었습니다";
                break;
            case "consulting":
            case "reviewing":
                if ("apply".equals(oldStatus)) {
                    title = "💬 " + bank + " 상담·심사 시작";
                    body = c.getManager() != null ? "담당: " + c.getManager() : "담당자가 검토 중입니다";
                } else return; // consulting↔reviewing 같은 그룹 내 전환은 무시
                break;
            default:
                return;
        }

        webPushService.sendToPhone(c.getResident_phone(), title, body, url);
    }

    private static String formatEok(long won) {
        long eok = won / 100_000_000L;
        long man = (won % 100_000_000L) / 10_000L;
        if (eok > 0 && man > 0) return eok + "억 " + String.format("%,d", man) + "만원";
        if (eok > 0) return eok + "억원";
        return String.format("%,d", man) + "만원";
    }

    // 은행 접수 리스트 조회
    @GetMapping("/consultations")
    public List<ConsultationRequest> getAll(
            @RequestParam(required = false) String bank_name,
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String ownership,
            @RequestParam(required = false) String manager,
            @RequestParam(required = false) String loan_status
    ) {
        // DB에서 은행 타입만 조회 (전체 로드 방지)
        List<ConsultationRequest> all = (bank_name != null && !bank_name.isEmpty())
                ? repository.findBankConsultationsByVendorName(bank_name)
                : repository.findAllBankConsultations();

        // 추가 필터 (division/ownership/manager/loan_status는 DB 인덱스 미적용 → 메모리 필터 유지)
        if (division != null && !division.isEmpty())
            all = all.stream().filter(r -> division.equals(r.getDivision())).collect(Collectors.toList());
        if (ownership != null && !ownership.isEmpty())
            all = all.stream().filter(r -> ownership.equals(r.getOwnership())).collect(Collectors.toList());
        if (manager != null && !manager.isEmpty())
            all = all.stream().filter(r -> manager.equals(r.getManager())).collect(Collectors.toList());
        if (loan_status != null && !loan_status.isEmpty())
            all = all.stream().filter(r -> loan_status.equals(r.getLoan_status())).collect(Collectors.toList());

        return all;
    }

    // 본인 담당 건만 조회 (JWT loginId 기반).
    // - 팀장(bank_manager): vendor_name 매칭, 전체 (assignee 무관)
    // - 상담사(bank_consultant): assignee_vendor_id == 본인 id (legacy 데이터는 manager displayName 매칭으로 폴백)
    // - cancel/done은 기본 제외 (?include_done=true 시 포함)
    @GetMapping("/consultations/mine")
    public List<ConsultationRequest> getMine(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(name = "include_done", required = false, defaultValue = "false") boolean includeDone
    ) {
        String loginId = (String) request.getAttribute("auth.loginId");
        if (loginId == null) return java.util.Collections.emptyList();
        Vendor v = vendorRepository.findByLoginId(loginId).orElse(null);
        if (v == null) return java.util.Collections.emptyList();

        // role 결정: vendor.role > vendorType 폴백
        String bankRole = v.getRole();
        if (bankRole == null || bankRole.isEmpty()) {
            bankRole = "은행상담사".equals(v.getVendorType()) ? "bank_consultant" : "bank_manager";
        }

        List<ConsultationRequest> list;
        if ("bank_consultant".equals(bankRole)) {
            // 상담사: assignee_vendor_id 우선, 없으면 manager displayName 폴백
            UUID myId = v.getId();
            String myName = v.getBankManager();
            list = repository.findBankConsultationsByVendorName(v.getVendorName()).stream()
                    .filter(r -> {
                        if (r.getAssignee_vendor_id() != null) return myId.equals(r.getAssignee_vendor_id());
                        return myName != null && myName.equals(r.getManager()); // legacy
                    })
                    .collect(Collectors.toList());
        } else {
            // 팀장: 은행 전체
            list = repository.findBankConsultationsByVendorName(v.getVendorName());
        }

        if (!includeDone) {
            list = list.stream()
                    .filter(r -> !"done".equals(r.getLoan_status()) && !"cancel".equals(r.getLoan_status()))
                    .collect(Collectors.toList());
        }
        return list;
    }

    // 팀장이 본인 팀의 상담사 목록 조회 (대리 접속 화면용).
    @GetMapping("/team/consultants")
    public List<Map<String, Object>> getTeamConsultants(jakarta.servlet.http.HttpServletRequest request) {
        String loginId = (String) request.getAttribute("auth.loginId");
        if (loginId == null) return java.util.Collections.emptyList();
        Vendor manager = vendorRepository.findByLoginId(loginId).orElse(null);
        if (manager == null) return java.util.Collections.emptyList();

        // role 가드: bank_manager만 허용
        String role = manager.getRole();
        if (role == null) role = "은행상담사".equals(manager.getVendorType()) ? "bank_consultant" : "bank_manager";
        if (!"bank_manager".equals(role)) return java.util.Collections.emptyList();

        return vendorRepository.findAllByParentVendorId(manager.getId()).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()) || c.getIsActive() == null)
                .map(c -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", c.getId());
                    m.put("login_id", c.getLoginId());
                    m.put("display_name", c.getBankManager());
                    m.put("phone", c.getPhone());
                    m.put("is_active", c.getIsActive());
                    m.put("must_change_password", c.getMustChangePassword());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // 단건 조회
    @GetMapping("/consultations/{id}")
    public ConsultationRequest getOne(@PathVariable UUID id) {
        return repository.findById(id).orElseThrow();
    }

    // 신규 상담 등록 (은행 상담사가 인박스에서 직접 등록).
    // JWT loginId 로 vendor 조회 → vendor_name / assignee_vendor_id / manager 자동 세팅,
    // loan_status="apply" 로 시작해서 인박스 "신규" 카테고리에 잡히도록 한다.
    @PostMapping("/consultations")
    public ResponseEntity<ConsultationRequest> create(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody Map<String, Object> body
    ) {
        String loginId = (String) request.getAttribute("auth.loginId");
        if (loginId == null) return ResponseEntity.status(401).build();
        Vendor v = vendorRepository.findByLoginId(loginId).orElse(null);
        if (v == null) return ResponseEntity.status(403).build();

        ConsultationRequest c = new ConsultationRequest();
        c.setVendor_name(v.getVendorName());
        c.setVendor_type("bank"); // findBankConsultationsByVendorName 쿼리의 vendor_type IN ('은행','bank') 필터에 잡히도록.
        c.setAssignee_vendor_id(v.getId());
        c.setManager(v.getBankManager());
        c.setLoan_status("apply");
        c.setStage_changed_at(OffsetDateTime.now());
        c.setReceive_date(LocalDate.now());

        c.setResident_name(asString(body.get("resident_name")));
        c.setResident_phone(asString(body.get("resident_phone")));
        c.setComplex_name(asString(body.get("complex_name")));
        c.setDong(asString(body.get("dong")));
        c.setHo(asString(body.get("ho")));
        c.setApt_type(asString(body.get("apt_type")));
        c.setMemo(asString(body.get("memo")));
        Long loanAmount = asLong(body.get("loan_amount"));
        if (loanAmount != null) c.setLoan_amount(loanAmount);

        ConsultationRequest saved = repository.save(c);
        log.info("[은행 신규 등록] id: {}, 상담사: {}({}), 고객: {}",
                saved.getId(), v.getBankManager(), loginId, saved.getResident_name());
        return ResponseEntity.ok(saved);
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

    // 은행 상담사 필드 저장/수정
    @PutMapping("/consultations/{id}")
    public ConsultationRequest update(@PathVariable UUID id, @RequestBody ConsultationRequest req) {
        ConsultationRequest existing = repository.findById(id).orElseThrow();
        log.info("[상담 수정] id: {}", id);

        if (req.getResident_no() != null) existing.setResident_no(req.getResident_no());
        if (req.getManager() != null) existing.setManager(req.getManager());
        if (req.getTransfer_date() != null) existing.setTransfer_date(req.getTransfer_date());
        if (req.getDivision() != null) existing.setDivision(req.getDivision());
        if (req.getOwnership() != null) existing.setOwnership(req.getOwnership());
        if (req.getDong() != null) existing.setDong(req.getDong());
        if (req.getHo() != null) existing.setHo(req.getHo());
        if (req.getExecution_date() != null) existing.setExecution_date(req.getExecution_date());
        if (req.getLoan_amount() != null) existing.setLoan_amount(req.getLoan_amount());
        if (req.getReceive_date() != null) existing.setReceive_date(req.getReceive_date());
        if (req.getDocument_date() != null) existing.setDocument_date(req.getDocument_date());
        if (req.getApt_type() != null) existing.setApt_type(req.getApt_type());
        if (req.getProduct() != null) existing.setProduct(req.getProduct());
        if (req.getRepayment_method() != null) existing.setRepayment_method(req.getRepayment_method());
        if (req.getLoan_period() != null) existing.setLoan_period(req.getLoan_period());
        if (req.getDeferment() != null) existing.setDeferment(req.getDeferment());
        if (req.getJoint_owner_name() != null) existing.setJoint_owner_name(req.getJoint_owner_name());
        if (req.getJoint_owner_rrn() != null) existing.setJoint_owner_rrn(req.getJoint_owner_rrn());
        if (req.getJoint_owner_tel() != null) existing.setJoint_owner_tel(req.getJoint_owner_tel());
        if (req.getCustomer_deposit() != null) existing.setCustomer_deposit(req.getCustomer_deposit());
        if (req.getDeferred_interest_account() != null) existing.setDeferred_interest_account(req.getDeferred_interest_account());
        if (req.getBalance_account() != null) existing.setBalance_account(req.getBalance_account());
        if (req.getOption_account() != null) existing.setOption_account(req.getOption_account());
        if (req.getInterim_virtual_account() != null) existing.setInterim_virtual_account(req.getInterim_virtual_account());
        if (req.getSpecial_notes() != null) existing.setSpecial_notes(req.getSpecial_notes());
        if (req.getDocuments_checked() != null) existing.setDocuments_checked(req.getDocuments_checked());
        if (req.getConsultation_checks() != null) existing.setConsultation_checks(req.getConsultation_checks());
        if (req.getExisting_homes() != null) existing.setExisting_homes(req.getExisting_homes());
        if (req.getExisting_credit_loan() != null) existing.setExisting_credit_loan(req.getExisting_credit_loan());
        if (req.getExisting_collateral_loan() != null) existing.setExisting_collateral_loan(req.getExisting_collateral_loan());
        if (req.getCredit_score_type() != null) existing.setCredit_score_type(req.getCredit_score_type());
        if (req.getCredit_score() != null) existing.setCredit_score(req.getCredit_score());
        if (req.getSale_price_amount() != null) existing.setSale_price_amount(req.getSale_price_amount());

        // ===== v3 확장 필드 =====
        if (req.getMoving_in_date() != null) existing.setMoving_in_date(req.getMoving_in_date());
        if (req.getSpouse_phone() != null) existing.setSpouse_phone(req.getSpouse_phone());
        if (req.getApproved_amount() != null) existing.setApproved_amount(req.getApproved_amount());
        if (req.getApproved_rate() != null) existing.setApproved_rate(req.getApproved_rate());
        if (req.getApproved_notified_at() != null) existing.setApproved_notified_at(req.getApproved_notified_at());
        if (req.getCustomer_accepted_at() != null) existing.setCustomer_accepted_at(req.getCustomer_accepted_at());
        if (req.getSigning_date() != null) existing.setSigning_date(req.getSigning_date());
        if (req.getSigning_time() != null) existing.setSigning_time(req.getSigning_time());
        if (req.getAdditional_loan_amount() != null) existing.setAdditional_loan_amount(req.getAdditional_loan_amount());
        if (req.getBank_branch() != null) existing.setBank_branch(req.getBank_branch());
        if (req.getBank_manager_phone() != null) existing.setBank_manager_phone(req.getBank_manager_phone());
        if (req.getBank_manager_fax() != null) existing.setBank_manager_fax(req.getBank_manager_fax());
        if (req.getCanceled_reason() != null) existing.setCanceled_reason(req.getCanceled_reason());

        // 정산 (6단계 대출실행)
        if (req.getSettle_middle_principal() != null) existing.setSettle_middle_principal(req.getSettle_middle_principal());
        if (req.getSettle_middle_interest() != null) existing.setSettle_middle_interest(req.getSettle_middle_interest());
        if (req.getSettle_middle_bank() != null) existing.setSettle_middle_bank(req.getSettle_middle_bank());
        if (req.getSettle_middle_account() != null) existing.setSettle_middle_account(req.getSettle_middle_account());
        if (req.getSettle_balance_principal() != null) existing.setSettle_balance_principal(req.getSettle_balance_principal());
        if (req.getSettle_balance_interest() != null) existing.setSettle_balance_interest(req.getSettle_balance_interest());
        if (req.getSettle_balance_account() != null) existing.setSettle_balance_account(req.getSettle_balance_account());
        if (req.getSettle_balcony() != null) existing.setSettle_balcony(req.getSettle_balcony());
        if (req.getSettle_options() != null) existing.setSettle_options(req.getSettle_options());
        if (req.getSettle_guarantee_fee() != null) existing.setSettle_guarantee_fee(req.getSettle_guarantee_fee());
        if (req.getSettle_mgmt_fee() != null) existing.setSettle_mgmt_fee(req.getSettle_mgmt_fee());
        if (req.getSettle_mgmt_account() != null) existing.setSettle_mgmt_account(req.getSettle_mgmt_account());
        if (req.getSettle_moving_allowance() != null) existing.setSettle_moving_allowance(req.getSettle_moving_allowance());
        if (req.getSettle_moving_bank() != null) existing.setSettle_moving_bank(req.getSettle_moving_bank());
        if (req.getSettle_moving_account() != null) existing.setSettle_moving_account(req.getSettle_moving_account());
        if (req.getSettle_stamp_duty() != null) existing.setSettle_stamp_duty(req.getSettle_stamp_duty());
        if (req.getSettle_stamp_duty_additional() != null) existing.setSettle_stamp_duty_additional(req.getSettle_stamp_duty_additional());
        if (req.getExecution_completed() != null) existing.setExecution_completed(req.getExecution_completed());
        if (req.getMemo_log() != null) existing.setMemo_log(req.getMemo_log());
        if (req.getLast_sms_sent_at() != null) existing.setLast_sms_sent_at(req.getLast_sms_sent_at());

        String oldStatus = existing.getLoan_status();
        boolean stageChanged = false;
        if (req.getLoan_status() != null) {
            String newStatus = req.getLoan_status();
            existing.setLoan_status(newStatus);
            if (!java.util.Objects.equals(oldStatus, newStatus)) {
                existing.setStage_changed_at(OffsetDateTime.now());
                stageChanged = true;
            }
        }
        if (req.getMemo() != null) existing.setMemo(req.getMemo());

        ConsultationRequest saved = repository.save(existing);
        if (stageChanged) {
            sendStageChangePush(saved, oldStatus, saved.getLoan_status());
        }
        return saved;
    }

    // 상태 변경 (단계 전환 시 stage_changed_at 자동 갱신)
    @PatchMapping("/consultations/{id}/status")
    public ConsultationRequest updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        ConsultationRequest existing = repository.findById(id).orElseThrow();
        String oldStatus = existing.getLoan_status();
        String newStatus = body.get("loan_status");
        log.info("[단계 변경] id: {}, 단계: {} -> {}", id, oldStatus, newStatus);
        boolean stageChanged = false;
        if (newStatus != null && !java.util.Objects.equals(oldStatus, newStatus)) {
            existing.setLoan_status(newStatus);
            existing.setStage_changed_at(OffsetDateTime.now());
            stageChanged = true;
        }
        ConsultationRequest saved = repository.save(existing);
        if (stageChanged) {
            sendStageChangePush(saved, oldStatus, newStatus);
        }
        return saved;
    }

    /**
     * 자서 일정 캘린더 설정 (v2 — opt-out 방식).
     * Body: {
     *   window_start: "YYYY-MM-DD" (optional, default 오늘+3),
     *   window_end:   "YYYY-MM-DD" (optional, default 오늘+30),
     *   excluded_dates: ["YYYY-MM-DD", ...],
     *   available_times: ["HH:MM", ...],
     *   available_locations: ["...", ...]
     * }
     * 첫 설정 시(또는 변경 시) 입주민 앱으로 푸시.
     */
    @PutMapping("/consultations/{id}/signing-calendar")
    public ConsultationRequest setSigningCalendar(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        ConsultationRequest existing = repository.findById(id).orElseThrow();
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        boolean isFirstSetup = (existing.getSigning_available_times() == null);
        try {
            if (body.get("window_start") != null) {
                existing.setSigning_window_start(LocalDate.parse(body.get("window_start").toString()));
            }
            if (body.get("window_end") != null) {
                existing.setSigning_window_end(LocalDate.parse(body.get("window_end").toString()));
            }
            if (body.get("excluded_dates") != null) {
                existing.setSigning_excluded_dates(om.writeValueAsString(body.get("excluded_dates")));
            }
            if (body.get("available_times") != null) {
                existing.setSigning_available_times(om.writeValueAsString(body.get("available_times")));
            }
            if (body.get("available_locations") != null) {
                existing.setSigning_available_locations(om.writeValueAsString(body.get("available_locations")));
            }
            // 새 설정 시 기존 선택 무효화
            existing.setSigning_selected_date(null);
            existing.setSigning_selected_time(null);
            existing.setSigning_selected_location_str(null);
            existing.setSigning_selected_at(null);
            existing.setSigning_confirmed_at(null);
        } catch (Exception e) {
            throw new RuntimeException("calendar 설정 직렬화 실패", e);
        }
        ConsultationRequest saved = repository.save(existing);
        log.info("[자서 캘린더 설정] id={} firstSetup={}", id, isFirstSetup);

        if (isFirstSetup && saved.getResident_phone() != null) {
            String bank = saved.getVendor_name() != null ? saved.getVendor_name() : "은행";
            webPushService.sendToPhone(
                    saved.getResident_phone(),
                    "📅 " + bank + " 자서 일정 도착",
                    "가능한 날짜와 시간을 선택해주세요",
                    "/my/consultations/" + saved.getId()
            );
        }
        return saved;
    }

    /**
     * 같은 은행의 다른 상담건 자서 예약 카운트 (더블부킹 표시용).
     * 반환: { "YYYY-MM-DD": [ { time, customerName }, ... ] }
     */
    @GetMapping("/consultations/{id}/signing-calendar/bookings")
    public Map<String, List<Map<String, String>>> getOtherBookings(@PathVariable UUID id) {
        ConsultationRequest current = repository.findById(id).orElseThrow();
        if (current.getVendor_name() == null) return Map.of();
        List<ConsultationRequest> others = repository.findConfirmedSigningsByBank(current.getVendor_name(), id);
        Map<String, List<Map<String, String>>> byDate = new LinkedHashMap<>();
        for (ConsultationRequest r : others) {
            if (r.getSigning_date() == null) continue;
            String key = r.getSigning_date().toString();
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("time", r.getSigning_time() != null ? r.getSigning_time() : "");
            entry.put("customer", r.getResident_name() != null ? r.getResident_name() : "");
            byDate.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }
        return byDate;
    }

    /**
     * 자서 후 누락 서류 → 입주민 앱 푸시 알림.
     * Body: { missing_doc_names: ["주민등록 등본", "인감증명서", ...] }
     * 다음 미팅에 지참하도록 안내.
     */
    @PostMapping("/consultations/{id}/notify-missing-docs")
    public ResponseEntity<?> notifyMissingDocs(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        ConsultationRequest c = repository.findById(id).orElseThrow();
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) body.get("missing_doc_names");
        if (names == null || names.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_doc_names 필수"));
        }
        if (c.getResident_phone() == null) {
            return ResponseEntity.status(409).body(Map.of("error", "입주민 연락처가 없습니다"));
        }
        String bank = c.getVendor_name() != null ? c.getVendor_name() : "은행";
        String preview = names.size() <= 3
                ? String.join(", ", names)
                : names.subList(0, 3).stream().collect(Collectors.joining(", ")) + " 외 " + (names.size() - 3) + "건";
        webPushService.sendToPhone(
                c.getResident_phone(),
                "📌 " + bank + " 누락 서류 안내",
                preview + " — 다음 일정에 지참 부탁드립니다",
                "/my/consultations/" + c.getId()
        );
        log.info("[누락서류 푸시] id={} count={}", id, names.size());
        return ResponseEntity.ok(Map.of("ok", true, "notified", names.size()));
    }

    /**
     * 상담사 → 입주민 메시지 (b2c_messages 에 append + 입주민 앱 푸시).
     */
    @PostMapping("/consultations/{id}/message")
    public ConsultationRequest sendMessage(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        ConsultationRequest c = repository.findById(id).orElseThrow();
        String text = body.get("text");
        String byName = body.get("by");
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text 필수");
        }
        MessagesHelper.append(c, "CONSULTANT", byName != null ? byName : c.getManager(), text);
        ConsultationRequest saved = repository.save(c);
        log.info("[상담사 메시지] id={} by={} len={}", id, byName, text.length());
        if (saved.getResident_phone() != null) {
            String bank = saved.getVendor_name() != null ? saved.getVendor_name() : "은행";
            String preview = text.length() > 50 ? text.substring(0, 50) + "..." : text;
            webPushService.sendToPhone(
                    saved.getResident_phone(),
                    "💬 " + bank + " 상담사 메시지",
                    preview,
                    "/my/consultations/" + saved.getId()
            );
        }
        return saved;
    }

    /** [Legacy] 자서 일정 슬롯 제시 — 구 모델 호환 유지 */
    @PutMapping("/consultations/{id}/signing-slots")
    public ConsultationRequest setSigningSlots(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        ConsultationRequest existing = repository.findById(id).orElseThrow();
        Object slotsObj = body.get("slots");
        if (slotsObj == null) {
            throw new IllegalArgumentException("slots 필수");
        }
        // JSON 직렬화해서 저장 (List<Map> → JSON 문자열)
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(slotsObj);
            existing.setSigning_offered_slots(json);
            // 새 슬롯 제시 시 기존 선택은 무효화
            existing.setSigning_selected_slot_index(null);
            existing.setSigning_selected_at(null);
            existing.setSigning_confirmed_at(null);
        } catch (Exception e) {
            throw new RuntimeException("slots 직렬화 실패", e);
        }
        ConsultationRequest saved = repository.save(existing);
        log.info("[자서 슬롯 제시] id={} slots={}", id, saved.getSigning_offered_slots());

        // 입주민 앱 푸시
        if (saved.getResident_phone() != null) {
            String bank = saved.getVendor_name() != null ? saved.getVendor_name() : "은행";
            webPushService.sendToPhone(
                    saved.getResident_phone(),
                    "📅 " + bank + " 자서 일정 도착",
                    "가능한 시간을 선택해주세요",
                    "/my/consultations/" + saved.getId()
            );
        }
        return saved;
    }

    /**
     * [v2] 자서 일정 확정 — 입주민이 선택한 (date,time,location) 을 상담사가 확정.
     * 확정 시 signing_date / signing_time / signing_location 셋팅 + 입주민 푸시.
     */
    @PostMapping("/consultations/{id}/confirm-signing-calendar")
    public ConsultationRequest confirmSigningCalendar(@PathVariable UUID id) {
        ConsultationRequest existing = repository.findById(id).orElseThrow();
        if (existing.getSigning_selected_date() == null) {
            throw new IllegalStateException("입주민이 아직 일정을 선택하지 않았습니다");
        }
        existing.setSigning_date(existing.getSigning_selected_date());
        existing.setSigning_time(existing.getSigning_selected_time());
        existing.setSigning_location(existing.getSigning_selected_location_str());
        existing.setSigning_confirmed_at(OffsetDateTime.now());
        ConsultationRequest saved = repository.save(existing);
        log.info("[자서 캘린더 확정] id={} date={} time={} location={}", id,
                saved.getSigning_date(), saved.getSigning_time(), saved.getSigning_location());

        if (saved.getResident_phone() != null) {
            String bank = saved.getVendor_name() != null ? saved.getVendor_name() : "은행";
            String body = saved.getSigning_date() + (saved.getSigning_time() != null ? " " + saved.getSigning_time() : "")
                    + (saved.getSigning_location() != null ? " · " + saved.getSigning_location() : "");
            webPushService.sendToPhone(
                    saved.getResident_phone(),
                    "✅ " + bank + " 자서 예약 확정",
                    body,
                    "/my/consultations/" + saved.getId()
            );
        }
        return saved;
    }

    /** [Legacy] 자서 일정 확정 — 구 슬롯 모델 호환 유지 */
    @PostMapping("/consultations/{id}/confirm-signing-slot")
    public ConsultationRequest confirmSigningSlot(@PathVariable UUID id) {
        ConsultationRequest existing = repository.findById(id).orElseThrow();
        if (existing.getSigning_selected_slot_index() == null) {
            throw new IllegalStateException("입주민이 아직 슬롯을 선택하지 않았습니다");
        }
        if (existing.getSigning_offered_slots() == null) {
            throw new IllegalStateException("제시된 슬롯이 없습니다");
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            List<Map<String, String>> slots = om.readValue(existing.getSigning_offered_slots(), List.class);
            int idx = existing.getSigning_selected_slot_index();
            if (idx < 0 || idx >= slots.size()) {
                throw new IllegalStateException("선택된 슬롯 인덱스가 범위를 벗어남");
            }
            Map<String, String> slot = slots.get(idx);
            String dateStr = slot.get("date");
            String timeStr = slot.get("time");
            String location = slot.get("location");
            if (dateStr != null) existing.setSigning_date(LocalDate.parse(dateStr));
            if (timeStr != null) existing.setSigning_time(timeStr);
            if (location != null) existing.setSigning_location(location);
            existing.setSigning_confirmed_at(OffsetDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("슬롯 확정 실패: " + e.getMessage(), e);
        }
        ConsultationRequest saved = repository.save(existing);
        log.info("[자서 슬롯 확정] id={} date={} time={} location={}", id,
                saved.getSigning_date(), saved.getSigning_time(), saved.getSigning_location());

        // 입주민 앱 푸시
        if (saved.getResident_phone() != null) {
            String bank = saved.getVendor_name() != null ? saved.getVendor_name() : "은행";
            String body = saved.getSigning_date() + (saved.getSigning_time() != null ? " " + saved.getSigning_time() : "")
                    + (saved.getSigning_location() != null ? " · " + saved.getSigning_location() : "");
            webPushService.sendToPhone(
                    saved.getResident_phone(),
                    "✅ " + bank + " 자서 예약 확정",
                    body,
                    "/my/consultations/" + saved.getId()
            );
        }
        return saved;
    }

    // 엑셀 다운로드
    @GetMapping("/consultations/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam(required = false) String bank_name
    ) throws Exception {
        List<ConsultationRequest> list = (bank_name != null && !bank_name.isEmpty())
                ? repository.findBankConsultationsByVendorName(bank_name)
                : repository.findAllBankConsultations();

        // 은행 담당자/연락처/팩스 조회 (vendor_accounts 테이블)
        String bankManager = "";
        String bankPhone   = "";
        String bankFax     = "";
        if (bank_name != null && !bank_name.isEmpty()) {
            vendorRepository.findByVendorName(bank_name).ifPresent(v -> {
                // 람다 내에서 변수 할당이 안 되므로 배열 사용
            });
            Vendor vendor = vendorRepository.findByVendorName(bank_name).orElse(null);
            if (vendor != null) {
                bankManager = vendor.getBankManager() != null ? vendor.getBankManager() : "";
                bankPhone   = vendor.getPhone()       != null ? vendor.getPhone()       : "";
                bankFax     = vendor.getFax()         != null ? vendor.getFax()         : "";
            }
        }

        log.info("[엑셀 다운로드] 은행: {}, 건수: {}", bank_name != null ? bank_name : "전체", list.size());

        byte[] data = excelService.generateExcel(
                complexName, complexFullName,
                approvalNo, totalLimit,
                bank_name != null ? bank_name : "",
                bankManager, bankPhone, bankFax,
                list);

        String filename = URLEncoder.encode(
                complexName + "_접수리스트_" + LocalDate.now() + ".xlsx",
                StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    // 은행별 집계 (관리자 대시보드용).
    // 각 은행 vendor_name 별로 상담건수/실행건수/실행금액/대기건수를 한 번에 묶어서 반환.
    // 프론트가 8회 호출하는 대신 1회로 처리할 수 있게 하기 위함.
    @GetMapping("/summary/by-bank")
    public List<Map<String, Object>> getSummaryByBank() {
        List<ConsultationRequest> all = repository.findAllBankConsultations();

        Map<String, List<ConsultationRequest>> grouped = all.stream()
                .filter(r -> r.getVendor_name() != null && !r.getVendor_name().isEmpty())
                .collect(Collectors.groupingBy(ConsultationRequest::getVendor_name));

        return grouped.entrySet().stream()
                .map(e -> {
                    String bankName = e.getKey();
                    List<ConsultationRequest> rows = e.getValue();
                    long totalCount = rows.stream().filter(r -> !"cancel".equals(r.getLoan_status())).count();
                    long doneCount = rows.stream().filter(r -> "done".equals(r.getLoan_status())).count();
                    long doneAmount = rows.stream()
                            .filter(r -> "done".equals(r.getLoan_status()) && r.getLoan_amount() != null)
                            .mapToLong(ConsultationRequest::getLoan_amount).sum();
                    long waitCount = rows.stream()
                            .filter(r -> r.getLoan_status() == null || "wait".equals(r.getLoan_status()) || "apply".equals(r.getLoan_status()))
                            .count();
                    long todayCount = rows.stream().filter(r -> {
                        if (r.getCreatedAt() == null) return false;
                        return r.getCreatedAt().toLocalDate().equals(LocalDate.now());
                    }).count();
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("bank_name",   bankName);
                    m.put("total_count", totalCount);
                    m.put("done_count",  doneCount);
                    m.put("done_amount", doneAmount);
                    m.put("wait_count",  waitCount);
                    m.put("today_count", todayCount);
                    return m;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("done_amount"), (Long) a.get("done_amount")))
                .collect(Collectors.toList());
    }

    // 집계 현황
    @GetMapping("/summary")
    public Map<String, Object> getSummary(@RequestParam(required = false) String bank_name) {
        List<ConsultationRequest> all = (bank_name != null && !bank_name.isEmpty())
                ? repository.findBankConsultationsByVendorName(bank_name)
                : repository.findAllBankConsultations();

        long totalCount  = all.stream().filter(r -> !"cancel".equals(r.getLoan_status())).count();
        long totalAmount = all.stream()
                .filter(r -> !"cancel".equals(r.getLoan_status()) && r.getLoan_amount() != null)
                .mapToLong(ConsultationRequest::getLoan_amount).sum();
        long cancelCount  = all.stream().filter(r -> "cancel".equals(r.getLoan_status())).count();
        long cancelAmount = all.stream()
                .filter(r -> "cancel".equals(r.getLoan_status()) && r.getLoan_amount() != null)
                .mapToLong(ConsultationRequest::getLoan_amount).sum();
        long doneCount  = all.stream().filter(r -> "done".equals(r.getLoan_status())).count();
        long waitCount  = all.stream().filter(r -> r.getLoan_status() == null || "wait".equals(r.getLoan_status())).count();

        // 7단계 파이프라인 카운트 (상담사 관리용)
        long applyCount      = all.stream().filter(r -> "apply".equals(r.getLoan_status())).count();
        long consultingCount = all.stream().filter(r -> "consulting".equals(r.getLoan_status())).count();
        long reviewingCount  = all.stream().filter(r -> "reviewing".equals(r.getLoan_status())).count();
        long resultCount     = all.stream().filter(r -> "result".equals(r.getLoan_status())).count();
        long executingCount  = all.stream().filter(r -> "executing".equals(r.getLoan_status())).count();
        long todayCount = all.stream().filter(r -> {
            if (r.getCreatedAt() == null) return false;
            return r.getCreatedAt().toLocalDate().equals(LocalDate.now());
        }).count();

        // 상품별(고정/변동) × 취소여부 breakdown (product 필드 사용)
        long fixedCount = all.stream().filter(r -> "고정".equals(r.getProduct()) && !"cancel".equals(r.getLoan_status())).count();
        long fixedAmount = all.stream().filter(r -> "고정".equals(r.getProduct()) && !"cancel".equals(r.getLoan_status()) && r.getLoan_amount() != null)
                .mapToLong(ConsultationRequest::getLoan_amount).sum();
        long varCount = all.stream().filter(r -> "변동".equals(r.getProduct()) && !"cancel".equals(r.getLoan_status())).count();
        long varAmount = all.stream().filter(r -> "변동".equals(r.getProduct()) && !"cancel".equals(r.getLoan_status()) && r.getLoan_amount() != null)
                .mapToLong(ConsultationRequest::getLoan_amount).sum();

        // 오늘 실행예정 / 서류 미제출 / 이번주 실행예정
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        long todayExecCount = all.stream().filter(r -> today.equals(r.getExecution_date()) && !"cancel".equals(r.getLoan_status())).count();
        long docMissingCount = all.stream().filter(r -> r.getDocument_date() == null && !"cancel".equals(r.getLoan_status()) && r.getReceive_date() != null).count();
        long weekExecCount = all.stream().filter(r -> r.getExecution_date() != null
                && !"cancel".equals(r.getLoan_status())
                && !r.getExecution_date().isBefore(today)
                && r.getExecution_date().isBefore(weekEnd)).count();

        // 은행 정보 (bank_name 지정된 경우)
        String bankManager = "", bankPhone = "", bankFax = "";
        if (bank_name != null && !bank_name.isEmpty()) {
            Vendor v = vendorRepository.findByVendorName(bank_name).orElse(null);
            if (v != null) {
                bankManager = v.getBankManager() != null ? v.getBankManager() : "";
                bankPhone   = v.getPhone() != null ? v.getPhone() : "";
                bankFax     = v.getFax() != null ? v.getFax() : "";
            }
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("total_count",  totalCount);
        result.put("total_amount", totalAmount);
        result.put("cancel_count", cancelCount);
        result.put("cancel_amount",cancelAmount);
        result.put("done_count",   doneCount);
        result.put("wait_count",   waitCount);
        result.put("today_count",  todayCount);
        result.put("fixed_count",  fixedCount);
        result.put("fixed_amount", fixedAmount);
        result.put("var_count",    varCount);
        result.put("var_amount",   varAmount);
        result.put("today_exec_count",   todayExecCount);
        result.put("doc_missing_count",  docMissingCount);
        result.put("week_exec_count",    weekExecCount);
        result.put("complex_name",       complexName);
        result.put("complex_full_name",  complexFullName);
        result.put("approval_no",        approvalNo);
        result.put("total_limit",        totalLimit);
        result.put("bank_manager",       bankManager);
        result.put("bank_phone",         bankPhone);
        result.put("bank_fax",           bankFax);
        result.put("apply_count",        applyCount);
        result.put("consulting_count",   consultingCount);
        result.put("reviewing_count",    reviewingCount);
        result.put("result_count",       resultCount);
        result.put("executing_count",    executingCount);
        return result;
    }
}
