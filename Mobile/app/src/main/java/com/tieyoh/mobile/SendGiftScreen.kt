package com.tieyoh.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBorder
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import kotlinx.coroutines.launch

// Sections 10-13 of the "Interactive Social/Gifting Feed" spec. Lands
// here with the recipient already chosen -- from the Feed, the circle
// list, or the center Send button -- so the sender never has to search
// for them again. Shows the recipient's own saved wish-list merchants
// first (purposeful gifting -- a suggestion, not a restriction), falls
// through to the full Merchant Search for "Other," then collects an
// amount and actually sends. Merchant selection is hoisted up to the
// caller (selectedMerchant/onSelectMerchant) rather than kept as local
// state, since navigating out to Merchant Search and back would otherwise
// wipe out whatever the sender had already picked here.
@Composable
fun SendGiftScreen(
    currentUserId: Long,
    recipient: FriendResult,
    selectedMerchant: MerchantDto?,
    onSelectMerchant: (MerchantDto) -> Unit,
    onSearchOtherMerchant: () -> Unit,
    onSent: (SentGiftInfo) -> Unit,
    onCancel: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var favoriteMerchants by remember { mutableStateOf<List<MerchantDto>>(emptyList()) }
    var isLoadingFavorites by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    // Who can see this gift in the Feed is no longer chosen per-send --
    // it is derived entirely from the sender's Profile > Privacy >
    // "share gifting with your circle" setting, fetched below.
    var visibility by remember { mutableStateOf("CONTACTS") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val recipientUserId = recipient.contactUserId

    LaunchedEffect(recipientUserId) {
        if (recipientUserId == null) {
            isLoadingFavorites = false
            return@LaunchedEffect
        }
        favoriteMerchants = try {
            RetrofitClient.userApi.getMerchantSelections(recipientUserId)
        } catch (e: Exception) {
            emptyList()
        }
        isLoadingFavorites = false
    }

    // Sole source of truth for gift visibility: the sender's Profile >
    // Privacy > "share gifting with your circle" setting. There is no
    // per-gift override anymore.
    LaunchedEffect(currentUserId) {
        try {
            val profile = RetrofitClient.userApi.getProfile(currentUserId)
            visibility = if (profile.shareGiftingWithCircle == false) "PRIVATE" else "CONTACTS"
        } catch (e: Exception) {
            // Best-effort -- keep the CONTACTS default if this fails.
        }
    }

    fun submit() {
        errorMessage = null
        val merchant = selectedMerchant
        if (merchant == null) {
            errorMessage = "Choose a merchant first."
            return
        }
        val amount = amountText.trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            errorMessage = "Enter an amount greater than \$0."
            return
        }
        if (recipientUserId == null) {
            errorMessage = "${recipient.displayName} hasn't joined Tieyoh yet."
            return
        }

        coroutineScope.launch {
            isSending = true
            try {
                val result = RetrofitClient.giftApi.sendGift(
                    GiftRequest(
                        senderId = currentUserId,
                        recipientId = recipientUserId,
                        merchantId = merchant.id,
                        amount = amount,
                        visibility = visibility
                    )
                )
                // The full-screen confirmation (GiftSentScreen) replaces
                // the old toast -- it's a much clearer "yes, that went
                // through" than a toast that can be missed.
                onSent(
                    SentGiftInfo(
                        recipientDisplayName = recipient.displayName,
                        merchantName = merchant.brandName,
                        amount = result.amount,
                        currency = result.currency ?: "USD"
                    )
                )
            } catch (e: Exception) {
                errorMessage = "Couldn't send that gift right now."
            } finally {
                isSending = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TieyohNavy
                    )
                }
            }
        }

        item {
            Text(
                text = "Send a Gift",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TieyohNavy
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SearchResultAvatar(
                    profilePictureBase64 = recipient.profilePictureBase64,
                    displayName = recipient.displayName
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(recipient.displayName, fontWeight = FontWeight.Bold, color = TieyohNavy)
                    recipient.tieyohId?.let {
                        Text(it, color = TieyohMuted, fontSize = 13.sp)
                    }
                }
            }
        }

        selectedMerchant?.let { merchant ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TieyohCardBg)
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Gift for", style = MaterialTheme.typography.labelMedium, color = TieyohMuted)
                        Text(merchant.brandName, fontWeight = FontWeight.Bold, color = TieyohNavy)
                        merchant.category?.let {
                            Text(it, fontSize = 12.sp, color = TieyohMuted)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = { Text("Amount") },
                prefix = { Text("$") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (recipientUserId != null) {
            item {
                Text(
                    text = "${recipient.displayName.substringBefore(" ")}'s favorite places",
                    fontWeight = FontWeight.Bold,
                    color = TieyohNavy
                )
            }

            if (isLoadingFavorites) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TieyohAccent)
                    }
                }
            } else if (favoriteMerchants.isEmpty()) {
                item {
                    Text(
                        "${recipient.displayName} hasn't picked any favorite places yet.",
                        color = TieyohMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                items(favoriteMerchants, key = { it.id }) { merchant ->
                    MerchantTile(
                        title = merchant.brandName,
                        subtitle = merchant.category ?: "",
                        selected = selectedMerchant?.id == merchant.id,
                        onClick = { onSelectMerchant(merchant) }
                    )
                }
            }
        }

        item {
            MerchantTile(
                title = "Other",
                subtitle = "Find another merchant",
                selected = false,
                onClick = onSearchOtherMerchant
            )
        }

        errorMessage?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }

        item {
            TieyohPrimaryButton(
                text = "Send Gift",
                onClick = { submit() },
                enabled = !isSending,
                isLoading = isSending
            )
        }

        item {
            TieyohSecondaryButton(text = "Cancel", onClick = onCancel)
        }
    }
}

@Composable
private fun MerchantTile(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) TieyohCardBg else Color.White)
            .border(1.dp, TieyohBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = TieyohNavy)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = TieyohMuted, fontSize = 13.sp)
            }
        }
    }
}
