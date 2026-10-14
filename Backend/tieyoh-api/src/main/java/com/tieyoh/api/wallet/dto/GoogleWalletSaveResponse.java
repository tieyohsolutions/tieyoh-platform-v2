package com.tieyoh.api.wallet.dto;

// configured = false (saveJwt null) means Tieyoh doesn't have a Google
// Wallet issuer account wired up yet -- see GoogleWalletService's header
// comment. The mobile client shows an honest "not set up yet" message in
// that case rather than trying to launch a save flow with nothing to save.
public class GoogleWalletSaveResponse {

    private boolean configured;
    private String saveJwt;

    public GoogleWalletSaveResponse() {
    }

    public GoogleWalletSaveResponse(boolean configured, String saveJwt) {
        this.configured = configured;
        this.saveJwt = saveJwt;
    }

    public static GoogleWalletSaveResponse notConfigured() {
        return new GoogleWalletSaveResponse(false, null);
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getSaveJwt() {
        return saveJwt;
    }

    public void setSaveJwt(String saveJwt) {
        this.saveJwt = saveJwt;
    }
}
