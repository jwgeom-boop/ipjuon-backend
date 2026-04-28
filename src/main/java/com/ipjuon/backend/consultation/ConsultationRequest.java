package com.ipjuon.backend.consultation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "consultation_requests")
public class ConsultationRequest {

    @Id
    @GeneratedValue
    private UUID id;

    // 기본 상담 정보 (앱에서 수신)
    private String resident_name;
    private String resident_phone;
    private String preferred_time;
    private String vendor_name;
    private String vendor_type;
    private String complex_name;
    private String unit_number;
    private String consultation_date;
    private String apt_name;
    private String contractor;
    private String sale_price;
    private String credit_loan;
    private String collateral_loan;
    private String desired_loan;
    private String income;
    private String desired_date;
    private String notes;
    private String status = "대기중";
    private String memo;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // 은행 상담사 입력 필드
    private String resident_no;       // 주민등록번호 (채무자)
    private String manager;           // 담당 (표시명, legacy — assignee_vendor_id 우선)

    @Column(name = "assignee_vendor_id")
    private UUID assignee_vendor_id;  // 담당 상담사 vendor.id FK (2026-04-24)
    private String transfer_date;     // 전매일
    private String division;          // 구분 (조합/일반)
    private String ownership;         // 명의 (단독/공동)
    private String dong;              // 동
    private String ho;                // 호수
    private LocalDate execution_date; // 실행일
    private Long loan_amount;         // 대출신청금
    private LocalDate receive_date;   // 접수일
    private LocalDate document_date;  // 서류전달일
    private String apt_type;          // 타입 (59/71/84 등)
    private String product;           // 상품 (고정/변동)
    private String repayment_method;  // 상환방식
    private String loan_period;       // 기간
    private String deferment;         // 거치
    private String joint_owner_name;  // 공동명의자
    private String joint_owner_rrn;   // 공동명의자 주민번호
    private String joint_owner_tel;   // 공동명의자 연락처
    private Long customer_deposit;    // 고객준비금
    private String deferred_interest_account; // 후불이자계좌
    private String balance_account;   // 잔금계좌
    private String option_account;    // 옵션계좌
    private String interim_virtual_account;   // 중도금가상계좌
    private String special_notes;     // 불비/특이사항
    private String loan_status;       // 대출상태 (apply/consulting/reviewing/result/executing/done/cancel)

    @Column(name = "stage_changed_at")
    private OffsetDateTime stage_changed_at; // 현재 단계 진입 시각 (체류일 계산용)

    @Column(name = "documents_checked", columnDefinition = "text")
    private String documents_checked; // 체크된 서류 목록 (쉼표 구분 key: idcard,resident_cert,...)

    @Column(name = "consultation_checks", columnDefinition = "text")
    private String consultation_checks; // 상담 체크리스트 (spec §7)

    // 상담 시 확인 정보
    private String existing_homes;         // 주택보유 상태 (무주택/생애최초/1주택/2주택/3주택이상)
    private Long existing_credit_loan;     // 기 신용대출 (원)
    private Long existing_collateral_loan; // 기 담보대출 (원)
    private String credit_score_type;      // 신용평가사 (KCB/NICE)
    private Integer credit_score;          // 신용점수 (0-1000)
    private Long sale_price_amount;        // 분양가 (원, 상담사 확정)

    // ===== v3: 6단계 파이프라인 확장 필드 =====
    // 자서예약 단계 필수
    private LocalDate moving_in_date;      // 입주일(이사일) - D-day 계산용
    private String spouse_phone;           // 배우자 연락처

    // 가심사결과 단계
    private Long approved_amount;          // 승인금액 (원)
    private String approved_rate;          // 금리 (예: "4.5%")
    @Column(name = "approved_notified_at")
    private OffsetDateTime approved_notified_at; // 결과 통보 시각
    @Column(name = "customer_accepted_at")
    private OffsetDateTime customer_accepted_at; // 고객 수용 확인

    // 자서 단계
    private LocalDate signing_date;        // 자서일
    private String signing_time;           // 자서 시간 (예: "10:00")

    // 추가 대출 / 은행 담당자
    private Long additional_loan_amount;   // 추가대출 (원)
    private String bank_branch;            // 지점
    private String bank_manager_phone;     // 담당자 전화
    private String bank_manager_fax;       // 담당자 팩스

    // 취소
    private String canceled_reason;        // 취소 사유

    // ===== 대출실행 정산 (6단계 핵심) =====
    // 중도금 (원금 + 이자, 중도금 대출은행 계좌)
    private Long settle_middle_principal;
    private Long settle_middle_interest;
    private String settle_middle_bank;
    private String settle_middle_account;

    // 분양잔금 (원금 + 이자, 시행사 계좌)
    private Long settle_balance_principal;
    private Long settle_balance_interest;
    private String settle_balance_account;

    // 발코니 확장 / 유상옵션 / 보증수수료 (시행사 계좌)
    private Long settle_balcony;            // 발코니 확장
    private Long settle_options;            // 유상옵션
    private Long settle_guarantee_fee;      // 보증수수료(대납이자)

    // 선수관리비 (관리사무소 계좌)
    private Long settle_mgmt_fee;
    private String settle_mgmt_account;

    // 이주비 (이주비 은행 계좌)
    private Long settle_moving_allowance;
    private String settle_moving_bank;
    private String settle_moving_account;

    // 인지대 (정액)
    private Long settle_stamp_duty;              // 인지대(대출)
    private Long settle_stamp_duty_additional;   // 인지대(추가대출)

    // 실행 완료 플래그 (execution 단계 내부 상태)
    private Boolean execution_completed;

    // 타임스탬프 메모 로그 (누적 text, "YYYY-MM-DD HH:mm | user | message\n" 형식)
    @Column(name = "memo_log", columnDefinition = "text")
    private String memo_log;

    // 자서예약 SMS 안내문 마지막 발송(복사) 시각 — 팀장 대시보드에서 발송 누락 추적용
    @Column(name = "last_sms_sent_at")
    private OffsetDateTime last_sms_sent_at;

    // ===== 입주민 보고값 (B2C 앱) =====
    // 중도금이자: 실행일 당일 입주민이 은행에서 확인 후 앱으로 보고 → 상담사가 확인하고 settle_middle_interest 확정
    @Column(name = "reported_middle_interest")
    private Long reported_middle_interest;
    @Column(name = "reported_middle_interest_at")
    private OffsetDateTime reported_middle_interest_at;

    // 입주민 측 준비서류 체크리스트 — 쉼표 구분 doc id (예: "resident-cert,resident-abstr,...")
    @Column(name = "resident_doc_checks", columnDefinition = "text")
    private String resident_doc_checks;
    @Column(name = "resident_doc_checks_at")
    private OffsetDateTime resident_doc_checks_at;

    // 입주민이 앱에서 액션한 가장 최근 시각 (수용/취소/일정선택/이자보고/체크리스트 변경 등)
    // 상담사 사이트의 알림함 카운트 계산용 (lastSeen vs this 비교)
    @Column(name = "resident_last_action_at")
    private OffsetDateTime resident_last_action_at;
    // 마지막 입주민 액션 종류 (notify / display 용)
    private String resident_last_action_type;

    // ===== 자서 일정 캘린더 워크플로 (B2C 앱, v2 — opt-out 방식) =====
    // 표시 기간 (없으면 오늘+3 ~ 오늘+30일 default)
    @Column(name = "signing_window_start")
    private LocalDate signing_window_start;
    @Column(name = "signing_window_end")
    private LocalDate signing_window_end;
    // 상담사가 제외한 날짜 JSON 배열 (예: ["2026-05-12","2026-05-15"])
    @Column(name = "signing_excluded_dates", columnDefinition = "text")
    private String signing_excluded_dates;
    // 가능 시간대 JSON 배열 (예: ["09:00","10:00",...])
    @Column(name = "signing_available_times", columnDefinition = "text")
    private String signing_available_times;
    // 가능 장소 JSON 배열 (예: ["부전동지점 2층","본점 1층"])
    @Column(name = "signing_available_locations", columnDefinition = "text")
    private String signing_available_locations;
    // 입주민이 선택한 일정 (확정 전 임시값)
    @Column(name = "signing_selected_date")
    private LocalDate signing_selected_date;
    private String signing_selected_time;
    private String signing_selected_location_str;
    @Column(name = "signing_selected_at")
    private OffsetDateTime signing_selected_at;
    // 상담사가 최종 확정한 시각 (확정 시 signing_date / signing_time / signing_location 셋팅됨)
    @Column(name = "signing_confirmed_at")
    private OffsetDateTime signing_confirmed_at;
    // 확정된 자서 장소
    private String signing_location;
    // ===== Legacy (M3에서 사용했던 슬롯 모델 — 호환을 위해 유지하되 신규 흐름은 위 필드 사용) =====
    @Column(name = "signing_offered_slots", columnDefinition = "text")
    private String signing_offered_slots;
    @Column(name = "signing_selected_slot_index")
    private Integer signing_selected_slot_index;

    // Getters and Setters - 기본
    public UUID getId() { return id; }
    public String getResident_name() { return resident_name; }
    public void setResident_name(String v) { this.resident_name = v; }
    public String getResident_phone() { return resident_phone; }
    public void setResident_phone(String v) { this.resident_phone = v; }
    public String getPreferred_time() { return preferred_time; }
    public void setPreferred_time(String v) { this.preferred_time = v; }
    public String getVendor_name() { return vendor_name; }
    public void setVendor_name(String v) { this.vendor_name = v; }
    public String getVendor_type() { return vendor_type; }
    public void setVendor_type(String v) { this.vendor_type = v; }
    public String getComplex_name() { return complex_name; }
    public void setComplex_name(String v) { this.complex_name = v; }
    public String getUnit_number() { return unit_number; }
    public void setUnit_number(String v) { this.unit_number = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getMemo() { return memo; }
    public void setMemo(String v) { this.memo = v; }
    @JsonProperty("created_at")
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getConsultation_date() { return consultation_date; }
    public void setConsultation_date(String v) { this.consultation_date = v; }
    public String getApt_name() { return apt_name; }
    public void setApt_name(String v) { this.apt_name = v; }
    public String getContractor() { return contractor; }
    public void setContractor(String v) { this.contractor = v; }
    public String getSale_price() { return sale_price; }
    public void setSale_price(String v) { this.sale_price = v; }
    public String getCredit_loan() { return credit_loan; }
    public void setCredit_loan(String v) { this.credit_loan = v; }
    public String getCollateral_loan() { return collateral_loan; }
    public void setCollateral_loan(String v) { this.collateral_loan = v; }
    public String getDesired_loan() { return desired_loan; }
    public void setDesired_loan(String v) { this.desired_loan = v; }
    public String getIncome() { return income; }
    public void setIncome(String v) { this.income = v; }
    public String getDesired_date() { return desired_date; }
    public void setDesired_date(String v) { this.desired_date = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }

    // Getters and Setters - 은행 상담사 필드
    public String getResident_no() { return resident_no; }
    public void setResident_no(String v) { this.resident_no = v; }
    public String getManager() { return manager; }
    public void setManager(String v) { this.manager = v; }
    public UUID getAssignee_vendor_id() { return assignee_vendor_id; }
    public void setAssignee_vendor_id(UUID v) { this.assignee_vendor_id = v; }
    public String getTransfer_date() { return transfer_date; }
    public void setTransfer_date(String v) { this.transfer_date = v; }
    public String getDivision() { return division; }
    public void setDivision(String v) { this.division = v; }
    public String getOwnership() { return ownership; }
    public void setOwnership(String v) { this.ownership = v; }
    public String getDong() { return dong; }
    public void setDong(String v) { this.dong = v; }
    public String getHo() { return ho; }
    public void setHo(String v) { this.ho = v; }
    public LocalDate getExecution_date() { return execution_date; }
    public void setExecution_date(LocalDate v) { this.execution_date = v; }
    public Long getLoan_amount() { return loan_amount; }
    public void setLoan_amount(Long v) { this.loan_amount = v; }
    public LocalDate getReceive_date() { return receive_date; }
    public void setReceive_date(LocalDate v) { this.receive_date = v; }
    public LocalDate getDocument_date() { return document_date; }
    public void setDocument_date(LocalDate v) { this.document_date = v; }
    public String getApt_type() { return apt_type; }
    public void setApt_type(String v) { this.apt_type = v; }
    public String getProduct() { return product; }
    public void setProduct(String v) { this.product = v; }
    public String getRepayment_method() { return repayment_method; }
    public void setRepayment_method(String v) { this.repayment_method = v; }
    public String getLoan_period() { return loan_period; }
    public void setLoan_period(String v) { this.loan_period = v; }
    public String getDeferment() { return deferment; }
    public void setDeferment(String v) { this.deferment = v; }
    public String getJoint_owner_name() { return joint_owner_name; }
    public void setJoint_owner_name(String v) { this.joint_owner_name = v; }
    public String getJoint_owner_rrn() { return joint_owner_rrn; }
    public void setJoint_owner_rrn(String v) { this.joint_owner_rrn = v; }
    public String getJoint_owner_tel() { return joint_owner_tel; }
    public void setJoint_owner_tel(String v) { this.joint_owner_tel = v; }
    public Long getCustomer_deposit() { return customer_deposit; }
    public void setCustomer_deposit(Long v) { this.customer_deposit = v; }
    public String getDeferred_interest_account() { return deferred_interest_account; }
    public void setDeferred_interest_account(String v) { this.deferred_interest_account = v; }
    public String getBalance_account() { return balance_account; }
    public void setBalance_account(String v) { this.balance_account = v; }
    public String getOption_account() { return option_account; }
    public void setOption_account(String v) { this.option_account = v; }
    public String getInterim_virtual_account() { return interim_virtual_account; }
    public void setInterim_virtual_account(String v) { this.interim_virtual_account = v; }
    public String getSpecial_notes() { return special_notes; }
    public void setSpecial_notes(String v) { this.special_notes = v; }
    public String getLoan_status() { return loan_status; }
    public void setLoan_status(String v) { this.loan_status = v; }
    @JsonProperty("stage_changed_at")
    public OffsetDateTime getStage_changed_at() { return stage_changed_at; }
    public void setStage_changed_at(OffsetDateTime v) { this.stage_changed_at = v; }

    public String getDocuments_checked() { return documents_checked; }
    public void setDocuments_checked(String v) { this.documents_checked = v; }

    public String getConsultation_checks() { return consultation_checks; }
    public void setConsultation_checks(String v) { this.consultation_checks = v; }

    public String getExisting_homes() { return existing_homes; }
    public void setExisting_homes(String v) { this.existing_homes = v; }
    public Long getExisting_credit_loan() { return existing_credit_loan; }
    public void setExisting_credit_loan(Long v) { this.existing_credit_loan = v; }
    public Long getExisting_collateral_loan() { return existing_collateral_loan; }
    public void setExisting_collateral_loan(Long v) { this.existing_collateral_loan = v; }
    public String getCredit_score_type() { return credit_score_type; }
    public void setCredit_score_type(String v) { this.credit_score_type = v; }
    public Integer getCredit_score() { return credit_score; }
    public void setCredit_score(Integer v) { this.credit_score = v; }
    public Long getSale_price_amount() { return sale_price_amount; }
    public void setSale_price_amount(Long v) { this.sale_price_amount = v; }

    // ===== v3 확장 필드 Getters/Setters =====
    public LocalDate getMoving_in_date() { return moving_in_date; }
    public void setMoving_in_date(LocalDate v) { this.moving_in_date = v; }
    public String getSpouse_phone() { return spouse_phone; }
    public void setSpouse_phone(String v) { this.spouse_phone = v; }

    public Long getApproved_amount() { return approved_amount; }
    public void setApproved_amount(Long v) { this.approved_amount = v; }
    public String getApproved_rate() { return approved_rate; }
    public void setApproved_rate(String v) { this.approved_rate = v; }
    @JsonProperty("approved_notified_at")
    public OffsetDateTime getApproved_notified_at() { return approved_notified_at; }
    public void setApproved_notified_at(OffsetDateTime v) { this.approved_notified_at = v; }
    @JsonProperty("customer_accepted_at")
    public OffsetDateTime getCustomer_accepted_at() { return customer_accepted_at; }
    public void setCustomer_accepted_at(OffsetDateTime v) { this.customer_accepted_at = v; }

    public LocalDate getSigning_date() { return signing_date; }
    public void setSigning_date(LocalDate v) { this.signing_date = v; }
    public String getSigning_time() { return signing_time; }
    public void setSigning_time(String v) { this.signing_time = v; }

    public Long getAdditional_loan_amount() { return additional_loan_amount; }
    public void setAdditional_loan_amount(Long v) { this.additional_loan_amount = v; }
    public String getBank_branch() { return bank_branch; }
    public void setBank_branch(String v) { this.bank_branch = v; }
    public String getBank_manager_phone() { return bank_manager_phone; }
    public void setBank_manager_phone(String v) { this.bank_manager_phone = v; }
    public String getBank_manager_fax() { return bank_manager_fax; }
    public void setBank_manager_fax(String v) { this.bank_manager_fax = v; }

    public String getCanceled_reason() { return canceled_reason; }
    public void setCanceled_reason(String v) { this.canceled_reason = v; }

    // 정산 필드
    public Long getSettle_middle_principal() { return settle_middle_principal; }
    public void setSettle_middle_principal(Long v) { this.settle_middle_principal = v; }
    public Long getSettle_middle_interest() { return settle_middle_interest; }
    public void setSettle_middle_interest(Long v) { this.settle_middle_interest = v; }
    public String getSettle_middle_bank() { return settle_middle_bank; }
    public void setSettle_middle_bank(String v) { this.settle_middle_bank = v; }
    public String getSettle_middle_account() { return settle_middle_account; }
    public void setSettle_middle_account(String v) { this.settle_middle_account = v; }

    public Long getSettle_balance_principal() { return settle_balance_principal; }
    public void setSettle_balance_principal(Long v) { this.settle_balance_principal = v; }
    public Long getSettle_balance_interest() { return settle_balance_interest; }
    public void setSettle_balance_interest(Long v) { this.settle_balance_interest = v; }
    public String getSettle_balance_account() { return settle_balance_account; }
    public void setSettle_balance_account(String v) { this.settle_balance_account = v; }

    public Long getSettle_balcony() { return settle_balcony; }
    public void setSettle_balcony(Long v) { this.settle_balcony = v; }
    public Long getSettle_options() { return settle_options; }
    public void setSettle_options(Long v) { this.settle_options = v; }
    public Long getSettle_guarantee_fee() { return settle_guarantee_fee; }
    public void setSettle_guarantee_fee(Long v) { this.settle_guarantee_fee = v; }

    public Long getSettle_mgmt_fee() { return settle_mgmt_fee; }
    public void setSettle_mgmt_fee(Long v) { this.settle_mgmt_fee = v; }
    public String getSettle_mgmt_account() { return settle_mgmt_account; }
    public void setSettle_mgmt_account(String v) { this.settle_mgmt_account = v; }

    public Long getSettle_moving_allowance() { return settle_moving_allowance; }
    public void setSettle_moving_allowance(Long v) { this.settle_moving_allowance = v; }
    public String getSettle_moving_bank() { return settle_moving_bank; }
    public void setSettle_moving_bank(String v) { this.settle_moving_bank = v; }
    public String getSettle_moving_account() { return settle_moving_account; }
    public void setSettle_moving_account(String v) { this.settle_moving_account = v; }

    public Long getSettle_stamp_duty() { return settle_stamp_duty; }
    public void setSettle_stamp_duty(Long v) { this.settle_stamp_duty = v; }
    public Long getSettle_stamp_duty_additional() { return settle_stamp_duty_additional; }
    public void setSettle_stamp_duty_additional(Long v) { this.settle_stamp_duty_additional = v; }

    public Boolean getExecution_completed() { return execution_completed; }
    public void setExecution_completed(Boolean v) { this.execution_completed = v; }

    public String getMemo_log() { return memo_log; }
    public void setMemo_log(String v) { this.memo_log = v; }

    @JsonProperty("last_sms_sent_at")
    public OffsetDateTime getLast_sms_sent_at() { return last_sms_sent_at; }
    public void setLast_sms_sent_at(OffsetDateTime v) { this.last_sms_sent_at = v; }

    // 입주민 보고값
    public Long getReported_middle_interest() { return reported_middle_interest; }
    public void setReported_middle_interest(Long v) { this.reported_middle_interest = v; }
    @JsonProperty("reported_middle_interest_at")
    public OffsetDateTime getReported_middle_interest_at() { return reported_middle_interest_at; }
    public void setReported_middle_interest_at(OffsetDateTime v) { this.reported_middle_interest_at = v; }
    public String getResident_doc_checks() { return resident_doc_checks; }
    public void setResident_doc_checks(String v) { this.resident_doc_checks = v; }
    @JsonProperty("resident_doc_checks_at")
    public OffsetDateTime getResident_doc_checks_at() { return resident_doc_checks_at; }
    public void setResident_doc_checks_at(OffsetDateTime v) { this.resident_doc_checks_at = v; }
    @JsonProperty("resident_last_action_at")
    public OffsetDateTime getResident_last_action_at() { return resident_last_action_at; }
    public void setResident_last_action_at(OffsetDateTime v) { this.resident_last_action_at = v; }
    public String getResident_last_action_type() { return resident_last_action_type; }
    public void setResident_last_action_type(String v) { this.resident_last_action_type = v; }

    // 자서 일정 캘린더 (v2)
    public LocalDate getSigning_window_start() { return signing_window_start; }
    public void setSigning_window_start(LocalDate v) { this.signing_window_start = v; }
    public LocalDate getSigning_window_end() { return signing_window_end; }
    public void setSigning_window_end(LocalDate v) { this.signing_window_end = v; }
    public String getSigning_excluded_dates() { return signing_excluded_dates; }
    public void setSigning_excluded_dates(String v) { this.signing_excluded_dates = v; }
    public String getSigning_available_times() { return signing_available_times; }
    public void setSigning_available_times(String v) { this.signing_available_times = v; }
    public String getSigning_available_locations() { return signing_available_locations; }
    public void setSigning_available_locations(String v) { this.signing_available_locations = v; }
    public LocalDate getSigning_selected_date() { return signing_selected_date; }
    public void setSigning_selected_date(LocalDate v) { this.signing_selected_date = v; }
    public String getSigning_selected_time() { return signing_selected_time; }
    public void setSigning_selected_time(String v) { this.signing_selected_time = v; }
    public String getSigning_selected_location_str() { return signing_selected_location_str; }
    public void setSigning_selected_location_str(String v) { this.signing_selected_location_str = v; }
    @JsonProperty("signing_selected_at")
    public OffsetDateTime getSigning_selected_at() { return signing_selected_at; }
    public void setSigning_selected_at(OffsetDateTime v) { this.signing_selected_at = v; }
    @JsonProperty("signing_confirmed_at")
    public OffsetDateTime getSigning_confirmed_at() { return signing_confirmed_at; }
    public void setSigning_confirmed_at(OffsetDateTime v) { this.signing_confirmed_at = v; }
    public String getSigning_location() { return signing_location; }
    public void setSigning_location(String v) { this.signing_location = v; }

    // Legacy
    public String getSigning_offered_slots() { return signing_offered_slots; }
    public void setSigning_offered_slots(String v) { this.signing_offered_slots = v; }
    public Integer getSigning_selected_slot_index() { return signing_selected_slot_index; }
    public void setSigning_selected_slot_index(Integer v) { this.signing_selected_slot_index = v; }
}
