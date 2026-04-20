package com.ipjuon.backend.invite;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "invite_logs")
public class Invite {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "complex_name")
    private String complexName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "status")
    private String status;

    @Column(name = "method")
    private String method;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_by")
    private String sentBy;

    @CreationTimestamp
    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "opened_at")
    private OffsetDateTime openedAt;

    @Column(name = "registered_at")
    private OffsetDateTime registeredAt;

    public UUID getId() { return id; }
    public String getComplexName() { return complexName; }
    public void setComplexName(String complexName) { this.complexName = complexName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getSentBy() { return sentBy; }
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }
    public OffsetDateTime getSentAt() { return sentAt; }
    public OffsetDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(OffsetDateTime openedAt) { this.openedAt = openedAt; }
    public OffsetDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(OffsetDateTime registeredAt) { this.registeredAt = registeredAt; }
}
