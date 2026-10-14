package com.tieyoh.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.tieyoh.mobile.ui.theme.TieyohBlue
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohTealGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Reached by tapping "Transaction History" on the My Card screen
// (MyCardActionsSection in MainActivity.kt). Same page shape and design
// language as WalletOverviewScreen.kt right next door: back arrow + "My
// Card" breadcrumb, big bold centered title, muted subtitle, then content.
//
// Honest gap, same one WalletOverviewScreen.kt already documents: there is
// no purchase/spend ledger endpoint in the backend yet, and the two ways
// to actually pay (PayWithQrScreen, PayWithCardScreen) are themselves still
// coming-soon placeholders -- neither records a transaction anywhere
// today. So this screen is real, working UI wired to a real (currently
// empty) state instead of fabricated rows: search, filtering, and the row
// layout below are ready to receive live data the moment a transactions
// endpoint exists -- swap the hardcoded emptyList() for a LaunchedEffect
// call against it and everything else here keeps working as-is.
private enum class TransactionFilter(val label: String) {
    ALL("All"),
    PARTNERED_QR("Partnered (QR)"),
    OTHER_CARD("Other (Card)")
}

// partnered = true means this was a QR-code payment at a Tieyoh-partnered
// merchant; partnered = false means it was a prepaid-card payment at a
// non-partnered merchant -- the same two payment paths as
// ChooseHowToPaySection on the My Card screen.
private data class TransactionHistoryItem(
    val id: Long,
    val merchantName: String,
    val merchantLogoUrl: String?,
    val partnered: Boolean,
    val amount: Double,
    val occurredAtMillis: Long
)

@Composable
fun TransactionHistoryScreen(
    currentUser: AuthResponse,
    onBack: () -> Unit
) {
    val transactions = remember { emptyList<TransactionHistoryItem>() }

    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(TransactionFilter.ALL) }

    val filteredTransactions = remember(transactions, searchQuery, filter) {
        transactions
            .filter { tx ->
                when (filter) {
                    TransactionFilter.ALL -> true
                    TransactionFilter.PARTNERED_QR -> tx.partnered
                    TransactionFilter.OTHER_CARD -> !tx.partnered
                }
            }
            .filter { tx ->
                searchQuery.isBlank() || tx.merchantName.contains(searchQuery, ignoreCase = true)
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
            text = "Transaction History",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = TieyohNavy,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Every purchase you've made with your Tieyoh QR code or prepaid card, all in one place.",
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
            placeholder = { Text("Search transactions") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TieyohMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionFilter.values().forEach { option ->
                TieyohSegmentedPill(
                    label = option.label,
                    selected = filter == option,
                    onClick = { filter = option },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            transactions.isEmpty() -> {
                TransactionHistoryEmptyState(
                    title = "No transactions yet",
                    message = "Once you pay with your QR code at a partnered merchant or your prepaid card anywhere else, it'll show up here."
                )
            }

            filteredTransactions.isEmpty() -> {
                TransactionHistoryEmptyState(
                    title = "No matches",
                    message = if (searchQuery.isNotBlank()) {
                        "No transactions match \"$searchQuery\"."
                    } else {
                        "No transactions in this category yet."
                    }
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredTransactions.forEach { tx ->
                        TransactionHistoryRow(transaction = tx)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TransactionHistoryEmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TieyohMuted.copy(alpha = 0.08f))
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TieyohNavy
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = TieyohMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TransactionHistoryRow(transaction: TransactionHistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TieyohCardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (transaction.partnered && !transaction.merchantLogoUrl.isNullOrBlank()) {
            AsyncImage(
                model = transaction.merchantLogoUrl,
                contentDescription = transaction.merchantName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TieyohMuted.copy(alpha = 0.08f))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(TieyohBlue, TieyohTealGradient))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.partnered) Icons.Outlined.QrCode2 else Icons.Outlined.CreditCard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchantName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = TieyohNavy
            )
            Text(
                text = "${formatTransactionDate(transaction.occurredAtMillis)} · " +
                    if (transaction.partnered) "Paid with QR (partnered)" else "Paid with card",
                fontSize = 12.sp,
                color = TieyohMuted
            )
        }

        Text(
            text = "-$${formatTransactionAmount(transaction.amount)}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TieyohNavy
        )
    }
}

private fun formatTransactionAmount(value: Double): String {
    return if (value == Math.floor(value)) value.toLong().toString() else "%.2f".format(value)
}

private fun formatTransactionDate(epochMillis: Long): String {
    return SimpleDateFormat("MM/dd/yy", Locale.US).format(Date(epochMillis))
}
