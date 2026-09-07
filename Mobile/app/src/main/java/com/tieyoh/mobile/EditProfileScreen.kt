package com.tieyoh.mobile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Month/day only -- matches ProfileSetupScreen's birthday fields. Kept as
// a separate list here (rather than sharing ProfileSetupScreen's private
// val) since that one is private to its own file.
private val editMonthOptions = listOf(
    1 to "January", 2 to "February", 3 to "March", 4 to "April",
    5 to "May", 6 to "June", 7 to "July", 8 to "August",
    9 to "September", 10 to "October", 11 to "November", 12 to "December"
)

// Lets an already-created account go back and change email, phone,
// address, birthday, and holidays celebrated. Privacy (auto-accept
// circle invites, share gifting with your circle) lives on its own
// screen, PrivacySettingsScreen, reachable separately from Profile.
// Tieyoh ID and gender are shown read-only here (this screen
// isn't the place to change them) but still have to be resent on save --
// ProfileSetupRequest's tieyohId/phone/gender are @NotBlank on the
// backend, and saveProfile() only touches fields that are supplied, so
// the fetched values are round-tripped unchanged.
//
// email is the one field here that can fail to save on its own (someone
// else already has it) independent of the rest of the form -- see the
// 409 handling in save() below.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    currentUserId: Long,
    onDone: () -> Unit,
    // Lets the caller (MainActivity) swap in the freshly-saved account so
    // the Profile header and anywhere else currentUser.email/etc. is read
    // from doesn't keep showing the old value after a save. Defaults to a
    // no-op so existing call sites don't have to change.
    onProfileUpdated: (AuthResponse) -> Unit = {}
) {
    val context = LocalContext.current

    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var tieyohId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthMonth by remember { mutableStateOf<Int?>(null) }
    var birthDay by remember { mutableStateOf<Int?>(null) }
    var holidaysCelebrated by remember { mutableStateOf(setOf<String>()) }

    // Privacy (auto-accept circle invites, share gifting with your
    // circle) moved to its own screen -- see PrivacySettingsScreen --
    // reachable from Profile as its own row, not part of this one.

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentUserId) {
        try {
            val profile = RetrofitClient.userApi.getProfile(currentUserId)
            email = profile.email.orEmpty()
            tieyohId = profile.tieyohId.orEmpty()
            phone = profile.phone.orEmpty()
            address = profile.address.orEmpty()
            gender = profile.gender.orEmpty()
            birthMonth = profile.birthMonth
            birthDay = profile.birthDay
            holidaysCelebrated = holidaysToLabels(profile.holidaysCelebrated)
            loaded = true
        } catch (e: Exception) {
            loadError = true
        }
    }

    fun save() {
        errorMessage = null
        emailError = null
        val trimmedEmail = email.trim()
        if (trimmedEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailError = "That doesn't look like a valid email address."
            return
        }
        coroutineScope.launch {
            isSaving = true
            try {
                val updated = RetrofitClient.userApi.saveProfile(
                    userId = currentUserId,
                    request = ProfileSetupRequest(
                        tieyohId = tieyohId,
                        phone = phone,
                        address = address,
                        gender = gender,
                        birthMonth = birthMonth,
                        birthDay = birthDay,
                        holidaysCelebrated = holidaysToStored(holidaysCelebrated),
                        email = trimmedEmail.ifBlank { null }
                    )
                )
                onProfileUpdated(updated)
                onDone()
            } catch (e: HttpException) {
                errorMessage = if (e.code() == 409) {
                    "That email address is already in use."
                } else {
                    "Something went wrong (code ${e.code()}). Please try again."
                }
            } catch (e: Exception) {
                errorMessage = "Couldn't reach the server. Is tieyoh-api running?"
            } finally {
                isSaving = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDone) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TieyohNavy
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Edit profile",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TieyohNavy
        )

        Spacer(Modifier.height(32.dp))

        if (!loaded && !loadError) {
            CircularProgressIndicator(color = TieyohAccent)
        } else if (loadError) {
            Text(
                text = "Couldn't load your profile. Is tieyoh-api running?",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        } else {
            Text(
                text = "Contact info",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TieyohNavy,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = { Text("Email") },
                isError = emailError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            emailError?.let { message ->
                Spacer(Modifier.height(4.dp))
                Text(text = message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))

            PhoneNumberField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone number",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Birthday (optional)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TieyohNavy,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "So your circle gets a heads-up when it's coming up. We only ask for month and day -- never your birth year.",
                fontSize = 12.sp,
                color = TieyohMuted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                var monthExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = monthExpanded,
                    onExpandedChange = { monthExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = editMonthOptions.firstOrNull { it.first == birthMonth }?.second ?: "Month",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Month") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = monthExpanded,
                        onDismissRequest = { monthExpanded = false }
                    ) {
                        editMonthOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    birthMonth = value
                                    monthExpanded = false
                                }
                            )
                        }
                    }
                }

                var dayExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = birthDay?.toString() ?: "Day",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        (1..31).forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value.toString()) },
                                onClick = {
                                    birthDay = value
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Holidays you celebrate (optional)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TieyohNavy,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "So your circle knows which occasions matter to you.",
                fontSize = 12.sp,
                color = TieyohMuted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            HolidayCheckboxList(
                selected = holidaysCelebrated,
                onToggle = { label ->
                    holidaysCelebrated = if (holidaysCelebrated.contains(label)) {
                        holidaysCelebrated - label
                    } else {
                        holidaysCelebrated + label
                    }
                }
            )

            errorMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))

            TieyohPrimaryButton(
                text = "Save",
                onClick = { save() },
                enabled = !isSaving,
                isLoading = isSaving
            )

            Spacer(Modifier.height(12.dp))

            TieyohSecondaryButton(
                text = "Cancel",
                onClick = onDone
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}
