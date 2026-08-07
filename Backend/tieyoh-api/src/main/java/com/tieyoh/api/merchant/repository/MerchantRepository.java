package com.tieyoh.api.merchant.repository;

import com.tieyoh.api.merchant.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByTmid(String tmid);

    boolean existsByTmid(String tmid);
}
