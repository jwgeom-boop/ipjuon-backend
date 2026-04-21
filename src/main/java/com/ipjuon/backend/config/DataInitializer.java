package com.ipjuon.backend.config;

import com.ipjuon.backend.consultation.ConsultationRequest;
import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.vendor.Vendor;
import com.ipjuon.backend.vendor.VendorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ConsultationRepository repository;
    private final VendorRepository vendorRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(ConsultationRepository repository,
                           VendorRepository vendorRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.vendorRepository = vendorRepository;
        this.passwordEncoder = passwordEncoder;
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
            Vendor v = vendorRepository.findByLoginId(b[0]).orElseGet(Vendor::new);
            v.setLoginId(b[0]);
            v.setPassword(passwordEncoder.encode(b[1])); // 매 시작마다 기본 비밀번호로 리셋
            v.setVendorName(b[2]);
            v.setVendorType("은행");
            v.setPhone(b[3]);
            v.setStatus("active"); // 항상 활성화
            vendorRepository.save(v);
        }
        System.out.println("✅ 은행 계정 " + banks.length + "개 리셋 완료 (status=active, 기본 비밀번호)");
    }

    // ── 샘플 상담 데이터 초기화 ──
    private void initSampleConsultations() {
        long bankCount = repository.findAllBankConsultations().stream()
                .filter(r -> r.getManager() != null)
                .count();
        // 40건 미만이면 전체 seed 데이터 삭제 후 재삽입 (시드 마커: special_notes = "SEED")
        if (bankCount >= 44) return;

        // 기존 SEED 데이터만 삭제 (실제 운영 데이터는 보존)
        repository.findAllBankConsultations().stream()
                .filter(r -> "SEED".equals(r.getSpecial_notes()))
                .forEach(r -> repository.deleteById(r.getId()));

        LocalDate today = LocalDate.now();

        // 7단계 파이프라인: apply(신청) → consulting(상담중) → reviewing(심사중) → result(결과대기) → executing(실행예정) → done(실행완료) / cancel(취소)
        // stageOffset: 현재 단계 진입 후 경과일 (음수, 체류일 경고 테스트용)
        Object[][] samples = {
            // {name, phone, bank, dong, ho, manager, division, ownership, apt_type, product, loan_status, loan_amount, receiveOffset, execOffset, docOffset, stageOffset, memo}

            // ─── 상담신청 (apply, 신규 접수, 5건) ───
            {"김철수", "010-1111-2222", "신한은행",   "101", "1502", "이영희", "조합", "단독", "84", "고정", "apply",  350000000L,  -1,  null, null, -1, "신규 접수, 상담 예약 필요"},
            {"박영수", "010-2222-3333", "하나은행",   "102", "1801", "박민수", "조합", "공동", "84", "고정", "apply",  380000000L,  -1,  null, null, -1, null},
            {"이민정", "010-3333-4444", "신한은행",   "103", "1201", "김상훈", "조합", "공동", "71", "고정", "apply",  300000000L,   0,  null, null,  0, null},
            {"최지훈", "010-4444-5555", "KB국민은행", "104", "601",  "최현우", "일반", "단독", "59", "변동", "apply",  220000000L,  -2,  null, null, -2, "연락 시도 중"},
            {"정수연", "010-5555-6666", "우리은행",   "105", "1101", "정다은", "조합", "단독", "84", "고정", "apply",  360000000L,   0,  null, null,  0, null},

            // ─── 상담중 (consulting, 계약서 작성 진행, 7건) ───
            {"강동원", "010-6666-7777", "하나은행",   "106", "901",  "박민수", "조합", "공동", "71", "고정", "consulting", 280000000L, -3, null, null, -1, null},
            {"윤서연", "010-7777-8888", "NH농협은행", "107", "1401", "김영주", "조합", "단독", "84", "고정", "consulting", 370000000L, -5, null, null, -3, "서류 안내 중"},
            {"임재현", "010-8888-9999", "KB국민은행", "108", "501",  "최현우", "일반", "단독", "59", "변동", "consulting", 200000000L, -6, null, null, -5, "정체: 서류 독촉 필요"},
            {"한지민", "010-9999-0000", "우리은행",   "109", "1301", "정다은", "조합", "공동", "71", "고정", "consulting", 260000000L, -2, null, null, -2, null},
            {"오준혁", "010-1234-5678", "신한은행",   "110", "701",  "이영희", "조합", "단독", "71", "고정", "consulting", 290000000L, -4, null, null, -2, null},
            {"서민호", "010-3030-3030", "IBK기업은행","111", "1002", "김태현", "일반", "단독", "59", "변동", "consulting", 240000000L, -3, null, null, -1, null},
            {"문소영", "010-4040-5050", "KB국민은행", "112", "1402", "최현우", "일반", "단독", "84", "변동", "consulting", 360000000L, -7, null, null, -4, "공동명의자 연락 대기"},

            // ─── 심사중 (reviewing, 본사 심사 접수, 8건) ───
            {"장예진", "010-5060-7080", "NH농협은행", "113", "601",  "김영주", "조합", "단독", "59", "고정", "reviewing", 210000000L, -10, null, -5, -2, null},
            {"노재원", "010-7070-8080", "신한은행",   "114", "1703", "이영희", "조합", "단독", "84", "고정", "reviewing", 370000000L, -8,  null, -3, -1, null},
            {"권유리", "010-9090-1010", "하나은행",   "115", "802",  "박민수", "일반", "공동", "71", "변동", "reviewing", 255000000L, -12, null, -6, -4, "정체: 심사 지연"},
            {"배준호", "010-2020-4040", "IBK기업은행", "116", "1201", "김태현", "조합", "단독", "84", "고정", "reviewing", 345000000L, -9,  null, -4, -2, null},
            {"최진우", "010-8080-9090", "부산은행",   "117", "902",  "한소영", "일반", "단독", "59", "변동", "reviewing", 215000000L, -11, null, -5, -3, null},
            {"김나영", "010-1212-3434", "대구은행",   "118", "1103", "이상민", "조합", "공동", "71", "고정", "reviewing", 295000000L, -15, null, -8, -6, "정체: 6일째, 본사 재확인 필요"},
            {"박동현", "010-5656-7878", "우리은행",   "119", "1702", "정다은", "일반", "단독", "84", "변동", "reviewing", 340000000L, -7,  null, -3, -1, null},
            {"정하늘", "010-1010-1010", "신한은행",   "120", "1501", "이영희", "조합", "단독", "84", "고정", "reviewing", 360000000L, -10, null, -5, -2, null},

            // ─── 결과대기 (result, 승인/부결 나옴, 고객 통보 전, 4건) ───
            {"송재민", "010-2121-2121", "신한은행",   "121", "1001", "김상훈", "일반", "단독", "59", "변동", "result",  230000000L, -14, null, -10, -1, "승인, 고객 통보 필요"},
            {"이채원", "010-3232-3232", "하나은행",   "122", "1302", "박민수", "조합", "공동", "71", "고정", "result",  285000000L, -13, null, -9, -2, "승인, 실행일 협의"},
            {"김다은", "010-4343-4343", "KB국민은행", "123", "801",  "최현우", "조합", "단독", "84", "고정", "result",  355000000L, -12, null, -8, 0, null},
            {"홍민석", "010-5454-5454", "우리은행",   "124", "1602", "정다은", "일반", "단독", "59", "변동", "result",  245000000L, -15, null, -10, -3, "부결: 재심사 문의"},

            // ─── 실행예정 (executing, 실행일 확정, 8건 = 오늘 5 + 이번주 3) ───
            {"유혜진", "010-6565-6565", "NH농협은행", "125", "1203", "김영주", "조합", "공동", "71", "고정", "executing", 310000000L, -18, 0, -12, -3, "오늘 실행, 서류 체크 완료"},
            {"조현우", "010-7676-7676", "IBK기업은행","126", "903",  "김태현", "조합", "단독", "84", "고정", "executing", 375000000L, -16, 0, -10, -2, null},
            {"안지연", "010-8787-8787", "신한은행",   "127", "1402", "이영희", "일반", "단독", "59", "변동", "executing", 225000000L, -15, 0, -8, -2, "대출약정 14:00"},
            {"황성민", "010-9898-9898", "하나은행",   "128", "602",  "박민수", "조합", "공동", "71", "고정", "executing", 295000000L, -14, 0, -8, -1, null},
            {"백수아", "010-1313-1414", "KB국민은행", "129", "1101", "최현우", "조합", "단독", "84", "고정", "executing", 350000000L, -13, 0, -7, -2, null},
            {"전유진", "010-2424-3535", "부산은행",   "130", "1702", "한소영", "일반", "단독", "59", "변동", "executing", 210000000L, -20, 2, -15, -4, null},
            {"류민석", "010-4646-5757", "대구은행",   "131", "802",  "이상민", "조합", "공동", "71", "고정", "executing", 280000000L, -18, 3, -12, -3, "잔금조회 필요"},
            {"김태영", "010-5858-6969", "하나은행",   "132", "1502", "박민수", "일반", "단독", "71", "변동", "executing", 260000000L, -15, 5, -10, -2, null},

            // ─── 실행완료 (done, 차트용, 10건) ───
            {"이슬기", "010-7979-8080", "신한은행",   "133", "1802", "이영희", "조합", "공동", "84", "고정", "done",   385000000L, -35, -25, -30, -25, null},
            {"박하람", "010-8181-9292", "하나은행",   "134", "1001", "박민수", "조합", "단독", "71", "고정", "done",   310000000L, -30, -22, -26, -22, null},
            {"권지훈", "010-9393-1010", "KB국민은행", "135", "603",  "최현우", "일반", "단독", "59", "변동", "done",   225000000L, -28, -20, -23, -20, null},
            {"신예린", "010-1414-2525", "우리은행",   "136", "1301", "정다은", "조합", "공동", "84", "고정", "done",   375000000L, -25, -18, -21, -18, null},
            {"오상현", "010-2626-3737", "NH농협은행", "137", "1403", "김영주", "조합", "단독", "71", "고정", "done",   285000000L, -22, -15, -19, -15, null},
            {"김민서", "010-3838-4949", "IBK기업은행","138", "502",  "김태현", "일반", "단독", "59", "변동", "done",   215000000L, -20, -12, -17, -12, null},
            {"홍지훈", "010-4141-5151", "신한은행",   "139", "1502", "이영희", "조합", "단독", "84", "고정", "done",   360000000L, -18, -10, -14, -10, null},
            {"백승호", "010-5252-6262", "하나은행",   "140", "1001", "박민수", "조합", "공동", "71", "고정", "done",   295000000L, -16, -8,  -12, -8, null},
            {"정가람", "010-6363-7373", "KB국민은행", "141", "801",  "최현우", "조합", "단독", "84", "고정", "done",   350000000L, -14, -6,  -10, -6, null},
            {"김재영", "010-7474-8484", "부산은행",   "142", "1702", "한소영", "일반", "단독", "59", "변동", "done",   210000000L, -12, -4,  -8,  -4, null},

            // ─── 취소 (cancel, 2건) ───
            {"조민석", "010-4444-0000", "신한은행",   "143", "601",  "김상훈", "조합", "단독", "84", "고정", "cancel", 320000000L,  -30, null, -25, -20, "고객 사정으로 취소"},
            {"류가영", "010-5858-0000", "하나은행",   "144", "1502", "박민수", "일반", "단독", "71", "변동", "cancel", 260000000L,  -20, null, -15, -10, "심사 부결 후 취소"},
        };

        for (Object[] s : samples) {
            ConsultationRequest r = new ConsultationRequest();
            r.setResident_name((String) s[0]);
            r.setResident_phone((String) s[1]);
            r.setVendor_name((String) s[2]);
            r.setVendor_type("은행");
            r.setComplex_name("창원힐스테이트");
            r.setDong((String) s[3]);
            r.setHo((String) s[4]);
            r.setManager((String) s[5]);
            r.setDivision((String) s[6]);
            r.setOwnership((String) s[7]);
            r.setApt_type((String) s[8]);
            r.setProduct((String) s[9]);
            r.setLoan_status((String) s[10]);
            r.setLoan_amount((Long) s[11]);
            Integer recOffset   = (Integer) s[12];
            Integer execOffset  = (Integer) s[13];
            Integer docOffset   = (Integer) s[14];
            Integer stageOffset = (Integer) s[15];
            if (recOffset != null)   r.setReceive_date(today.plusDays(recOffset));
            if (execOffset != null)  r.setExecution_date(today.plusDays(execOffset));
            if (docOffset != null)   r.setDocument_date(today.plusDays(docOffset));
            if (stageOffset != null) r.setStage_changed_at(OffsetDateTime.now().plusDays(stageOffset));
            r.setMemo((String) s[16]);
            r.setSpecial_notes("SEED"); // 시드 마커
            r.setStatus("대기중");
            r.setPreferred_time("오전");
            r.setLoan_period("30년");
            r.setRepayment_method("원리금균등");
            repository.save(r);
        }

        System.out.println("✅ 샘플 데이터 " + samples.length + "건 삽입 완료 (7단계 파이프라인: apply/consulting/reviewing/result/executing/done/cancel)");
    }
}
