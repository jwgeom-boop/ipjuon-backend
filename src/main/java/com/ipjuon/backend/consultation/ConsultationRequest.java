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
    private String manager;           // 담당
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
    private String loan_status;       // 대출상태 (wait/done/cancel)

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
    public String getManager() { return manager; }
    public void setManager(String v) { this.manager = v; }
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
}
