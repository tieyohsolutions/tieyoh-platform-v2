package com.tieyoh.api.gift.service;

import com.tieyoh.api.auth.entity.AppUser;
import com.tieyoh.api.auth.repository.AppUserRepository;
import com.tieyoh.api.auth.service.NotificationService;
import com.tieyoh.api.gift.dto.GiftCommentRequest;
import com.tieyoh.api.gift.dto.GiftCommentResult;
import com.tieyoh.api.gift.dto.GiftDetailResult;
import com.tieyoh.api.gift.dto.GiftRequest;
import com.tieyoh.api.gift.dto.GiftResult;
import com.tieyoh.api.gift.entity.Gift;
import com.tieyoh.api.gift.entity.GiftComment;
import com.tieyoh.api.gift.repository.GiftCommentRepository;
import com.tieyoh.api.gift.repository.GiftRepository;
import com.tieyoh.api.merchant.entity.Merchant;
import com.tieyoh.api.merchant.repository.MerchantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// The actual "Send a Gift" backend -- there was nothing here at all
// before this (see tieyoh-platform-overview.md's "Not yet built" section
// for the prior state): picking a merchant in the app was a dead end.
// This is deliberately minimal, per the scoping conversation with the
// user: no real payment processing, no charge, no linked-payment-method
// check -- a gift is recorded COMPLETED the instant it's sent. That's
// enough for the Feed's GIFT_ACTIVITY event to have real data to show,
// which was the actual goal; wiring this up to a real payment rail is a
// separate, future project.
@Service
public class GiftService {

    private static final Set<String> VALID_VISIBILITIES = Set.of("PRIVATE", "CONTACTS", "PUBLIC");

    private static final int MAX_COMMENT_LENGTH = 240;

    private final GiftRepository giftRepository;
    private final AppUserRepository appUserRepository;
    private final MerchantRepository merchantRepository;
    private final NotificationService notificationService;
    private final GiftCommentRepository giftCommentRepository;

    public GiftService(GiftRepository giftRepository,
                        AppUserRepository appUserRepository,
                        MerchantRepository merchantRepository,
                        NotificationService notificationService,
                        GiftCommentRepository giftCommentRepository) {
        this.giftRepository = giftRepository;
        this.appUserRepository = appUserRepository;
        this.merchantRepository = merchantRepository;
        this.notificationService = notificationService;
        this.giftCommentRepository = giftCommentRepository;
    }

    public GiftResult sendGift(GiftRequest request) {
        if (request.getSenderId().equals(request.getRecipientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't send a gift to yourself");
        }

        AppUser sender = appUserRepository.findById(request.getSenderId())
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender not found"));

        AppUser recipient = appUserRepository.findById(request.getRecipientId())
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));

        Merchant merchant = null;
        if (request.getMerchantId() != null) {
            merchant = merchantRepository.findById(request.getMerchantId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found"));
        }

        String visibility = request.getVisibility();
        if (visibility == null || visibility.isBlank()) {
            visibility = "CONTACTS";
        } else if (!VALID_VISIBILITIES.contains(visibility)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "visibility must be one of " + VALID_VISIBILITIES);
        }

        Gift gift = new Gift();
        gift.setSenderId(sender.getId());
        gift.setRecipientId(recipient.getId());
        gift.setMerchantId(merchant == null ? null : merchant.getId());
        gift.setAmount(request.getAmount());
        gift.setVisibility(visibility);
        Gift saved = giftRepository.save(gift);

        // The extension point the Notification entity's own code comment
        // already called out: "adding GIFT_RECEIVED later is just a new
        // call to notify(...) from wherever a gift actually completes."
        String amountText = "$" + saved.getAmount().stripTrailingZeros().toPlainString();
        String merchantText = merchant == null ? "" : " for " + merchant.getBrandName();
        notificationService.notify(
                recipient.getId(),
                "GIFT_RECEIVED",
                sender.getId(),
                sender.getDisplayName(),
                sender.getDisplayName() + " sent you a " + amountText + " gift" + merchantText + ".",
                null,
                saved.getId());

        return toResult(saved);
    }

    // Backs GiftReceivedScreen: unlike GiftResult (which is just what
    // sendGift() itself needs to hand back), this resolves sender/
    // recipient/merchant display names too, since the recipient's
    // full-screen confirmation is reached later, from a notification tap,
    // with none of that context already in memory client-side the way the
    // sender's own confirmation has it right after submit().
    public GiftDetailResult getGiftDetail(Long id) {
        Gift gift = giftRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gift not found"));

        AppUser sender = appUserRepository.findById(gift.getSenderId()).orElse(null);
        AppUser recipient = appUserRepository.findById(gift.getRecipientId()).orElse(null);
        Merchant merchant = gift.getMerchantId() == null
                ? null
                : merchantRepository.findById(gift.getMerchantId()).orElse(null);

        return new GiftDetailResult(
                gift.getId(),
                gift.getSenderId(),
                sender == null ? null : sender.getDisplayName(),
                gift.getRecipientId(),
                recipient == null ? null : recipient.getDisplayName(),
                gift.getMerchantId(),
                merchant == null ? null : merchant.getBrandName(),
                gift.getAmount(),
                gift.getCurrency(),
                gift.getVisibility(),
                gift.getStatus(),
                gift.getCreatedAt() == null ? null : gift.getCreatedAt().toString());
    }

    // The "thank the sender" flow (and, symmetrically, a sender replying
    // back) -- deliberately restricted to the gift's own two parties, per
    // the spec's "only sender and receiver can comment" rule. Free text or
    // a single emoji both just land in the same `body` column; the client
    // decides which UI produced it.
    public GiftCommentResult addComment(Long giftId, GiftCommentRequest request) {
        Gift gift = giftRepository.findById(giftId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gift not found"));

        if (!request.getAuthorUserId().equals(gift.getSenderId())
                && !request.getAuthorUserId().equals(gift.getRecipientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the sender or recipient can comment on this gift");
        }

        String body = request.getBody() == null ? "" : request.getBody().trim();
        if (body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment can't be empty");
        }
        if (body.length() > MAX_COMMENT_LENGTH) {
            body = body.substring(0, MAX_COMMENT_LENGTH);
        }

        AppUser author = appUserRepository.findById(request.getAuthorUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found"));

        GiftComment comment = new GiftComment();
        comment.setGiftId(gift.getId());
        comment.setAuthorUserId(author.getId());
        comment.setAuthorDisplayName(author.getDisplayName());
        comment.setBody(body);
        GiftComment saved = giftCommentRepository.save(comment);

        Long otherPartyId = author.getId().equals(gift.getSenderId()) ? gift.getRecipientId() : gift.getSenderId();
        notificationService.notify(
                otherPartyId,
                "GIFT_COMMENT_RECEIVED",
                author.getId(),
                author.getDisplayName(),
                author.getDisplayName() + ": " + body,
                null,
                gift.getId());

        return toCommentResult(saved);
    }

    public List<GiftCommentResult> getComments(Long giftId) {
        return giftCommentRepository.findByGiftIdOrderByCreatedAtAsc(giftId)
                .stream()
                .map(this::toCommentResult)
                .collect(Collectors.toList());
    }

    private GiftCommentResult toCommentResult(GiftComment comment) {
        return new GiftCommentResult(
                comment.getId(),
                comment.getGiftId(),
                comment.getAuthorUserId(),
                comment.getAuthorDisplayName(),
                comment.getBody(),
                comment.getCreatedAt() == null ? null : comment.getCreatedAt().toString());
    }

    private GiftResult toResult(Gift gift) {
        return new GiftResult(
                gift.getId(),
                gift.getSenderId(),
                gift.getRecipientId(),
                gift.getMerchantId(),
                gift.getAmount(),
                gift.getCurrency(),
                gift.getVisibility(),
                gift.getStatus(),
                gift.getCreatedAt() == null ? null : gift.getCreatedAt().toString());
    }
}
