package com.tieyoh.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log as AndroidLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import coil.compose.AsyncImage
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohGiftRed
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// "Find Partnered Merchants", reached from the "Pay with QR" screen
// (PayWithQrScreen.kt). Deliberately built as a browse-only sibling of
// AddMerchantSheet's Search/Map flow (MainActivity.kt, "Add a merchant to
// your list"): same segmented Search/Map toggle, the same single combined
// search box + Search button (TieyohSecondaryButton -- the app's one
// button standard, same font/size/colors as every other screen), the same
// map view with markers, "Search this area", and location-permission
// handling. Two differences from that screen: this is a full nav screen
// (reached via TieyohApp's own back-stack, not a modal Dialog), and there
// is no selection/save step here -- this screen exists purely so someone
// can find somewhere to pay, not to build a list.
//
// It only ever shows PARTNERED merchants: merchants that have signed up
// to accept a scanned Tieyoh payment QR code (Merchant.partnered on the
// backend, see MerchantRepository.searchEligiblePartneredMerchants /
// findNearbyPartnered). The much larger general merchant registry (every
// merchant Tieyoh knows about, most of which are NOT partnered) never
// shows up here -- that's still what AddMerchantSheet and
// MerchantSelectionScreen search.
private enum class PartneredSearchMode(val label: String) {
    SEARCH("Search"),
    MAP("Map")
}

private fun formatPartneredDistance(miles: Double): String {
    return if (miles < 0.1) "Nearby" else "%.1f mi away".format(miles)
}

// Every result on this screen is already guaranteed partnered (see the
// class comment / searchPartneredMerchants), but this still checks
// .partnered defensively so the same helper can be copy-pasted into a
// screen that mixes partnered and non-partnered merchants without
// silently showing a logo somewhere it shouldn't. Per spec, a
// non-partnered merchant is represented by name alone -- no icon, not
// even a placeholder.
@Composable
private fun MerchantLogoThumb(merchant: MerchantDto) {
    if (merchant.partnered && !merchant.logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = merchant.logoUrl,
            contentDescription = merchant.brandName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TieyohMuted.copy(alpha = 0.08f))
        )
        Spacer(Modifier.width(12.dp))
    }
}

@Composable
fun PartneredMerchantSearchScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(PartneredSearchMode.SEARCH) }
    var searchText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MerchantDto>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var locationServicesDisabled by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showMapView by remember { mutableStateOf(true) }
    var showSearchThisArea by remember { mutableStateOf(false) }
    var lastSearchedCenter by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(LocationManager::class.java)
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    // Shared by both "locate me" (fetchNearby, below) and the map's
    // "Search this area" button -- the only difference is whether the
    // camera should jump to the new center or stay put because the user
    // just put it there themselves by panning.
    suspend fun fetchNearbyAt(lat: Double, lng: Double, moveCamera: Boolean) {
        isSearching = true
        hasSearched = true
        errorMessage = null
        try {
            results = RetrofitClient.merchantApi.getNearbyPartneredMerchants(lat = lat, lng = lng)
            lastSearchedCenter = LatLng(lat, lng)
            showSearchThisArea = false
            if (moveCamera) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 14f)
            }
        } catch (e: Exception) {
            AndroidLog.e("PartneredMerchantSearch", "Failed to fetch nearby partnered merchants", e)
            results = emptyList()
            errorMessage = "Unable to find partnered merchants near you right now."
        } finally {
            isSearching = false
        }
    }

    fun switchMode(newMode: PartneredSearchMode) {
        if (mode == newMode) return
        mode = newMode
        searchText = ""
        results = emptyList()
        hasSearched = false
        errorMessage = null
        locationServicesDisabled = false
        showSearchThisArea = false
        lastSearchedCenter = null
        if (newMode == PartneredSearchMode.MAP) {
            showMapView = true
        }
    }

    fun search() {
        if (searchText.isBlank()) return
        coroutineScope.launch {
            isSearching = true
            hasSearched = true
            errorMessage = null
            try {
                // One combined query, OR'd server-side across name and
                // every location field, same as the general merchant
                // search -- just restricted to partnered merchants only.
                results = RetrofitClient.merchantApi.searchPartneredMerchants(q = searchText.trim())
            } catch (e: Exception) {
                results = emptyList()
                errorMessage = "Unable to search partnered merchants right now."
            } finally {
                isSearching = false
            }
        }
    }

    fun fetchNearby() {
        // Checked up front so a device with Location switched off
        // entirely gets a direct link to the right settings screen
        // instead of a generic "couldn't find your location" dead end.
        if (!isLocationEnabled()) {
            hasSearched = true
            results = emptyList()
            locationServicesDisabled = true
            errorMessage = "Location is turned off on this device."
            return
        }
        locationServicesDisabled = false

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        coroutineScope.launch {
            isSearching = true
            hasSearched = true
            errorMessage = null
            try {
                var location = try {
                    fusedClient.lastLocation.await()
                } catch (e: Exception) {
                    AndroidLog.w("PartneredMerchantSearch", "lastLocation failed, falling back to getCurrentLocation", e)
                    null
                }

                if (location == null) {
                    location = fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                }

                if (location == null) {
                    results = emptyList()
                    errorMessage = "Couldn't get your location -- make sure location is turned on."
                    isSearching = false
                } else {
                    fetchNearbyAt(location.latitude, location.longitude, moveCamera = true)
                }
            } catch (e: Exception) {
                AndroidLog.e("PartneredMerchantSearch", "Failed to fetch nearby partnered merchants", e)
                results = emptyList()
                errorMessage = "Unable to find partnered merchants near you right now."
                isSearching = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            fetchNearby()
        } else {
            errorMessage = "Location permission is needed to find partnered merchants near you."
        }
    }

    fun useMyLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasLocationPermission = hasPermission

        if (hasPermission) {
            fetchNearby()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Auto-locate the moment Map mode is entered, same as AddMerchantSheet
    // -- switchMode() only fires this once per entry (it early-returns if
    // the mode doesn't actually change), so re-tapping Map after it's
    // already active won't spam relocate; leaving and coming back will.
    LaunchedEffect(mode) {
        if (mode == PartneredSearchMode.MAP && lastSearchedCenter == null) {
            useMyLocation()
        }
    }

    // Offers "Search this area" once the user pans away from wherever was
    // last searched and lets go -- not while the map is still moving
    // under their finger, and not for a jitter-sized nudge.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            val last = lastSearchedCenter
            if (last != null) {
                val distanceMeters = FloatArray(1)
                Location.distanceBetween(last.latitude, last.longitude, center.latitude, center.longitude, distanceMeters)
                if (distanceMeters[0] > 400) {
                    showSearchThisArea = true
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            text = "Partnered Merchants",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = TieyohNavy
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Only merchants that accept your Tieyoh QR code show up here.",
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = TieyohMuted
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(TieyohMuted.copy(alpha = 0.10f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PartneredSearchMode.values().forEach { m ->
                val isActive = mode == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isActive) Color.White else Color.Transparent)
                        .clickable { switchMode(m) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        m.label,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) TieyohNavy else TieyohMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (mode == PartneredSearchMode.MAP) {
            if (locationServicesDisabled) {
                TieyohSecondaryButton(
                    text = "Turn on Location",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TieyohMuted.copy(alpha = 0.10f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Map" to true, "List" to false).forEach { (label, isMap) ->
                    val isActive = showMapView == isMap
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) Color.White else Color.Transparent)
                            .clickable { showMapView = isMap }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) TieyohNavy else TieyohMuted
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search by name, address, city, or zip") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            // Spelled out explicitly -- not obvious from the placeholder
            // alone once it's scrolled out of view while typing that this
            // one box covers all four.
            Text(
                text = "Try a business name like \"Starbucks,\" a full or partial street address, a city, or a ZIP code.",
                fontSize = 11.5.sp,
                color = TieyohMuted
            )

            Spacer(Modifier.height(8.dp))

            // Same button standard as every other screen in the app --
            // TieyohPrimaryButton (the filled gradient pill), the exact
            // component the app's main CTAs use everywhere else.
            TieyohPrimaryButton(
                text = "Search",
                onClick = { search() }
            )
        }

        Spacer(Modifier.height(14.dp))

        if (mode == PartneredSearchMode.MAP && showMapView) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
                ) {
                    results.forEach { merchant ->
                        val lat = merchant.latitude
                        val lng = merchant.longitude
                        if (lat != null && lng != null) {
                            Marker(
                                state = MarkerState(position = LatLng(lat, lng)),
                                title = merchant.brandName,
                                snippet = merchant.category
                            )
                        }
                    }
                }

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TieyohAccent)
                    }
                }

                if (hasSearched && results.isEmpty() && !isSearching) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("No partnered merchants found in this area.", fontSize = 12.sp, color = TieyohMuted)
                    }
                }

                if (showSearchThisArea) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 12.dp)
                    ) {
                        TieyohSecondaryButton(
                            text = "Search this area",
                            onClick = {
                                val target = cameraPositionState.position.target
                                coroutineScope.launch {
                                    fetchNearbyAt(target.latitude, target.longitude, moveCamera = false)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            when {
                isSearching -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TieyohAccent)
                }

                hasSearched && results.isEmpty() -> Text(
                    if (mode == PartneredSearchMode.MAP)
                        "No partnered merchants found near you."
                    else
                        "No partnered merchants found -- try a different search.",
                    color = TieyohMuted,
                    fontSize = 13.sp
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(results, key = { it.id }) { merchant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.dp,
                                    color = TieyohMuted.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MerchantLogoThumb(merchant)
                            Column {
                                Text(merchant.brandName, fontWeight = FontWeight.Bold, color = TieyohNavy)
                                val subtitle = listOfNotNull(
                                    merchant.category,
                                    merchant.distanceMiles?.let { formatPartneredDistance(it) }
                                ).joinToString("  ·  ")
                                if (subtitle.isNotEmpty()) {
                                    Text(subtitle, fontSize = 12.sp, color = TieyohMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, color = TieyohGiftRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))
    }
}
