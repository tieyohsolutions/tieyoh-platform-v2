package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val genderOptions = listOf(
    Triple("MALE", "Male", "♂"),
    Triple("FEMALE", "Female", "♀"),
    Triple("OTHER", "Other", "⚥")
)

private const val MIN_AGE_YEARS = 13L

// Last screen of profile setup (Choose @t id -> Privacy -> here). Matches
// the "Tell us a bit about yourself" reference image: fonts, colors, and
// sizes follow the same tokens as the rest of the Sep 2026 redesign.
// Replaces the previous version of this screen entirely -- phone is no
// longer collected here since the "Let's get started" screen already
// captured and the account-creation step already persisted it (see
// AuthService.signup); asking again would just be redundant and this
// reference image doesn't show a phone field at all.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    tieyohId: String,
    currentUser: AuthResponse,
    initialBirthDate: LocalDate?,
    initialGender: String?,
    initialHolidaysCelebrated: Set<String>,
    initialAddress: String,
    onFieldsChanged: (birthDate: LocalDate?, gender: String?, holidaysCelebrated: Set<String>, address: String) -> Unit,
    onBack: () -> Unit,
    onProfileComplete: (AuthResponse) -> Unit
) {
    var birthDate by remember { mutableStateOf(initialBirthDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf(initialGender) }
    var holidaysCelebrated by remember { mutableStateOf(initialHolidaysCelebrated) }
    var address by remember { mutableStateOf(initialAddress) }

    // Reports the latest field values up to the caller so they survive this
    // screen leaving and re-entering composition (e.g. Continue to Find
    // Friends, then Back) instead of resetting blank every time.
    LaunchedEffect(birthDate, gender, holidaysCelebrated, address) {
        onFieldsChanged(birthDate, gender, holidaysCelebrated, address)
    }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TieyohBrandBlue,
        focusedLabelColor = TieyohBrandBlue,
        cursorColor = TieyohBrandBlue
    )

    fun submit() {
        errorMessage = null

        val date = birthDate
        if (date == null) {
            errorMessage = "Please select your date of birth."
            return
        }

        val selectedGender = gender
        if (selectedGender == null) {
            errorMessage = "Please select Male, Female, or Other."
            return
        }

        coroutineScope.launch {
            isLoading = true
            try {
                val updated = RetrofitClient.userApi.saveProfile(
                    userId = currentUser.id,
                    request = ProfileSetupRequest(
                        tieyohId = tieyohId,
                        address = address.trim().ifBlank { null },
                        gender = selectedGender,
                        birthMonth = date.monthValue,
                        birthDay = date.dayOfMonth,
                        holidaysCelebrated = holidaysToStored(holidaysCelebrated)
                    )
                )
                onProfileComplete(updated)
            } catch (e: HttpException) {
                errorMessage = when (e.code()) {
                    409 -> "That @t ID is already in use -- please go back and choose another."
                    400 -> "Please double-check your details."
                    else -> "Something went wrong (code ${e.code()}). Please try again."
                }
            } catch (e: Exception) {
                errorMessage = "Couldn't reach the server. Is tieyoh-api running?"
            } finally {
                isLoading = false
            }
        }
    }

    val canContinue = birthDate != null && gender != null && !isLoading

    if (showDatePicker) {
        val maxSelectableMillis = LocalDate.now().minusYears(MIN_AGE_YEARS)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDate
                ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
                ?: maxSelectableMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= maxSelectableMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        birthDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = TieyohBrandBlue, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TieyohMuted)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Same decorative wave as the other post-signup screens.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1.4f)
                .height(120.dp)
                .offset(y = 55.dp)
                .clip(RoundedCornerShape(topStart = 600.dp, topEnd = 600.dp))
                .background(TieyohBrandTeal.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1.1f)
                .height(90.dp)
                .offset(y = 40.dp)
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

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Tell us a bit about yourself",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "This helps us personalize your experience and connect you with the right people.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            FieldLabel("Date of birth", required = true)
            Spacer(Modifier.height(6.dp))

            val dateText = birthDate?.let {
                "%02d / %02d / %04d".format(it.monthValue, it.dayOfMonth, it.year)
            } ?: ""

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("MM / DD / YYYY") },
                    trailingIcon = {
                        Icon(Icons.Filled.DateRange, contentDescription = "Pick date", tint = TieyohBrandBlue)
                    },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showDatePicker = true }
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = "You must be at least $MIN_AGE_YEARS years old to create an account.",
                fontSize = 12.sp,
                color = TieyohMuted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            FieldLabel("Sex", required = true)
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                genderOptions.forEach { (value, label, symbol) ->
                    val selected = gender == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) TieyohBrandBlue.copy(alpha = 0.08f) else Color.White)
                            .border(
                                width = 1.5.dp,
                                color = if (selected) TieyohBrandBlue else TieyohMuted.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { gender = value },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = symbol,
                                fontSize = 16.sp,
                                color = if (selected) TieyohBrandBlue else TieyohBrandNavy
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) TieyohBrandBlue else TieyohBrandNavy
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            FieldLabel("Key holidays you celebrate", required = false)
            Spacer(Modifier.height(6.dp))

            HolidaysDropdown(
                selected = holidaysCelebrated,
                onToggle = { label ->
                    holidaysCelebrated = if (holidaysCelebrated.contains(label)) {
                        holidaysCelebrated - label
                    } else {
                        holidaysCelebrated + label
                    }
                },
                colors = fieldColors
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = "Helps us suggest relevant content and connection opportunities.",
                fontSize = 12.sp,
                color = TieyohMuted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            FieldLabel("Address", required = false)
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                placeholder = { Text("Enter your address") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TieyohBrandBlue)
                },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = "Used to suggest local connections and events near you.",
                fontSize = 12.sp,
                color = TieyohMuted,
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { message ->
                Spacer(Modifier.height(14.dp))
                Text(message, color = TieyohGiftRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))

            val pillShape = RoundedCornerShape(26.dp)
            val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(if (canContinue) brandGradient else Brush.horizontalGradient(listOf(TieyohMuted.copy(alpha = 0.35f), TieyohMuted.copy(alpha = 0.35f))))
                    .clickable(enabled = canContinue) { submit() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String, required: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TieyohBrandNavy)
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (required) "(required)" else "(optional)",
            fontSize = 13.sp,
            color = TieyohMuted
        )
    }
}

// Same picklist/storage format as the old checkbox version
// (HolidayOptions.kt) -- restyled here to match the new field look
// (brand-blue focus colors, "Select holidays" placeholder instead of
// "None selected").
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HolidaysDropdown(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    colors: androidx.compose.material3.TextFieldColors
) {
    var expanded by remember { mutableStateOf(false) }

    val orderedSelected = HOLIDAY_OPTIONS.filter { selected.contains(it) }
    val summary = when {
        orderedSelected.isEmpty() -> ""
        orderedSelected.size <= 2 -> orderedSelected.joinToString(", ")
        else -> "${orderedSelected.take(2).joinToString(", ")} +${orderedSelected.size - 2} more"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = summary,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Select holidays") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = colors,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            HOLIDAY_OPTIONS.forEach { label ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selected.contains(label),
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = TieyohBrandBlue)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = label, fontSize = 14.sp, color = TieyohBrandNavy)
                        }
                    },
                    // Deliberately doesn't close the menu -- picking several
                    // holidays shouldn't mean reopening it after every tap.
                    onClick = { onToggle(label) }
                )
            }
        }
    }
}
