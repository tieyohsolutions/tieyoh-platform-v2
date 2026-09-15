package com.tieyoh.api.gift.repository;

import com.tieyoh.api.gift.entity.Gift;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
