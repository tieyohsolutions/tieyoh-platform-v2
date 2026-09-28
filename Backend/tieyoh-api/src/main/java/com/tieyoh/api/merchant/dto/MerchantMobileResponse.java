package com.tieyoh.api.merchant.dto;

import java.math.BigDecimal;

public class MerchantMobileResponse {

    private Long id;
    private String pmid;
    private String tmid;
    private String brandName;
    private String logoUrl;
    private String category;
    private String status;
    // Whether this merchant accepts scanned Tieyoh payment QR codes -- see
    // Merchant.partnered. Populated on every response this DTO backs (not
    // just Find Partnered Merchants) so any screen can show a "Partnered"
    // badge if it wants to, even though today only the partnered-only
    // endpoints actually filter on it.
    private Boolean partnered;
    // Both null/zero unless this response is backing "My picks" -- Send
    // Gift's favorite-places use of this same DTO just ignores them.
    private BigDecimal targetAmount;
    private BigDecimal amountReceived;
    // Both only set on Map/Near-me results (getNearbyMerchants /
    // getNearbyPartneredMerchants) -- null everywhere else. latitude/
    // longitude back marker placement on the map; distanceMiles backs the
    // "N.N mi away" line in list view.
    private Double distanceMiles;
    private Double latitude;
    private Double longitude;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPmid() {
        return pmid;
    }

    public void setPmid(String pmid) {
        this.pmid = pmid;
    }

    public String getTmid() {
        return tmid;
    }

    public void setTmid(String tmid) {
        this.tmid = tmid;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getPartnered() {
        return partnered;
    }

    public void setPartnered(Boolean partnered) {
        this.partnered = partnered;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }

    public Double getDistanceMiles() {
        return distanceMiles;
    }

    public void setDistanceMiles(Double distanceMiles) {
        this.distanceMiles = distanceMiles;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
