package com.tieyoh.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tieyoh.mobile.ui.theme.TieyohTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TieyohTheme {
                TieyohApp()
            }
        }
    }
}

private enum class TieyohScreen {
    DASHBOARD,
    MY_GIFTS,
    SEND_GIFT,
    SEARCH_MERCHANT,
    PROFILE
}

private data class FriendActivity(
    val title: String,
    val detail: String
)

@Composable
fun TieyohApp() {

    var currentScreen by remember {
        mutableStateOf(TieyohScreen.DASHBOARD)
    }

    Scaffold(
        topBar = {
            TieyohTopBar(
                onLogoClick = {
                    currentScreen = TieyohScreen.DASHBOARD
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
        }
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            when (currentScreen) {

                TieyohScreen.DASHBOARD -> {
                    DashboardScreen(
                        onSendGift = {
                            currentScreen = TieyohScreen.SEND_GIFT
                        },
                        onSearchMerchant = {
                            currentScreen = TieyohScreen.SEARCH_MERCHANT
                        }
                    )
                }

                TieyohScreen.MY_GIFTS -> MyGiftsScreen()

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

                TieyohScreen.PROFILE -> ProfileScreen()
            }
        }
    }
}

@Composable
private fun TieyohTopBar(
    onLogoClick: () -> Unit
) {

    Surface(
        shadowElevation = 2.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = painterResource(
                    id = R.drawable.tieyoh_logo
                ),
                contentDescription = "Tieyoh",
                modifier = Modifier
                    .height(38.dp)
                    .clickable {
                        onLogoClick()
                    }
            )
        }
    }
}

@Composable
private fun TieyohBottomNavigation(
    currentScreen: TieyohScreen,
    onNavigate: (TieyohScreen) -> Unit
) {

    NavigationBar {

        NavigationBarItem(
            selected = currentScreen == TieyohScreen.MY_GIFTS,
            onClick = { onNavigate(TieyohScreen.MY_GIFTS) },
            icon = { Text("G") },
            label = { Text("My Gifts") }
        )

        NavigationBarItem(
            selected = currentScreen == TieyohScreen.SEND_GIFT,
            onClick = { onNavigate(TieyohScreen.SEND_GIFT) },
            icon = { Text("+") },
            label = { Text("Send Gift") }
        )

        NavigationBarItem(
            selected = currentScreen == TieyohScreen.SEARCH_MERCHANT,
            onClick = { onNavigate(TieyohScreen.SEARCH_MERCHANT) },
            icon = { Text("⌕") },
            label = { Text("Search") }
        )

        NavigationBarItem(
            selected = currentScreen == TieyohScreen.PROFILE,
            onClick = { onNavigate(TieyohScreen.PROFILE) },
            icon = { Text("P") },
            label = { Text("Profile") }
        )
    }
}

@Composable
private fun DashboardScreen(
    onSendGift: () -> Unit,
    onSearchMerchant: () -> Unit
) {

    val activities = listOf(
        FriendActivity(
            title = "Maya sent Jordan a birthday gift",
            detail = "Gift designated for Bluebird Cafe"
        ),
        FriendActivity(
            title = "Chris and Alex exchanged gifts",
            detail = "Celebrating their anniversary"
        ),
        FriendActivity(
            title = "Daniel thanked Sam for a gift",
            detail = "Used at a local restaurant"
        ),
        FriendActivity(
            title = "Hannah sent a congratulations gift",
            detail = "A new Tieyoh exchange between friends"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 20.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            Text(
                text = "What's happening",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Gifts and exchanges from people you're connected with on Tieyoh.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        items(activities) { activity ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = activity.detail,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSendGift,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send a Gift")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onSearchMerchant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search for a Merchant")
            }
        }
    }
}

@Composable
private fun MyGiftsScreen() {

    val gifts = listOf(
        "Birthday Gift • Available",
        "Dinner Gift • Available",
        "Congratulations Gift • Used"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 20.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            Text(
                text = "My Gifts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Mock gift data for the first mobile prototype.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        items(gifts) { gift ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = gift,
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
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
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(actionLabel)
        }

        Spacer(modifier = Modifier.height(14.dp))

        selectedMerchant?.let { merchant ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "Selected merchant",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Text(
                        text = merchant.brandName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    merchant.category?.let { category ->
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodySmall
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
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error
                )
            }

            hasSearched && merchants.isEmpty() -> {
                Text("No eligible merchants found.")
            }

            merchants.isNotEmpty() -> {
                Text(
                    text = "${merchants.size} merchant(s) found",
                    style = MaterialTheme.typography.labelLarge
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelect()
            }
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
                    fontWeight = FontWeight.SemiBold
                )

                merchant.category?.let { category ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                merchant.tmid?.let { tmid ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tmid,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(
                text = if (selected) "Selected" else "›",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ProfileScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "Tieyoh Member",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Profile, friends, payment settings, and preferences will be added here."
                )
            }
        }
    }
}
