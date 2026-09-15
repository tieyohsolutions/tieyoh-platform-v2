package com.tieyoh.api.auth.dto;

import java.util.List;

// Body for POST /api/users/{ownerTieyohId}/friends/check-contacts. Reuses
// InvitationRecipientRequest's displayName/phone/email shape rather than
// introducing a near-identical DTO -- this is the same "one contact"
// shape the invite-batch flow already sends, just for a read-only check
// instead of a real invite/add.
public class ContactMatchBatchRequest {

    private List<InvitationRecipientRequest> contacts;

    public ContactMatchBatchRequest() {
    }

    public List<InvitationRecipientRequest> getContacts() {
        return contacts;
    }

    public void setContacts(List<InvitationRecipientRequest> contacts) {
        this.contacts = contacts;
    }
}
