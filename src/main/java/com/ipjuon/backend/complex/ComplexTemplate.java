package com.ipjuon.backend.complex;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 단지(아파트) 입주안내문 템플릿.
 * 한 단지의 분양/관리비/옵션대금/관리사무소 등 모든 은행이 공통으로 쓰는 정보를 1곳에 보관.
 * 평형별로 다른 관리비 예치금 금액은 ComplexTemplateAptFee 에 별도 행으로 저장.
 */
@Entity
@Table(name = "complex_templates")
public class ComplexTemplate {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "complex_name", unique = true, nullable = false, length = 100)
    private String complex_name;

    // ===== 1. 관리비 예치금 (단지 공통) =====
    private String mgmt_fee_bank;          // KB 국민은행
    private String mgmt_fee_account;       // 064601-04-131949
    private String mgmt_fee_holder;        // (주)케이티팝스
    private String mgmt_fee_timing;        // 입주증 발급 전 (미납 시 키불출 불가)

    // ===== 2. 관리사무소 =====
    private String mgmt_office_location;   // 단지 내 관리사무소
    private String mgmt_office_phone;      // 02-6956-6338
    private String mgmt_office_fax;        // 02-6956-6339
    private String mgmt_office_open_date;  // 2026년 1월 12일부터 문의 가능

    // ===== 3. 납부방법 안내 =====
    @Column(columnDefinition = "text")
    private String payment_methods;        // 무통장입금, 인터넷뱅킹, 모바일뱅킹 (현장 직접수납 불가)

    @Column(columnDefinition = "text")
    private String payment_notes;          // 동호수 및 성명기재 / 1일 이체한도 사전 증액

    // ===== 4. 일반 분양대금 =====
    @Column(columnDefinition = "text")
    private String general_balance_note;   // 공급계약서 1조 ⓒ항 가상계좌번호 확인

    private String general_balance_holder; // 잠실 미성크로바아파트주택재건축정비사업조합 / 롯데건설㈜

    // ===== 5. 일반 옵션대금 =====
    private String general_option_bank;
    private String general_option_account; // 465101-01-311967
    private String general_option_holder;  // 롯데건설㈜

    // ===== 6. 조합 분양대금 (재건축 조합 있는 단지) =====
    @Column(columnDefinition = "text")
    private String union_balance_note;
    private String union_balance_holder;

    // ===== 7. 조합 옵션대금 =====
    private String union_option_bank;
    private String union_option_account;   // 086801-01-011822
    private String union_option_holder;

    // ===== 8. 중도금대출 상환 =====
    @Column(columnDefinition = "text")
    private String middle_loan_note;       // 해당은행에서 상환금액 확인 후 직접상환

    // ===== 9. 분양대금 조회 =====
    @Column(length = 500)
    private String sale_price_inquiry_url; // https://www.lottecastle.co.kr

    // ===== 10. 정책 (입주ON 내부) =====
    private Long stamp_duty = 75000L;      // 인지대 (보통 75,000원 고정)
    private BigDecimal guarantee_fee_rate; // 보증수수료율
    private BigDecimal middle_loan_rate;   // 중도금이자율

    // ===== 메타 (충돌 추적) =====
    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;              // loginId

    @Column(name = "updated_by")
    private String updatedBy;              // loginId

    @Column(name = "updated_by_role")
    private String updatedByRole;          // admin / bank_manager / bank_consultant

    @Column(name = "updated_by_bank")
    private String updatedByBank;          // 어느 은행 사람이 마지막 수정했는지 (감사용)

    // === Getters / Setters ===
    public UUID getId() { return id; }
    public String getComplex_name() { return complex_name; }
    public void setComplex_name(String v) { this.complex_name = v; }

    public String getMgmt_fee_bank() { return mgmt_fee_bank; }
    public void setMgmt_fee_bank(String v) { this.mgmt_fee_bank = v; }
    public String getMgmt_fee_account() { return mgmt_fee_account; }
    public void setMgmt_fee_account(String v) { this.mgmt_fee_account = v; }
    public String getMgmt_fee_holder() { return mgmt_fee_holder; }
    public void setMgmt_fee_holder(String v) { this.mgmt_fee_holder = v; }
    public String getMgmt_fee_timing() { return mgmt_fee_timing; }
    public void setMgmt_fee_timing(String v) { this.mgmt_fee_timing = v; }

    public String getMgmt_office_location() { return mgmt_office_location; }
    public void setMgmt_office_location(String v) { this.mgmt_office_location = v; }
    public String getMgmt_office_phone() { return mgmt_office_phone; }
    public void setMgmt_office_phone(String v) { this.mgmt_office_phone = v; }
    public String getMgmt_office_fax() { return mgmt_office_fax; }
    public void setMgmt_office_fax(String v) { this.mgmt_office_fax = v; }
    public String getMgmt_office_open_date() { return mgmt_office_open_date; }
    public void setMgmt_office_open_date(String v) { this.mgmt_office_open_date = v; }

    public String getPayment_methods() { return payment_methods; }
    public void setPayment_methods(String v) { this.payment_methods = v; }
    public String getPayment_notes() { return payment_notes; }
    public void setPayment_notes(String v) { this.payment_notes = v; }

    public String getGeneral_balance_note() { return general_balance_note; }
    public void setGeneral_balance_note(String v) { this.general_balance_note = v; }
    public String getGeneral_balance_holder() { return general_balance_holder; }
    public void setGeneral_balance_holder(String v) { this.general_balance_holder = v; }

    public String getGeneral_option_bank() { return general_option_bank; }
    public void setGeneral_option_bank(String v) { this.general_option_bank = v; }
    public String getGeneral_option_account() { return general_option_account; }
    public void setGeneral_option_account(String v) { this.general_option_account = v; }
    public String getGeneral_option_holder() { return general_option_holder; }
    public void setGeneral_option_holder(String v) { this.general_option_holder = v; }

    public String getUnion_balance_note() { return union_balance_note; }
    public void setUnion_balance_note(String v) { this.union_balance_note = v; }
    public String getUnion_balance_holder() { return union_balance_holder; }
    public void setUnion_balance_holder(String v) { this.union_balance_holder = v; }

    public String getUnion_option_bank() { return union_option_bank; }
    public void setUnion_option_bank(String v) { this.union_option_bank = v; }
    public String getUnion_option_account() { return union_option_account; }
    public void setUnion_option_account(String v) { this.union_option_account = v; }
    public String getUnion_option_holder() { return union_option_holder; }
    public void setUnion_option_holder(String v) { this.union_option_holder = v; }

    public String getMiddle_loan_note() { return middle_loan_note; }
    public void setMiddle_loan_note(String v) { this.middle_loan_note = v; }

    public String getSale_price_inquiry_url() { return sale_price_inquiry_url; }
    public void setSale_price_inquiry_url(String v) { this.sale_price_inquiry_url = v; }

    public Long getStamp_duty() { return stamp_duty; }
    public void setStamp_duty(Long v) { this.stamp_duty = v; }
    public BigDecimal getGuarantee_fee_rate() { return guarantee_fee_rate; }
    public void setGuarantee_fee_rate(BigDecimal v) { this.guarantee_fee_rate = v; }
    public BigDecimal getMiddle_loan_rate() { return middle_loan_rate; }
    public void setMiddle_loan_rate(BigDecimal v) { this.middle_loan_rate = v; }

    @JsonProperty("created_at")
    public OffsetDateTime getCreatedAt() { return createdAt; }
    @JsonProperty("updated_at")
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @JsonProperty("created_by")
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    @JsonProperty("updated_by")
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String v) { this.updatedBy = v; }
    @JsonProperty("updated_by_role")
    public String getUpdatedByRole() { return updatedByRole; }
    public void setUpdatedByRole(String v) { this.updatedByRole = v; }
    @JsonProperty("updated_by_bank")
    public String getUpdatedByBank() { return updatedByBank; }
    public void setUpdatedByBank(String v) { this.updatedByBank = v; }
}
