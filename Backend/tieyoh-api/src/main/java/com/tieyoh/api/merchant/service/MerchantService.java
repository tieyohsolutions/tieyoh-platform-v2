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

    public Optional<Merchant> getMerchantById(Long id) {
        return merchantRepository.findById(id);
    }

    public Optional<Merchant> getMerchantByTmid(String tmid) {
        return merchantRepository.findByTmid(tmid);
    }

    public Optional<Merchant> getMerchantBySource(
            String source,
            String sourceMerchantId) {

        return merchantRepository.findBySourceAndSourceMerchantId(
                source,
                sourceMerchantId);
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

    public Optional<Merchant> deactivateMerchant(Long id) {

        Optional<Merchant> merchantOptional =
                merchantRepository.findById(id);

        if (merchantOptional.isEmpty()) {
            return Optional.empty();
        }

        Merchant merchant = merchantOptional.get();
        merchant.setStatus("INACTIVE");

        return Optional.of(
                merchantRepository.save(merchant)
        
            );
    }
}