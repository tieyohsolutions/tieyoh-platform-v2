package com.tieyoh.api.gift.repository;

import com.tieyoh.api.gift.entity.GiftComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftCommentRepository extends JpaRepository<GiftComment, Long> {

    List<GiftComment> findByGiftIdOrderByCreatedAtAsc(Long giftId);
}
