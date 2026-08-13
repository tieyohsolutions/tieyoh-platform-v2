package com.tieyoh.api.merchant.service;

import com.tieyoh.api.merchant.entity.Merchant;
import com.tieyoh.api.merchant.repository.MerchantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
