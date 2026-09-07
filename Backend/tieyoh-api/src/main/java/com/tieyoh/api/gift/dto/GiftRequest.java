package com.tieyoh.api.gift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

// senderId is trusted straight from the client, same as every other
// endpoint in this backend (there's no session/auth context yet -- see
// tieyoh-auth-architecture.md) -- consistent with the existing trust
// model rather than a special case just for this endpoint.
public class GiftRequest {

    @NotNull(message = "senderId is required")
    private Long senderId;

    @NotNull(message = "recipientId is required")
    private Long recipientId;

    private Long merchantId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private java.math.BigDecimal amount;

    // Optional; defaults to CONTACTS server-side if omitted. Only
    // PRIVATE/CONTACTS are exposed in the mobile UI today, but PUBLIC is
    // accepted and fully enforced so the API doesn't need to change if
    // the client adds it later.
    private String visibility;

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

    public java.math.BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(java.math.BigDecimal amount) {
        this.amount = amount;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
}
