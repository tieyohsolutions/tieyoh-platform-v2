package com.tieyoh.api.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// One row per person in a user's "circle". The owner is identified by
// their Tieyoh @t- id (ownerTieyohId), not an internal numeric id -- every
// owner is, by definition, a completed account with one by the time any
// screen creates these rows. If contactTieyohId is set, this contact
// already has a real Tieyoh account (status ACTIVE) and gifting to them is
// fully unlocked -- contactUserId is kept alongside it purely as a numeric
// convenience reference for other parts of the app (profile pictures, the
// feed's second-degree traversal, gift/connection-request flows), not as
// the source of truth. If contactTieyohId is null, this is still just a
// pending invite (status PENDING) sent by phone or email -- shown greyed
// out until they sign up, at which point promotePendingConnections() links
// it to their new account and flips it to ACTIVE automatically.
@Entity
@Table(name = "friend_connection")
public class FriendConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_tieyoh_id", nullable = false)
    private String ownerTieyohId;

    @Column(name = "contact_user_id")
    private Long contactUserId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "contact_tieyoh_id")
    private String contactTieyohId;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public FriendConnection() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerTieyohId() {
        return ownerTieyohId;
    }

    public void setOwnerTieyohId(String ownerTieyohId) {
        this.ownerTieyohId = ownerTieyohId;
    }

    public Long getContactUserId() {
        return contactUserId;
    }

    public void setContactUserId(Long contactUserId) {
        this.contactUserId = contactUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactTieyohId() {
        return contactTieyohId;
    }

    public void setContactTieyohId(String contactTieyohId) {
        this.contactTieyohId = contactTieyohId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
