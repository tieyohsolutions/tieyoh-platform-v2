package com.tieyoh.mobile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Shown from the top bar's add-people icon (managing your circle) and from
// the bottom nav's Send button (picking who to gift) -- same screen either
// way. Your circle (backed by GET/POST /api/users/{id}/friends) is listed
// up front: ACTIVE entries are real Tieyoh accounts, full color and
// clickable straight into the gift flow; PENDING entries are invited but
// haven't joined yet, shown greyed out with a clock badge. Typing a name
// searches the circle first; if nobody there matches, it falls back to the
// live Tieyoh-wide search + invite flow. Selecting an ACTIVE person calls
// onSelectRecipient; onDone is the back/cancel action.
@Composable
fun ContactsInviteScreen(
    currentUserId: Long,
    // The signed-in user's own Tieyoh @t- id -- used (instead of
    // currentUserId) for every circle/invite call, since friend_connection
    // rows are now linked by @t- id, not internal numeric id. currentUserId
    // is still used for the Tieyoh-wide user search below, which excludes
    // the searcher by numeric id.
    ownerTieyohId: String,
    onDone: () -> Unit,
    onSelectRecipient: (FriendResult) -> Unit = {},
    title: String = "Add someone to your circle",
    subtitle: String = "Search by Tieyoh ID, phone, or email -- no need to type \"@t-\", it's added automatically.",
    doneLabel: String = "Done",
    // Non-null only for the "manage my circle" entry points (top-bar + and
    // the post-signup prompt) -- shows a second button for the bulk,
    // multi-select "Invite Friends" flow. Left null for the send-a-gift
    // picker, where inviting a batch of strangers isn't the point.
    onInviteFriendsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<FriendResult>>(emptyList()) }
    var friendsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(ownerTieyohId) {
        friends = try {
            RetrofitClient.friendApi.getFriends(ownerTieyohId)
        } catch (e: Exception) {
            emptyList()
        }
        friendsLoaded = true
    }

    var showInviteToTieyoh by remember { mutableStateOf(false) }

    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<UserSearchResult>>(emptyList()) }
    var searchCompletedForQuery by remember { mutableStateOf(false) }
    // Set right before showing the confirm dialog below -- an existing
    // account is a real person's real account, so linking to one now
    // requires an explicit yes rather than a single accidental tap.
    var pendingConfirmUser by remember { mutableStateOf<UserSearchResult?>(null) }

    val trimmedQuery = query.trim()

    // Anyone already in the circle whose name or @t id matches what's
    // typed -- checked before ever touching the network.
    val circleMatches = if (trimmedQuery.isEmpty()) {
        emptyList()
    } else {
        friends.filter { f ->
            f.displayName.contains(trimmedQuery, ignoreCase = true) ||
                (f.tieyohId?.contains(trimmedQuery, ignoreCase = true) == true)
        }
    }

    // A live Tieyoh-wide search runs any time something's typed, even when
    // the circle already has a match on that text -- a circle hit for one
    // person (e.g. an already-added "Smith") says nothing about whether
    // someone else shares that name, so it must never suppress the network
    // fallback (that used to make a second same-name match disappear the
    // moment the first one was added to the circle). Results already in
    // the circle are filtered out below (visibleSearchResults) instead of
    // skipped here, so adding someone never hides anyone else.
    LaunchedEffect(query, friendsLoaded) {
        if (!friendsLoaded) return@LaunchedEffect
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            isSearching = false
            searchCompletedForQuery = false
            searchResults = emptyList()
            return@LaunchedEffect
        }
        searchCompletedForQuery = false
        isSearching = true
        delay(350)
        try {
            searchResults = RetrofitClient.userApi.searchUsers(
                q = trimmed,
                excludeUserId = currentUserId
            )
        } catch (e: Exception) {
            searchResults = emptyList()
        } finally {
            isSearching = false
            searchCompletedForQuery = true
        }
    }

    // Search hits that are already in the circle (just added, or already
    // a friend before this query even ran) -- excluded so "On Tieyoh"
    // never offers a duplicate Invite for someone who's already there.
    val visibleSearchResults = searchResults.filter { user ->
        friends.none { it.contactUserId == user.id }
    }

    fun mergeFriend(result: FriendResult) {
        friends = (friends.filter { it.id != result.id } + result).sortedBy { it.displayName }
    }

    // Refetches the whole circle rather than splicing in a single row --
    // needed because InvitationBatchResponse (unlike addFriend) doesn't
    // hand back a FriendResult to merge; it only says whether a shareable
    // link was created. Simpler and just as correct to re-pull the list,
    // which already covers both outcomes (a fresh PENDING row, or an
    // immediate ACTIVE/PENDING_APPROVAL link if the phone/email actually
    // matched an existing account).
    suspend fun refreshFriends() {
        friends = try {
            RetrofitClient.friendApi.getFriends(ownerTieyohId)
        } catch (e: Exception) {
            friends
        }
    }

    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}

    // Every invite to someone not yet confirmed to be a Tieyoh account now
    // goes through InvitationService instead of a bare, token-less PENDING
    // FriendConnection -- that's what actually gives the recipient a real
    // link back to whoever invited them once they sign up, rather than
    // depending on their eventual phone/email happening to match exactly
    // what was typed here. If the phone/email already belongs to a real
    // account, the backend links it immediately and inviteUrl comes back
    // null -- nothing to share in that case.
    fun createInvitationAndShare(name: String, phone: String?, email: String?, successMessage: String) {
        coroutineScope.launch {
            try {
                val response = RetrofitClient.invitationApi.createBatch(
                    InvitationBatchRequest(
                        inviterTieyohId = ownerTieyohId,
                        recipients = listOf(InvitationRecipientRequest(displayName = name, email = email, phone = phone))
                    )
                )
                refreshFriends()
                val inviteUrl = response.invitations.firstOrNull()?.inviteUrl
                if (inviteUrl != null) {
                    if (phone != null) {
                        // A phone-number invite is a text message -- Tieyoh
                        // sending it directly, not the person picking an app
                        // from a share sheet. There's no SMS service wired
                        // up yet, so InviteMessageSender logs what would be
                        // sent (visible in Logcat) until one is connected.
                        InviteMessageSender.sendTextMessage(name, phone, inviteUrl)
                        Toast.makeText(
                            context,
                            "Text invite sent to $name (no SMS service connected yet -- see Logcat)",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        shareLauncher.launch(Intent.createChooser(shareIntentFor(inviteUrl), "Share invitation"))
                    }
                } else {
                    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't invite $name right now", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val name = (if (nameIndex >= 0) cursor.getString(nameIndex) else null) ?: "Contact"
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    var phone = ""
                    if (idIndex >= 0 && hasPhoneIndex >= 0 && cursor.getInt(hasPhoneIndex) > 0) {
                        val contactId = cursor.getString(idIndex)
                        context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )?.use { phoneCursor ->
                            if (phoneCursor.moveToFirst()) {
                                val numIndex = phoneCursor.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER
                                )
                                if (numIndex >= 0) phone = phoneCursor.getString(numIndex) ?: ""
                            }
                        }
                    }
                    if (phone.isNotBlank()) {
                        // The Contacts app can hand back a number in almost
                        // any punctuation, and without a country code at
                        // all if it's a local contact -- normalize to the
                        // same E.164 shape every other phone entry point
                        // now produces, assuming this device's own country
                        // when the contact itself doesn't specify one.
                        val normalizedPhone = toE164OrRaw(phone, defaultCountryForDevice(context)) ?: phone
                        createInvitationAndShare(
                            name = name,
                            phone = normalizedPhone,
                            email = null,
                            successMessage = "Added $name to your circle"
                        )
                    } else {
                        Toast.makeText(context, "$name has no phone number to invite", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun inviteTieyohUser(user: UserSearchResult) {
        coroutineScope.launch {
            try {
                val result = RetrofitClient.friendApi.addFriend(
                    ownerTieyohId = ownerTieyohId,
                    request = AddFriendRequest(
                        contactUserId = user.id,
                        displayName = user.displayName,
                        phone = null,
                        email = null
                    )
                )
                mergeFriend(result)
                Toast.makeText(context, "Invite sent to ${user.displayName}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't invite ${user.displayName} right now", Toast.LENGTH_SHORT).show()
            }
        }
        query = ""
    }

    fun inviteByPhoneOrEmail(name: String, phone: String?, email: String?) {
        createInvitationAndShare(
            name = name,
            phone = phone,
            email = email,
            successMessage = "Added $name to your circle"
        )
    }

    fun onFriendTapped(friend: FriendResult) {
        if (friend.status == "ACTIVE") {
            onSelectRecipient(friend)
        } else if (friend.status == "PENDING_APPROVAL") {
            Toast.makeText(
                context,
                "${friend.displayName} joined Tieyoh but hasn't approved adding you to their circle yet.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "${friend.displayName} hasn't joined Tieyoh yet -- you can gift them once they sign up.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (showInviteToTieyoh) {
        InviteToTieyohScreen(
            prefillName = if (trimmedQuery.contains("@") || trimmedQuery.any { it.isDigit() }) "" else trimmedQuery,
            onCancel = { showInviteToTieyoh = false },
            onSend = { name, phone, email ->
                inviteByPhoneOrEmail(name = name, phone = phone, email = email)
                showInviteToTieyoh = false
                query = ""
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDone, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TieyohNavy
                )
            }
            Text(
                text = "Feed",
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(text = subtitle, color = TieyohMuted)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Add people to my circle") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        val noMatches = trimmedQuery.isNotEmpty() && circleMatches.isEmpty() &&
            searchCompletedForQuery && visibleSearchResults.isEmpty()

        if (trimmedQuery.isEmpty()) {
            // Nothing typed -- show the whole circle.
            Spacer(modifier = Modifier.height(16.dp))

            TieyohSecondaryButton(
                text = "Add from contacts",
                onClick = { pickContactLauncher.launch(null) }
            )

            if (onInviteFriendsClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TieyohSecondaryButton(
                    text = "Invite friends",
                    onClick = onInviteFriendsClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = TieyohMuted.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(16.dp))

            if (!friendsLoaded) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TieyohAccent)
                }
                Spacer(modifier = Modifier.weight(1f))
            } else if (friends.isEmpty()) {
                Text(
                    text = "Your circle is empty. Add someone above to get started.",
                    color = TieyohMuted
                )
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Text(text = "Your circle", fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(friends) { friend ->
                        FriendRow(friend = friend, onClick = { onFriendTapped(friend) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        } else if (circleMatches.isNotEmpty() || isSearching || visibleSearchResults.isNotEmpty()) {
            // Circle matches and live Tieyoh matches now render together in
            // one scrollable list instead of one hiding the other -- an
            // already-added circle member sharing a name (e.g. one of two
            // "Smith"s) must never suppress the search that finds the
            // other one.
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                if (circleMatches.isNotEmpty()) {
                    // Already in the circle -- active ones jump straight
                    // into the gift flow, pending ones explain they're not
                    // joined yet.
                    item {
                        Text(text = "In your circle", fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(circleMatches) { friend ->
                        FriendRow(friend = friend, onClick = { onFriendTapped(friend) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (isSearching) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TieyohAccent)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else if (visibleSearchResults.isNotEmpty()) {
                    // Live matches found on Tieyoh but not yet in the
                    // circle -- tap "Invite" to add them.
                    item {
                        Text(text = "On Tieyoh", fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(visibleSearchResults) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TieyohCardBg)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SearchResultAvatar(
                                profilePictureBase64 = user.profilePictureBase64,
                                displayName = user.displayName
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = user.displayName, fontWeight = FontWeight.Bold, color = TieyohNavy)
                                Text(
                                    text = user.tieyohId ?: (user.maskedPhone ?: user.maskedEmail ?: ""),
                                    color = TieyohMuted,
                                    fontSize = 12.sp
                                )
                                user.maskedEmail?.let { maskedEmail ->
                                    if (user.tieyohId != null) {
                                        Text(text = maskedEmail, color = TieyohMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                            // Inline in a search-result row -- same colors as
                            // every other button, compact width to fit here.
                            TieyohPrimaryButton(
                                text = "Invite",
                                onClick = { pendingConfirmUser = user },
                                fillWidth = false
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        } else if (noMatches) {
            // No one in the circle or on Tieyoh matches -- offer to invite
            // them directly instead of the old pair of inline phone/email
            // forms. Opens the dedicated InviteToTieyohScreen below,
            // prefilled with whatever was typed here.
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No one on Tieyoh matches \"$trimmedQuery\" yet.",
                color = TieyohMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            TieyohPrimaryButton(
                text = "\"$trimmedQuery\" isn't on Tieyoh -- Invite them",
                onClick = { showInviteToTieyoh = true }
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        TieyohPrimaryButton(
            text = doneLabel,
            onClick = onDone
        )
    }

    pendingConfirmUser?.let { user ->
        AlertDialog(
            onDismissRequest = { pendingConfirmUser = null },
            title = { Text("Add ${user.displayName}?") },
            text = {
                Text(
                    "This looks like ${user.displayName}" +
                        (user.tieyohId?.let { " ($it)" } ?: "") +
                        (user.maskedEmail?.let { " -- $it" } ?: "") +
                        ". Add them to your circle?"
                )
            },
            confirmButton = {
                // Native AlertDialog button slot -- kept as a standard
                // compact TextButton (a full-size pill doesn't fit this
                // system dialog layout), just recolored to match.
                TextButton(
                    onClick = {
                        inviteTieyohUser(user)
                        pendingConfirmUser = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = TieyohBrandBlue)
                ) {
                    Text("Yes, add them")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingConfirmUser = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = TieyohBrandBlue)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// One row in the circle list -- full color and clickable when ACTIVE (a
// real Tieyoh account, tap to start a gift), greyed out with a pending
// clock badge otherwise.
@Composable
private fun FriendRow(friend: FriendResult, onClick: () -> Unit) {
    val isActive = friend.status == "ACTIVE"
    val isPendingApproval = friend.status == "PENDING_APPROVAL"

    val subtitle = when {
        isActive -> friend.tieyohId ?: ""
        isPendingApproval -> "Pending -- waiting for their approval"
        else -> "Pending -- hasn't joined Tieyoh yet"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) TieyohCardBg else TieyohCardBg.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchResultAvatar(
            profilePictureBase64 = if (isActive) friend.profilePictureBase64 else null,
            displayName = friend.displayName,
            dimmed = !isActive
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.displayName,
                fontWeight = FontWeight.Bold,
                color = if (isActive) TieyohNavy else TieyohMuted
            )
            Text(
                text = subtitle,
                color = TieyohMuted,
                fontSize = 12.sp
            )
        }
        if (isActive) {
            Text(text = "›", color = TieyohAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        } else {
            Text(text = "⏳", fontSize = 16.sp)
        }
    }
}

// A small circular avatar -- shows the person's profile picture (sent by
// the server as base64) if they've set one, otherwise falls back to their
// initial on a tinted circle, same visual language as the initials
// fallback on the current user's own Profile screen. dimmed mutes it for
// a pending (not-yet-joined) circle entry.
@Composable
internal fun SearchResultAvatar(profilePictureBase64: String?, displayName: String, dimmed: Boolean = false) {
    val bitmap = remember(profilePictureBase64) {
        profilePictureBase64?.let { decodeBase64ToBitmap(it) }
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(TieyohAccent.copy(alpha = if (dimmed) 0.08f else 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "$displayName's profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = displayName.firstOrNull()?.uppercase() ?: "?",
                color = if (dimmed) TieyohMuted else TieyohAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

internal fun decodeBase64ToBitmap(base64: String): Bitmap? {
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}

// Shown once during signup, right after the payment-method step. Offers to
// jump straight into ContactsInviteScreen, or skip it for now -- the same
// screen stays reachable later from the invite button in the top bar.
@Composable
fun SendGiftPromptScreen(
    currentUserId: Long,
    ownerTieyohId: String,
    onDone: () -> Unit,
    onBack: () -> Unit = onDone
) {
    var showContacts by remember { mutableStateOf(false) }
    var showInviteFriends by remember { mutableStateOf(false) }

    if (showInviteFriends) {
        InviteFriendsScreen(ownerTieyohId = ownerTieyohId, onDone = { showInviteFriends = false })
        return
    }

    if (showContacts) {
        ContactsInviteScreen(
            currentUserId = currentUserId,
            ownerTieyohId = ownerTieyohId,
            onDone = onDone,
            onInviteFriendsClick = { showInviteFriends = true }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart),
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
                text = "Payment Methods",
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ready to send a gift to someone?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bring in a few contacts now, or do it later from the invite button.",
            color = TieyohMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        TieyohPrimaryButton(
            text = "Yes, let's add someone",
            onClick = { showContacts = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TieyohSecondaryButton(
            text = "Not now",
            onClick = onDone
        )
        }
    }
}
