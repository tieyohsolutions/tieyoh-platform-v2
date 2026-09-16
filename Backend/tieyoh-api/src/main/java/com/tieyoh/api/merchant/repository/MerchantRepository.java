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

    // Powers "Near me" in the Add-a-merchant flow -- uses the latitude/
    // longitude already stored on merchant_location from the Outscraper
    // import, no map rendering or geocoding involved. Haversine distance
    // in miles from the given point; LEAST(1, ...) guards acos() against
    // a >1 argument from floating-point rounding when a location is
    // (near) exactly the query point, which would otherwise return NaN.
    @Query(value = """
            SELECT m.id AS id,
                   m.tmid AS tmid,
                   m.brand_name AS brandName,
                   m.logo_url AS logoUrl,
                   m.category AS category,
                   m.status AS status,
                   pm.pmid AS pmid,
                   (3959 * acos(LEAST(1,
                       cos(radians(:lat)) * cos(radians(ml.latitude)) *
                       cos(radians(ml.longitude) - radians(:lng)) +
                       sin(radians(:lat)) * sin(radians(ml.latitude))
                   ))) AS distanceMiles
            FROM merchant m
            JOIN merchant_location ml ON ml.merchant_id = m.id
            LEFT JOIN parent_merchant pm ON pm.id = m.parent_merchant_id
            WHERE m.status = 'ACTIVE'
              AND ml.latitude IS NOT NULL
              AND ml.longitude IS NOT NULL
              AND (3959 * acos(LEAST(1,
                       cos(radians(:lat)) * cos(radians(ml.latitude)) *
                       cos(radians(ml.longitude) - radians(:lng)) +
                       sin(radians(:lat)) * sin(radians(ml.latitude))
                   ))) <= :radiusMiles
            ORDER BY distanceMiles ASC
            LIMIT 50
            """, nativeQuery = true)
    List<MerchantWithDistance> findNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMiles") double radiusMiles
    );

    interface MerchantWithDistance {
        Long getId();
        String getTmid();
        String getBrandName();
        String getLogoUrl();
        String getCategory();
        String getStatus();
        String getPmid();
        Double getDistanceMiles();
    }
}