package com.tieyoh.mobile

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy

// Reached by tapping the small QR code preview on the Profile tab (see
// ProfileScreen in MainActivity.kt). Matches the "My QR Code" reference
// image: fonts, colors, and the same button standard used everywhere else
// in the app (TieyohPrimaryButton / TieyohSecondaryButton), just with
// leading icons here to match that reference's layout.
//
// The "link" behind Share/Copy/the QR code itself is a placeholder in the
// invitation link's own domain shape (https://tieyoh.com/u/<id>) -- there's
// no public profile route on the backend that resolves a URL like this yet
// (the only real link-generation in this codebase is InvitationService's
// one-time, 30-day invite tokens, a different feature -- see
// tieyoh-invite-system-fix.md "Fix, part 5: real deep links", which is
// scoped but explicitly not-started even for THAT link type). Sharing/
// copying still works right now -- it puts real text on the clipboard or
// into the share sheet -- and the QR now encodes this same URL (not the
// bare @t- id) so a generic phone camera at least recognizes it as a link
// instead of inert text. But scanning it still won't pop up "download
// Tieyoh and follow this person" -- that needs the same Android App
// Links / hosted assetlinks.json / real landing page / deferred-install
// linking that part 5 already calls for, none of which exists yet for
// any link in this app. Flagging here rather than quietly shipping a
// dead link as if it were live.
@Composable
fun MyQrCodeScreen(
    currentUser: AuthResponse,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val tieyohId = currentUser.tieyohId ?: ""
    val handle = tieyohId.removePrefix("@t-").removePrefix("@").ifBlank { currentUser.displayName }
    val profileLink = "https://tieyoh.com/u/$handle"

    val avatarBitmap = remember(currentUser.profilePictureBase64) {
        currentUser.profilePictureBase64?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }
    val initials = currentUser.displayName.trim().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    val qrBitmap = remember(profileLink) { QrCodeGenerator.generate(profileLink, sizePx = 480) }

    fun share() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Follow me on Tieyoh")
            putExtra(Intent.EXTRA_TEXT, "Follow me on Tieyoh! $profileLink")
        }
        context.startActivity(Intent.createChooser(intent, "Share QR code"))
    }

    fun copyLink() {
        clipboardManager.setText(AnnotatedString(profileLink))
        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
    }

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
                text = "Profile",
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(TieyohAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (avatarBitmap != null) {
                Image(
                    bitmap = avatarBitmap.asImageBitmap(),
                    contentDescription = "${currentUser.displayName}'s profile picture",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    text = initials.ifBlank { "?" },
                    color = TieyohAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = currentUser.displayName,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = TieyohNavy
        )

        if (tieyohId.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "@$handle",
                color = TieyohMuted,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(TieyohMuted.copy(alpha = 0.08f))
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "My QR Code",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TieyohNavy
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Have someone scan your QR code to follow you or find you on Tieyoh.",
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = TieyohMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(196.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code for $tieyohId",
                        modifier = Modifier.fillMaxSize()
                    )
                    // The small brand mark sitting on top of the QR pattern
                    // -- most QR readers tolerate a logo covering the
                    // center since the format carries its own error
                    // correction, same idea as the reference image.
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "t",
                            color = TieyohBrandBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    }
                } else {
                    Text("Couldn't generate QR code", color = TieyohMuted, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            TieyohPrimaryButton(
                text = "Share QR Code",
                onClick = { share() },
                icon = Icons.Filled.IosShare
            )

            Spacer(Modifier.height(8.dp))

            TieyohSecondaryButton(
                text = "Copy My Link",
                onClick = { copyLink() },
                icon = Icons.Filled.ContentCopy
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
                    imageVector = Icons.Filled.Group,
                    contentDescription = null,
                    tint = TieyohAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Grow your circle", fontWeight = FontWeight.Bold, color = TieyohAccent, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Share your QR code so friends and family can follow you on Tieyoh.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = TieyohMuted
                )
            }
        }

        // The FAB-clearance fix now lives once, app-wide, in TieyohApp's
        // Surface padding (MainActivity.kt) -- this is just normal
        // breathing room below the tip card, not a FAB workaround.
        Spacer(Modifier.height(20.dp))
    }
}
