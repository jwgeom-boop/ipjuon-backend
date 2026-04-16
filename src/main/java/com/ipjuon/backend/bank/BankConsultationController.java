package com.ipjuon.backend.bank;

import com.ipjuon.backend.consultation.ConsultationRequest;
import com.ipjuon.backend.consultation.ConsultationRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bank")
@CrossOrigin(origins = "*")
public class BankConsultationController {

    private final ConsultationRepository repository;
    private final BankExcelExportService excelService;

    public BankConsultationController(ConsultationRepository repository,
                                       BankExcelExportService excelService) {
        this.repository = repository;
        this.excelService = excelService;
    }

    // 은행 접수 리스트 조회 (vendor_type이 은행/bank인 것만)
    @GetMapping("/consultations")
    public List<ConsultationRequest> getAll(
            @RequestParam(required = false) String bank_name,
            @RequestParam(required = false) String division,
            @RequestParam(required = false) String ownership,
            @RequestParam(required = false) String manager,
            @RequestParam(required = false) String loan_status
    ) {
        List<ConsultationRequest> all = repository.findAll().stream()
                .filter(r -> "은행".equals(r.getVendor_type()) || "bank".equals(r.getVendor_type()))
                .collect(Collectors.toList());

        // 은행별 필터 (본인 은행 데이터만)
        if (bank_name != null && !bank_name.isEmpty()) {
            all = all.stream().filter(r -> bank_name.equals(r.getVendor_name())).collect(Collectors.toList());
        }

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
        if (req.getLoan_status() != null) existing.setLoan_status(req.getLoan_status());
        if (req.getMemo() != null) existing.setMemo(req.getMemo());

        return repository.save(existing);
    }

    // 상태 변경
    @PatchMapping("/consultations/{id}/status")
    public ConsultationRequest updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        ConsultationRequest existing = repository.findById(id).orElseThrow();
        if (body.get("loan_status") != null) existing.setLoan_status(body.get("loan_status"));
        return repository.save(existing);
    }

    // 엑셀 다운로드
    @GetMapping("/consultations/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam(required = false) String bank_name
    ) throws Exception {
        List<ConsultationRequest> list = repository.findAll().stream()
                .filter(r -> "은행".equals(r.getVendor_type()) || "bank".equals(r.getVendor_type()))
                .filter(r -> bank_name == null || bank_name.isEmpty() || bank_name.equals(r.getVendor_name()))
                .collect(Collectors.toList());

        String complexName = "창원 힐스테이트 마크로엔";
        byte[] data = excelService.generateExcel(complexName, "1132500000010", 20_000_000_000L, list);

        String filename = URLEncoder.encode(
                complexName + "_접수리스트_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    // 집계 현황
    @GetMapping("/summary")
    public Map<String, Object> getSummary(@RequestParam(required = false) String bank_name) {
        List<ConsultationRequest> all = repository.findAll().stream()
                .filter(r -> "은행".equals(r.getVendor_type()) || "bank".equals(r.getVendor_type()))
                .collect(Collectors.toList());

        if (bank_name != null && !bank_name.isEmpty()) {
            all = all.stream().filter(r -> bank_name.equals(r.getVendor_name())).collect(Collectors.toList());
        }

        long totalCount = all.stream().filter(r -> !"cancel".equals(r.getLoan_status())).count();
        long totalAmount = all.stream()
                .filter(r -> !"cancel".equals(r.getLoan_status()) && r.getLoan_amount() != null)
                .mapToLong(ConsultationRequest::getLoan_amount).sum();

        long cancelCount = all.stream().filter(r -> "cancel".equals(r.getLoan_status())).count();
        long cancelAmount = all.stream()
                .filter(r -> "cancel".equals(r.getLoan_status()) && r.getLoan_amount() != null)
                .mapToLong(ConsultationRequest::getLoan_amount).sum();

        long doneCount = all.stream().filter(r -> "done".equals(r.getLoan_status())).count();
        long waitCount = all.stream().filter(r -> r.getLoan_status() == null || "wait".equals(r.getLoan_status())).count();

        long todayCount = all.stream().filter(r -> {
            if (r.getCreatedAt() == null) return false;
            java.time.LocalDate today = java.time.LocalDate.now();
            return r.getCreatedAt().toLocalDate().equals(today);
        }).count();

        return Map.of(
                "total_count", totalCount,
                "total_amount", totalAmount,
                "cancel_count", cancelCount,
                "cancel_amount", cancelAmount,
                "done_count", doneCount,
                "wait_count", waitCount,
                "today_count", todayCount
        );
    }
}
