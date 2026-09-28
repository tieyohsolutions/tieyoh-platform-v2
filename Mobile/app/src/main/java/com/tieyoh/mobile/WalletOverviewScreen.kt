package com.tieyoh.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBlue
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohGiftRed
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohTealGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Reached by tapping the "Wallet Overview" card at the top of the My Card
// screen (WalletOverviewCard in MainActivity.kt). This is the real "My
// Gift Cards" list, built from two existing backend endpoints rather than
// a dedicated wallet/ledger API:
//
//  - UserApi.getMerchantSelections(userId) -> List<MerchantDto>, the same
//    call "My picks" already uses. Each MerchantDto here carries
//    amountReceived (the running total your circle has gifted you toward
//    that merchant) and partnered (whether it's a Tieyoh partner, which
//    per spec is the only case that gets a logo -- everyone else just
//    gets their plain name).
//  - FeedApi.getFeed(viewerId) -> FeedItem list, filtered down to this
//    user's GIFT_ACTIVITY entries, to build the per-merchant "activity"
//    drill-down (MerchantActivityScreen below).
//
// One honest gap: there is no "spend"/purchase/ledger endpoint anywhere
// in this backend yet -- checked GiftApi.kt and MerchantApi.kt in full,
// neither has one, and no other payment feature in the app has real spend
// tracking either (they're all "coming soon" placeholders). So the
// activity screen only ever shows real gift-received entries, plus a
// plain note that spending history isn't tracked yet, rather than making
// up dollar amounts for a page that's about someone's money.
@Composable
fun WalletOverviewScreen(
    currentUser: AuthResponse,
    onBack: () -> Unit
) {
    var merchants by remember { mutableStateOf<List<MerchantDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedMerchant by remember { mutableStateOf<MerchantDto?>(null) }

    LaunchedEffect(currentUser.id) {
        isLoading = true
        try {
            merchants = RetrofitClient.userApi.getMerchantSelections(currentUser.id)
                .filter { (it.amountReceived ?: 0.0) > 0.0 }
                .sortedByDescending { it.amountReceived ?: 0.0 }
            loadError = null
        } catch (e: Exception) {
            loadError = "Couldn't load your gift cards right now."
        } finally {
            isLoading = false
        }
    }

    val activeMerchant = selectedMerchant
    if (activeMerchant != null) {
        MerchantActivityScreen(
            currentUser = currentUser,
            merchant = activeMerchant,
            onBack = { selectedMerchant = null }
        )
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredMerchants = remember(merchants, searchQuery) {
        if (searchQuery.isBlank()) {
            merchants
        } else {
            merchants.filter { it.brandName.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
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

        Text(
            text = "My Gift Cards",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = TieyohNavy,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Every balance your circle has gifted you, ready to spend with that merchant.",
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = TieyohMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search merchants") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TieyohMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TieyohAccent)
                }
            }

            loadError != null -> {
                Text(loadError.orEmpty(), color = TieyohGiftRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            merchants.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(TieyohMuted.copy(alpha = 0.08f))
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No gift cards yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TieyohNavy
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "When someone in your circle gifts you money toward a merchant, it'll show up here.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TieyohMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            filteredMerchants.isEmpty() -> {
                Text(
                    text = "No merchants match \"$searchQuery\".",
                    fontSize = 14.sp,
                    color = TieyohMuted
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredMerchants.forEach { merchant ->
                        MerchantBalanceRow(
                            merchant = merchant,
                            onClick = { selectedMerchant = merchant }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        GiftCardsEncouragementBanner()

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MerchantBalanceRow(merchant: MerchantDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(width = 1.dp, color = TieyohNavy.copy(alpha = 0.08f), shape = RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Per spec: only a Tieyoh-partnered merchant gets its logo shown
        // -- everyone else is represented by name alone.
        if (merchant.partnered && !merchant.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = merchant.logoUrl,
                contentDescription = merchant.brandName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TieyohMuted.copy(alpha = 0.08f))
            )
            Spacer(Modifier.width(14.dp))
        }

        Text(
            text = merchant.brandName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = TieyohNavy,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "$${formatWalletAmount(merchant.amountReceived ?: 0.0)}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TieyohNavy
        )

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(TieyohNavy.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            WalletOverviewChevronGlyph(color = TieyohNavy, size = 13.dp)
        }
    }
}

@Composable
private fun GiftCardsEncouragementBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohCardBg)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(TieyohBlue, TieyohTealGradient))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CardGiftcard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Add merchants to My Picks so your circle knows exactly what to gift you.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = TieyohNavy,
            modifier = Modifier.weight(1f)
        )
    }
}

// Per-merchant drill-down, reached by tapping the chevron on a
// MerchantBalanceRow. Shown in place of the list (local state in
// WalletOverviewScreen above) rather than as its own nav destination,
// since it's just a deeper view of the same page.
@Composable
private fun MerchantActivityScreen(
    currentUser: AuthResponse,
    merchant: MerchantDto,
    onBack: () -> Unit
) {
    var activity by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser.id, merchant.id) {
        isLoading = true
        when (val result = FeedRepository.loadPage(viewerId = currentUser.id, page = 0, size = 50)) {
            is FeedPageResult.Success -> {
                activity = result.items.filter { item ->
                    item.eventType == "GIFT_ACTIVITY" &&
                        item.recipient?.userId == currentUser.id &&
                        item.merchant?.merchantId == merchant.id
                }.sortedByDescending { it.occurredAt ?: 0L }
                loadError = null
            }
            is FeedPageResult.Error -> {
                loadError = result.message
            }
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
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
                text = "My Gift Cards",
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = merchant.brandName,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = TieyohNavy
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "$${formatWalletAmount(merchant.amountReceived ?: 0.0)} balance",
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = TieyohAccent
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "ACTIVITY",
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = TieyohMuted
        )

        Spacer(Modifier.height(10.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TieyohAccent)
                }
            }

            loadError != null -> {
                Text(loadError.orEmpty(), color = TieyohGiftRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            activity.isEmpty() -> {
                Text(
                    text = "No gift activity for ${merchant.brandName} yet.",
                    fontSize = 14.sp,
                    color = TieyohMuted
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activity.forEach { item -> GiftActivityRow(item = item) }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Honest placeholder for the half of this feature that has no
        // backend yet -- see the file header comment for why this stays
        // a plain note instead of fabricated dollar amounts.
        Text(
            text = "Purchases you make with this balance aren't tracked yet, so this list currently shows only what you've received. We'll add your spending history here once that's ready.",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = TieyohMuted
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun GiftActivityRow(item: FeedItem) {
    val senderName = item.sender?.displayName ?: "Someone"
    val amount = item.gift?.amount?.let { formatWalletAmount(it) }
    val dateText = item.occurredAt?.let { formatActivityDate(it) }.orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TieyohCardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(TieyohBlue, TieyohTealGradient))),
            contentAlignment = Alignment.Center
        ) {
            Text(personInitials(senderName), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = if (amount != null) "$senderName gifted you \$$amount" else "$senderName sent a gift",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TieyohNavy
            )
            if (dateText.isNotBlank()) {
                Text(dateText, fontSize = 12.sp, color = TieyohMuted)
            }
        }
    }
}

private fun formatWalletAmount(value: Double): String {
    return if (value == Math.floor(value)) value.toLong().toString() else "%.2f".format(value)
}

private fun formatActivityDate(epochMillis: Long): String {
    return SimpleDateFormat("MM/dd/yy", Locale.US).format(Date(epochMillis))
}
