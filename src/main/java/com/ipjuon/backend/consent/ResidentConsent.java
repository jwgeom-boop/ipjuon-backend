package com.ipjuon.backend.consent;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 입주민 개인정보 활용 동의서.
 * ipjuon-app 에서 입주민이 동의하면 → 모든 마감 안 된 은행에 ConsultationRequest 자동 생성됨.
 * 동의 시각/약관 버전 등 감사 추적용.
 */
@Entity
@Table(name = "resident_consents")
public class ResidentConsent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 50)
    private String resident_name;

    @Column(nullable = false, length = 30)
    private String resident_phone;

    private String resident_complex;     // 단지명
    private String dong;                 // 동 (선택)
    private String ho;                   // 호 (선택)
    private String apt_type;             // 평형 (선택)

    @Column(length = 20)
    private String terms_version;        // 동의 약관 버전 (예: "v1.0")

    private Integer distributed_count;   // 자동 생성된 ConsultationRequest 개수

    @Column(length = 500)
    private String distributed_banks;    // 분배 은행 목록 (쉼표 구분, 감사용)

    @Column(name = "invite_id")
    private UUID invite_id;              // SMS 링크 클릭 시 invite 추적 (선택)

    @CreationTimestamp
    @Column(name = "consented_at")
    private OffsetDateTime consentedAt;

    @Column(name = "user_agent", length = 500)
    private String userAgent;            // 감사용

    public UUID getId() { return id; }
    public String getResident_name() { return resident_name; }
    public void setResident_name(String v) { this.resident_name = v; }
    public String getResident_phone() { return resident_phone; }
    public void setResident_phone(String v) { this.resident_phone = v; }
    public String getResident_complex() { return resident_complex; }
    public void setResident_complex(String v) { this.resident_complex = v; }
    public String getDong() { return dong; }
    public void setDong(String v) { this.dong = v; }
    public String getHo() { return ho; }
    public void setHo(String v) { this.ho = v; }
    public String getApt_type() { return apt_type; }
    public void setApt_type(String v) { this.apt_type = v; }
    public String getTerms_version() { return terms_version; }
    public void setTerms_version(String v) { this.terms_version = v; }
    public Integer getDistributed_count() { return distributed_count; }
    public void setDistributed_count(Integer v) { this.distributed_count = v; }
    public String getDistributed_banks() { return distributed_banks; }
    public void setDistributed_banks(String v) { this.distributed_banks = v; }

    @JsonProperty("invite_id")
    public UUID getInvite_id() { return invite_id; }
    public void setInvite_id(UUID v) { this.invite_id = v; }

    @JsonProperty("consented_at")
    public OffsetDateTime getConsentedAt() { return consentedAt; }

    @JsonProperty("user_agent")
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
}
