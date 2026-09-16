package com.tieyoh.api.merchant.controller;

import com.tieyoh.api.merchant.dto.MerchantLocationResponse;
import com.tieyoh.api.merchant.dto.MerchantMobileResponse;
import com.tieyoh.api.merchant.dto.MerchantResponse;
import com.tieyoh.api.merchant.entity.Merchant;
import com.tieyoh.api.merchant.service.MerchantLocationService;
import com.tieyoh.api.merchant.service.MerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;
    private final MerchantLocationService merchantLocationService;

    public MerchantController(
            MerchantService merchantService,
            MerchantLocationService merchantLocationService) {
        this.merchantService = merchantService;
        this.merchantLocationService = merchantLocationService;
    }

    @GetMapping("/mobile")
    public List<MerchantMobileResponse> getMerchantsForMobile() {
        return merchantService.getAllMerchantsWithParent()
                .stream()
                .map(this::toMobileResponse)
                .toList();
    }

    @GetMapping("/mobile/search")
    public List<MerchantMobileResponse> searchMerchantsForMobile(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String zip,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String merchantType) {

        return merchantService.searchEligibleMerchants(name, zip, address, merchantType)
                .stream()
                .map(this::toMobileResponse)
                .toList();
    }

    @GetMapping("/mobile/nearby")
    public List<MerchantMobileResponse> getNearbyMerchantsForMobile(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false, defaultValue = "25") double radiusMiles) {

        return merchantService.getNearbyMerchants(lat, lng, radiusMiles);
    }

    @GetMapping
    public List<Merchant> getAllMerchants() {
        return merchantService.getAllMerchants();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Merchant> getMerchantById(@PathVariable Long id) {
        return merchantService.getMerchantById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tmid/{tmid}")
    public ResponseEntity<Merchant> getMerchantByTmid(@PathVariable String tmid) {
        return merchantService.getMerchantByTmid(tmid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Merchant> createMerchant(@RequestBody Merchant merchant) {
        Merchant savedMerchant = merchantService.saveMerchant(merchant);
        return ResponseEntity.ok(savedMerchant);
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<MerchantResponse> getMerchantDetails(@PathVariable Long id) {
        return merchantService.getMerchantById(id)
                .map(merchant -> {
                    MerchantResponse response = new MerchantResponse();
                    response.setId(merchant.getId());
                    response.setTmid(merchant.getTmid());
                    response.setBrandName(merchant.getBrandName());
                    response.setWebsite(merchant.getWebsite());
                    response.setLogoUrl(merchant.getLogoUrl());
                    response.setPhone(merchant.getPhone());
                    response.setCategory(merchant.getCategory());
                    response.setStatus(merchant.getStatus());

                    response.setLocations(
                            merchantLocationService.getLocationsByMerchant(merchant.getId())
                                    .stream()
                                    .map(location -> {
                                        MerchantLocationResponse locationResponse = new MerchantLocationResponse();
                                        locationResponse.setId(location.getId());
                                        locationResponse.setLocationName(location.getLocationName());
                                        locationResponse.setAddressLine1(location.getAddressLine1());
                                        locationResponse.setAddressLine2(location.getAddressLine2());
                                        locationResponse.setCity(location.getCity());
                                        locationResponse.setState(location.getState());
                                        locationResponse.setPostalCode(location.getPostalCode());
                                        locationResponse.setCountry(location.getCountry());
                                        locationResponse.setPhone(location.getPhone());
                                        return locationResponse;
                                    })
                                    .collect(Collectors.toList())
                    );

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Merchant> deactivateMerchant(@PathVariable Long id) {
        return merchantService.deactivateMerchant(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private MerchantMobileResponse toMobileResponse(Merchant merchant) {
        MerchantMobileResponse response = new MerchantMobileResponse();
        response.setId(merchant.getId());
        response.setTmid(merchant.getTmid());
        response.setBrandName(merchant.getBrandName());
        response.setLogoUrl(merchant.getLogoUrl());
        response.setCategory(merchant.getCategory());
        response.setStatus(merchant.getStatus());

        if (merchant.getParentMerchant() != null) {
            response.setPmid(merchant.getParentMerchant().getPmid());
        }

        return response;
    }
}
