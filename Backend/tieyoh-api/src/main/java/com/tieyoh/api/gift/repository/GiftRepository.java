package com.tieyoh.api.gift.repository;

import com.tieyoh.api.gift.entity.Gift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GiftRepository extends JpaRepository<Gift, Long> {

    // Feeds FeedService's GIFT_ACTIVITY sourcing. Fetches everything
    // platform-wide within the lookback window and lets FeedService
    // filter down to what each viewer is actually allowed to see --
    // correct and simple for this prototype's scale; if that ever
    // becomes a real performance problem, this is the query to replace
    // with something scoped server-side to the viewer's network first.
    List<Gift> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);

    // Powers "My picks": how much a user has actually received toward
    // each merchant on their list, derived from real completed gifts
    // rather than a separately-tracked running total. One grouped query
    // per user rather than one per merchant.
    @Query("""
            SELECT g.merchantId AS merchantId, SUM(g.amount) AS total
            FROM Gift g
            WHERE g.recipientId = :recipientId
              AND g.status = 'COMPLETED'
              AND g.merchantId IS NOT NULL
            GROUP BY g.merchantId
            """)
    List<MerchantReceivedTotal> sumReceivedByRecipientGroupedByMerchant(
            @Param("recipientId") Long recipientId
    );

    interface MerchantReceivedTotal {
        Long getMerchantId();
        java.math.BigDecimal getTotal();
    }
}
