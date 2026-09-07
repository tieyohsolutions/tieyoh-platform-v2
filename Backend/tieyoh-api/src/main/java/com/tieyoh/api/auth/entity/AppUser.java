package com.tieyoh.api.auth.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    // Added alongside displayName for the onboarding redesign, which
    // collects these as two separate fields rather than one free-text
    // name. displayName itself is untouched and still what search, the
    // feed, friend lists, and notifications all read.
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "tieyoh_id")
    private String tieyohId;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "gender")
    private String gender;

    @Column(name = "profile_picture_base64", columnDefinition = "TEXT")
    private String profilePictureBase64;

    @Column(nullable = false)
    private String status = "ACTIVE";

    // Whether someone adding this user to their circle links both sides
    // immediately (true, the default -- matches how adding someone already
    // worked before this setting existed) or first requires this user's
    // approval (false), surfaced as a Profile toggle. See FriendService for
    // where this actually gets checked.
    @Column(name = "auto_accept_circle_invites", nullable = false)
    private Boolean autoAcceptCircleInvites = true;

    // Default visibility shown/pre-selected on the Send Gift screen's
    // existing per-gift visibility choice (CONTACTS vs PRIVATE) -- true
    // means gifts default to visible to the sender's circle, matching
    // GiftService's own server-side default when no visibility is sent
    // at all. This is only a convenience default; the actual per-gift
    // choice (and GiftService's enforcement of it) is unaffected and can
    // still be overridden for any single gift.
    @Column(name = "share_gifting_with_circle", nullable = false)
    private Boolean shareGiftingWithCircle = true;

    // Month/day only -- deliberately no birth year anywhere on this
    // entity. The Feed's UPCOMING_BIRTHDAY event only ever needs to
    // compute "how many days until this date recurs," never an age.
    // Both null until the user sets a birthday (optional, from the
    // profile-setup screen); an event never appears for someone who
    // hasn't set one.
    @Column(name = "birth_month")
    private Integer birthMonth;

    @Column(name = "birth_day")
    private Integer birthDay;

    // Comma-separated labels chosen from a fixed picklist on the mobile
    // side (see HolidayOptions.kt) -- the user's own explicit, self-
    // reported set of holidays they personally celebrate. Deliberately
    // separate from holiday_definition (the Feed's general
    // market/calendar occasions, not tied to any one person) -- this is
    // the explicit per-user preference the Interactive Feed spec said
    // would be needed before ever personalizing a culturally/religiously
    // specific holiday to a given user. Null/blank until set; optional,
    // same as birthday.
    @Column(name = "holidays_celebrated")
    private String holidaysCelebrated;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AppUser() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public String getProfilePictureBase64() {
        return profilePictureBase64;
    }

    public void setProfilePictureBase64(String profilePictureBase64) {
        this.profilePictureBase64 = profilePictureBase64;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
