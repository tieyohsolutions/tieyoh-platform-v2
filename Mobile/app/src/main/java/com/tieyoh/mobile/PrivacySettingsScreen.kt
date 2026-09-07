package com.tieyoh.mobile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import kotlinx.coroutines.launch

// Its own screen, separate from Edit Profile -- previously these two
// toggles lived inside EditProfileScreen's own "Privacy" section, moved
// back out per the user's request to keep Privacy as its own distinct
// edit option under Profile rather than folded into profile-field
// editing. Both toggles save immediately when flipped, via the same
// PATCH /api/users/{id}/settings endpoint EditProfileScreen used.
@Composable
fun PrivacySettingsScreen(
    currentUserId: Long,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }

    var autoAcceptCircleInvites by remember { mutableStateOf(true) }
    var shareGiftingWithCircle by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        try {
            val profile = RetrofitClient.userApi.getProfile(currentUserId)
            autoAcceptCircleInvites = profile.autoAcceptCircleInvites ?: true
            shareGiftingWithCircle = profile.shareGiftingWithCircle ?: true
            loaded = true
        } catch (e: Exception) {
            loadError = true
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
            text = "Privacy",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TieyohNavy
        )

        Spacer(Modifier.height(32.dp))

        if (!loaded && !loadError) {
            CircularProgressIndicator(color = TieyohAccent)
        } else if (loadError) {
            Text(
                text = "Couldn't load your settings. Is tieyoh-api running?",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Auto-accept circle invites", fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 14.sp)
                    Text(
                        text = if (autoAcceptCircleInvites) {
                            "Anyone who adds you joins your circle right away"
                        } else {
                            "You'll approve each request before they join your circle"
                        },
                        color = TieyohMuted,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = autoAcceptCircleInvites,
                    onCheckedChange = { checked ->
                        val previous = autoAcceptCircleInvites
                        autoAcceptCircleInvites = checked
                        coroutineScope.launch {
                            try {
                                RetrofitClient.userApi.updateSettings(
                                    userId = currentUserId,
                                    request = UserSettingsRequest(autoAcceptCircleInvites = checked)
                                )
                            } catch (e: Exception) {
                                autoAcceptCircleInvites = previous
                                Toast.makeText(context, "Couldn't update setting right now", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Share gifting with your circle", fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 14.sp)
                    Text(
                        text = if (shareGiftingWithCircle) {
                            "Gifts you send default to visible to your circle"
                        } else {
                            "Gifts you send default to private -- just you and the recipient"
                        },
                        color = TieyohMuted,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = shareGiftingWithCircle,
                    onCheckedChange = { checked ->
                        val previous = shareGiftingWithCircle
                        shareGiftingWithCircle = checked
                        coroutineScope.launch {
                            try {
                                RetrofitClient.userApi.updateSettings(
                                    userId = currentUserId,
                                    request = UserSettingsRequest(shareGiftingWithCircle = checked)
                                )
                            } catch (e: Exception) {
                                shareGiftingWithCircle = previous
                                Toast.makeText(context, "Couldn't update setting right now", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            TieyohPrimaryButton(text = "Done", onClick = onDone)
        }

        Spacer(Modifier.height(40.dp))
    }
}
