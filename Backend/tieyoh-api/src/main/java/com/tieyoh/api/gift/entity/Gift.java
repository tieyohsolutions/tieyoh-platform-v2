package com.tieyoh.api.gift.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// A real gift-send record. No payment processing behind this yet -- a
// gift is recorded COMPLETED the instant it's sent, which is genuinely
// all "sending a gift" means in this prototype today. sender/recipient
// are plain numeric FKs (both sides are always a real, ACTIVE Tieyoh
// account -- you can only gift someone who's already joined), matching
// how FriendConnection/Notification already work in numeric ids rather
// than tID for this kind of row.
@Entity
@Table(name = "gift")
public class Gift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "USD";

    // PRIVATE / CONTACTS / PUBLIC -- see FeedService for enforcement.
    @Column(nullable = false)
    private String visibility = "CONTACTS";

    @Column(nullable = false)
    private String status = "COMPLETED";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Gift() {
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

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
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
}
