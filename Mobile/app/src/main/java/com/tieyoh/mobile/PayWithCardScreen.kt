package com.tieyoh.mobile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBlue
import com.tieyoh.mobile.ui.theme.TieyohCardFaceEnd
import com.tieyoh.mobile.ui.theme.TieyohCardFaceMid
import com.tieyoh.mobile.ui.theme.TieyohCardFaceStart
import com.tieyoh.mobile.ui.theme.TieyohGiftRed
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohSponsoredLogoBg

// Reached by tapping "Pay with Card" on the My Card screen
// (ChooseHowToPaySection -> "All Other Merchants" tile). Same page shape
// as PayWithQrScreen right next to it: back arrow, big bold title +
// centered muted subtitle in a soft rounded wrapper, one primary action
// button, then a stack of secondary rows and an info callout below.
//
// The card face below the title reuses TieyohCardFaceStart/Mid/End --
// three color tokens already defined in Color.kt for exactly this ("My
// Card landing page... soft tints for the card face's background
// gradient") but never actually used anywhere until now.
//
// Card Details / Manage Card / Set as Default Payment Method and "Add to
// Wallet" are all placeholders for now, same as every other coming-soon
// action already on MyCardActionsSection (wallet provisioning, virtual
// card, transaction history, etc.) -- there's no prepaid-card backend yet
// (balance, card issuance/tokenization, wallet provisioning APIs), so
// nothing here is wired to a real card or a real wallet push.
@Composable
fun PayWithCardScreen(
    currentUser: AuthResponse,
    onBack: () -> Unit
) {
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
            Text(
                text = "My Card",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = TieyohNavy,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Add your tieyoh card to your mobile wallet for easy, secure payments.",
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = TieyohMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            PrepaidCardFace(currentUser = currentUser)

            Spacer(Modifier.height(14.dp))

            // Same size as the "Add to Wallet" button on the Pay with QR
            // screen -- this screen's standard button sizing rather than
            // the earlier enlarged hero-CTA treatment.
            TieyohPrimaryButton(
                text = "Add to Wallet",
                onClick = { /* Apple/Google/Samsung Wallet provisioning -- coming soon, see MyCardActionsSection's identical placeholder */ },
                leadingContent = { WalletStackGlyph(size = 32.dp) },
                backgroundColor = TieyohNavy
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Automatically adds to Apple Wallet or Samsung Wallet based on your device.",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = TieyohMuted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MyCardActionRow(
                modifier = Modifier.fillMaxWidth(),
                title = "Card Details",
                subtitle = "View card information",
                icon = { color -> Icon(imageVector = Icons.Filled.CreditCard, contentDescription = null, tint = color, modifier = Modifier.size(26.dp)) },
                onClick = { /* card details -- coming soon, no prepaid-card backend yet */ }
            )
            MyCardActionRow(
                modifier = Modifier.fillMaxWidth(),
                title = "Manage Card",
                subtitle = "Freeze, replace or settings",
                icon = { color -> Icon(imageVector = Icons.Filled.Settings, contentDescription = null, tint = color, modifier = Modifier.size(26.dp)) },
                onClick = { /* freeze/replace/settings -- coming soon */ }
            )
            MyCardActionRow(
                modifier = Modifier.fillMaxWidth(),
                title = "Set as Default Payment Method",
                subtitle = "Use for online checkout",
                icon = { color -> Icon(imageVector = Icons.Filled.RadioButtonChecked, contentDescription = null, tint = color, modifier = Modifier.size(26.dp)) },
                onClick = { /* default payment method -- coming soon */ }
            )
            // Moved here from the main My Card page's MyCardActionsSection --
            // generating a single-use virtual card number is something you do
            // with this specific prepaid card, not a generic My Card action,
            // so it belongs on the card's own screen alongside Card
            // Details/Manage Card/Set as Default rather than a level up.
            // VirtualCardGlyph is defined in MainActivity.kt (internal, same
            // package) since it lives in that file's shared "My Card glyphs"
            // section with the rest of this screen family's hand-drawn icons.
            MyCardActionRow(
                modifier = Modifier.fillMaxWidth(),
                title = "One-Time Virtual Card",
                subtitle = "For online purchases",
                icon = { color -> VirtualCardGlyph(color = color, size = 26.dp) },
                onClick = { /* single-use virtual card number -- coming soon */ }
            )
        }

        Spacer(Modifier.height(10.dp))

        // Same "How this works" info-row shape as PayWithQrScreen, just
        // retitled/recolored for this card (TieyohAccent is the app's
        // teal/green accent token -- reused here rather than a new color).
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
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = TieyohAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Your card is secure.", fontWeight = FontWeight.Bold, color = TieyohAccent, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Payments are processed using industry-standard security and tokenization.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = TieyohMuted
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

// The physical/virtual card face: tieyoh wordmark top-left, "My Gift
// Card" label top-right (this one card doubles as the gift card, per the
// reference design), and the account holder's name pinned to the bottom
// -- same treatment (bold, uppercase, navy) as the name under the QR code
// on PayWithQrScreen, so the two "your card" surfaces read as the same
// physical object.
@Composable
private fun PrepaidCardFace(currentUser: AuthResponse) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(TieyohCardFaceStart, TieyohCardFaceMid, TieyohCardFaceEnd)
                )
            )
            // Light border so the card's edge reads clearly against the
            // page's own near-white background instead of blending into it.
            .border(width = 1.dp, color = TieyohNavy.copy(alpha = 0.14f), shape = RoundedCornerShape(18.dp))
    ) {
        // Decorative wave motif across the lower half of the card, same
        // idea as the reference design -- a placeholder shape for now
        // (two soft overlapping curves) until the exact wave reference
        // image is in hand, at which point only this Canvas block should
        // need to change.
        CardWaveOverlay(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tieyoh_logo_transparent),
                    contentDescription = "tieyoh",
                    modifier = Modifier.height(38.dp)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "My Gift Card",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TieyohNavy
                    )
                    Text(
                        text = "use for all my gifts",
                        fontSize = 13.sp,
                        color = TieyohMuted
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = currentUser.displayName.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TieyohNavy
            )
        }
    }
}

// Two soft, overlapping curves anchored to the bottom-right, lightest
// layer first so the second reads as sitting on top of it -- a generic
// "wave" placeholder built from the card face's own gradient tones
// (TieyohCardFaceMid/End) rather than a plain diagonal gradient. Swap the
// path math here once the actual wave reference image arrives; nothing
// else on this screen should need to change.
@Composable
private fun CardWaveOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val backWave = Path().apply {
            moveTo(0f, h * 0.68f)
            cubicTo(
                w * 0.32f, h * 0.48f,
                w * 0.58f, h * 0.92f,
                w, h * 0.58f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        // Lighter, not just a slightly different off-white -- a real
        // white highlight band so it actually separates from the base
        // gradient instead of blending into it.
        drawPath(path = backWave, color = Color.White.copy(alpha = 0.55f))

        val frontWave = Path().apply {
            moveTo(0f, h * 0.85f)
            cubicTo(
                w * 0.34f, h * 0.68f,
                w * 0.62f, h * 1.05f,
                w, h * 0.78f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        // Clearly bluer (TieyohBlue, not one of the near-white card-face
        // tints) at low alpha -- a visible second band instead of another
        // shade indistinguishable from the base gradient.
        drawPath(path = frontWave, color = TieyohBlue.copy(alpha = 0.16f))
    }
}

// "Add to Wallet" button icon: an original glyph in the same spirit as
// the reference image (four cards cascading out of a wallet pocket) --
// same layered-band composition and pocket notch, redrawn from scratch
// in Tieyoh's own brand colors and proportions rather than using that
// specific app's icon file directly.
@Composable
internal fun WalletStackGlyph(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 28f
        val cardLeft = 3f * s
        val cardWidth = 22f * s
        val cardHeight = 16f * s
        val cardRadius = CornerRadius(3f * s, 3f * s)
        val cardTops = listOf(1f * s, 4.5f * s, 8f * s, 11.5f * s)
        val cardColors = listOf(TieyohBlue, TieyohAccent, TieyohSponsoredLogoBg, TieyohGiftRed)

        // Each card is drawn full-size at a slightly lower position than
        // the last, so only a band near the top of each one peeks out
        // from behind the card drawn after it.
        cardTops.forEachIndexed { index, top ->
            drawRoundRect(
                color = cardColors[index],
                topLeft = Offset(cardLeft, top),
                size = Size(cardWidth, cardHeight),
                cornerRadius = cardRadius
            )
        }

        // Wallet pocket in front, covering the bottom of the frontmost
        // (red) card. Wider than the card stack, like the reference.
        val pocketTop = 17.5f * s
        val pocketHeight = 10.5f * s
        val centerX = this.size.width / 2f
        val pocketPath = Path().apply {
            val left = 0.5f * s
            val right = 27.5f * s
            val notchWidth = 8f * s
            val notchDepth = 3.5f * s
            moveTo(left + 3f * s, pocketTop)
            lineTo(centerX - notchWidth / 2f, pocketTop)
            quadraticBezierTo(centerX, pocketTop + notchDepth, centerX + notchWidth / 2f, pocketTop)
            lineTo(right - 3f * s, pocketTop)
            quadraticBezierTo(right, pocketTop, right, pocketTop + 3f * s)
            lineTo(right, pocketTop + pocketHeight - 3f * s)
            quadraticBezierTo(right, pocketTop + pocketHeight, right - 3f * s, pocketTop + pocketHeight)
            lineTo(left + 3f * s, pocketTop + pocketHeight)
            quadraticBezierTo(left, pocketTop + pocketHeight, left, pocketTop + pocketHeight - 3f * s)
            lineTo(left, pocketTop + 3f * s)
            quadraticBezierTo(left, pocketTop, left + 3f * s, pocketTop)
            close()
        }
        drawPath(path = pocketPath, color = Color(0xFFF4EFE7))
    }
}


