package com.tieyoh.api.merchant.repository;

import com.tieyoh.api.merchant.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByTmid(String tmid);

    Optional<Merchant> findBySourceAndSourceMerchantId(
            String source,
            String sourceMerchantId);

    boolean existsByTmid(String tmid);

    @Query(value = "SELECT nextval('tmid_sequence')", nativeQuery = true)
    Long getNextTmidSequence();
}