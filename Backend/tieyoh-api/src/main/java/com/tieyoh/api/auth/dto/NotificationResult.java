package com.tieyoh.api.auth.dto;

// createdAt is sent as a plain pre-formatted string (not a raw LocalDateTime)
// so its wire format doesn't depend on whatever Jackson date config this
// backend happens to have -- the mobile client just displays it as-is.
public class NotificationResult {

    private Long id;
    private String type;
    private Long actorUserId;
    private String actorDisplayName;
    private String message;
    private boolean read;
    private String createdAt;
    private Long connectionRequestId;
    private Long giftId;

    public NotificationResult() {
    }

    public NotificationResult(Long id, String type, Long actorUserId, String actorDisplayName, String message,
                               boolean read, String createdAt, Long connectionRequestId, Long giftId) {
        this.id = id;
        this.type = type;
        this.actorUserId = actorUserId;
        this.actorDisplayName = actorDisplayName;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
        this.connectionRequestId = connectionRequestId;
        this.giftId = giftId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
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
}
