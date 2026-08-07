package com.tieyoh.api.merchant.service;

import com.tieyoh.api.merchant.dto.MerchantImportRecord;
import com.tieyoh.api.merchant.entity.Merchant;
import com.tieyoh.api.merchant.entity.MerchantLocation;
import org.springframework.stereotype.Service;

@Service
public class MerchantImportService {

    private final MerchantService merchantService;
    private final MerchantLocationService merchantLocationService;

    public MerchantImportService(
            MerchantService merchantService,
            MerchantLocationService merchantLocationService) {
        this.merchantService = merchantService;
        this.merchantLocationService = merchantLocationService;
    }

    public Merchant importMerchant(MerchantImportRecord record) {

        Merchant merchant = new Merchant();
        merchant.setBrandName(record.getBrandName());
        merchant.setWebsite(record.getWebsite());
        merchant.setLogoUrl(record.getLogoUrl());
        merchant.setPhone(record.getPhone());
        merchant.setCategory(record.getCategory());
        merchant.setSource(record.getSource());
        merchant.setSourceMerchantId(record.getSourceMerchantId());
        merchant.setStatus("ACTIVE");

        Merchant savedMerchant = merchantService.saveMerchant(merchant);

        MerchantLocation location = new MerchantLocation();
        location.setMerchant(savedMerchant);
        location.setLocationName(record.getLocationName());
        location.setAddressLine1(record.getAddressLine1());
        location.setAddressLine2(record.getAddressLine2());
        location.setCity(record.getCity());
        location.setState(record.getState());
        location.setPostalCode(record.getPostalCode());
        location.setCountry(record.getCountry());
        location.setLatitude(record.getLatitude());
        location.setLongitude(record.getLongitude());
        location.setPhone(record.getPhone());
        location.setSourceLocationId(record.getSourceMerchantId());

        merchantLocationService.saveLocation(location);

        return savedMerchant;
    }
}