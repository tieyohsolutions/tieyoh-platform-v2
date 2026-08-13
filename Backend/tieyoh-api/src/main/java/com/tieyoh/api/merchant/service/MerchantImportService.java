package com.tieyoh.api.merchant.service;

import com.tieyoh.api.merchant.dto.MerchantImportRecord;
import com.tieyoh.api.merchant.entity.Merchant;
import com.tieyoh.api.merchant.entity.MerchantLocation;
import com.tieyoh.api.merchant.entity.ParentMerchant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantImportService {

    private final MerchantService merchantService;
    private final MerchantLocationService merchantLocationService;
    private final ParentMerchantService parentMerchantService;

    public MerchantImportService(
            MerchantService merchantService,
            MerchantLocationService merchantLocationService,
            ParentMerchantService parentMerchantService) {

        this.merchantService = merchantService;
        this.merchantLocationService = merchantLocationService;
        this.parentMerchantService = parentMerchantService;
    }

   @Transactional
    public Merchant importMerchant(MerchantImportRecord record) {

        Merchant existingMerchant = merchantService
            .getMerchantBySource(
                record.getSource(),
                record.getSourceMerchantId()
            )
            .orElse(null);

        if (existingMerchant != null) {
            return existingMerchant;
        }

        // Step 1: Find or create the parent merchant / PMID
        ParentMerchant parentMerchant =
                parentMerchantService
                        .getParentMerchantByBrandName(record.getBrandName())
                        .orElse(null);

        if (parentMerchant == null) {

            parentMerchant = new ParentMerchant();
            parentMerchant.setBrandName(record.getBrandName());
            parentMerchant.setWebsite(record.getWebsite());
            parentMerchant.setLogoUrl(record.getLogoUrl());
            parentMerchant.setStatus("ACTIVE");

            parentMerchant =
                    parentMerchantService.saveParentMerchant(parentMerchant);
        }

        // Step 2: Create the branch / affiliate merchant and its TMID
        Merchant merchant = new Merchant();

        merchant.setParentMerchant(parentMerchant);
        merchant.setBrandName(record.getBrandName());
        merchant.setWebsite(record.getWebsite());
        merchant.setLogoUrl(record.getLogoUrl());
        merchant.setPhone(record.getPhone());
        merchant.setCategory(record.getCategory());
        merchant.setSource(record.getSource());
        merchant.setSourceMerchantId(record.getSourceMerchantId());
        merchant.setStatus("ACTIVE");

        Merchant savedMerchant =
                merchantService.saveMerchant(merchant);

        // Step 3: Create the physical merchant location
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