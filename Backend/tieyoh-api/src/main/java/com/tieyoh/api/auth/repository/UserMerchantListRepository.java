package com.tieyoh.api.auth.repository;

import com.tieyoh.api.auth.entity.UserMerchantList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserMerchantListRepository extends JpaRepository<UserMerchantList, Long> {

    // Eagerly fetches merchant (and its parent merchant) in the same query --
    // both are LAZY associations, and getMerchantSelections/toMobileResponse
    // reads them after this call returns, outside of any @Transactional
    // boundary, so a plain findByUserId here would throw
    // LazyInitializationException the moment the response is built.
    @Query("SELECT e FROM UserMerchantList e " +
            "JOIN FETCH e.merchant m " +
            "LEFT JOIN FETCH m.parentMerchant " +
            "WHERE e.user.id = :userId")
    List<UserMerchantList> findByUserId(@Param("userId") Long userId);

    Optional<UserMerchantList> findByUserIdAndMerchantId(Long userId, Long merchantId);

    void deleteByUserId(Long userId);

    long countByUserId(Long userId);
}
