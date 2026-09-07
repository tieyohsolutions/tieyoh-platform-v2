package com.tieyoh.api.auth.dto;

// A person shown in the feed's network section: either a direct circle
// member (relationship DIRECT, tap to gift) or a contact of one of your
// contacts (relationship SECOND_DEGREE, tap to send a connection request).
// connectionRequestStatus is null unless a request already exists between
// the viewer and this person: "PENDING_SENT" (viewer already asked) or
// "PENDING_RECEIVED" (this person already asked the viewer -- resolve it
// from the notifications list instead of asking again).
public class FeedPersonResult {

    private Long userId;
    private String displayName;
    private String tieyohId;
    private String profilePictureBase64;
    private String relationship;
    private String connectionRequestStatus;

    public FeedPersonResult() {
    }

    public FeedPersonResult(Long userId, String displayName, String tieyohId, String profilePictureBase64,
                             String relationship, String connectionRequestStatus) {
        this.userId = userId;
        this.displayName = displayName;
        this.tieyohId = tieyohId;
        this.profilePictureBase64 = profilePictureBase64;
        this.relationship = relationship;
        this.connectionRequestStatus = connectionRequestStatus;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTieyohId() {
        return tieyohId;
    }

    public void setTieyohId(String tieyohId) {
        this.tieyohId = tieyohId;
    }

    public String getProfilePictureBase64() {
        return profilePictureBase64;
    }

    public void setProfilePictureBase64(String profilePictureBase64) {
        this.profilePictureBase64 = profilePictureBase64;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getConnectionRequestStatus() {
        return connectionRequestStatus;
    }

    public void setConnectionRequestStatus(String connectionRequestStatus) {
        this.connectionRequestStatus = connectionRequestStatus;
    }
}
