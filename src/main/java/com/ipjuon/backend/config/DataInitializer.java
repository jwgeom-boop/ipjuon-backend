package com.ipjuon.backend.config;

import com.ipjuon.backend.bankprofile.BankProfile;
import com.ipjuon.backend.bankprofile.BankProfileRepository;
import com.ipjuon.backend.complex.ComplexTemplate;
import com.ipjuon.backend.complex.ComplexTemplateAptFee;
import com.ipjuon.backend.complex.ComplexTemplateAptFeeRepository;
import com.ipjuon.backend.complex.ComplexTemplateRepository;
import com.ipjuon.backend.consultation.ConsultationRequest;
import com.ipjuon.backend.consultation.ConsultationRepository;
import com.ipjuon.backend.vendor.Vendor;
import com.ipjuon.backend.vendor.VendorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ConsultationRepository repository;
    private final VendorRepository vendorRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ComplexTemplateRepository complexTemplateRepository;
    private final ComplexTemplateAptFeeRepository complexTemplateAptFeeRepository;
    private final BankProfileRepository bankProfileRepository;

    public DataInitializer(ConsultationRepository repository,
                           VendorRepository vendorRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           ComplexTemplateRepository complexTemplateRepository,
                           ComplexTemplateAptFeeRepository complexTemplateAptFeeRepository,
                           BankProfileRepository bankProfileRepository) {
        this.repository = repository;
        this.vendorRepository = vendorRepository;
        this.passwordEncoder = passwordEncoder;
        this.complexTemplateRepository = complexTemplateRepository;
        this.complexTemplateAptFeeRepository = complexTemplateAptFeeRepository;
        this.bankProfileRepository = bankProfileRepository;
    }

    // @Transactional: initComplexTemplates() 의 @Modifying deleteByTemplateId 는
    // 활성 트랜잭션 내에서만 실행 가능. CommandLineRunner.run() 은 Spring proxy 를 통해 호출되므로
    // 여기에 붙이면 모든 시드 작업이 단일 트랜잭션에서 안전하게 수행된다.
    @Override
    @Transactional
    public void run(String... args) {
        initBankVendors();
        initBankConsultants();
        initComplexTemplates();
        initBankProfiles();
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
            v.setRole("bank_manager");
            v.setParentVendorId(null);
            v.setIsActive(true);
            v.setMustChangePassword(false);
            vendorRepository.save(v);
        }
        System.out.println("✅ 은행 계정 " + banks.length + "개 리셋 완료 (status=active, 기본 비밀번호)");
    }

    // ── 은행 상담사 계정 초기화 ──
    // 명명 규칙: 팀장 = `{은행키}`, 상담사 = `{은행키}{NN}` (예: shinhan01)
    // 프론트 parseBankLoginId() 정규식 ^([a-z]+)(\d{2})$ 와 일치해야 함
    private void initBankConsultants() {
        // {bankKey, vendorName, phone}
        String[][] banks = {
            {"shinhan", "신한은행",    "02-1599-8000"},
            {"hana",    "하나은행",    "02-1599-1111"},
            {"kb",      "KB국민은행",  "02-1588-9999"},
            {"woori",   "우리은행",    "02-1588-5000"},
            {"nh",      "NH농협은행",  "02-1588-2100"},
            {"ibk",     "IBK기업은행", "02-1588-2588"},
            {"busan",   "부산은행",    "02-1588-6200"},
            {"daegu",   "대구은행",    "02-1588-5050"},
        };
        // 프론트 DEMO_ASSIGNEE_BY_NO 매핑과 일치
        String[] consultantNames = {"김주임", "이대리", "박과장"};

        int seeded = 0;
        for (String[] b : banks) {
            // 팀장 vendor 조회 (parent_vendor_id 세팅용)
            Vendor manager = vendorRepository.findByLoginId(b[0]).orElse(null);
            UUID managerId = manager != null ? manager.getId() : null;
            for (int i = 1; i <= consultantNames.length; i++) {
                String loginId = String.format("%s%02d", b[0], i);
                String displayName = consultantNames[i - 1];
                Vendor v = vendorRepository.findByLoginId(loginId).orElseGet(Vendor::new);
                v.setLoginId(loginId);
                v.setPassword(passwordEncoder.encode(loginId + "_2024!"));
                v.setVendorName(b[1]);
                v.setVendorType("은행상담사");
                v.setBankManager(displayName); // 담당자 표시명
                v.setPhone(b[2]);
                v.setStatus("active");
                v.setRole("bank_consultant");
                v.setParentVendorId(managerId);
                v.setIsActive(true);
                v.setMustChangePassword(false); // 데모 단계에선 변경 강제 X
                vendorRepository.save(v);
                seeded++;
            }
        }
        System.out.println("✅ 은행 상담사 계정 " + seeded + "개 리셋 완료 (예: shinhan01 / shinhan01_2024!)");
    }

    // ── 단지 템플릿 시드 (입주안내문 정보) ──
    // 6개 단지 등록: 잠실 미성크로바(실데이터) + 5개 가상.
    // 매 부팅마다 갱신 (idempotent). 평형별 관리비 예치금도 같이 등록.
    private void initComplexTemplates() {
        Object[][] complexes = {
            // {complex_name, mgmt_bank, mgmt_account, mgmt_holder, mgmt_office_phone, mgmt_office_fax,
            //  general_option_bank, general_option_account, general_option_holder, stamp_duty}
            {"잠실 미성크로바", "KB 국민은행", "064601-04-131949", "(주)케이티팝스",
             "02-6956-6338", "02-6956-6339",
             "국민", "465101-01-311967", "롯데건설㈜", 75000L},
            {"봄여름가을겨울3차", "농협", "356-1102-3344-55", "관리사무소",
             "051-891-1235", "051-256-3766",
             "국민은행", "101437-04-002570", "시행사", 75000L},
            {"포항학산더휴", "KB국민은행", "064-12-345678", "포항학산관리㈜",
             "054-275-3300", "054-275-3301",
             "신한은행", "110-456-789012", "더휴개발㈜", 75000L},
            {"자이더테라스파크", "신한은행", "110-456-789012", "테라스관리㈜",
             "031-555-7000", "031-555-7001",
             "우리은행", "1002-345-678901", "GS건설㈜", 75000L},
            {"힐스테이트포레스타", "하나은행", "234-910283-67890", "포레스타관리㈜",
             "032-555-2200", "032-555-2201",
             "KB국민은행", "987-654-321012", "현대건설㈜", 75000L},
            {"창원 힐스테이트 마크로엔", "NH농협", "301-0123-4567-89", "마크로엔관리㈜",
             "055-265-1100", "055-265-1101",
             "KB국민은행", "765-432-101234", "현대건설㈜", 75000L},
        };

        // 평형별 관리비 예치금 — 단지명 기준
        Map<String, long[][]> aptFeesByComplex = new HashMap<>();
        // 잠실 미성크로바: PDF 17개 평형 그대로
        aptFeesByComplex.put("잠실 미성크로바", new long[][]{
            {45, 307000}, {51, 346000},
            // 59A/59B/74A/74B/74C 등 영문 suffix는 string 처리 — 별도 메서드로
        });
        // 위 long[][]은 숫자만 가능 → 별도 List<Object[]>로 처리
        Map<String, Object[][]> aptFees = new HashMap<>();
        aptFees.put("잠실 미성크로바", new Object[][]{
            {"45", 307000L}, {"51", 346000L}, {"59A", 377000L}, {"59B", 379000L},
            {"74A", 453000L}, {"74B", 456000L}, {"74C", 457000L},
            {"84A", 505000L}, {"84B", 506000L}, {"84C", 511000L},
            {"95", 561000L}, {"106", 618000L}, {"126", 729000L},
            {"129A", 779000L}, {"129B", 781000L}, {"134", 807000L}, {"145", 872000L},
        });
        aptFees.put("봄여름가을겨울3차", new Object[][]{
            {"84", 350000L},
        });
        aptFees.put("포항학산더휴", new Object[][]{
            {"59", 280000L}, {"74", 350000L}, {"84", 420000L},
        });
        aptFees.put("자이더테라스파크", new Object[][]{
            {"84", 430000L}, {"99", 510000L}, {"116", 620000L},
        });
        aptFees.put("힐스테이트포레스타", new Object[][]{
            {"84", 410000L}, {"102", 530000L}, {"118", 580000L},
        });
        aptFees.put("창원 힐스테이트 마크로엔", new Object[][]{
            {"75", 380000L}, {"84", 430000L}, {"99", 520000L},
        });

        int seeded = 0;
        for (Object[] c : complexes) {
            String name = (String) c[0];
            ComplexTemplate t = complexTemplateRepository.findByComplex_name(name).orElseGet(ComplexTemplate::new);
            t.setComplex_name(name);
            t.setMgmt_fee_bank((String) c[1]);
            t.setMgmt_fee_account((String) c[2]);
            t.setMgmt_fee_holder((String) c[3]);
            t.setMgmt_fee_timing("입주증 발급 전 (미납 시 키불출 불가)");
            t.setMgmt_office_location("단지 내 관리사무소");
            t.setMgmt_office_phone((String) c[4]);
            t.setMgmt_office_fax((String) c[5]);
            t.setMgmt_office_open_date("입주 1개월 전부터 문의 가능");
            t.setPayment_methods("① 무통장입금, 인터넷뱅킹, 모바일뱅킹 (현장 직접수납 불가) ② 동호수 및 성명기재");
            t.setPayment_notes("예시: 101동 101호 홍길동 → 1010101홍길동 (동·호·이름순, 공백제거)");
            t.setGeneral_balance_note("공급계약서 1조 ⓒ항 가상계좌번호 확인");
            t.setGeneral_balance_holder(name + " 시행사");
            t.setGeneral_option_bank((String) c[6]);
            t.setGeneral_option_account((String) c[7]);
            t.setGeneral_option_holder((String) c[8]);
            t.setMiddle_loan_note("해당은행에서 상환금액 확인 후 직접상환 (중도금대출세대에 한함)");
            t.setStamp_duty((Long) c[9]);
            t.setGuarantee_fee_rate(new BigDecimal("0.0030"));   // 0.3% 가정
            t.setMiddle_loan_rate(new BigDecimal("0.0450"));     // 4.5% 가정
            t.setCreatedBy("ipjuon");
            t.setUpdatedBy("ipjuon");
            t.setUpdatedByRole("admin");
            ComplexTemplate saved = complexTemplateRepository.save(t);

            // 평형별 금액 — 매 부팅마다 갱신 (delete + reinsert)
            complexTemplateAptFeeRepository.deleteByTemplateId(saved.getId());
            Object[][] fees = aptFees.getOrDefault(name, new Object[0][]);
            int order = 0;
            for (Object[] f : fees) {
                ComplexTemplateAptFee fee = new ComplexTemplateAptFee();
                fee.setTemplate_id(saved.getId());
                fee.setApt_type((String) f[0]);
                fee.setMgmt_fee_amount((Long) f[1]);
                fee.setDisplay_order(order++);
                complexTemplateAptFeeRepository.save(fee);
            }
            seeded++;
        }
        System.out.println("✅ 단지 템플릿 " + seeded + "개 시드 완료 (잠실 미성크로바 실데이터 + 5개 가상)");
    }

    // ── 은행 프로필 시드 (입주민 앱 카드 콘텐츠) ──
    // 8개 은행에 기본 인사글/취급상품/영업시간 등록.
    // 매 부팅마다 갱신 (idempotent) - 운영 데이터 덮어쓰지 않게 빈 필드만 채움.
    private void initBankProfiles() {
        Object[][] profiles = {
            // {bankName, greeting, products, business_hours, contact_phone}
            {"신한은행",
             "안녕하세요, 신한은행 입주ON 전담팀입니다. 빠르고 정확한 잔금대출 상담을 약속드립니다.",
             "잔금대출 (고정/변동), 추가대출, 보증보험 (HUG/HF), 중도금대출 상환",
             "평일 09:00~18:00 (점심 12:00~13:00 제외)",
             "02-1599-8000"},
            {"하나은행",
             "하나은행 입주잔금팀입니다. 입주민 여러분의 안정적인 자금 마련을 도와드립니다.",
             "잔금대출, 모기지론, 추가대출, 신용대출 연계 상품",
             "평일 09:00~18:00",
             "02-1599-1111"},
            {"KB국민은행",
             "KB국민은행 부전동지점 입주잔금 전담입니다. 친절하고 신속한 상담드립니다.",
             "잔금대출 (고정/변동), 신혼부부 우대상품, 다자녀 우대",
             "평일 09:00~18:00",
             "02-1588-9999"},
            {"우리은행",
             "우리은행 입주ON 전담팀입니다. 한눈에 보는 맞춤 대출 안내를 드립니다.",
             "잔금대출, 보금자리론 연계, 우대금리 상담",
             "평일 09:00~18:00",
             "02-1588-5000"},
            {"NH농협은행",
             "NH농협은행 입주잔금팀입니다. 전국 단지 대출 경험을 바탕으로 도와드립니다.",
             "잔금대출, 농업인 우대 상품, 청년·신혼 우대",
             "평일 09:00~18:00",
             "02-1588-2100"},
            {"IBK기업은행",
             "IBK기업은행 입주잔금팀입니다. 직장인 우대 상품으로 빠르게 처리해드립니다.",
             "잔금대출, 직장인 우대, IBK 멤버십 우대금리",
             "평일 09:00~18:00",
             "02-1588-2588"},
            {"부산은행",
             "부산은행 입주잔금팀입니다. 부산·경남 지역 단지 전문 상담드립니다.",
             "잔금대출, 지역 우대 상품, 동백전 연계 우대",
             "평일 09:00~18:00",
             "02-1588-6200"},
            {"대구은행",
             "대구은행 입주잔금팀입니다. 대구·경북 지역 단지 전문 상담드립니다.",
             "잔금대출, IM뱅크 연계, 지역 우대",
             "평일 09:00~18:00",
             "02-1588-5050"},
        };

        int seeded = 0;
        for (Object[] p : profiles) {
            String name = (String) p[0];
            BankProfile bp = bankProfileRepository.findByBank_name(name).orElseGet(BankProfile::new);
            bp.setBank_name(name);
            // 기존 운영 데이터 덮어쓰지 않게 빈 필드만 채움
            if (bp.getGreeting() == null || bp.getGreeting().isEmpty())             bp.setGreeting((String) p[1]);
            if (bp.getProducts() == null || bp.getProducts().isEmpty())             bp.setProducts((String) p[2]);
            if (bp.getBusiness_hours() == null || bp.getBusiness_hours().isEmpty()) bp.setBusiness_hours((String) p[3]);
            if (bp.getContact_phone() == null || bp.getContact_phone().isEmpty())   bp.setContact_phone((String) p[4]);
            if (bp.getIs_closed() == null) bp.setIs_closed(false);
            if (bp.getUpdatedBy() == null) {
                bp.setUpdatedBy("ipjuon");
                bp.setUpdatedByRole("admin");
            }
            bankProfileRepository.save(bp);
            seeded++;
        }
        System.out.println("✅ 은행 프로필 " + seeded + "개 시드 완료 (인사글/취급상품/영업시간 - 빈 필드만 채움)");
    }

    // ── 샘플 상담 데이터 초기화 ──
    // SEED 마커가 붙은 데이터는 매 시작마다 삭제 후 재삽입 (manager 로테이션 + assignee_vendor_id 백필 보장).
    // SEED 마커가 없는 운영 데이터는 보존.
    //
    // 생성 규칙: 8은행 × 3상담사 × 9템플릿(7활성+2완료) + 은행당 취소 1건 = 224건
    // → 각 상담사: 7건 활성 + 2건 완료 (HomeInbox 데모용 충분한 양)
    // → 각 팀장(은행): 21건 활성 + 6건 완료 + 1건 취소
    // → 6개 단지에 분산 (단지별 평형은 시드 단지의 apt_fees 와 매칭)
    private void initSampleConsultations() {
        repository.findAllBankConsultations().stream()
                .filter(r -> "SEED".equals(r.getSpecial_notes()))
                .forEach(r -> repository.deleteById(r.getId()));

        LocalDate today = LocalDate.now();

        String[] BANK_NAMES = {
            "신한은행", "하나은행", "KB국민은행", "우리은행",
            "NH농협은행", "IBK기업은행", "부산은행", "대구은행"
        };
        String[] CONSULTANTS = {"김주임", "이대리", "박과장"};

        // 한국 이름 풀 (성 20 × 이름 30, gcd(7,30)=1 / gcd(13,20)=1로 균등 분포)
        String[] LAST_NAMES = {"김","이","박","최","정","강","조","윤","장","임","한","오","서","신","권","황","안","송","전","홍"};
        String[] FIRST_NAMES = {
            "민준","서연","도윤","지우","서준","지유","주원","하은","예준","수아",
            "시우","지민","건우","유나","현우","예린","우진","다은","지호","수빈",
            "선우","예나","민서","지윤","준서","서영","지환","유진","동현","가은"
        };

        // 단계 템플릿: {loan_status, recOffset, execOffset, docOffset, stageOffset, memo}
        Object[][] STAGE_TEMPLATES = {
            {"apply",                -1,   null, null, -1,  "신규 접수, 상담 예약 필요"},
            {"consulting",           -4,   null, null, -2,  "서류 안내 중"},
            {"reviewing",            -9,   null, -4,   -3,  null},
            {"result",               -12,  null, -8,   -1,  "승인, 고객 통보 필요"},
            {"signing_reservation",  -14,  null, -10,  -2,  "자서일 확정"},
            {"signing",              -16,  null, -12,  -1,  "자서 완료, 실행 대기"},
            {"executing",            -18,  0,    -10,  -2,  "오늘 실행 예정"},
            {"done",                 -32,  -22,  -27,  -22, null},
            {"done",                 -45,  -35,  -40,  -35, null}
        };

        String[] DIVISIONS  = {"조합", "일반"};
        String[] OWNERSHIPS = {"단독", "공동"};
        String[] PRODUCTS   = {"고정", "변동"};

        // 6개 단지에 분산 (단지명 + 평형별 매핑은 initComplexTemplates() 의 시드와 1:1 매칭)
        String[] COMPLEX_NAMES = {
            "잠실 미성크로바", "봄여름가을겨울3차", "포항학산더휴",
            "자이더테라스파크", "힐스테이트포레스타", "창원 힐스테이트 마크로엔"
        };
        String[][] COMPLEX_APT_TYPES = {
            {"59A", "74A", "84A"},  // 잠실 (대표 평형 3종)
            {"84"},                  // 봄여름가을겨울3차 (단일)
            {"59", "74", "84"},      // 포항학산더휴
            {"84", "99", "116"},     // 자이더테라스파크
            {"84", "102", "118"},    // 힐스테이트포레스타
            {"75", "84", "99"}       // 창원
        };

        Map<String, UUID> consultantIdCache = new HashMap<>();
        int seq = 0;

        for (String bankName : BANK_NAMES) {
            for (String displayName : CONSULTANTS) {
                final String fBankName = bankName;
                final String fDisplayName = displayName;
                UUID consultantId = consultantIdCache.computeIfAbsent(bankName + "|" + displayName, k ->
                    vendorRepository.findByVendorNameAndBankManager(fBankName, fDisplayName)
                            .map(Vendor::getId).orElse(null));

                for (Object[] tpl : STAGE_TEMPLATES) {
                    String name  = LAST_NAMES[(seq * 13) % LAST_NAMES.length]
                                 + FIRST_NAMES[(seq * 7) % FIRST_NAMES.length];
                    String phone = String.format("010-%04d-%04d",
                                                 1000 + (seq * 13) % 9000,
                                                 1000 + (seq * 17) % 9000);
                    int complexIdx = seq % COMPLEX_NAMES.length;
                    String complexName = COMPLEX_NAMES[complexIdx];
                    String[] aptOptions = COMPLEX_APT_TYPES[complexIdx];
                    String aptType = aptOptions[seq % aptOptions.length];
                    // 단지별로 동 번호도 분리 (단지 인덱스 × 100 + 동)
                    String dong  = String.valueOf(101 + complexIdx * 50 + (seq % 20));
                    String ho    = String.valueOf((1 + seq % 18) * 100 + (1 + seq % 4));
                    long amount  = 200_000_000L + ((seq % 20) * 10_000_000L);

                    ConsultationRequest r = new ConsultationRequest();
                    r.setResident_name(name);
                    r.setResident_phone(phone);
                    r.setVendor_name(bankName);
                    r.setVendor_type("은행");
                    r.setComplex_name(complexName);
                    r.setDong(dong);
                    r.setHo(ho);
                    r.setManager(displayName);
                    r.setAssignee_vendor_id(consultantId);
                    r.setDivision(DIVISIONS[seq % 2]);
                    r.setOwnership(OWNERSHIPS[(seq / 2) % 2]);
                    r.setApt_type(aptType);
                    r.setProduct(PRODUCTS[(seq / 3) % 2]);
                    r.setLoan_status((String) tpl[0]);
                    r.setLoan_amount(amount);

                    Integer recOffset   = (Integer) tpl[1];
                    Integer execOffset  = (Integer) tpl[2];
                    Integer docOffset   = (Integer) tpl[3];
                    Integer stageOffset = (Integer) tpl[4];
                    if (recOffset != null)   r.setReceive_date(today.plusDays(recOffset));
                    if (execOffset != null)  r.setExecution_date(today.plusDays(execOffset));
                    if (docOffset != null)   r.setDocument_date(today.plusDays(docOffset));
                    if (stageOffset != null) r.setStage_changed_at(OffsetDateTime.now().plusDays(stageOffset));

                    r.setMemo((String) tpl[5]);
                    r.setSpecial_notes("SEED");
                    r.setStatus("대기중");
                    r.setPreferred_time("오전");
                    r.setLoan_period("30년");
                    r.setRepayment_method("원리금균등");
                    repository.save(r);
                    seq++;
                }
            }
        }

        // 은행당 취소 1건 (상담사 로테이션)
        int cancelIdx = 0;
        for (String bankName : BANK_NAMES) {
            String displayName = CONSULTANTS[cancelIdx % CONSULTANTS.length];
            UUID consultantId = consultantIdCache.get(bankName + "|" + displayName);

            String name = LAST_NAMES[(seq * 13) % LAST_NAMES.length]
                        + FIRST_NAMES[(seq * 7) % FIRST_NAMES.length];

            // 취소 건도 6개 단지 로테이션 (cancelIdx 0~7 → 단지 0~5 + 0~1)
            int cancelComplexIdx = cancelIdx % COMPLEX_NAMES.length;
            String cancelComplexName = COMPLEX_NAMES[cancelComplexIdx];
            String[] cancelAptOptions = COMPLEX_APT_TYPES[cancelComplexIdx];
            String cancelAptType = cancelAptOptions[0]; // 단지의 첫 평형

            ConsultationRequest r = new ConsultationRequest();
            r.setResident_name(name);
            r.setResident_phone(String.format("010-%04d-9999", 1000 + cancelIdx * 100));
            r.setVendor_name(bankName);
            r.setVendor_type("은행");
            r.setComplex_name(cancelComplexName);
            r.setDong(String.valueOf(150 + cancelIdx));
            r.setHo("1801");
            r.setManager(displayName);
            r.setAssignee_vendor_id(consultantId);
            r.setDivision(DIVISIONS[cancelIdx % 2]);
            r.setOwnership("단독");
            r.setApt_type(cancelAptType);
            r.setProduct("고정");
            r.setLoan_status("cancel");
            r.setLoan_amount(280_000_000L);
            r.setReceive_date(today.minusDays(25));
            r.setStage_changed_at(OffsetDateTime.now().minusDays(15));
            r.setMemo(cancelIdx % 2 == 0 ? "고객 사정으로 취소" : "심사 부결 후 취소");
            r.setSpecial_notes("SEED");
            r.setStatus("대기중");
            r.setPreferred_time("오전");
            r.setLoan_period("30년");
            r.setRepayment_method("원리금균등");
            repository.save(r);
            cancelIdx++;
            seq++;
        }

        int total = BANK_NAMES.length * CONSULTANTS.length * STAGE_TEMPLATES.length + BANK_NAMES.length;
        System.out.println("✅ 샘플 데이터 " + total + "건 삽입 완료 (은행 "
            + BANK_NAMES.length + " × 상담사 " + CONSULTANTS.length
            + " × 단계 " + STAGE_TEMPLATES.length + " + 취소 " + BANK_NAMES.length + ")");
    }

    // 사용하지 않는 옛 하드코딩 시드 (참고용 보존, 실제 호출 안 함)
    @SuppressWarnings("unused")
    private void initSampleConsultationsLegacy() {
        LocalDate today = LocalDate.now();
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

            // ─── 자서예약 (signing_reservation, 자서일 확정, 5건) ───
            {"이수빈", "010-1515-1616", "신한은행",   "145", "702",  "이영희", "조합", "단독", "84", "고정", "signing_reservation", 360000000L, -16, null, -12, -2, "자서일 확정, 서류 준비"},
            {"박도현", "010-1717-1818", "하나은행",   "146", "1203", "박민수", "조합", "공동", "71", "고정", "signing_reservation", 300000000L, -15, null, -11, -3, null},
            {"최유나", "010-1919-2020", "KB국민은행", "147", "1501", "최현우", "일반", "단독", "59", "변동", "signing_reservation", 240000000L, -14, null, -10, -1, "자서일 D-3"},
            {"윤태민", "010-2121-2323", "우리은행",   "148", "902",  "정다은", "조합", "단독", "84", "고정", "signing_reservation", 355000000L, -13, null, -9, -4, "정체: 고객 일정 재조율"},
            {"장미래", "010-2424-2626", "NH농협은행", "149", "1102", "김영주", "조합", "공동", "71", "고정", "signing_reservation", 290000000L, -12, null, -8, 0, null},

            // ─── 자서 (signing, 자서 진행 중/완료, 4건) ───
            {"임성호", "010-2727-2929", "신한은행",   "150", "1602", "이영희", "조합", "단독", "84", "고정", "signing", 370000000L, -18, null, -14, -1, "자서 완료, 실행 대기"},
            {"강보라", "010-3030-3232", "하나은행",   "151", "801",  "박민수", "일반", "공동", "71", "변동", "signing", 270000000L, -17, null, -13, -2, null},
            {"오세영", "010-3333-3535", "IBK기업은행","152", "1401", "김태현", "조합", "단독", "84", "고정", "signing", 345000000L, -16, null, -12, -1, null},
            {"송지아", "010-3636-3838", "KB국민은행", "153", "603",  "최현우", "조합", "단독", "59", "고정", "signing", 230000000L, -15, null, -11, -3, "정체: 자서 후 서류 보완 중"},

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

        // 데모용: 시드 데이터의 manager를 상담사 displayName(김주임/이대리/박과장)으로 로테이션 덮어쓰기.
        // 프론트 lockAssignee가 본인(예: shinhan01 → 김주임) 건만 필터하려면 manager가 displayName이어야 함.
        String[] CONSULTANT_NAMES = {"김주임", "이대리", "박과장"};
        // assignee_vendor_id 백필용 캐시: (vendorName, displayName) -> vendor.id
        Map<String, UUID> consultantIdCache = new HashMap<>();
        int rowIdx = 0;
        for (Object[] s : samples) {
            ConsultationRequest r = new ConsultationRequest();
            r.setResident_name((String) s[0]);
            r.setResident_phone((String) s[1]);
            r.setVendor_name((String) s[2]);
            r.setVendor_type("은행");
            r.setComplex_name("창원힐스테이트");
            r.setDong((String) s[3]);
            r.setHo((String) s[4]);
            String displayName = CONSULTANT_NAMES[rowIdx++ % CONSULTANT_NAMES.length];
            r.setManager(displayName);
            // assignee_vendor_id FK 세팅
            String bankName = (String) s[2];
            String cacheKey = bankName + "|" + displayName;
            UUID consultantId = consultantIdCache.computeIfAbsent(cacheKey, k ->
                vendorRepository.findByVendorNameAndBankManager(bankName, displayName)
                        .map(Vendor::getId)
                        .orElse(null)
            );
            r.setAssignee_vendor_id(consultantId);
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

        System.out.println("✅ 샘플 데이터 " + samples.length + "건 삽입 완료 (v3 9단계: apply/consulting/reviewing/result/signing_reservation/signing/executing/done/cancel)");
    }
}

