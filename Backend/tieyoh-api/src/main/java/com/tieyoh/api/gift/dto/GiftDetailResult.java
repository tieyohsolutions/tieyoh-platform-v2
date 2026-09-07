package com.tieyoh.api.gift.dto;

import java.math.BigDecimal;

// Richer than GiftResult -- resolves sender/recipient/merchant display
// names server-side, since this is what backs GiftReceivedScreen, reached
// later from a notification tap with no local context about the gift
// (unlike the sender's own confirmation, shown right after sendGift()
// while the sender/merchant are still in memory on the client).
public class GiftDetailResult {

    private Long id;
    private Long senderId;
    private String senderDisplayName;
    private Long recipientId;
    private String recipientDisplayName;
    private Long merchantId;
    private String merchantName;
    private BigDecimal amount;
    private String currency;
    private String visibility;
    private String status;
    private String createdAt;

    public GiftDetailResult() {
    }

    public GiftDetailResult(Long id, Long senderId, String senderDisplayName, Long recipientId,
                             String recipientDisplayName, Long merchantId, String merchantName,
                             BigDecimal amount, String currency, String visibility, String status,
                             String createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.senderDisplayName = senderDisplayName;
        this.recipientId = recipientId;
        this.recipientDisplayName = recipientDisplayName;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.amount = amount;
        this.currency = currency;
        this.visibility = visibility;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getSenderDisplayName() {
        return senderDisplayName;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public String getRecipientDisplayName() {
        return recipientDisplayName;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
