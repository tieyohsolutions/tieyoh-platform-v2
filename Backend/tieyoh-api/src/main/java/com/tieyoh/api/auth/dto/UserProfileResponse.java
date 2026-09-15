package com.tieyoh.api.auth.dto;

// Backs the mobile Edit Profile screen: everything ProfileSetupRequest
// can set, read back so that screen can pre-fill its fields (including
// the ones it doesn't let the user change, like tieyohId/gender -- those
// still have to be resent on save since PATCH /{id}/profile treats them
// as required, not partial-update fields). Also carries the two Privacy
// toggles (auto-accept circle invites, share gifting with circle) so
// Edit Profile's Privacy section can pre-fill those too, without a
// separate round trip.
public class UserProfileResponse {

    private String email;
    private String tieyohId;
    private String phone;
    private String address;
    private String gender;
    private Integer birthMonth;
    private Integer birthDay;
    private String holidaysCelebrated;
    private Boolean autoAcceptCircleInvites;
    private Boolean shareGiftingWithCircle;

    public UserProfileResponse() {
    }

    public UserProfileResponse(String email, String tieyohId, String phone, String address, String gender,
                                Integer birthMonth, Integer birthDay, String holidaysCelebrated,
                                Boolean autoAcceptCircleInvites, Boolean shareGiftingWithCircle) {
        this.email = email;
        this.tieyohId = tieyohId;
        this.phone = phone;
        this.address = address;
        this.gender = gender;
        this.birthMonth = birthMonth;
        this.birthDay = birthDay;
        this.holidaysCelebrated = holidaysCelebrated;
        this.autoAcceptCircleInvites = autoAcceptCircleInvites;
        this.shareGiftingWithCircle = shareGiftingWithCircle;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getBirthMonth() {
        return birthMonth;
    }

    public void setBirthMonth(Integer birthMonth) {
        this.birthMonth = birthMonth;
    }

    public Integer getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(Integer birthDay) {
        this.birthDay = birthDay;
    }

    public String getHolidaysCelebrated() {
        return holidaysCelebrated;
    }

    public void setHolidaysCelebrated(String holidaysCelebrated) {
        this.holidaysCelebrated = holidaysCelebrated;
    }
}
