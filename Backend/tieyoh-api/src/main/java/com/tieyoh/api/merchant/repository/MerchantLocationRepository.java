package com.tieyoh.api.merchant.repository;

import com.tieyoh.api.merchant.entity.MerchantLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MerchantLocationRepository extends JpaRepository<MerchantLocation, Long> {

    List<MerchantLocation> findByMerchantId(Long merchantId);

    List<MerchantLocation> findByPostalCode(String postalCode);
}
