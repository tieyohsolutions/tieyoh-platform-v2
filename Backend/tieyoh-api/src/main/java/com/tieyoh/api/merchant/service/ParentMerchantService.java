package com.tieyoh.api.merchant.service;

import com.tieyoh.api.merchant.entity.ParentMerchant;
import com.tieyoh.api.merchant.repository.ParentMerchantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class ParentMerchantService {

    private final ParentMerchantRepository parentMerchantRepository;

    public ParentMerchantService(
            ParentMerchantRepository parentMerchantRepository) {
        this.parentMerchantRepository = parentMerchantRepository;
    }

    public List<ParentMerchant> getAllParentMerchants() {
        return parentMerchantRepository.findAll();
    }

    public Optional<ParentMerchant> getParentMerchantById(Long id) {
        return parentMerchantRepository.findById(id);
    }

    public Optional<ParentMerchant> getParentMerchantByPmid(String pmid) {
        return parentMerchantRepository.findByPmid(pmid);
    }

    public Optional<ParentMerchant> getParentMerchantByBrandName(
            String brandName) {
        return parentMerchantRepository.findByBrandNameIgnoreCase(brandName);
    }

    public ParentMerchant saveParentMerchant(ParentMerchant parentMerchant) {

        if (parentMerchant.getPmid() == null ||
                parentMerchant.getPmid().isBlank()) {

            Long nextNumber = parentMerchantRepository.getNextPmidSequence();

            String generatedPmid =
             String.format("p%012d", nextNumber);

            parentMerchant.setPmid(generatedPmid);
        }

        return parentMerchantRepository.save(parentMerchant);
    }
}
