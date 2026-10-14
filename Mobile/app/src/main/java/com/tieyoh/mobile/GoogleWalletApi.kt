package com.tieyoh.mobile

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Real, working "Add to Google Wallet" endpoints -- see
// Backend/tieyoh-api's GoogleWalletService for the server side. Both
// return configured = false with no saveJwt until Tieyoh's real Google
// Wallet issuer ID + service account key are dropped into the backend's
// config (GOOGLE_WALLET_ISSUER_ID / GOOGLE_WALLET_SERVICE_ACCOUNT_KEY_PATH)
// -- honest gap, not a stub: once those two values exist, these calls
// start returning a real signed JWT and the save flow just works, no
// other code on either side needs to change.
data class GoogleWalletSaveResponse(
    val configured: Boolean,
    val saveJwt: String?
)

interface GoogleWalletApi {

    // qrPayload is whatever PayWithQrScreen is already showing as the QR
    // code (today: the Phase-1 stub payload; later: the real backend-minted
    // token) -- the wallet pass's barcode always mirrors the same value the
    // in-app QR code shows, so scanning either one reads identically.
    @GET("api/users/{userId}/wallet/google/qr-card")
    suspend fun getQrCardSaveJwt(
        @Path("userId") userId: Long,
        @Query("qrPayload") qrPayload: String
    ): GoogleWalletSaveResponse

    // Dummy/visual-only pass for now -- mirrors the card face shown at the
    // top of PayWithCardScreen (Tieyoh branding + cardholder name), no
    // barcode and no real payment capability. Swap this out once Highnote
    // provisioning is wired in for PayWithCardScreen; this endpoint is the
    // only thing that needs to change on the backend side to start minting
    // a real provisioned-card pass instead.
    @GET("api/users/{userId}/wallet/google/prepaid-card")
    suspend fun getPrepaidCardSaveJwt(@Path("userId") userId: Long): GoogleWalletSaveResponse
}
