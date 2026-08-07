package com.tieyoh.api.merchant.dto;

import java.util.List;

public class MerchantResponse {

    private Long id;
    private String tmid;
    private String brandName;
    private String website;
    private String phone;
    private String category;
    private String status;
    private List<MerchantLocationResponse> locations;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public List<MerchantLocationResponse> getLocations() {
        return locations;
    }

    public void setLocations(List<MerchantLocationResponse> locations) {
        this.locations = locations;
    }
}