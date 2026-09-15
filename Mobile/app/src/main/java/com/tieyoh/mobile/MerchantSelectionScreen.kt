package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohGiftRed
import com.tieyoh.mobile.ui.theme.TieyohMuted
import kotlinx.coroutines.launch


// Formerly "Add merchants to your list" -- renamed and restyled to match
// the "What places make you happy?" reference image: fonts, colors, and
// sizes follow the same tokens as the rest of the redesign, and the old
// two-field (merchant name + city/zip) search collapses into the single
// search bar shown there, with "Use My Current Location" as the
// alternate path. Underneath, this is still the exact same real search
// (MerchantApi.searchMerchants) and save (UserApi.saveMerchantSelections)
// this screen already did -- only the presentation changed. Two pieces
// are NOT wired up yet, matching what was flagged when the mockup was
// shown: the search box always searches by name only for now (typing an
// address here doesn't yet filter by location -- the backend ANDs name/
// zip/address together rather than ORing them, so the single combined
// field can't safely populate both without a lot of false negatives);
// and "Use My Current Location" doesn't request location permission or
// read the device's GPS yet -- tapping it is a no-op placeholder until
// that's built, the same way the contacts screen got its own permission
// pre-prompt built separately from the list screen behind it.
//
// onBack has no clean destination of its own this deep in onboarding
// (this step is reached from a top-level session state, not the nested
// profile-setup flow the earlier back buttons live in) -- it defaults to
// the same place Skip already goes, which is a judgment call flagged the
// same way ChooseIdScreen's back arrow was, not a confirmed decision.
@Composable
fun MerchantSelectionScreen(
    currentUser: AuthResponse,
    onDone: () -> Unit,
    onBack: () -> Unit = onDone
) {
    var searchText by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("") }

    var results by remember { mutableStateOf<List<MerchantDto>>(emptyList()) }
    var selected by remember { mutableStateOf<List<MerchantDto>>(emptyList()) }

    var isSearching by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    fun search() {
        if (searchText.isBlank() && locationText.isBlank()) return
        coroutineScope.launch {
            isSearching = true
            hasSearched = true
            errorMessage = null
            try {
                results = RetrofitClient.merchantApi.searchMerchants(
                    name = searchText.trim().ifBlank { null },
                    zip = null,
                    address = locationText.trim().ifBlank { null },
                    merchantType = null
                )
            } catch (e: Exception) {
                results = emptyList()
                errorMessage = "Unable to search places right now."
            } finally {
                isSearching = false
            }
        }
    }

    fun toggle(merchant: MerchantDto) {
        // Open-ended for now -- no cap on how many merchants can be selected.
        selected = if (selected.any { it.id == merchant.id }) {
            selected.filterNot { it.id == merchant.id }
        } else {
            selected + merchant
        }
    }

    fun finish() {
        coroutineScope.launch {
            isSaving = true
            errorMessage = null
            try {
                RetrofitClient.userApi.saveMerchantSelections(
                    userId = currentUser.id,
                    request = MerchantSelectionRequest(merchantIds = selected.map { it.id })
                )
                onDone()
            } catch (e: Exception) {
                errorMessage = "Couldn't save your list right now. You can add places later from My List."
            } finally {
                isSaving = false
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TieyohBrandBlue,
        focusedLabelColor = TieyohBrandBlue,
        cursorColor = TieyohBrandBlue
    )
    val pillShape = RoundedCornerShape(26.dp)
    val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

    Box(modifier = Modifier.fillMaxSize()) {

        // Same decorative wave as the other post-signup screens.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1.4f)
                .height(120.dp)
                .offset(y = 70.dp)
                .clip(RoundedCornerShape(topStart = 600.dp, topEnd = 600.dp))
                .background(TieyohBrandTeal.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1.1f)
                .height(90.dp)
                .offset(y = 55.dp)
                .clip(RoundedCornerShape(topStart = 600.dp, topEnd = 600.dp))
                .background(TieyohBrandBlue.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TieyohBrandNavy
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Image(
                painter = painterResource(id = R.drawable.tieyoh_logo),
                contentDescription = "Tieyoh",
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "What places\nmake you happy?",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Search for your favorite places to get personalized recommendations and connect with people who share your interests.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // The illustration only shows before a search has been run --
            // once results are on screen there isn't room for it, same
            // idea as ChooseIdScreen's field collapsing once it has an
            // inline result message to show.
            if (!hasSearched) {
                Spacer(Modifier.height(18.dp))
                Image(
                    painter = painterResource(id = R.drawable.happy_places_icon),
                    contentDescription = "Places you'll love",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .aspectRatio(236f / 120f)
                )
            }

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search for a place or type an address...") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = TieyohMuted,
                        modifier = Modifier.clickable { search() }
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(TieyohMuted.copy(alpha = 0.25f)))
                Text(
                    "OR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TieyohMuted,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                Box(modifier = Modifier.weight(1f).height(1.dp).background(TieyohMuted.copy(alpha = 0.25f)))
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TieyohBrandBlue.copy(alpha = 0.07f))
                    .clickable {
                        // Not wired to real device location yet -- see the
                        // class comment. This is a placeholder tap target
                        // until location permission + GPS lookup are built,
                        // the same two-step pattern (permission pre-prompt,
                        // then the real feature) used for contacts.
                        errorMessage = "Finding places near you isn't available yet -- try searching by name for now."
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = TieyohBrandBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Use My Current Location", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TieyohBrandBlue)
                    Text("Find popular places near me", fontSize = 12.5.sp, color = TieyohBrandNavyLight2)
                }
            }

            if (hasSearched) {
                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSearching) "Searching..." else "${results.size} place(s) found",
                        fontSize = 13.sp,
                        color = TieyohMuted
                    )
                    Text(
                        text = "${selected.size} selected",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TieyohBrandNavy
                    )
                }

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(items = results, key = { it.id }) { merchant ->
                        val isSelected = selected.any { it.id == merchant.id }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) TieyohBrandBlue.copy(alpha = 0.08f) else Color.White)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) TieyohBrandBlue else TieyohMuted.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { toggle(merchant) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { toggle(merchant) },
                                colors = CheckboxDefaults.colors(checkedColor = TieyohBrandBlue)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(merchant.brandName, fontWeight = FontWeight.Bold, color = TieyohBrandNavy)
                                merchant.category?.let {
                                    Text(it, fontSize = 12.sp, color = TieyohMuted)
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            errorMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, color = TieyohGiftRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(if (!isSaving) brandGradient else Brush.horizontalGradient(listOf(TieyohMuted.copy(alpha = 0.35f), TieyohMuted.copy(alpha = 0.35f))))
                    .clickable(enabled = !isSaving) { finish() },
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(Color.White)
                    .border(width = 1.5.dp, color = TieyohBrandBlue, shape = pillShape)
                    .clickable(enabled = !isSaving) { onDone() },
                contentAlignment = Alignment.Center
            ) {
                Text("Skip for Now", color = TieyohBrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
