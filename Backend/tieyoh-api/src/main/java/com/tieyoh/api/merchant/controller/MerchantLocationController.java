package com.tieyoh.api.merchant.controller;

import com.tieyoh.api.merchant.entity.Merchant;
import com.tieyoh.api.merchant.entity.MerchantLocation;
import com.tieyoh.api.merchant.service.MerchantLocationService;
import com.tieyoh.api.merchant.service.MerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
public class MerchantLocationController {

    private final MerchantLocationService merchantLocationService;
    private final MerchantService merchantService;

    public MerchantLocationController(
            MerchantLocationService merchantLocationService,
            MerchantService merchantService) {
        this.merchantLocationService = merchantLocationService;
        this.merchantService = merchantService;
    }

    @GetMapping("/{merchantId}/locations")
    public List<MerchantLocation> getLocationsByMerchant(
            @PathVariable Long merchantId) {
        return merchantLocationService.getLocationsByMerchant(merchantId);
    }

    @PostMapping("/{merchantId}/locations")
    public ResponseEntity<MerchantLocation> createLocation(
            @PathVariable Long merchantId,
            @RequestBody MerchantLocation location) {

        Merchant merchant = merchantService.getMerchantById(merchantId)
                .orElse(null);

        if (merchant == null) {
            return ResponseEntity.notFound().build();
        }

        location.setMerchant(merchant);

        MerchantLocation savedLocation =
                merchantLocationService.saveLocation(location);

        return ResponseEntity.ok(savedLocation);
    }
}