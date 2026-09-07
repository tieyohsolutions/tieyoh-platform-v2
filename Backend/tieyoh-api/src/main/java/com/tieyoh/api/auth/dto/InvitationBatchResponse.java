package com.tieyoh.api.auth.dto;

import java.util.List;

public class InvitationBatchResponse {

    private List<InvitationResult> invitations;

    public InvitationBatchResponse() {
    }

    public InvitationBatchResponse(List<InvitationResult> invitations) {
        this.invitations = invitations;
    }

    public List<InvitationResult> getInvitations() {
        return invitations;
    }

    public void setInvitations(List<InvitationResult> invitations) {
        this.invitations = invitations;
    }
}
