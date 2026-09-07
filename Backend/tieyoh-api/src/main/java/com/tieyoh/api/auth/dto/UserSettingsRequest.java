package com.tieyoh.api.auth.dto;

// Kept as its own small DTO (rather than folding into ProfileSetupRequest)
// since this represents ongoing account preferences editable any time from
// the Profile screen, not one-time profile setup fields. Null fields are
// left untouched -- only non-null ones are applied -- so this can grow to
// carry more than one setting later without every caller having to send
// all of them every time.
public class UserSettingsRequest {

    private Boolean autoAcceptCircleInvites;

    // Default visibility pre-selected on the Send Gift screen: true =
    // "CONTACTS" (share with circle), false = "PRIVATE" (keep private).
    // Same null-means-untouched convention as autoAcceptCircleInvites.
    private Boolean shareGiftingWithCircle;

    public UserSettingsRequest() {
    }

    public Boolean getAutoAcceptCircleInvites() {
        return autoAcceptCircleInvites;
    }

    public void setAutoAcceptCircleInvites(Boolean autoAcceptCircleInvites) {
        this.autoAcceptCircleInvites = autoAcceptCircleInvites;
    }

    public Boolean getShareGiftingWithCircle() {
        return shareGiftingWithCircle;
    }

    public void setShareGiftingWithCircle(Boolean shareGiftingWithCircle) {
        this.shareGiftingWithCircle = shareGiftingWithCircle;
    }
}
