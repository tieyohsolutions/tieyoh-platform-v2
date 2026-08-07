package com.tieyoh.api.merchant.service;

import com.tieyoh.api.merchant.entity.MerchantLocation;
import com.tieyoh.api.merchant.repository.MerchantLocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MerchantLocationService {

    private final MerchantLocationRepository merchantLocationRepository;

    public MerchantLocationService(MerchantLocationRepository merchantLocationRepository) {
        this.merchantLocationRepository = merchantLocationRepository;
    }

    public List<MerchantLocation> getLocationsByMerchant(Long merchantId) {
        return merchantLocationRepository.findByMerchantId(merchantId);
    }

    public List<MerchantLocation> getLocationsByPostalCode(String postalCode) {
        return merchantLocationRepository.findByPostalCode(postalCode);
    }

    public MerchantLocation saveLocation(MerchantLocation location) {
        return merchantLocationRepository.save(location);
    }
}
