package com.tieyoh.api.auth.dto;

import java.math.BigDecimal;

// Body for both adding a merchant (targetAmount optional -- a pick can be
// added with no target and one set later) and editing an existing pick's
// target (targetAmount null clears it).
public class MerchantTargetRequest {

    private BigDecimal targetAmount;

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }
}
