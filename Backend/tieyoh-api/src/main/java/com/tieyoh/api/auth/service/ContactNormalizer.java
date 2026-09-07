package com.tieyoh.api.auth.service;

import java.util.Locale;

// Shared phone/email normalization so the same real-world contact is
// recognized as "the same person" everywhere it matters -- deduping circle
// entries in FriendService and hashing recipients in InvitationService --
// no matter which screen or flow the raw string came from (typed by hand,
// read off a device contact via the single-contact picker, or read off the
// device address book in bulk via Invite Friends).
public final class ContactNormalizer {

    private ContactNormalizer() {
    }

    public static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c) || (c == '+' && sb.length() == 0)) {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
