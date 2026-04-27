package com.ipjuon.backend.bankprofile;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 은행 프로필 — 입주민 앱(ipjuon-app)에서 보이는 은행 카드/상세 콘텐츠.
 * 은행 팀장이 v4 [은행 프로필] 화면에서 직접 관리.
 * bank_name 은 vendor.vendor_name 과 매칭.
 */
@Entity
@Table(name = "bank_profiles")
public class BankProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "bank_name", unique = true, nullable = false, length = 50)
    private String bank_name;            // "신한은행", "KB국민은행" 등

    @Column(columnDefinition = "text")
    private String greeting;             // 인사글

    @Column(columnDefinition = "text")
    private String products;             // 취급상품 (잔금대출 종류, 금리 정책 등)

    private String business_hours;       // 영업시간 (예: "평일 09:00~18:00")

    @Column(columnDefinition = "text")
    private String notice;               // 공지사항 (단기 안내)

    @Column(name = "is_closed", nullable = false)
    private Boolean is_closed = false;   // 마감 여부

    private String closing_message;      // 마감 시 표시 메시지 (예: "이번 차수 모집 마감")

    private String contact_phone;        // 대표 연락처
    private String contact_email;        // 대표 이메일

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;            // loginId

    @Column(name = "updated_by_role")
    private String updatedByRole;

    // === Getters / Setters ===
    public UUID getId() { return id; }
    public String getBank_name() { return bank_name; }
    public void setBank_name(String v) { this.bank_name = v; }

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
