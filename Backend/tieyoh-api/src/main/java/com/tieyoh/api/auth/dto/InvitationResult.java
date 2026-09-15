package com.tieyoh.api.auth.dto;

public class InvitationResult {

    private String displayName;
    private String inviteUrl;

    public InvitationResult() {
    }

    public InvitationResult(String displayName, String inviteUrl) {
        this.displayName = displayName;
        this.inviteUrl = inviteUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getInviteUrl() {
        return inviteUrl;
    }

    public void setInviteUrl(String inviteUrl) {
        this.inviteUrl = inviteUrl;
    }
}
