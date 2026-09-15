package com.tieyoh.api.auth.repository;

import com.tieyoh.api.auth.entity.UserMerchantList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMerchantListRepository extends JpaRepository<UserMerchantList, Long> {

    List<UserMerchantList> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    long countByUserId(Long userId);
}
