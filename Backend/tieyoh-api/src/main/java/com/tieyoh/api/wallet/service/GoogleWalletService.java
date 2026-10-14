package com.tieyoh.api.wallet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tieyoh.api.auth.entity.AppUser;
import com.tieyoh.api.auth.repository.AppUserRepository;
import com.tieyoh.api.wallet.dto.GoogleWalletSaveResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Real "Add to Google Wallet" integration -- not a stub. Two passes:
//   - the QR pass (getQrCardSaveJwt): mirrors whatever qrPayload
//     PayWithQrScreen is already showing as a barcode, so scanning either
//     one reads identically. Swaps in the real payment token automatically
//     once the mobile side stops calling generateStubPaymentQrPayload().
//   - the prepaid-card pass (getPrepaidCardSaveJwt): dummy/visual only --
//     Tieyoh branding + cardholder name, same as PrepaidCardFace on
//     PayWithCardScreen, no barcode and no payment capability. Meant to be
//     replaced once Highnote provisioning gives PayWithCardScreen a real
//     issued card; only this method's object fields need to change then,
//     nothing on the mobile side or in the controller.
//
// Auth model: a downloaded service-account JSON key, loaded from disk and
// used to sign JWTs locally (RS256). This project briefly hit a dead end
// where "iam.disableServiceAccountKeyCreation" blocked key downloads
// entirely, and the service was rewritten to impersonate the service
// account via Google's IAM Credentials API instead (no key file). That
// blocker got resolved (see project notes) and a real key was downloaded
// after all, so this reverts back to the simpler, standard approach:
// loading the key file directly, same as Google's own Wallet docs show.
// Keep the key file out of git -- it's a real secret.
//
// Both public methods return GoogleWalletSaveResponse.notConfigured() --
// configured: false, no JWT, nothing thrown -- until google.wallet.issuer-id
// and google.wallet.service-account-key-path are both set to real values
// (see application.properties). That's the ONLY thing standing between
// this and actually working: the REST calls to Google, the JWT signing,
// all of it is real and exercised the moment those two config values
// exist and the key file is actually there.
@Service
public class GoogleWalletService {

    private static final Logger log = LoggerFactory.getLogger(GoogleWalletService.class);

    private static final String WALLET_API_BASE = "https://walletobjects.googleapis.com/walletobjects/v1";
    private static final String WALLET_SCOPE = "https://www.googleapis.com/auth/wallet_object.issuer";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    // TieyohBrandBlue (Color.kt) -- the blue end of the app's primary-button
    // gradient / the blue end of the "yoh" wordmark gradient in the real
    // logo. Wallet passes only support a single solid hexBackgroundColor
    // (no gradients), so this is the closest single-color match to the
    // actual brand mark now used as the pass logo. Kept in sync by hand
    // since this service has no reason to depend on the mobile module --
    // if TieyohBrandBlue ever changes, update this literal too.
    private static final String BRAND_BLUE_HEX = "#1565FF";

    private final AppUserRepository appUserRepository;
    private final String issuerId;
    private final String serviceAccountKeyPath;
    private final String logoUrl;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Parsed lazily from serviceAccountKeyPath on first use and cached --
    // credentialsLoadAttempted distinguishes "haven't tried yet" from
    // "tried and the key file is missing/bad" so a misconfigured deploy
    // doesn't re-attempt file IO + parsing on every single request.
    private volatile boolean credentialsLoadAttempted = false;
    private volatile String serviceAccountEmail;
    private volatile PrivateKey privateKey;

    private volatile String cachedAccessToken;
    private volatile Instant cachedAccessTokenExpiry = Instant.EPOCH;

    public GoogleWalletService(
            AppUserRepository appUserRepository,
            @Value("${google.wallet.issuer-id:}") String issuerId,
            @Value("${google.wallet.service-account-key-path:}") String serviceAccountKeyPath,
            @Value("${google.wallet.logo-url:}") String logoUrl) {
        this.appUserRepository = appUserRepository;
        this.issuerId = issuerId;
        this.serviceAccountKeyPath = serviceAccountKeyPath;
        this.logoUrl = logoUrl;
    }

    public boolean isConfigured() {
        return issuerId != null && !issuerId.isBlank() && loadCredentials();
    }

    public GoogleWalletSaveResponse getQrCardSaveJwt(Long userId, String qrPayload) {
        if (!isConfigured()) {
            return GoogleWalletSaveResponse.notConfigured();
        }
        AppUser user = findUser(userId);

        String classId = issuerId + ".tieyoh_qr_card";
        String objectId = issuerId + ".qr_card_" + user.getId();

        Map<String, Object> object = new LinkedHashMap<>();
        object.put("id", objectId);
        object.put("classId", classId);
        object.put("genericType", "GENERIC_TYPE_UNSPECIFIED");
        object.put("hexBackgroundColor", BRAND_BLUE_HEX);
        object.put("cardTitle", localizedValue("Tieyoh Pay"));
        object.put("header", localizedValue(user.getDisplayName()));
        if (user.getTieyohId() != null && !user.getTieyohId().isBlank()) {
            object.put("subheader", localizedValue(user.getTieyohId()));
        }
        object.put("textModulesData", List.of(
                textModule("pay_note", "Use at checkout",
                        "Scan this at a partnered merchant to pay from your Tieyoh balance.")
        ));
        Map<String, Object> barcode = new LinkedHashMap<>();
        barcode.put("type", "QR_CODE");
        barcode.put("value", qrPayload);
        object.put("barcode", barcode);
        applyLogo(object);

        return upsertAndBuildResponse(classId, objectId, object);
    }

    public GoogleWalletSaveResponse getPrepaidCardSaveJwt(Long userId) {
        if (!isConfigured()) {
            return GoogleWalletSaveResponse.notConfigured();
        }
        AppUser user = findUser(userId);

        String classId = issuerId + ".tieyoh_prepaid_card";
        String objectId = issuerId + ".prepaid_card_" + user.getId();

        Map<String, Object> object = new LinkedHashMap<>();
        object.put("id", objectId);
        object.put("classId", classId);
        object.put("genericType", "GENERIC_TYPE_UNSPECIFIED");
        object.put("hexBackgroundColor", BRAND_BLUE_HEX);
        object.put("cardTitle", localizedValue("My Gift Card"));
        object.put("header", localizedValue(user.getDisplayName()));
        object.put("subheader", localizedValue("use for all my gifts"));
        object.put("textModulesData", List.of(
                textModule("status_note", "Status",
                        "Visual only for now -- real provisioning is coming.")
        ));
        applyLogo(object);

        return upsertAndBuildResponse(classId, objectId, object);
    }

    private GoogleWalletSaveResponse upsertAndBuildResponse(String classId, String objectId, Map<String, Object> object) {
        ensureClassExists(classId);
        upsertObject(objectId, object);
        return new GoogleWalletSaveResponse(true, buildSaveJwt(objectId, classId));
    }

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void applyLogo(Map<String, Object> object) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return;
        }
        Map<String, Object> sourceUri = new LinkedHashMap<>();
        sourceUri.put("uri", logoUrl);
        Map<String, Object> logo = new LinkedHashMap<>();
        logo.put("sourceUri", sourceUri);
        object.put("logo", logo);
    }

    private Map<String, Object> localizedValue(String value) {
        Map<String, Object> translated = new LinkedHashMap<>();
        translated.put("language", "en-US");
        translated.put("value", value);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("defaultValue", translated);
        return wrapper;
    }

    private Map<String, Object> textModule(String id, String header, String body) {
        Map<String, Object> module = new LinkedHashMap<>();
        module.put("id", id);
        module.put("header", header);
        module.put("body", body);
        return module;
    }

    // Insert-or-ignore: classes rarely change once created, so a 409
    // (already exists) just means a previous call already created it.
    private void ensureClassExists(String classId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", classId);
        try {
            restClient.post()
                    .uri(WALLET_API_BASE + "/genericClass")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict alreadyExists) {
            // Fine -- nothing on the class itself needs updating.
        }
    }

    // Insert-or-update: a 409 means the user already added this pass
    // before, so PUT refreshes its fields (name, QR payload, etc.) in case
    // anything has changed since.
    private void upsertObject(String objectId, Map<String, Object> object) {
        try {
            restClient.post()
                    .uri(WALLET_API_BASE + "/genericObject")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(object)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict alreadyExists) {
            restClient.put()
                    .uri(WALLET_API_BASE + "/genericObject/" + objectId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(object)
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    // The JWT Google Wallet's Android PayClient.savePassesJwt() consumes
    // to launch the on-device save flow -- same shape as the "Save to
    // Google Wallet" web link's JWT, just handed to PayClient instead of
    // embedded in a URL. References the object by id only (no origins
    // field -- that's a web-only concern) since it was just
    // inserted/updated above, so Google already has the current fields.
    private String buildSaveJwt(String objectId, String classId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("genericObjects", List.of(Map.of("id", objectId)));

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", serviceAccountEmail);
        claims.put("aud", "google");
        claims.put("typ", "savetowallet");
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("payload", payload);

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setClaims(claims)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    // OAuth2 JWT-bearer flow (RFC 7523): sign a short-lived assertion with
    // the same service-account key and trade it for a bearer token scoped
    // to the Wallet Objects API, cached until shortly before it expires.
    @SuppressWarnings("unchecked")
    private synchronized String fetchAccessToken() {
        Instant now = Instant.now();
        if (cachedAccessToken != null && now.isBefore(cachedAccessTokenExpiry)) {
            return cachedAccessToken;
        }

        Instant exp = now.plusSeconds(3600);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", serviceAccountEmail);
        claims.put("scope", WALLET_SCOPE);
        claims.put("aud", TOKEN_ENDPOINT);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", exp.getEpochSecond());

        String assertion = Jwts.builder()
                .setClaims(claims)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.add("assertion", assertion);

        Map<String, Object> response = restClient.post()
                .uri(TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        String accessToken = response != null ? (String) response.get("access_token") : null;
        if (accessToken == null) {
            throw new IllegalStateException("Google OAuth2 token exchange returned no access_token");
        }
        Number expiresIn = response.get("expires_in") instanceof Number n ? n : 3600;

        cachedAccessToken = accessToken;
        // Refresh a minute early so a request never straddles the actual expiry.
        cachedAccessTokenExpiry = now.plusSeconds(Math.max(60, expiresIn.longValue() - 60));
        return accessToken;
    }

    private boolean loadCredentials() {
        if (credentialsLoadAttempted) {
            return privateKey != null;
        }
        synchronized (this) {
            if (credentialsLoadAttempted) {
                return privateKey != null;
            }
            credentialsLoadAttempted = true;
            if (serviceAccountKeyPath == null || serviceAccountKeyPath.isBlank()) {
                return false;
            }
            try {
                JsonNode root = objectMapper.readTree(new File(serviceAccountKeyPath));
                String email = root.path("client_email").asText(null);
                String pem = root.path("private_key").asText(null);
                if (email == null || pem == null) {
                    log.warn("Google Wallet service account key at {} is missing client_email or private_key",
                            serviceAccountKeyPath);
                    return false;
                }
                String cleanedPem = pem
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
                byte[] der = Base64.getDecoder().decode(cleanedPem);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
                this.privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
                this.serviceAccountEmail = email;
                return true;
            } catch (Exception e) {
                log.warn("Failed to load Google Wallet service account key from {}", serviceAccountKeyPath, e);
                return false;
            }
        }
    }
}
