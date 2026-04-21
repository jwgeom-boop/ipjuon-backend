package com.ipjuon.backend.bank;

import com.ipjuon.backend.consultation.ConsultationRequest;
import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.vendor.Vendor;
import com.ipjuon.backend.vendor.VendorRepository;
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
                                       VendorRepository vendorRepository) {
        this.repository = repository;
        this.excelService = excelService;
        this.vendorRepository = vendorRepository;
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

    // 단건 조회
    @GetMapping("/consultations/{id}")
    public ConsultationRequest getOne(@PathVariable UUID id) {
        return repository.findById(id).orElseThrow();
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
        if (req.getLoan_status() != null) {
            String oldStatus = existing.getLoan_status();
            String newStatus = req.getLoan_status();
            existing.setLoan_status(newStatus);
            if (!java.util.Objects.equals(oldStatus, newStatus)) {
                existing.setStage_changed_at(OffsetDateTime.now());
            }
        }
        if (req.getMemo() != null) existing.setMemo(req.getMemo());

        return repository.save(existing);
    }

    // 상태 변경 (단계 전환 시 stage_changed_at 자동 갱신)
    @PatchMapping("/consultations/{id}/status")
    public ConsultationRequest updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        ConsultationRequest existing = repository.findById(id).orElseThrow();
        String newStatus = body.get("loan_status");
        log.info("[단계 변경] id: {}, 단계: {} -> {}", id, existing.getLoan_status(), newStatus);
        if (newStatus != null && !java.util.Objects.equals(existing.getLoan_status(), newStatus)) {
            existing.setLoan_status(newStatus);
            existing.setStage_changed_at(OffsetDateTime.now());
        }
        return repository.save(existing);
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
