package com.tieyoh.api.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// One invitation token for one specific person the inviter selected from
// their phone contacts. Deliberately one row per recipient, never a
// shared token -- this is what lets Tieyoh know exactly which invitation
// resulted in a registration (see acceptedUserId). The raw token itself
// is never stored here, only its SHA-256 hash (tokenHash); the raw token
// is generated and returned to the client once, at creation time, and
// reconstructed for lookup by hashing whatever comes back in the URL.
// Same treatment for the recipient's phone/email: only a hash is kept,
// not the plaintext value, since it's not needed once the invite is
// created and shared.
@Entity
@Table(name = "invitation")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inviter_user_id", nullable = false)
    private Long inviterUserId;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_email_hash")
    private String recipientEmailHash;

    @Column(name = "recipient_phone_hash")
    private String recipientPhoneHash;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private String status = "CREATED";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_user_id")
    private Long acceptedUserId;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    public Invitation() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInviterUserId() {
        return inviterUserId;
    }

    public void setInviterUserId(Long inviterUserId) {
        this.inviterUserId = inviterUserId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientEmailHash() {
        return recipientEmailHash;
    }

    public void setRecipientEmailHash(String recipientEmailHash) {
        this.recipientEmailHash = recipientEmailHash;
    }

    public String getRecipientPhoneHash() {
        return recipientPhoneHash;
    }

    public void setRecipientPhoneHash(String recipientPhoneHash) {
        this.recipientPhoneHash = recipientPhoneHash;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getAcceptedUserId() {
        return acceptedUserId;
    }

    public void setAcceptedUserId(Long acceptedUserId) {
        this.acceptedUserId = acceptedUserId;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
