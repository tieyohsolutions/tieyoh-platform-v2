package com.tieyoh.api.gift.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// A short reply on a specific gift -- the "thank the sender" flow (and,
// symmetrically, a sender replying back). Restricted in GiftService to
// the gift's own sender and recipient; "body" holds either free text or
// a single emoji, both stored the same way.
@Entity
@Table(name = "gift_comment")
public class GiftComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gift_id", nullable = false)
    private Long giftId;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Column(name = "author_display_name", nullable = false)
    private String authorDisplayName;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GiftComment() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGiftId() {
        return giftId;
    }

    public void setGiftId(Long giftId) {
        this.giftId = giftId;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(Long authorUserId) {
        this.authorUserId = authorUserId;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName) {
        this.authorDisplayName = authorDisplayName;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
