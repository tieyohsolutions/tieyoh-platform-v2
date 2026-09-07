package com.tieyoh.api.feed.dto;

public class MerchantSummary {

    private Long merchantId;
    private String tmid;
    private String brandName;
    private String logoUrl;
    private String category;

    public MerchantSummary() {
    }

    public MerchantSummary(Long merchantId, String tmid, String brandName, String logoUrl, String category) {
        this.merchantId = merchantId;
        this.tmid = tmid;
        this.brandName = brandName;
        this.logoUrl = logoUrl;
        this.category = category;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public String getTmid() {
        return tmid;
    }

    public String getBrandName() {
        return brandName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getCategory() {
        return category;
    }
}
