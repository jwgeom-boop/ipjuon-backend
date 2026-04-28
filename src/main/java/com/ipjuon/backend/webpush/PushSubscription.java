package com.ipjuon.backend.webpush;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 입주민 앱 Web Push 구독 정보.
 * 한 phone 당 여러 device(브라우저별) 가능 → endpoint UNIQUE.
 */
@Entity
@Table(name = "push_subscriptions",
       indexes = { @Index(name = "idx_push_subs_phone", columnList = "phone") })
public class PushSubscription {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String phone; // 정규화된 digits-only

    @Column(nullable = false, unique = true, length = 1024)
    private String endpoint;

    @Column(nullable = false, length = 256)
    private String p256dh;

    @Column(nullable = false, length = 256)
    private String auth;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    public UUID getId() { return id; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String v) { this.endpoint = v; }
    public String getP256dh() { return p256dh; }
    public void setP256dh(String v) { this.p256dh = v; }
    public String getAuth() { return auth; }
    public void setAuth(String v) { this.auth = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    @JsonProperty("created_at")
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime v) { this.createdAt = v; }
    @JsonProperty("last_used_at")
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(OffsetDateTime v) { this.lastUsedAt = v; }
}
