package com.ipjuon.backend.consultation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "consultation_requests")
public class ConsultationRequest {

    @Id
    @GeneratedValue
    private UUID id;

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

    // Getters and Setters
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
}
