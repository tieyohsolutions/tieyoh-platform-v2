package com.tieyoh.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBlue
import com.tieyoh.mobile.ui.theme.TieyohBlueTileBg
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohTealGradient
import com.tieyoh.mobile.ui.theme.TieyohTrack
import java.time.LocalDate
import java.util.Locale

// Reached by tapping "Activity & Insights" on the My Card screen
// (MyCardActionsSection in MainActivity.kt). Same page shape as
// TransactionHistoryScreen.kt and WalletOverviewScreen.kt right next to
// it: back arrow + "My Card" breadcrumb, big bold centered title, muted
// subtitle, then content.
//
// Honest gap, the same one TransactionHistoryScreen.kt and
// WalletOverviewScreen.kt already document: there is no purchase/spend
// ledger endpoint in the backend yet, so there is no real data to power
// any of these charts today. Every composable below -- the daily bar
// chart, the category breakdown, the ranked location/purchase lists -- is
// real, working UI, built and ready to receive real per-range data the
// moment a spending/insights endpoint exists; swap the hardcoded
// emptyList()s for a LaunchedEffect call against it and everything else
// (the range selector, tabs, selection state, empty states) keeps working
// as-is. Until then each panel shows an honest "nothing yet" state rather
// than fabricated numbers.

private data class SpendingBar(val label: String, val amount: Double)
// Backed by MerchantCategoryBucket (see MerchantCategoryBucket.kt) rather than an
// ad-hoc name/color pair -- once a real spend-ledger endpoint exists, this list is
// built by grouping transactions through bucketForMerchantCategory(merchant.category)
// and summing amounts per bucket.
private data class SpendingCategory(val bucket: MerchantCategoryBucket, val amount: Double)
private data class InsightLocation(val name: String, val partnered: Boolean, val visits: Int, val total: Double)
private data class InsightItem(val name: String, val merchant: String, val count: Int, val total: Double)

private enum class InsightsTab(val label: String) {
    SPENDING("Spending"),
    LOCATIONS("Locations"),
    ITEMS("Top Purchases")
}

private enum class InsightsRange(val label: String, val rangeLabel: String, val prevLabel: String) {
    WEEK("Week", "This Week", "last week"),
    MONTH("Month", "This Month", "last month"),
    YEAR("Year", "This Year", "last year")
}

private enum class LocationFilter(val label: String) {
    ALL("All"),
    PARTNERED("Partnered (QR)"),
    OTHER("Other (Card)")
}

private enum class ItemSort(val label: String) {
    FREQUENCY("Most frequent"),
    AMOUNT("Highest spent")
}

// How many years back the Year picker offers (current year inclusive).
private const val YEAR_PICKER_SPAN = 5

@Composable
fun ActivityInsightsScreen(
    currentUser: AuthResponse,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf(InsightsTab.SPENDING) }
    var range by remember { mutableStateOf(InsightsRange.WEEK) }
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var locationFilter by remember { mutableStateOf(LocationFilter.ALL) }
    var itemSort by remember { mutableStateOf(ItemSort.FREQUENCY) }

    // Longer-than-a-year view: tapping Year reveals a picker for which
    // year to look at (defaults to the current year), so the screen isn't
    // capped at "this year" -- going further back than 5 years isn't
    // wired yet since there's no real account-history date to bound it
    // against; extend YEAR_PICKER_SPAN once real data can tell us how far
    // back a user's history actually goes.
    val currentYear = remember { LocalDate.now().year }
    var selectedYear by remember { mutableStateOf(currentYear) }
    val pickerYears = remember(currentYear) { (currentYear downTo currentYear - YEAR_PICKER_SPAN + 1).toList() }

    LaunchedEffect(range, selectedYear) {
        selectedBarIndex = null
        selectedCategory = null
    }

    val periodLabel = if (range == InsightsRange.YEAR) {
        if (selectedYear == currentYear) "This Year" else selectedYear.toString()
    } else {
        range.rangeLabel
    }
    val periodPhrase = if (range == InsightsRange.YEAR) {
        if (selectedYear == currentYear) "this year" else "in $selectedYear"
    } else {
        range.label.lowercase(Locale.US).let { "this $it" }
    }

    // See the file-header comment: no spend ledger exists yet, so every
    // range/year combination currently resolves to no data. Swapping this
    // for a real per-range, per-year API call is the only change needed
    // once one exists.
    val bars = remember(range, selectedYear) { emptyList<SpendingBar>() }
    // Buckets: MerchantCategoryBucket, derived from the real merchant.category field
    // (see MerchantCategoryBucket.kt) -- not sample data.
    val categories = remember(range, selectedYear) { emptyList<SpendingCategory>() }
    val locations = remember(range, selectedYear) { emptyList<InsightLocation>() }
    val items = remember(range, selectedYear) { emptyList<InsightItem>() }

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
            text = "Activity & Insights",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = TieyohNavy,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "See where your money's going — spending, places, and what you buy most with your card.",
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = TieyohMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InsightsRange.values().forEach { r ->
                TieyohSegmentedPill(
                    label = r.label,
                    selected = r == range,
                    onClick = { range = r },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (range == InsightsRange.YEAR) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pickerYears.forEach { year ->
                    TieyohSegmentedPill(
                        label = year.toString(),
                        selected = year == selectedYear,
                        onClick = { selectedYear = year }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        HeroSpendingCard(periodLabel = periodLabel, periodPhrase = periodPhrase, bars = bars)

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEEF1F5))
        ) {
            InsightsTab.values().forEach { t ->
                val active = t == tab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { tab = t },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = t.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = if (active) TieyohNavy else TieyohMuted,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (active) TieyohAccent else Color.Transparent)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when (tab) {
            InsightsTab.SPENDING -> SpendingPanel(
                bars = bars,
                range = range,
                categories = categories,
                selectedBarIndex = selectedBarIndex,
                onSelectBar = { selectedBarIndex = it },
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = if (selectedCategory == it) null else it }
            )

            InsightsTab.LOCATIONS -> LocationsPanel(
                locations = locations,
                filter = locationFilter,
                onFilterChange = { locationFilter = it },
                rangeLabel = periodLabel
            )

            InsightsTab.ITEMS -> ItemsPanel(
                items = items,
                sortBy = itemSort,
                onSortChange = { itemSort = it },
                rangeLabel = periodLabel
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeroSpendingCard(periodLabel: String, periodPhrase: String, bars: List<SpendingBar>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$periodLabel Spending".uppercase(Locale.US),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = TieyohMuted
        )

        val total = bars.sumOf { it.amount }
        Text(
            text = formatInsightAmount(total),
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (bars.isEmpty()) TieyohMuted else TieyohNavy
        )

        if (bars.isEmpty()) {
            Text(
                text = "No spending recorded yet $periodPhrase.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TieyohMuted
            )
        }
    }
}

@Composable
private fun InsightsEmptyState(title: String, message: String) {
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
private fun SpendingPanel(
    bars: List<SpendingBar>,
    range: InsightsRange,
    categories: List<SpendingCategory>,
    selectedBarIndex: Int?,
    onSelectBar: (Int) -> Unit,
    selectedCategory: String?,
    onSelectCategory: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "DAILY SPENDING",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = TieyohMuted
            )

            val displayBars = bars.ifEmpty { placeholderBarLabels(range).map { SpendingBar(it, 0.0) } }
            val hasRealSpending = bars.isNotEmpty()
            val maxVal = displayBars.maxOf { it.amount }.let { if (it > 0.0) it else 1.0 }
            val resolvedIndex = selectedBarIndex ?: (displayBars.size - 1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                displayBars.forEachIndexed { i, b ->
                    val selected = hasRealSpending && i == resolvedIndex
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelectBar(i) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(maxOf(4.dp, (b.amount / maxVal * 120).dp))
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(
                                    if (selected) Brush.verticalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))
                                    else Brush.verticalGradient(listOf(TieyohTrack, TieyohTrack))
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = b.label,
                            fontSize = 10.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) TieyohNavy else TieyohMuted
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(displayBars[resolvedIndex].label, fontSize = 13.sp, color = TieyohMuted)
                Text(
                    formatInsightAmount(displayBars[resolvedIndex].amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasRealSpending) TieyohNavy else TieyohMuted
                )
            }

            if (!hasRealSpending) {
                Text(
                    text = "Pay with your QR code at a partnered merchant or your card anywhere else to see daily spending here.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = TieyohMuted
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "BY CATEGORY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                color = TieyohMuted
            )

            val hasCategories = categories.isNotEmpty()
            val catTotal = categories.sumOf { it.amount }.let { if (it > 0.0) it else 1.0 }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(TieyohTrack),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                categories.forEach { c ->
                    val dim = selectedCategory != null && selectedCategory != c.bucket.label
                    Box(
                        modifier = Modifier
                            .weight(maxOf(0.03f, (c.amount / catTotal).toFloat()))
                            .fillMaxHeight()
                            .clickable { onSelectCategory(c.bucket.label) }
                            .background(c.bucket.color.copy(alpha = if (dim) 0.3f else 1f))
                    )
                }
            }

            if (hasCategories) {
                Column {
                    categories.forEach { c ->
                        val selected = selectedCategory == c.bucket.label
                        val dim = selectedCategory != null && !selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) TieyohCardBg else Color.White)
                                .clickable { onSelectCategory(c.bucket.label) }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(c.bucket.color.copy(alpha = if (dim) 0.45f else 1f))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = c.bucket.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TieyohNavy.copy(alpha = if (dim) 0.45f else 1f),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatInsightPct((c.amount / catTotal) * 100),
                                fontSize = 12.5.sp,
                                color = TieyohMuted.copy(alpha = if (dim) 0.45f else 1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = formatInsightAmount(c.amount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TieyohNavy.copy(alpha = if (dim) 0.45f else 1f)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No categories yet \u2014 once you start spending, we'll break it down by category here.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = TieyohMuted
                )
            }
        }
    }
}

@Composable
private fun LocationsPanel(
    locations: List<InsightLocation>,
    filter: LocationFilter,
    onFilterChange: (LocationFilter) -> Unit,
    rangeLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Where you spend most", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
            Text("Ranked by number of visits · $rangeLabel", fontSize = 12.5.sp, color = TieyohMuted)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LocationFilter.values().forEach { f ->
                TieyohSegmentedPill(
                    label = f.label,
                    selected = f == filter,
                    onClick = { onFilterChange(f) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val filtered = when (filter) {
            LocationFilter.ALL -> locations
            LocationFilter.PARTNERED -> locations.filter { it.partnered }
            LocationFilter.OTHER -> locations.filter { !it.partnered }
        }

        if (filtered.isEmpty()) {
            InsightsEmptyState(
                title = "No visits yet",
                message = "Merchants you pay at with Tieyoh will show up here, ranked by how often you visit."
            )
        } else {
            val maxVisits = filtered.maxOf { it.visits }.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filtered.forEach { loc -> LocationInsightRow(loc, maxVisits) }
            }
        }
    }
}

@Composable
private fun LocationInsightRow(location: InsightLocation, maxVisits: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (location.partnered) Brush.linearGradient(listOf(TieyohBlue, TieyohTealGradient))
                    else Brush.linearGradient(listOf(TieyohBlue, TieyohBlue))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (location.partnered) Icons.Outlined.QrCode2 else Icons.Outlined.CreditCard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(location.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
                Text(formatInsightAmount(location.total), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
            }
            Text(
                text = location.visits.toString() + if (location.visits == 1) " visit" else " visits",
                fontSize = 12.sp,
                color = TieyohMuted
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TieyohTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(location.visits.toFloat() / maxVisits.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(TieyohAccent)
                )
            }
        }
    }
}

@Composable
private fun ItemsPanel(
    items: List<InsightItem>,
    sortBy: ItemSort,
    onSortChange: (ItemSort) -> Unit,
    rangeLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("What you buy most", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
            Text("Your most frequent purchases · $rangeLabel", fontSize = 12.5.sp, color = TieyohMuted)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ItemSort.values().forEach { s ->
                TieyohSegmentedPill(
                    label = s.label,
                    selected = s == sortBy,
                    onClick = { onSortChange(s) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (items.isEmpty()) {
            InsightsEmptyState(
                title = "No purchases yet",
                message = "Your most frequent purchases will show up here once you start paying with Tieyoh."
            )
        } else {
            val sorted = if (sortBy == ItemSort.AMOUNT) items.sortedByDescending { it.total } else items.sortedByDescending { it.count }
            val maxCount = sorted.maxOf { it.count }.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sorted.forEachIndexed { i, item -> ItemInsightRow(rank = i + 1, item = item, maxCount = maxCount) }
            }
        }
    }
}

@Composable
private fun ItemInsightRow(rank: Int, item: InsightItem, maxCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(TieyohBlueTileBg),
            contentAlignment = Alignment.Center
        ) {
            Text(rank.toString(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TieyohBlue)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.name, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
                Text("×${item.count}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TieyohMuted)
            }
            Text(
                text = "${item.merchant} · ${formatInsightAmount(item.total)} total",
                fontSize = 12.sp,
                color = TieyohMuted
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TieyohTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.count.toFloat() / maxCount.toFloat())
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(TieyohAccent)
                )
            }
        }
    }
}

// Real, deterministic period labels (day names / month abbreviations) used to
// give the daily-spending chart a graceful zero-value shape when bars is
// empty -- not fabricated numbers, just the honest $0 laid out against the
// real structure of the selected period, so the chart never looks broken or
// disappears into plain text while there's no real spending yet.
private fun placeholderBarLabels(range: InsightsRange): List<String> = when (range) {
    InsightsRange.WEEK -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    InsightsRange.MONTH -> listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4")
    InsightsRange.YEAR -> listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
}

private fun formatInsightAmount(value: Double): String {
    return "$" + String.format(Locale.US, "%,.2f", value)
}

private fun formatInsightPct(value: Double): String {
    return Math.round(value).toString() + "%"
}
