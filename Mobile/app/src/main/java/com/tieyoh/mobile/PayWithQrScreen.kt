package com.tieyoh.mobile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch

// Reached by tapping "Pay with QR" on the My Card screen
// (ChooseHowToPaySection, MainActivity.kt). Shown to a partnered
// merchant's scanning app (a separate app, not built yet) to pay from the
// user's Tieyoh balance. No avatar/name/ID header at the top of this
// screen (unlike MyQrCodeScreen) -- the screen opens straight on the
// title, and the user's name + official Tieyoh ID are printed in the
// white space under the QR code itself instead, like a physical card.
//
// *** PHASE 1 OF 4 -- MOBILE UI ONLY. ***
// Per the build plan the product owner reviewed and approved before this
// screen was written, this ships with the QR payload clearly stubbed. It
// is NOT a real payment credential yet:
//   - generateStubPaymentQrPayload() below returns a placeholder string,
//     not a signed/encrypted token. There is no key material, no expiry,
//     and nothing server-issued behind it.
//   - Backend/tieyoh-api has no endpoint to mint a real token yet, no
//     balance ledger, and no merchant registry/eligibility check
//     (confirmed: that service is still just the bare Spring Boot
//     skeleton -- TieyohApiApplication.java and nothing else).
//   - The merchant-side scanning app that would read this code doesn't
//     exist yet either.
// Phases 2-4 -- a real backend-minted short-lived single-use signed token,
// the balance + merchant-registry backend behind it, and the redemption
// endpoint the merchant app calls -- are intentionally deferred to their
// own careful, security- and encryption-focused sessions. Nothing on this
// screen should be mistaken for production payment security, and nothing
// about a user's real balance or merchant eligibility is encoded here.
//
// "Share My QR Code" (added below "Find Partnered Merchants") lets someone
// who isn't standing in front of the merchant still get this code to them
// -- email it as an image attachment, or copy the image and paste it
// wherever it needs to go -- so a remote/online payment can still show the
// merchant the same code an in-person scan would use. Same Phase 1 caveat
// applies: it's sharing the stub image, not a live payment session.
//
// "Add to Wallet" is real, separately from the Phase 1 caveat above: it
// calls GoogleWalletApi for a signed save-to-Google-Wallet JWT
// (GoogleWalletService on the backend) and launches Google Wallet's own
// on-device save flow via GoogleWalletHelper.kt. The pass it creates
// mirrors qrPayload exactly -- today that's still the Phase 1 stub above,
// so the wallet pass is just as much a stub as the in-app QR code is;
// once generateStubPaymentQrPayload() is swapped for a real backend-minted
// token, the wallet pass automatically starts encoding the real one too,
// with no further change needed here.
@Composable
fun PayWithQrScreen(
    currentUser: AuthResponse,
    onBack: () -> Unit,
    onFindMerchants: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val walletAvailable by rememberGoogleWalletAvailable()
    val tieyohId = currentUser.tieyohId ?: ""

    // PLACEHOLDER ONLY -- see the Phase 1 note above. A real payment QR
    // must never encode raw balance/account data; it should be an opaque,
    // short-lived, single-use, server-minted token that a merchant's scan
    // exchanges for a live approve/decline call over HTTPS (Phase 2-4).
    // This stub exists purely so the mobile screen has *something* to
    // render during Phase 1 -- swap the body of this function out for a
    // real token fetched from the backend once it exists; nothing else on
    // this screen should need to change when that happens.
    fun generateStubPaymentQrPayload(): String =
        "TIEYOH-PAY-STUB:${currentUser.id}"

    val qrPayload = remember(currentUser.id) { generateStubPaymentQrPayload() }
    val qrBitmap = remember(qrPayload) { QrCodeGenerator.generate(qrPayload, sizePx = 480, marginModules = 1) }

    var showShareOptions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TieyohNavy
                )
            }
            Text(
                text = "My Card",
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(TieyohMuted.copy(alpha = 0.08f))
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen opens directly on this title -- big and bold, same
            // weight/color schema (FontWeight.Bold, TieyohNavy) every other
            // heading in the app uses, just a size step up since it's now
            // the screen's whole opener rather than a sub-heading under a
            // photo/name block.
            Text(
                text = "My Payment QR Code",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = TieyohNavy,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Show this code to a partnered merchant to pay from your Tieyoh balance.",
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = TieyohMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            // One white card holds the QR code and, in the white space
            // right below it, the account holder's name and official
            // Tieyoh ID -- same idea as the printed line under a barcode
            // on a physical membership/payment card. Kept snug (no big
            // gap) per the reference: name/ID sit close under the code.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(vertical = 12.dp, horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(268.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Payment QR code for $tieyohId",
                            modifier = Modifier.fillMaxSize()
                        )
                        // Brand mark over the QR -- QR error correction
                        // tolerates a logo covering the center. A rounded-
                        // square badge (not a circle) with the real "t"
                        // glyph cropped straight out of the official
                        // tieyoh wordmark asset (tieyoh_logo_transparent.png,
                        // just retinted to the brand blue used for accent
                        // marks) so this is pixel-for-pixel the same
                        // typeface as the real logo, not an approximation
                        // typed in a system font.
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_qr_brand_t),
                                contentDescription = "tieyoh",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .height(38.dp)
                                    .padding(bottom = 2.dp)
                            )
                        }
                    } else {
                        Text("Couldn't generate QR code", color = TieyohMuted, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = currentUser.displayName.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = TieyohNavy
                )

                if (tieyohId.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    // tieyohId is already stored with its official "@t-"
                    // prefix (see ChooseIdScreen's TIEYOH_ID_PREFIX) -- show
                    // it as-is, same as the header on the My Card screen.
                    Text(
                        text = tieyohId,
                        color = TieyohBrandDarkBlue,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = TieyohMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Unique to your account — used only at checkout",
                    fontSize = 12.sp,
                    color = TieyohMuted
                )
            }

            Spacer(Modifier.height(12.dp))

            // Same "Add to Wallet" button as the Pay with Card screen, at
            // this screen's standard button size (matching "Share My QR
            // Code" right below it) rather than that screen's enlarged
            // hero-CTA sizing -- WalletStackGlyph is declared internal
            // there specifically so it can be reused here without
            // duplicating the glyph.
            TieyohPrimaryButton(
                text = "Add to Wallet",
                onClick = {
                    if (!walletAvailable) {
                        Toast.makeText(context, "Google Wallet isn't available on this device.", Toast.LENGTH_SHORT).show()
                    } else {
                        scope.launch {
                            try {
                                val response = RetrofitClient.googleWalletApi.getQrCardSaveJwt(currentUser.id, qrPayload)
                                if (response.configured && response.saveJwt != null) {
                                    launchAddToGoogleWallet(context, response.saveJwt, GOOGLE_WALLET_QR_CARD_REQUEST_CODE)
                                } else {
                                    Toast.makeText(context, "Google Wallet isn't set up for Tieyoh yet.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Couldn't add to Google Wallet. Try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                leadingContent = { WalletStackGlyph(size = 32.dp) },
                backgroundColor = TieyohNavy
            )

            Spacer(Modifier.height(10.dp))

            // Lets someone pay remotely: this opens a small sheet to email
            // the QR code image or copy it, rather than requiring the
            // merchant to physically scan the phone screen.
            TieyohPrimaryButton(
                text = "Share My QR Code",
                onClick = { showShareOptions = true },
                icon = Icons.Filled.IosShare,
                backgroundColor = TieyohNavy
            )

            Spacer(Modifier.height(10.dp))

            TieyohSecondaryButton(
                text = "Find Partnered Merchants",
                onClick = onFindMerchants,
                icon = Icons.Filled.LocationOn,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(TieyohAccent.copy(alpha = 0.10f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TieyohAccent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = TieyohAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("How this works", fontWeight = FontWeight.Bold, color = TieyohAccent, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "The merchant scans your code to confirm you're approved to pay there and have enough balance. Your card and account details are never shared with them directly.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = TieyohMuted
                )
            }
        }

        // Same FAB-clearance note as MyQrCodeScreen: the app-wide fix lives
        // in TieyohApp's Surface padding, this is just normal breathing
        // room below the tip card.
        Spacer(Modifier.height(20.dp))
    }

    if (showShareOptions && qrBitmap != null) {
        Dialog(onDismissRequest = { showShareOptions = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Share My QR Code",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TieyohNavy
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Send this code to a merchant so you can pay them remotely.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TieyohMuted
                )

                Spacer(Modifier.height(16.dp))

                ShareQrOptionRow(
                    icon = Icons.Filled.Email,
                    label = "Email QR code",
                    onClick = {
                        showShareOptions = false
                        shareQrCodeByEmail(context, qrBitmap)
                    }
                )

                Spacer(Modifier.height(10.dp))

                ShareQrOptionRow(
                    icon = Icons.Filled.ContentCopy,
                    label = "Copy QR code image",
                    onClick = {
                        showShareOptions = false
                        copyQrCodeImage(context, qrBitmap)
                    }
                )
            }
        }
    }
}

// One row in the "Share My QR Code" sheet -- an icon, a label, and the
// same tap target styling for both Email and Copy so neither reads as the
// more/less important option.
@Composable
private fun ShareQrOptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TieyohMuted.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TieyohBrandDarkBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TieyohNavy
        )
    }
}

// Writes the current QR bitmap out to a cache file and hands back a
// FileProvider content:// URI other apps (Mail, the clipboard, etc.) can
// actually read -- a raw file:// URI would throw FileUriExposedException
// on modern Android. Shared by both share paths below. Returns null (and
// callers show a toast instead of crashing) if the write fails for any
// reason.
private fun qrImageUriFor(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(imagesDir, "tieyoh_payment_qr.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        null
    }
}

// Opens the system share sheet (Mail, Messages, Drive, or any other app
// that accepts an image) with the QR code image attached -- the "email it
// to a merchant" path, so someone who isn't standing in front of them can
// still get scanned and pay remotely.
private fun shareQrCodeByEmail(context: Context, bitmap: Bitmap) {
    val uri = qrImageUriFor(context, bitmap)
    if (uri == null) {
        Toast.makeText(context, "Couldn't prepare the QR code to share.", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "My Tieyoh Payment QR Code")
        putExtra(Intent.EXTRA_TEXT, "Scan this code to pay me on Tieyoh.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share QR code"))
}

// Copies the QR code image itself (not just a text link) onto the system
// clipboard so it can be pasted straight into an email, a chat, or
// wherever else the merchant needs it.
private fun copyQrCodeImage(context: Context, bitmap: Bitmap) {
    val uri = qrImageUriFor(context, bitmap)
    if (uri == null) {
        Toast.makeText(context, "Couldn't prepare the QR code to copy.", Toast.LENGTH_SHORT).show()
        return
    }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newUri(context.contentResolver, "Tieyoh Payment QR Code", uri))
    Toast.makeText(context, "QR code copied", Toast.LENGTH_SHORT).show()
}
