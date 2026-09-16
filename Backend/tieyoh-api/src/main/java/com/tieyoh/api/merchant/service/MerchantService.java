package com.tieyoh.api.merchant.service;

import com.tieyoh.api.merchant.dto.MerchantMobileResponse;
import com.tieyoh.api.merchant.entity.Merchant;
import com.tieyoh.api.merchant.repository.MerchantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    public List<Merchant> getAllMerchants() {
        return merchantRepository.findAll();
    }

    public List<Merchant> getAllMerchantsWithParent() {
        return merchantRepository.findAllWithParentMerchant();
    }

    public List<Merchant> searchEligibleMerchants(
            String name,
            String zip,
            String address,
            String merchantType) {

        return merchantRepository.searchEligibleMerchants(
                normalize(name),
                normalize(zip),
                normalizeAddress(address),
                normalize(merchantType)
        );
    }

    public Optional<Merchant> getMerchantById(Long id) {
        return merchantRepository.findById(id);
    }

    public Optional<Merchant> getMerchantByTmid(String tmid) {
        return merchantRepository.findByTmid(tmid);
    }

    public Merchant saveMerchant(Merchant merchant) {

        if (merchant.getTmid() == null || merchant.getTmid().isBlank()) {
            Long nextNumber = merchantRepository.getNextTmidSequence();
            String generatedTmid = String.format("t%012d", nextNumber);
            merchant.setTmid(generatedTmid);
        }

        return merchantRepository.save(merchant);
    }

    public boolean tmidExists(String tmid) {
        return merchantRepository.existsByTmid(tmid);
    }

    public Optional<Merchant> getMerchantBySource(
            String source,
            String sourceMerchantId) {

        return merchantRepository.findBySourceAndSourceMerchantId(
                source,
                sourceMerchantId
        );
    }

    public Optional<Merchant> deactivateMerchant(Long id) {

        return merchantRepository.findById(id)
                .map(merchant -> {
                    merchant.setStatus("INACTIVE");
                    return merchantRepository.save(merchant);
                });
    }

    // "Near me" in the Add-a-merchant flow -- distance from the device's
    // current GPS position, computed against the latitude/longitude
    // already stored on each merchant's location. No map rendering or
    // geocoding involved, just the coordinates already on file.
    public List<MerchantMobileResponse> getNearbyMerchants(double lat, double lng, double radiusMiles) {
        return merchantRepository.findNearby(lat, lng, radiusMiles)
                .stream()
                .map(this::toNearbyResponse)
                .collect(Collectors.toList());
    }

    private MerchantMobileResponse toNearbyResponse(MerchantRepository.MerchantWithDistance row) {
        MerchantMobileResponse response = new MerchantMobileResponse();
        response.setId(row.getId());
        response.setTmid(row.getTmid());
        response.setBrandName(row.getBrandName());
        response.setLogoUrl(row.getLogoUrl());
        response.setCategory(row.getCategory());
        response.setStatus(row.getStatus());
        response.setPmid(row.getPmid());
        response.setDistanceMiles(row.getDistanceMiles());
        return response;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeAddress(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replace(",", " ").replaceAll("\s+", " ");
    }
}
