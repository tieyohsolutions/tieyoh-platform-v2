package com.tieyoh.mobile

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBlue
import com.tieyoh.mobile.ui.theme.TieyohBorder
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohPersonGreen
import com.tieyoh.mobile.ui.theme.TieyohTealGradient
import java.util.Currency
import java.util.Locale

// Everything the sender's full-screen confirmation needs, gathered from
// what SendGiftScreen already has in memory right after a successful
// send (recipient, merchant, and the amount/currency echoed back by the
// server) -- no extra network round trip needed for this side, unlike
// the recipient's confirmation (see GiftReceivedScreen below).
data class SentGiftInfo(
    val recipientDisplayName: String,
    val merchantName: String?,
    val amount: Double,
    val currency: String
)

// Shown the instant a gift finishes sending (SendGiftScreen.submit()'s
// success path), per the reference mockup approved Sep 2, 2026. There's
// no history of this screen to come back to -- it's a one-time
// confirmation, not a receipt.
@Composable
fun GiftSentScreen(
    info: SentGiftInfo,
    onSendToSomeoneElse: () -> Unit,
    onBackToFeed: () -> Unit
) {
    GiftConfirmationScaffold(
        headline = "You sent a gift!",
        statusLabel = "SENT",
        amountText = formatGiftAmount(info.amount, info.currency),
        merchantName = info.merchantName,
        partyPrefix = "to",
        partyName = info.recipientDisplayName,
        primaryLabel = "Send to someone else",
        onPrimaryClick = onSendToSomeoneElse,
        onBackToFeed = onBackToFeed
    )
}

// Reached from tapping a GIFT_RECEIVED notification (and, once the
// Android App Links deep-link work lands, from opening an invite/gift
// text link straight into the app -- see tieyoh-platform-overview.md).
// Unlike the sender's confirmation, nothing about this gift is already
// in memory client-side, so it fetches the details itself.
@Composable
fun GiftReceivedScreen(
    giftId: Long,
    onThankSender: (senderId: Long, senderDisplayName: String) -> Unit,
    onBackToFeed: () -> Unit
) {
    var detail by remember { mutableStateOf<GiftDetailResult?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(giftId) {
        detail = try {
            RetrofitClient.giftApi.getGift(giftId)
        } catch (e: Exception) {
            loadFailed = true
            null
        }
    }

    when {
        loadFailed -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Couldn't load this gift right now.", color = TieyohMuted)
                Spacer(Modifier.height(16.dp))
                TieyohPrimaryButton(
                    text = "Back to Feed",
                    onClick = onBackToFeed,
                    fillWidth = false
                )
            }
        }

        detail == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TieyohAccent)
            }
        }

        else -> {
            val d = detail!!
            val senderName = d.senderDisplayName ?: "Someone"
            GiftConfirmationScaffold(
                headline = "You received a gift!",
                statusLabel = "RECEIVED",
                amountText = formatGiftAmount(d.amount, d.currency ?: "USD"),
                merchantName = d.merchantName,
                partyPrefix = "from",
                partyName = senderName,
                primaryLabel = "Thank ${senderName.substringBefore(" ")}",
                onPrimaryClick = { onThankSender(d.senderId, senderName) },
                onBackToFeed = onBackToFeed
            )
        }
    }
}

// The shared visual shell behind both screens above -- same layout, just
// different copy/colors/actions, matching the two-up reference mockup.
@Composable
private fun GiftConfirmationScaffold(
    headline: String,
    statusLabel: String,
    amountText: String,
    merchantName: String?,
    partyPrefix: String,
    partyName: String,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    onBackToFeed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToFeed, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TieyohNavy
                )
            }
            Text(
                text = "Feed",
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.tieyoh_logo),
            contentDescription = "Tieyoh",
            modifier = Modifier.height(22.dp)
        )

        Spacer(Modifier.height(36.dp))

        Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(TieyohBlue, TieyohTealGradient))),
                contentAlignment = Alignment.Center
            ) {
                Text("🎁", fontSize = 44.sp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(TieyohPersonGreen)
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = headline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(TieyohPersonGreen.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = "✓ $statusLabel",
                color = TieyohPersonGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(22.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(TieyohCardBg)
                .border(1.dp, TieyohBorder, RoundedCornerShape(18.dp))
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = amountText,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TieyohAccent
                )
                if (!merchantName.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(merchantName, fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 14.sp)
                    Text("Gift card", color = TieyohMuted, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Row {
            Text("$partyPrefix ", color = TieyohMuted, fontSize = 14.sp)
            Text(partyName, color = TieyohNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // A fixed spacer, not weight(1f) -- this Column is scrollable now
        // (see the header note above this function), and weight() requires a
        // bounded parent, which verticalScroll's Column isn't.
        Spacer(Modifier.height(28.dp))

        TieyohPrimaryButton(
            text = primaryLabel,
            onClick = onPrimaryClick
        )

        Spacer(Modifier.height(12.dp))

        TieyohSecondaryButton(
            text = "Back to Feed",
            onClick = onBackToFeed
        )
    }
}

internal fun formatGiftAmount(amount: Double, currency: String): String {
    val symbol = try {
        Currency.getInstance(currency).getSymbol(Locale.US)
    } catch (e: Exception) {
        "$"
    }
    val isWhole = amount == Math.floor(amount) && !amount.isInfinite()
    return if (isWhole) {
        "$symbol${amount.toLong()}"
    } else {
        String.format(Locale.US, "%s%.2f", symbol, amount)
    }
}
