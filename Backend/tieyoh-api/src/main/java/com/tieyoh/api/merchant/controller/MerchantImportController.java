package com.tieyoh.api.merchant.controller;

import com.tieyoh.api.merchant.dto.MerchantImportRecord;
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
    public ResponseEntity<Merchant> importMerchant(
            @RequestBody MerchantImportRecord record) {

        Merchant savedMerchant = merchantImportService.importMerchant(record);

        return ResponseEntity.ok(savedMerchant);
    }
}