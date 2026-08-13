package com.tieyoh.api.merchant.repository;

import com.tieyoh.api.merchant.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByTmid(String tmid);

    boolean existsByTmid(String tmid);

    @Query("""
            SELECT m
            FROM Merchant m
            LEFT JOIN FETCH m.parentMerchant
            WHERE m.source = :source
              AND m.sourceMerchantId = :sourceMerchantId
            """)
    Optional<Merchant> findBySourceAndSourceMerchantId(
            @Param("source") String source,
            @Param("sourceMerchantId") String sourceMerchantId
    );

    @Query("""
            SELECT m
            FROM Merchant m
            LEFT JOIN FETCH m.parentMerchant
            """)
    List<Merchant> findAllWithParentMerchant();

    @Query("""
            SELECT DISTINCT m
            FROM Merchant m
            LEFT JOIN FETCH m.parentMerchant
            WHERE m.status = 'ACTIVE'

              AND (
                    :name IS NULL
                    OR LOWER(m.brandName) LIKE
                       CONCAT(
                           '%',
                           LOWER(CAST(:name AS String)),
                           '%'
                       )
                  )

              AND (
                    :merchantType IS NULL
                    OR LOWER(COALESCE(m.category, '')) LIKE
                       CONCAT(
                           '%',
                           LOWER(CAST(:merchantType AS String)),
                           '%'
                       )
                  )

              AND (
                    :zip IS NULL
                    OR EXISTS (
                        SELECT ml.id
                        FROM MerchantLocation ml
                        WHERE ml.merchant = m
                          AND LOWER(COALESCE(ml.postalCode, '')) LIKE
                              CONCAT(
                                  '%',
                                  LOWER(CAST(:zip AS String)),
                                  '%'
                              )
                    )
                  )

              AND (
                    :address IS NULL
                    OR EXISTS (
                        SELECT ml2.id
                        FROM MerchantLocation ml2
                        WHERE ml2.merchant = m
                          AND LOWER(
                                CONCAT(
                                    CONCAT(
                                        CONCAT(
                                            COALESCE(ml2.addressLine1, ''),
                                            ' '
                                        ),
                                        CONCAT(
                                            COALESCE(ml2.addressLine2, ''),
                                            ' '
                                        )
                                    ),
                                    CONCAT(
                                        CONCAT(
                                            COALESCE(ml2.city, ''),
                                            ' '
                                        ),
                                        CONCAT(
                                            CONCAT(
                                                COALESCE(ml2.state, ''),
                                                ' '
                                            ),
                                            COALESCE(ml2.postalCode, '')
                                        )
                                    )
                                )
                              ) LIKE
                              CONCAT(
                                  '%',
                                  LOWER(CAST(:address AS String)),
                                  '%'
                              )
                    )
                  )

            ORDER BY m.brandName
            """)
    List<Merchant> searchEligibleMerchants(
            @Param("name") String name,
            @Param("zip") String zip,
            @Param("address") String address,
            @Param("merchantType") String merchantType
    );

    @Query(
            value = "SELECT nextval('tmid_sequence')",
            nativeQuery = true
    )
    Long getNextTmidSequence();
}