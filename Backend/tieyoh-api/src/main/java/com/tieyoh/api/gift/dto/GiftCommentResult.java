package com.tieyoh.api.gift.dto;

public class GiftCommentResult {

    private Long id;
    private Long giftId;
    private Long authorUserId;
    private String authorDisplayName;
    private String body;
    private String createdAt;

    public GiftCommentResult() {
    }

    public GiftCommentResult(Long id, Long giftId, Long authorUserId, String authorDisplayName,
                              String body, String createdAt) {
        this.id = id;
        this.giftId = giftId;
        this.authorUserId = authorUserId;
        this.authorDisplayName = authorDisplayName;
        this.body = body;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getGiftId() {
        return giftId;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public String getBody() {
        return body;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
