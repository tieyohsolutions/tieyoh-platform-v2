package com.tieyoh.api.feed.dto;

import com.tieyoh.api.gift.dto.GiftCommentResult;

import java.math.BigDecimal;
import java.util.List;

public class GiftInfo {

    private BigDecimal amount;
    private String currency;
    // The gift's "thank you" replies (see GiftService.addComment) --
    // included directly on the feed item so GiftActivityCard can render
    // them inline without a separate per-card network call. Usually 0 or
    // 1 entries in practice (a one-shot "thanks"), but not capped -- both
    // parties can reply.
    private List<GiftCommentResult> comments;

    public GiftInfo() {
    }

    public GiftInfo(BigDecimal amount, String currency, List<GiftCommentResult> comments) {
        this.amount = amount;
        this.currency = currency;
        this.comments = comments;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public List<GiftCommentResult> getComments() {
        return comments;
    }
}
