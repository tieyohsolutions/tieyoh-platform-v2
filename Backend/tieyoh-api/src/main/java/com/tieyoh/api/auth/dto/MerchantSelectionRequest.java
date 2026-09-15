package com.tieyoh.api.auth.dto;

import java.util.List;

public class MerchantSelectionRequest {

    private List<Long> merchantIds;

    public List<Long> getMerchantIds() {
        return merchantIds;
    }

    public void setMerchantIds(List<Long> merchantIds) {
        this.merchantIds = merchantIds;
    }
}
