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

    // query is a single combined search string, OR'd server-side across
    // the merchant's name and every location field (see
    // MerchantRepository.searchEligibleMerchants) -- the caller doesn't
    // need to know in advance whether it's a business name, an address,
    // a city, or a ZIP. merchantType stays a separate, independently
    // ANDed category filter.
    public List<Merchant> searchEligibleMerchants(
            String query,
            String merchantType) {

        return merchantRepository.searchEligibleMerchants(
                normalizeQuery(query),
                normalize(merchantType)
        );
    }

    // Same as searchEligibleMerchants, restricted to merchants that have
    // signed up to accept scanned Tieyoh payment QR codes -- backs "Find
    // Partnered Merchants" (PartneredMerchantSearchScreen.kt).
    public List<Merchant> searchEligiblePartneredMerchants(
            String query,
            String merchantType) {

        return merchantRepository.searchEligiblePartneredMerchants(
                normalizeQuery(query),
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

    // Flags (or unflags) a merchant as partnered -- i.e. that it has
    // signed up to accept scanned Tieyoh payment QR codes. This is
    // currently the only way anywhere in the app or database to set that
    // flag; there's no admin UI for it yet, just this endpoint
    // (MerchantController PATCH /{id}/partner and /{id}/unpartner).
    public Optional<Merchant> setPartnered(Long id, boolean partnered) {

        return merchantRepository.findById(id)
                .map(merchant -> {
                    merchant.setPartnered(partnered);
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

    // Same "near me" search, restricted to partnered merchants -- backs
    // the Map tab on "Find Partnered Merchants".
    public List<MerchantMobileResponse> getNearbyPartneredMerchants(double lat, double lng, double radiusMiles) {
        return merchantRepository.findNearbyPartnered(lat, lng, radiusMiles)
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
        response.setPartnered(row.getPartnered());
        response.setPmid(row.getPmid());
        response.setDistanceMiles(row.getDistanceMiles());
        response.setLatitude(row.getLatitude());
        response.setLongitude(row.getLongitude());
        return response;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    // Same light cleanup the old address-only search used (trim, drop
    // commas, collapse repeated whitespace) -- now applied to the single
    // combined query since it's matched against address fields too, not
    // just the merchant name.
    private String normalizeQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replace(",", " ").replaceAll("\s+", " ");
    }
}
