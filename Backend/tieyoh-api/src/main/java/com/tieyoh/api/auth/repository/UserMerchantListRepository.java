package com.tieyoh.api.auth.repository;

import com.tieyoh.api.auth.entity.UserMerchantList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMerchantListRepository extends JpaRepository<UserMerchantList, Long> {

    List<UserMerchantList> findByUserId(Long userId);

    Optional<UserMerchantList> findByUserIdAndMerchantId(Long userId, Long merchantId);

    void deleteByUserId(Long userId);

    long countByUserId(Long userId);
}
