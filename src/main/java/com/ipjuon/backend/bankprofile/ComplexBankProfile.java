package com.ipjuon.backend.bankprofile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 단지×은행 프로필 — 같은 은행이라도 단지마다 지점/인사글/영업시간 등이 다름.
 * 입주민 앱에서 단지+은행으로 조회 시 우선 사용. 없으면 BankProfile (글로벌) fallback.
 */
@Entity
@Table(
    name = "complex_bank_profiles",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_complex_bank",
        columnNames = {"complex_name", "bank_name"}
    )
)
public class ComplexBankProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "complex_name", nullable = false, length = 100)
    private String complex_name;        // "잠실 미성크로바"

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bank_name;           // "KB국민은행"

    private String branch_name;         // "부전동지점" — 단지 담당 지점

    @Column(columnDefinition = "text")
    private String greeting;            // 인사말 (단지·지점 맞춤)

    @Column(columnDefinition = "text")
    private String products;            // 취급 상품 (디딤돌·신생아특례 등)

    private String business_hours;

    @Column(columnDefinition = "text")
    private String notice;

    @Column(name = "is_closed", nullable = false)
    private Boolean is_closed = false;

    private String closing_message;

    private String contact_phone;
    private String contact_email;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_by_role")
    private String updatedByRole;

    public UUID getId() { return id; }
    public String getComplex_name() { return complex_name; }
    public void setComplex_name(String v) { this.complex_name = v; }
    public String getBank_name() { return bank_name; }
    public void setBank_name(String v) { this.bank_name = v; }
    public String getBranch_name() { return branch_name; }
    public void setBranch_name(String v) { this.branch_name = v; }

    public String getGreeting() { return greeting; }
    public void setGreeting(String v) { this.greeting = v; }
    public String getProducts() { return products; }
    public void setProducts(String v) { this.products = v; }
    public String getBusiness_hours() { return business_hours; }
    public void setBusiness_hours(String v) { this.business_hours = v; }
    public String getNotice() { return notice; }
    public void setNotice(String v) { this.notice = v; }
    public Boolean getIs_closed() { return is_closed; }
    public void setIs_closed(Boolean v) { this.is_closed = v; }
    public String getClosing_message() { return closing_message; }
    public void setClosing_message(String v) { this.closing_message = v; }
    public String getContact_phone() { return contact_phone; }
    public void setContact_phone(String v) { this.contact_phone = v; }
    public String getContact_email() { return contact_email; }
    public void setContact_email(String v) { this.contact_email = v; }

    @JsonProperty("created_at")
    public OffsetDateTime getCreatedAt() { return createdAt; }
    @JsonProperty("updated_at")
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    @JsonProperty("updated_by")
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String v) { this.updatedBy = v; }
    @JsonProperty("updated_by_role")
    public String getUpdatedByRole() { return updatedByRole; }
    public void setUpdatedByRole(String v) { this.updatedByRole = v; }
}
