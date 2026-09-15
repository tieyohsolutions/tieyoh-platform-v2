package com.tieyoh.api.feed.dto;

// A person as shown inline in a Feed item -- deliberately never just a
// display name. The mobile client identifies and acts on people (tap to
// send a gift) by userId, per the spec's explicit "Do NOT use display
// name as an identifier." Named profilePictureBase64 (not the spec's
// suggested "profileImageUrl") to match how this app already handles
// profile pictures everywhere else -- an inline base64 blob, never a
// hosted image URL -- so a mobile dev reading this field doesn't reach
// for an image loader that expects a real URL.
public class PersonSummary {

    private Long userId;
    private String displayName;
    private String tieyohId;
    private String profilePictureBase64;

    public PersonSummary() {
    }

    public PersonSummary(Long userId, String displayName, String tieyohId, String profilePictureBase64) {
        this.userId = userId;
        this.displayName = displayName;
        this.tieyohId = tieyohId;
        this.profilePictureBase64 = profilePictureBase64;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTieyohId() {
        return tieyohId;
    }

    public String getProfilePictureBase64() {
        return profilePictureBase64;
    }
}
