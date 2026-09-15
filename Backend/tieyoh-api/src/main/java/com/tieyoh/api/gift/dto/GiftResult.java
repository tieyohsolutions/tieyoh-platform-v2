package com.tieyoh.api.gift.dto;

import java.math.BigDecimal;

public class GiftResult {

    private Long id;
    private Long senderId;
    private Long recipientId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private String visibility;
    private String status;
    private String createdAt;

    public GiftResult() {
    }

    public GiftResult(Long id, Long senderId, Long recipientId, Long merchantId, BigDecimal amount,
                       String currency, String visibility, String status, String createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.merchantId = merchantId;
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

    public Long getRecipientId() {
        return recipientId;
    }

    public Long getMerchantId() {
        return merchantId;
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
