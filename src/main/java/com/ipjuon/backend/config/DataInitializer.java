package com.ipjuon.backend.config;

import com.ipjuon.backend.consultation.ConsultationRequest;
import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.vendor.Vendor;
import com.ipjuon.backend.vendor.VendorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ConsultationRepository repository;
    private final VendorRepository vendorRepository;

    public DataInitializer(ConsultationRepository repository, VendorRepository vendorRepository) {
        this.repository = repository;
        this.vendorRepository = vendorRepository;
    }

    @Override
    public void run(String... args) {
        initBankVendors();
        initSampleConsultations();
    }

    // ── 은행 계정 초기화 ──
    private void initBankVendors() {
        String[][] banks = {
            // {loginId, password, vendorName, phone}
            {"shinhan", "shinhan2024!", "신한은행",    "02-1599-8000"},
            {"hana",    "hana2024!",    "하나은행",    "02-1599-1111"},
            {"kb",      "kb2024!",      "KB국민은행",  "02-1588-9999"},
            {"woori",   "woori2024!",   "우리은행",    "02-1588-5000"},
            {"nh",      "nh2024!",      "NH농협은행",  "02-1588-2100"},
            {"ibk",     "ibk2024!",     "IBK기업은행", "02-1588-2588"},
            {"busan",   "busan2024!",   "부산은행",    "02-1588-6200"},
            {"daegu",   "daegu2024!",   "대구은행",    "02-1588-5050"},
        };

        for (String[] b : banks) {
            if (!vendorRepository.existsByLoginId(b[0])) {
                Vendor v = new Vendor();
                v.setLoginId(b[0]);
                v.setPassword(b[1]);
                v.setVendorName(b[2]);
                v.setVendorType("은행");
                v.setPhone(b[3]);
                v.setStatus("active");
                vendorRepository.save(v);
            }
        }
        System.out.println("✅ 은행 계정 초기화 완료");
    }

    // ── 샘플 상담 데이터 초기화 ──
    private void initSampleConsultations() {
        long bankCount = repository.findAll().stream()
                .filter(r -> "은행".equals(r.getVendor_type()) || "bank".equals(r.getVendor_type()))
                .filter(r -> r.getManager() != null)
                .count();
        if (bankCount > 0) return;

        String[][] samples = {
            // {resident_name, resident_phone, vendor_name, complex_name, dong, ho, manager, division, ownership, apt_type, product, loan_status, loan_amount, receive_date, execution_date}
            {"김철수", "010-1111-2222", "신한은행",    "창원힐스테이트", "101", "1502", "이영희", "조합", "단독", "84", "고정", "done",   "350000000", "2025-03-10", "2025-04-15"},
            {"박영수", "010-2222-3333", "신한은행",    "창원힐스테이트", "102", "801",  "이영희", "일반", "단독", "59", "변동", "wait",   "250000000", "2025-04-01", null},
            {"이민정", "010-3333-4444", "신한은행",    "창원힐스테이트", "103", "1201", "김상훈", "조합", "공동", "71", "고정", "wait",   "300000000", "2025-04-20", null},
            {"최지훈", "010-4444-5555", "신한은행",    "창원힐스테이트", "104", "601",  "김상훈", "일반", "단독", "84", "고정", "cancel", "320000000", "2025-03-15", null},
            {"정수연", "010-5555-6666", "하나은행",    "창원힐스테이트", "105", "1101", "박민수", "조합", "단독", "59", "변동", "done",   "220000000", "2025-03-20", "2025-04-10"},
            {"강동원", "010-6666-7777", "하나은행",    "창원힐스테이트", "106", "901",  "박민수", "조합", "공동", "71", "고정", "wait",   "280000000", "2025-04-25", null},
            {"윤서연", "010-7777-8888", "KB국민은행",  "창원힐스테이트", "107", "1401", "최현우", "일반", "단독", "84", "고정", "done",   "370000000", "2025-02-28", "2025-04-05"},
            {"임재현", "010-8888-9999", "KB국민은행",  "창원힐스테이트", "108", "501",  "최현우", "조합", "단독", "59", "변동", "wait",   "200000000", "2025-05-02", null},
            {"한지민", "010-9999-0000", "우리은행",    "창원힐스테이트", "109", "1301", "정다은", "일반", "공동", "71", "고정", "wait",   "260000000", "2025-05-05", null},
            {"오준혁", "010-1234-5678", "우리은행",    "창원힐스테이트", "110", "701",  "정다은", "조합", "단독", "84", "변동", "done",   "340000000", "2025-03-05", "2025-04-20"},
        };

        for (String[] s : samples) {
            ConsultationRequest r = new ConsultationRequest();
            r.setResident_name(s[0]);
            r.setResident_phone(s[1]);
            r.setVendor_name(s[2]);
            r.setVendor_type("은행");
            r.setComplex_name(s[3]);
            r.setDong(s[4]);
            r.setHo(s[5]);
            r.setManager(s[6]);
            r.setDivision(s[7]);
            r.setOwnership(s[8]);
            r.setApt_type(s[9]);
            r.setProduct(s[10]);
            r.setLoan_status(s[11]);
            r.setLoan_amount(Long.parseLong(s[12]));
            r.setReceive_date(LocalDate.parse(s[13]));
            if (s[14] != null) r.setExecution_date(LocalDate.parse(s[14]));
            r.setStatus("대기중");
            r.setPreferred_time("오전");
            repository.save(r);
        }

        System.out.println("✅ 샘플 데이터 10건 삽입 완료");
    }
}
