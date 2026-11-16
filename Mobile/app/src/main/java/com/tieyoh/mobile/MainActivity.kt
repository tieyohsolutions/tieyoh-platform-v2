package com.tieyoh.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBorder
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohFabRing
import com.tieyoh.mobile.ui.theme.TieyohGiftRed
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavInactive
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohSponsoredBg
import com.tieyoh.mobile.ui.theme.TieyohSponsoredBody
import com.tieyoh.mobile.ui.theme.TieyohSponsoredHeading
import com.tieyoh.mobile.ui.theme.TieyohSponsoredLabel
import com.tieyoh.mobile.ui.theme.TieyohSponsoredLogoBg
import com.tieyoh.mobile.ui.theme.TieyohTheme
import com.tieyoh.mobile.ui.theme.TieyohTrack
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TieyohTheme {
                var authenticatedUser by remember { mutableStateOf<AuthResponse?>(null) }

                val user = authenticatedUser
                if (user == null) {
                    LoginScreen(
                        onAuthenticated = { response -> authenticatedUser = response }
                    )
                } else {
                    TieyohApp(
                        currentUser = user,
                        onLogout = { authenticatedUser = null }
                    )
                }
            }
        }
    }
}

// Matches the four tabs + center Send action from the interactive prototype
// (Feed / My list / My card / Profile, with the Send FAB in the middle).
private enum class TieyohScreen {
    FEED,
    MY_LIST,
    MY_CARD,
    PROFILE,
    SEND_GIFT,
    SEARCH_MERCHANT
}

private data class WishTarget(
    val merchant: String,
    val current: Int,
    val goal: Int
)

private data class MerchantAllocation(
    val merchant: String,
    val amount: Double
)

@Composable
fun TieyohApp(
    currentUser: AuthResponse,
    onLogout: () -> Unit
) {

    var currentScreen by remember {
        mutableStateOf(TieyohScreen.FEED)
    }

    Scaffold(
        topBar = {
            TieyohTopBar(
                onLogoClick = {
                    currentScreen = TieyohScreen.FEED
                }
            )
        },
        bottomBar = {
            TieyohBottomNavigation(
                currentScreen = currentScreen,
                onNavigate = { screen ->
                    currentScreen = screen
                }
            )
        },
        floatingActionButton = {
            SendFab(
                onClick = { currentScreen = TieyohScreen.SEND_GIFT }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {

            when (currentScreen) {

                TieyohScreen.FEED -> {
                    FeedScreen(
                        onSeeFullFeed = { /* full feed list — coming soon */ }
                    )
                }

                TieyohScreen.MY_LIST -> MyListScreen()

                TieyohScreen.MY_CARD -> MyCardScreen()

                TieyohScreen.SEND_GIFT -> {
                    MerchantSearchScreen(
                        title = "Send a Gift",
                        subtitle = "Choose a merchant for the gift. Recipient selection comes next.",
                        actionLabel = "Search Eligible Merchants"
                    )
                }

                TieyohScreen.SEARCH_MERCHANT -> {
                    MerchantSearchScreen(
                        title = "Search for a Merchant",
                        subtitle = "Search the Tieyoh merchant registry.",
                        actionLabel = "Search Merchants"
                    )
                }

                TieyohScreen.PROFILE -> ProfileScreen(currentUser = currentUser, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun TieyohTopBar(
    onLogoClick: () -> Unit
) {

    Surface(
        color = TieyohNavy,
        shadowElevation = 0.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("tie") }
                    withStyle(SpanStyle(color = Color(0xFF5EEAD4))) { append("yoh") }
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clickable { onLogoClick() }
            )
        }
    }
}

@Composable
private fun SendFab(onClick: () -> Unit) {
    // Outer light ring + red gift-bag center, matching the reference
    // screenshot the user shared (red FAB instead of the teal one).
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(width = 2.dp, color = TieyohFabRing, shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(TieyohGiftRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\uD83C\uDF81",
                fontSize = 24.sp
            )
        }
    }
}

@Composable
private fun TieyohBottomNavigation(
    currentScreen: TieyohScreen,
    onNavigate: (TieyohScreen) -> Unit
) {

    NavigationBar(
        containerColor = Color.White,
        contentColor = TieyohNavInactive
    ) {

        NavigationBarItem(
            selected = currentScreen == TieyohScreen.FEED,
            onClick = { onNavigate(TieyohScreen.FEED) },
            icon = { Text("▤") },
            label = { Text("Feed") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TieyohAccent,
                selectedTextColor = TieyohAccent,
                unselectedIconColor = TieyohNavInactive,
                unselectedTextColor = TieyohNavInactive,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            selected = currentScreen == TieyohScreen.MY_LIST,
            onClick = { onNavigate(TieyohScreen.MY_LIST) },
            icon = { Text("☰") },
            label = { Text("My list") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TieyohAccent,
                selectedTextColor = TieyohAccent,
                unselectedIconColor = TieyohNavInactive,
                unselectedTextColor = TieyohNavInactive,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            selected = currentScreen == TieyohScreen.MY_CARD,
            onClick = { onNavigate(TieyohScreen.MY_CARD) },
            icon = { Text("▭") },
            label = { Text("My card") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TieyohAccent,
                selectedTextColor = TieyohAccent,
                unselectedIconColor = TieyohNavInactive,
                unselectedTextColor = TieyohNavInactive,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            selected = currentScreen == TieyohScreen.PROFILE,
            onClick = { onNavigate(TieyohScreen.PROFILE) },
            icon = { Text("◔") },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TieyohAccent,
                selectedTextColor = TieyohAccent,
                unselectedIconColor = TieyohNavInactive,
                unselectedTextColor = TieyohNavInactive,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun FeedScreen(
    onSeeFullFeed: () -> Unit
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                text = "Hi Hany, here's what your circle's up to",
                fontSize = 16.sp,
                color = TieyohMuted
            )
        }

        item { WishTargetCard() }

        item { SponsoredCard() }

        item {
            TransactionCard(
                initials = "ML",
                headlinePrefix = "Marcus Lee sent a ",
                giftLabel = "\$25 gift",
                headlineSuffix = " to Priya Patel",
                subtitle = "Whole Foods · 5h ago"
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, TieyohAccent, RoundedCornerShape(14.dp))
                    .clickable { onSeeFullFeed() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("See full feed", color = TieyohAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun WishTargetCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohCardBg)
            .padding(20.dp)
    ) {
        Text("Sarah Chen set a wish target", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
        Spacer(Modifier.height(4.dp))
        Text("Blue Bottle Coffee · 2h ago", fontSize = 14.sp, color = TieyohMuted)
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text("\$55", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
            Text(" / \$100", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TieyohMuted)
        }
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(TieyohTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(TieyohAccent)
            )
        }
    }
}

@Composable
private fun SponsoredCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohSponsoredBg)
            .padding(20.dp)
    ) {
        Text(
            "SPONSORED",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TieyohSponsoredLabel,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TieyohSponsoredLogoBg),
                contentAlignment = Alignment.Center
            ) {
                Text("BL", fontWeight = FontWeight.Bold, color = TieyohSponsoredHeading)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Blue Bottle Coffee", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TieyohSponsoredHeading)
                Text("20% off this week with Tieyoh", fontSize = 13.sp, color = TieyohSponsoredBody)
            }
        }
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TieyohNavy)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("View offer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun TransactionCard(
    initials: String,
    headlinePrefix: String,
    giftLabel: String,
    headlineSuffix: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohCardBg)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TieyohTrack),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TieyohNavy)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                buildAnnotatedString {
                    append(headlinePrefix)
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = TieyohAccent)) {
                        append(giftLabel)
                    }
                    append(headlineSuffix)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TieyohNavy
            )
            Text(subtitle, fontSize = 13.sp, color = TieyohMuted)
        }
    }
}

@Composable
private fun MyListScreen() {

    val wishTargets = listOf(
        WishTarget("Blue Bottle Coffee", 55, 100),
        WishTarget("Whole Foods", 20, 50),
        WishTarget("AMC Theatres", 0, 40)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Text("My list", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TieyohNavy)
            Spacer(Modifier.height(6.dp))
            Text(
                "Up to 10 merchants you'd love a gift from. Set a wish target and your circle can chip in toward it — no payment method needed to set one.",
                color = TieyohMuted
            )
        }

        items(wishTargets) { target ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(TieyohCardBg)
                    .padding(18.dp)
            ) {
                Text(target.merchant, fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("\$${target.current}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
                    Text(" / \$${target.goal}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TieyohMuted)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TieyohTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                if (target.goal > 0) (target.current.toFloat() / target.goal).coerceIn(0f, 1f) else 0f
                            )
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(TieyohAccent)
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, TieyohAccent, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ Add a merchant to your list", color = TieyohAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun MyCardScreen() {

    val allocations = listOf(
        MerchantAllocation("Blue Bottle Coffee", 40.0),
        MerchantAllocation("Whole Foods", 15.0)
    )
    val total = allocations.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("My card", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TieyohNavy)
        Spacer(Modifier.height(6.dp))
        Text(
            "One card for everything you've received. Funds stay grouped by merchant below until you spend them there — no separate gift card to juggle.",
            color = TieyohMuted
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(TieyohNavy)
                .padding(22.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("tie") }
                    withStyle(SpanStyle(color = Color(0xFF5EEAD4))) { append("yoh") }
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(28.dp))
            Text("Total balance", color = Color(0xFFB7D9D2), fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("\$${"%.2f".format(total)}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(18.dp))
        Text("Held by merchant", fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(allocations) { allocation ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(TieyohCardBg)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(allocation.merchant, color = TieyohNavy)
                    Text("\$${"%.2f".format(allocation.amount)}", color = TieyohNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MerchantSearchScreen(
    title: String,
    subtitle: String,
    actionLabel: String
) {

    var merchantName by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }
    var fullAddress by remember { mutableStateOf("") }
    var merchantType by remember { mutableStateOf("") }

    var merchants by remember {
        mutableStateOf<List<MerchantDto>>(emptyList())
    }

    var selectedMerchant by remember {
        mutableStateOf<MerchantDto?>(null)
    }

    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TieyohMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = merchantName,
            onValueChange = { merchantName = it },
            label = { Text("Merchant name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = zipCode,
            onValueChange = { zipCode = it },
            label = { Text("ZIP code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = fullAddress,
            onValueChange = { fullAddress = it },
            label = { Text("Full address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = merchantType,
            onValueChange = { merchantType = it },
            label = { Text("Merchant type") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    hasSearched = true
                    errorMessage = null
                    selectedMerchant = null

                    try {
                        merchants = RetrofitClient.merchantApi.searchMerchants(
                            name = merchantName.trim().ifBlank { null },
                            zip = zipCode.trim().ifBlank { null },
                            address = fullAddress.trim().ifBlank { null },
                            merchantType = merchantType.trim().ifBlank { null }
                        )
                    } catch (e: Exception) {
                        merchants = emptyList()
                        errorMessage = e.message ?: "Unable to search merchants"
                    } finally {
                        isLoading = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = TieyohAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(actionLabel)
        }

        Spacer(modifier = Modifier.height(14.dp))

        selectedMerchant?.let { merchant ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TieyohCardBg)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Selected merchant",
                        style = MaterialTheme.typography.labelMedium,
                        color = TieyohMuted
                    )

                    Text(
                        text = merchant.brandName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TieyohNavy
                    )

                    merchant.category?.let { category ->
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodySmall,
                            color = TieyohMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TieyohAccent)
                }
            }

            errorMessage != null -> {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error
                )
            }

            hasSearched && merchants.isEmpty() -> {
                Text("No eligible merchants found.", color = TieyohMuted)
            }

            merchants.isNotEmpty() -> {
                Text(
                    text = "${merchants.size} merchant(s) found",
                    style = MaterialTheme.typography.labelLarge,
                    color = TieyohNavy
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = merchants,
                        key = { merchant -> merchant.id }
                    ) { merchant ->
                        MerchantResultCard(
                            merchant = merchant,
                            selected = selectedMerchant?.id == merchant.id,
                            onSelect = {
                                selectedMerchant = merchant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MerchantResultCard(
    merchant: MerchantDto,
    selected: Boolean,
    onSelect: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) TieyohCardBg else Color.White)
            .border(1.dp, TieyohBorder, RoundedCornerShape(16.dp))
            .clickable { onSelect() }
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = merchant.brandName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TieyohNavy
                )

                merchant.category?.let { category ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = TieyohMuted
                    )
                }

                merchant.tmid?.let { tmid ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tmid,
                        style = MaterialTheme.typography.labelSmall,
                        color = TieyohMuted
                    )
                }
            }

            Text(
                text = if (selected) "Selected" else "›",
                style = MaterialTheme.typography.labelLarge,
                color = TieyohAccent
            )
        }
    }
}

@Composable
private fun ProfileScreen(
    currentUser: AuthResponse,
    onLogout: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(TieyohCardBg)
                .padding(18.dp)
        ) {
            Column {
                Text(
                    text = currentUser.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TieyohNavy
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentUser.email,
                    color = TieyohMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Friends, payment settings, and preferences will be added here.",
                    color = TieyohMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log out")
        }
    }
}
