package com.tieyoh.api.auth.dto;

// A row in the caller's own circle -- unlike UserSearchResult this is the
// owner looking at their own data, so contact info isn't masked.
public class FriendResult {

    private Long id;
    private Long contactUserId;
    private String displayName;
    private String tieyohId;
    private String phone;
    private String email;
    private String status;
    private String profilePictureBase64;

    public FriendResult() {
    }

    public FriendResult(Long id, Long contactUserId, String displayName, String tieyohId,
                         String phone, String email, String status, String profilePictureBase64) {
        this.id = id;
        this.contactUserId = contactUserId;
        this.displayName = displayName;
        this.tieyohId = tieyohId;
        this.phone = phone;
        this.email = email;
        this.status = status;
        this.profilePictureBase64 = profilePictureBase64;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContactUserId() {
        return contactUserId;
    }

    public void setContactUserId(Long contactUserId) {
        this.contactUserId = contactUserId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProfilePictureBase64() {
        return profilePictureBase64;
    }

    public void setProfilePictureBase64(String profilePictureBase64) {
        this.profilePictureBase64 = profilePictureBase64;
    }
}
