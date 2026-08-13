package com.tieyoh.api.merchant.dto;

public class MerchantImportResponse {

    private Long id;
    private String pmid;
    private String tmid;
    private String brandName;
    private String source;
    private String sourceMerchantId;
    private String status;

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceMerchantId() {
        return sourceMerchantId;
    }

    public void setSourceMerchantId(String sourceMerchantId) {
        this.sourceMerchantId = sourceMerchantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
