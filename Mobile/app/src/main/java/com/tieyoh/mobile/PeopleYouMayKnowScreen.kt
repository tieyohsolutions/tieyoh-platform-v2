package com.tieyoh.mobile

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohMuted
import kotlinx.coroutines.launch

// Step right after the contacts-permission screen, only reached once the
// OS has actually granted READ_CONTACTS (see ContactsPermissionScreen).
// Matches the "People you may know on Tieyoh" reference image, adapted
// per the follow-up spec: instead of an algorithmic "mutual friends"
// suggestion feed, this reads the device's own address book and checks it
// against real Tieyoh accounts (POST .../friends/check-contacts, read-only
// -- see FriendService.checkContacts). Matches are grouped up top with a
// Connect button; everyone else is grouped below with an Invite button.
//
// Connect calls the same addFriend endpoint the rest of the app already
// uses to add someone found via search -- FriendService.addFriend links
// the two accounts immediately (ACTIVE) UNLESS the other person has their
// own "approve circle invites first" preference on
// (AppUser.autoAcceptCircleInvites = false), in which case it comes back
// PENDING_APPROVAL and they get a connection-request notification instead.
// Either way this screen just reflects whatever status the server hands
// back -- there's no separate approval logic duplicated here.
//
// Invite calls the same invitation-batch endpoint "Invite Friends" already
// uses -- it creates a real, separately-redeemable invite link and a
// PENDING circle entry for that contact (shown greyed out with a clock
// badge wherever the circle/network is rendered elsewhere in the app),
// then hands the link to the device's own native share sheet so the
// person can actually send it by text, email, or whatever app they pick --
// Tieyoh's backend never sends anything itself (no SMS/email provider).
@Composable
fun PeopleYouMayKnowScreen(
    ownerTieyohId: String,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var matched by remember { mutableStateOf<List<ContactMatchResult>>(emptyList()) }
    var unmatched by remember { mutableStateOf<List<ContactMatchResult>>(emptyList()) }

    // matchedUserId -> the status addFriend actually returned ("ACTIVE" or
    // "PENDING_APPROVAL"), so the button label reflects reality instead of
    // assuming every Connect tap links instantly.
    var connectedStatus by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var connectingUserId by remember { mutableStateOf<Long?>(null) }

    // Keyed by phone-or-email since an unmatched contact has no user id yet.
    var invitedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var invitingKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ownerTieyohId) {
        isLoading = true
        loadError = false
        try {
            val deviceContacts = loadDeviceContacts(context)
            val response = RetrofitClient.friendApi.checkContacts(
                ownerTieyohId,
                ContactMatchBatchRequest(
                    contacts = deviceContacts.map {
                        InvitationRecipientRequest(displayName = it.displayName, email = it.email, phone = it.phone)
                    }
                )
            )
            matched = response.filter { it.matchedUserId != null }
                .sortedByDescending { it.mutualFriendsCount ?: 0 }
            unmatched = response.filter { it.matchedUserId == null }
        } catch (e: Exception) {
            loadError = true
        } finally {
            isLoading = false
        }
    }

    fun connect(result: ContactMatchResult) {
        val userId = result.matchedUserId ?: return
        coroutineScope.launch {
            connectingUserId = userId
            try {
                val response = RetrofitClient.friendApi.addFriend(
                    ownerTieyohId,
                    AddFriendRequest(contactUserId = userId, displayName = result.displayName, phone = null, email = null)
                )
                connectedStatus = connectedStatus + (userId to response.status)
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't connect right now. Please try again.", Toast.LENGTH_SHORT).show()
            } finally {
                connectingUserId = null
            }
        }
    }

    fun invite(result: ContactMatchResult) {
        val key = result.phone ?: result.email ?: return
        coroutineScope.launch {
            invitingKey = key
            try {
                val deviceCountry = defaultCountryForDevice(context)
                val response = RetrofitClient.invitationApi.createBatch(
                    InvitationBatchRequest(
                        inviterTieyohId = ownerTieyohId,
                        recipients = listOf(
                            InvitationRecipientRequest(
                                displayName = result.displayName,
                                email = result.email,
                                phone = result.phone?.let { toE164OrRaw(it, deviceCountry) }
                            )
                        )
                    )
                )
                val url = response.invitations.firstOrNull()?.inviteUrl
                invitedKeys = invitedKeys + key
                // A null url means this phone/email turned out to already
                // belong to a real account (a race against our earlier
                // read-only check) -- addFriend already linked them into
                // the circle server-side, there's nothing left to share.
                if (url != null) {
                    context.startActivity(Intent.createChooser(shareIntentFor(url), "Share invitation"))
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't create that invitation right now. Please try again.", Toast.LENGTH_SHORT).show()
            } finally {
                invitingKey = null
            }
        }
    }

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
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TieyohNavy
                    )
                }
                Text(
                    text = "Contacts Permission",
                    color = TieyohNavy,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            Image(
                painter = painterResource(id = R.drawable.tieyoh_logo),
                contentDescription = "Tieyoh",
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Connect with people\nyou may know",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "We checked your contacts against Tieyoh -- connect instantly with the ones already here, or invite the rest.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            when {
                isLoading -> {
                    Spacer(Modifier.weight(1f))
                    CircularProgressIndicator(color = TieyohBrandBlue)
                    Spacer(Modifier.weight(1f))
                }

                loadError -> {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Couldn't check your contacts right now. Is tieyoh-api running?",
                        fontSize = 14.sp,
                        color = TieyohBrandNavyLight2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.weight(1f))
                }

                matched.isEmpty() && unmatched.isEmpty() -> {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "We didn't find any contacts with a phone number or email to check.",
                        fontSize = 14.sp,
                        color = TieyohBrandNavyLight2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.weight(1f))
                }

                else -> {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        if (matched.isNotEmpty()) {
                            item {
                                SectionLabel("Already on Tieyoh")
                            }
                            items(matched, key = { "m-${it.matchedUserId}" }) { result ->
                                MatchedContactRow(
                                    result = result,
                                    status = result.matchedUserId?.let { connectedStatus[it] },
                                    isConnecting = connectingUserId == result.matchedUserId,
                                    onConnect = { connect(result) }
                                )
                            }
                        }
                        if (unmatched.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(6.dp))
                                SectionLabel("Not on Tieyoh yet")
                            }
                            items(unmatched, key = { it.phone ?: it.email ?: it.displayName }) { result ->
                                val key = result.phone ?: result.email ?: result.displayName
                                UnmatchedContactRow(
                                    result = result,
                                    isInvited = invitedKeys.contains(key),
                                    isInviting = invitingKey == key,
                                    onInvite = { invite(result) }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                    }
                }
            }

            val pillShape = RoundedCornerShape(26.dp)
            val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(brandGradient)
                    .clickable { onDone() },
                contentAlignment = Alignment.Center
            ) {
                Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(Color.White)
                    .border(width = 1.5.dp, color = TieyohBrandBlue, shape = pillShape)
                    .clickable { onDone() },
                contentAlignment = Alignment.Center
            ) {
                Text("Skip for Now", color = TieyohBrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// Matches the heading's font weight/color, per explicit feedback on the
// review mockup -- not the smaller, uppercase/letter-spaced label style
// used elsewhere in the app.
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = TieyohBrandDarkBlue,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun MatchedContactRow(
    result: ContactMatchResult,
    status: String?,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchResultAvatar(profilePictureBase64 = result.matchedProfilePictureBase64, displayName = result.displayName)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TieyohBrandNavy)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = null,
                    tint = TieyohMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${result.mutualFriendsCount ?: 0} mutual friends",
                    fontSize = 12.sp,
                    color = TieyohMuted
                )
            }
        }
        Spacer(Modifier.width(8.dp))

        val pillShape = RoundedCornerShape(20.dp)
        when {
            isConnecting -> {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TieyohBrandBlue)
            }
            status == "ACTIVE" -> {
                StatusPill(text = "Connected")
            }
            status == "PENDING_APPROVAL" -> {
                StatusPill(text = "Requested")
            }
            else -> {
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal)))
                        .clickable { onConnect() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Connect", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun UnmatchedContactRow(
    result: ContactMatchResult,
    isInvited: Boolean,
    isInviting: Boolean,
    onInvite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchResultAvatar(profilePictureBase64 = null, displayName = result.displayName)
        Spacer(Modifier.width(12.dp))
        Text(
            result.displayName,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TieyohBrandNavy,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))

        val pillShape = RoundedCornerShape(20.dp)
        when {
            isInviting -> {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TieyohBrandBlue)
            }
            isInvited -> {
                StatusPill(text = "Invited")
            }
            else -> {
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(Color.White)
                        .border(width = 1.5.dp, color = TieyohBrandBlue, shape = pillShape)
                        .clickable { onInvite() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Invite", color = TieyohBrandBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TieyohMuted.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = TieyohMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
