package com.tieyoh.api.merchant.controller;

import com.tieyoh.api.merchant.dto.MerchantImportRecord;
import com.tieyoh.api.merchant.dto.MerchantImportResponse;
import com.tieyoh.api.merchant.entity.Merchant;
import com.tieyoh.api.merchant.service.MerchantImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant-import")
public class MerchantImportController {

    private final MerchantImportService merchantImportService;

    public MerchantImportController(MerchantImportService merchantImportService) {
        this.merchantImportService = merchantImportService;
    }

    @PostMapping
    public ResponseEntity<MerchantImportResponse> importMerchant(
            @RequestBody MerchantImportRecord record) {

        Merchant merchant = merchantImportService.importMerchant(record);

        MerchantImportResponse response = new MerchantImportResponse();

        response.setId(merchant.getId());
        response.setPmid(merchant.getParentMerchant().getPmid());
        response.setTmid(merchant.getTmid());
        response.setBrandName(merchant.getBrandName());
        response.setSource(merchant.getSource());
        response.setSourceMerchantId(merchant.getSourceMerchantId());
        response.setStatus(merchant.getStatus());

        return ResponseEntity.ok(response);
    }
}