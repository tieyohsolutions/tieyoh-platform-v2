package com.tieyoh.api.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// One entry in a user's notification list (the bell icon). "type" is a
// free-form string on purpose -- CONNECTION_REQUEST_RECEIVED and
// CONNECTION_REQUEST_ACCEPTED are the two produced today, but more types
// (e.g. a future GIFT_RECEIVED once gifting itself is built) should be
// addable without a schema change. "message" is precomputed server-side
// so the client can just display it, regardless of type.
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_display_name")
    private String actorDisplayName;

    @Column(nullable = false)
    private String message;

    @Column(name = "connection_request_id")
    private Long connectionRequestId;

    @Column(name = "gift_id")
    private Long giftId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Notification() {
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorDisplayName() {
        return actorDisplayName;
    }

    public void setActorDisplayName(String actorDisplayName) {
        this.actorDisplayName = actorDisplayName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getConnectionRequestId() {
        return connectionRequestId;
    }

    public void setConnectionRequestId(Long connectionRequestId) {
        this.connectionRequestId = connectionRequestId;
    }

    public Long getGiftId() {
        return giftId;
    }

    public void setGiftId(Long giftId) {
        this.giftId = giftId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
