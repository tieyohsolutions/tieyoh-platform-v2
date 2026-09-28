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

    // One combined query, OR'd across the merchant's own name and every
    // field on each of its locations (street address, city, state, ZIP)
    // -- a caller no longer has to know in advance whether what they typed
    // is a business name or a piece of an address; "Starbucks", "123 Main
    // St", "St. Louis", and "63101" all match through this single param.
    // merchantType is a separate, independent facet (a category filter),
    // so it stays its own ANDed clause rather than folding into the OR.
    @Query("""
            SELECT DISTINCT m
            FROM Merchant m
            LEFT JOIN FETCH m.parentMerchant
            WHERE m.status = 'ACTIVE'

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
                    :query IS NULL
                    OR LOWER(m.brandName) LIKE
                       CONCAT(
                           '%',
                           LOWER(CAST(:query AS String)),
                           '%'
                       )
                    OR EXISTS (
                        SELECT ml.id
                        FROM MerchantLocation ml
                        WHERE ml.merchant = m
                          AND LOWER(
                                CONCAT(
                                    CONCAT(
                                        CONCAT(
                                            COALESCE(ml.addressLine1, ''),
                                            ' '
                                        ),
                                        CONCAT(
                                            COALESCE(ml.addressLine2, ''),
                                            ' '
                                        )
                                    ),
                                    CONCAT(
                                        CONCAT(
                                            COALESCE(ml.city, ''),
                                            ' '
                                        ),
                                        CONCAT(
                                            CONCAT(
                                                COALESCE(ml.state, ''),
                                                ' '
                                            ),
                                            COALESCE(ml.postalCode, '')
                                        )
                                    )
                                )
                              ) LIKE
                              CONCAT(
                                  '%',
                                  LOWER(CAST(:query AS String)),
                                  '%'
                              )
                    )
                  )

            ORDER BY m.brandName
            """)
    List<Merchant> searchEligibleMerchants(
            @Param("query") String query,
            @Param("merchantType") String merchantType
    );

    // Same OR-across-name-and-every-location-field search as
    // searchEligibleMerchants above, restricted to merchants that have
    // signed up to accept scanned Tieyoh payment QR codes (m.partnered =
    // true). Kept as its own query rather than adding a parameter to the
    // one above so the general merchant registry search (Add a merchant,
    // Send Gift's merchant picker, etc.) is never at risk of an
    // accidental behavior change from this -- "Find Partnered Merchants"
    // is the only caller of this one.
    @Query("""
            SELECT DISTINCT m
            FROM Merchant m
            LEFT JOIN FETCH m.parentMerchant
            WHERE m.status = 'ACTIVE'
              AND m.partnered = true

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
                    :query IS NULL
                    OR LOWER(m.brandName) LIKE
                       CONCAT(
                           '%',
                           LOWER(CAST(:query AS String)),
                           '%'
                       )
                    OR EXISTS (
                        SELECT ml.id
                        FROM MerchantLocation ml
                        WHERE ml.merchant = m
                          AND LOWER(
                                CONCAT(
                                    CONCAT(
                                        CONCAT(
                                            COALESCE(ml.addressLine1, ''),
                                            ' '
                                        ),
                                        CONCAT(
                                            COALESCE(ml.addressLine2, ''),
                                            ' '
                                        )
                                    ),
                                    CONCAT(
                                        CONCAT(
                                            COALESCE(ml.city, ''),
                                            ' '
                                        ),
                                        CONCAT(
                                            CONCAT(
                                                COALESCE(ml.state, ''),
                                                ' '
                                            ),
                                            COALESCE(ml.postalCode, '')
                                        )
                                    )
                                )
                              ) LIKE
                              CONCAT(
                                  '%',
                                  LOWER(CAST(:query AS String)),
                                  '%'
                              )
                    )
                  )

            ORDER BY m.brandName
            """)
    List<Merchant> searchEligiblePartneredMerchants(
            @Param("query") String query,
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
                   m.partnered AS partnered,
                   pm.pmid AS pmid,
                   ml.latitude AS latitude,
                   ml.longitude AS longitude,
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

    // Same "near me" Haversine search as findNearby above, restricted to
    // merchants that have signed up to accept scanned Tieyoh payment QR
    // codes (m.partnered = true) -- backs the Map tab on "Find Partnered
    // Merchants" (PartneredMerchantSearchScreen.kt).
    @Query(value = """
            SELECT m.id AS id,
                   m.tmid AS tmid,
                   m.brand_name AS brandName,
                   m.logo_url AS logoUrl,
                   m.category AS category,
                   m.status AS status,
                   m.partnered AS partnered,
                   pm.pmid AS pmid,
                   ml.latitude AS latitude,
                   ml.longitude AS longitude,
                   (3959 * acos(LEAST(1,
                       cos(radians(:lat)) * cos(radians(ml.latitude)) *
                       cos(radians(ml.longitude) - radians(:lng)) +
                       sin(radians(:lat)) * sin(radians(ml.latitude))
                   ))) AS distanceMiles
            FROM merchant m
            JOIN merchant_location ml ON ml.merchant_id = m.id
            LEFT JOIN parent_merchant pm ON pm.id = m.parent_merchant_id
            WHERE m.status = 'ACTIVE'
              AND m.partnered = true
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
    List<MerchantWithDistance> findNearbyPartnered(
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
        Boolean getPartnered();
        String getPmid();
        Double getLatitude();
        Double getLongitude();
        Double getDistanceMiles();
    }
}
