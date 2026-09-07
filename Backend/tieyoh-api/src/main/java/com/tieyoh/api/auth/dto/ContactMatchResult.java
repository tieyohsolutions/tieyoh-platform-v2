package com.tieyoh.api.auth.dto;

// One entry back from POST /api/users/{ownerTieyohId}/friends/check-contacts.
// matchedUserId is null when this phone/email doesn't belong to any real
// (ACTIVE) Tieyoh account -- the client groups those under "invite"
// instead of "connect". mutualFriendsCount is only meaningful when
// matched: the size of the overlap between the viewer's own ACTIVE circle
// and the matched account's ACTIVE circle -- the same friend-graph data
// FriendService.getNetwork() already traverses for the feed's
// second-degree section, not a separate/fabricated number.
public class ContactMatchResult {

    private String displayName;
    private String phone;
    private String email;
    private Long matchedUserId;
    private String matchedTieyohId;
    private String matchedProfilePictureBase64;
    private Integer mutualFriendsCount;

    public ContactMatchResult() {
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getMatchedUserId() {
        return matchedUserId;
    }

    public void setMatchedUserId(Long matchedUserId) {
        this.matchedUserId = matchedUserId;
    }

    public String getMatchedTieyohId() {
        return matchedTieyohId;
    }

    public void setMatchedTieyohId(String matchedTieyohId) {
        this.matchedTieyohId = matchedTieyohId;
    }

    public String getMatchedProfilePictureBase64() {
        return matchedProfilePictureBase64;
    }

    public void setMatchedProfilePictureBase64(String matchedProfilePictureBase64) {
        this.matchedProfilePictureBase64 = matchedProfilePictureBase64;
    }

    public Integer getMutualFriendsCount() {
        return mutualFriendsCount;
    }

    public void setMutualFriendsCount(Integer mutualFriendsCount) {
        this.mutualFriendsCount = mutualFriendsCount;
    }
}
