package com.tieyoh.api.wallet.controller;

import com.tieyoh.api.wallet.dto.GoogleWalletSaveResponse;
import com.tieyoh.api.wallet.service.GoogleWalletService;
import org.springframework.web.bind.annotation.*;

// Backs the "Add to Wallet" buttons on PayWithQrScreen and
// PayWithCardScreen -- see GoogleWalletService for what each pass
// actually contains and the honest-gap note on "configured: false".
@RestController
@RequestMapping("/api/users/{userId}/wallet/google")
public class GoogleWalletController {

    private final GoogleWalletService googleWalletService;

    public GoogleWalletController(GoogleWalletService googleWalletService) {
        this.googleWalletService = googleWalletService;
    }

    @GetMapping("/qr-card")
    public GoogleWalletSaveResponse getQrCardSaveJwt(
            @PathVariable Long userId,
            @RequestParam String qrPayload) {
        return googleWalletService.getQrCardSaveJwt(userId, qrPayload);
    }

    @GetMapping("/prepaid-card")
    public GoogleWalletSaveResponse getPrepaidCardSaveJwt(@PathVariable Long userId) {
        return googleWalletService.getPrepaidCardSaveJwt(userId);
    }
}
