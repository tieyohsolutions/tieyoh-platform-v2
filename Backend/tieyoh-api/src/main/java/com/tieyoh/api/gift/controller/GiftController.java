package com.tieyoh.api.gift.controller;

import com.tieyoh.api.gift.dto.GiftCommentRequest;
import com.tieyoh.api.gift.dto.GiftCommentResult;
import com.tieyoh.api.gift.dto.GiftDetailResult;
import com.tieyoh.api.gift.dto.GiftRequest;
import com.tieyoh.api.gift.dto.GiftResult;
import com.tieyoh.api.gift.service.GiftService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gifts")
public class GiftController {

    private final GiftService giftService;

    public GiftController(GiftService giftService) {
        this.giftService = giftService;
    }

    @PostMapping
    public GiftResult sendGift(@Valid @RequestBody GiftRequest request) {
        return giftService.sendGift(request);
    }

    // Backs GiftReceivedScreen -- fetched by gift id from a GIFT_RECEIVED
    // notification tap (or, once App Links deep-linking is built, from a
    // tapped invite text) rather than carried in memory the way the
    // sender's own confirmation screen has it right after sendGift().
    @GetMapping("/{id}")
    public GiftDetailResult getGift(@PathVariable Long id) {
        return giftService.getGiftDetail(id);
    }

    @PostMapping("/{giftId}/comments")
    public GiftCommentResult addComment(@PathVariable Long giftId, @Valid @RequestBody GiftCommentRequest request) {
        return giftService.addComment(giftId, request);
    }

    @GetMapping("/{giftId}/comments")
    public List<GiftCommentResult> getComments(@PathVariable Long giftId) {
        return giftService.getComments(giftId);
    }
}
