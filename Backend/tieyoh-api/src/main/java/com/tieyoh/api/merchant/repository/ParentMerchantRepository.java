package com.tieyoh.api.merchant.repository;

import com.tieyoh.api.merchant.entity.ParentMerchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ParentMerchantRepository
        extends JpaRepository<ParentMerchant, Long> {

    Optional<ParentMerchant> findByPmid(String pmid);

    Optional<ParentMerchant> findByBrandNameIgnoreCase(String brandName);

    boolean existsByPmid(String pmid);

    @Query(value = "SELECT nextval('pmid_sequence')", nativeQuery = true)
    Long getNextPmidSequence();
}