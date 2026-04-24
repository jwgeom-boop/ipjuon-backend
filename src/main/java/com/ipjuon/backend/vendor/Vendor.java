package com.ipjuon.backend.vendor;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_accounts")
public class Vendor {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "vendor_type")
    private String vendorType;

    @Column(name = "login_id")
    private String loginId;

    private String password;
    private String phone;
    private String fax;

    @Column(name = "bank_manager")
    private String bankManager;

    private String status = "active";

    // 역할 분리 (2026-04-24): bank_manager / bank_consultant / null(legacy)
    private String role;

    // 상담사 → 팀장 FK (팀장 본인은 null)
    @Column(name = "parent_vendor_id")
    private UUID parentVendorId;

    // 퇴사 시 false (계정 삭제 대신 비활성화)
    @Column(name = "is_active")
    private Boolean isActive = true;

    // 최초 로그인 시 비번 변경 강제
    @Column(name = "must_change_password")
    private Boolean mustChangePassword = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public String getVendorType() { return vendorType; }
    public void setVendorType(String vendorType) { this.vendorType = vendorType; }
    public String getLoginId() { return loginId; }
    public void setLoginId(String loginId) { this.loginId = loginId; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }
    public String getBankManager() { return bankManager; }
    public void setBankManager(String bankManager) { this.bankManager = bankManager; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public UUID getParentVendorId() { return parentVendorId; }
    public void setParentVendorId(UUID parentVendorId) { this.parentVendorId = parentVendorId; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
