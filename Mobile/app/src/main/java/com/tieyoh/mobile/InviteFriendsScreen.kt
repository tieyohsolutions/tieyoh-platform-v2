package com.tieyoh.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import kotlinx.coroutines.launch

// One entry from the phone's own address book, merged across its phone and
// email rows so each contact appears once even if it has both.
internal data class PickableContact(
    val contactId: String,
    val displayName: String,
    val phone: String?,
    val email: String?
)

// The bulk "Invite Friends" flow, reachable from the add-people screen
// (and, from there, from both the top-bar + button and the post-signup
// prompt). Unlike the single-contact system picker already on that screen,
// this one lets the user multi-select from their address book and get back
// one individually-redeemable invitation link per person -- never a single
// link shared across everyone. Nothing is sent from here: every link goes
// out through the device's own native share sheet, at the user's own
// initiative, the same way an AirDrop or a manually-composed text would.
@Composable
fun InviteFriendsScreen(
    ownerTieyohId: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
    }

    var contacts by remember { mutableStateOf<List<PickableContact>>(emptyList()) }
    var contactsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            contacts = loadDeviceContacts(context)
            contactsLoaded = true
        }
    }

    var query by remember { mutableStateOf("") }
    val filteredContacts = remember(contacts, query) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) contacts
        else contacts.filter { it.displayName.contains(trimmed, ignoreCase = true) }
    }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }

    var results by remember { mutableStateOf<List<InvitationResult>>(emptyList()) }
    var showResults by remember { mutableStateOf(false) }

    // Sequential "Share Queue": fires one native share sheet per invitation
    // link and moves to the next regardless of what the user did with the
    // last one -- share-sheet result codes aren't reliable, and no single
    // app can be assumed to support true personalized bulk sending.
    var shareQueue by remember { mutableStateOf<List<InvitationResult>>(emptyList()) }
    var shareQueueIndex by remember { mutableStateOf(0) }

    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        shareQueueIndex += 1
    }

    LaunchedEffect(shareQueueIndex, shareQueue) {
        if (shareQueue.isNotEmpty() && shareQueueIndex < shareQueue.size) {
            val next = shareQueue[shareQueueIndex]
            val url = next.inviteUrl
            if (url == null) {
                shareQueueIndex += 1
            } else {
                shareLauncher.launch(Intent.createChooser(shareIntentFor(url), "Share invitation"))
            }
        }
    }

    fun shareOne(result: InvitationResult) {
        val url = result.inviteUrl ?: return
        shareLauncher.launch(Intent.createChooser(shareIntentFor(url), "Share invitation"))
    }

    fun createInvitations() {
        val chosen = contacts.filter { selectedIds.contains(it.contactId) }
        if (chosen.isEmpty()) return
        isSubmitting = true
        // Contacts read straight from the address book are in whatever
        // punctuation (and, usually, no country code) the device happens
        // to store them in -- normalize each one to the same E.164 shape
        // every other phone entry point now produces before sending.
        val deviceCountry = defaultCountryForDevice(context)
        coroutineScope.launch {
            try {
                val response = RetrofitClient.invitationApi.createBatch(
                    InvitationBatchRequest(
                        inviterTieyohId = ownerTieyohId,
                        recipients = chosen.map {
                            InvitationRecipientRequest(
                                displayName = it.displayName,
                                email = it.email,
                                phone = it.phone?.let { rawPhone -> toE164OrRaw(rawPhone, deviceCountry) }
                            )
                        }
                    )
                )
                results = response.invitations
                showResults = true
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't create invitations right now", Toast.LENGTH_SHORT).show()
            } finally {
                isSubmitting = false
            }
        }
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
                text = "Add to Circle",
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Invite friends",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (showResults) {
            Text(
                text = "Each link works for one person -- share them however you like.",
                color = TieyohMuted
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TieyohCardBg)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = result.displayName, fontWeight = FontWeight.Bold, color = TieyohNavy)
                            Text(
                                text = if (result.inviteUrl != null) "Ready to share" else "Already on Tieyoh -- added to your circle",
                                color = TieyohMuted,
                                fontSize = 12.sp
                            )
                        }
                        if (result.inviteUrl != null) {
                            TieyohPrimaryButton(
                                text = "Share",
                                onClick = { shareOne(result) },
                                fillWidth = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            val shareable = results.filter { it.inviteUrl != null }
            if (shareable.isNotEmpty()) {
                TieyohPrimaryButton(
                    text = "Share All",
                    onClick = {
                        shareQueueIndex = 0
                        shareQueue = shareable
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            TieyohSecondaryButton(text = "Done", onClick = onDone)
        } else if (!permissionGranted) {
            Text(
                text = "Tieyoh needs access to your contacts so you can pick specific people to invite. " +
                    "Nothing is uploaded automatically -- only the contacts you choose to invite are sent.",
                color = TieyohMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            TieyohPrimaryButton(
                text = "Allow contacts access",
                onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
            )
            if (permissionDenied) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Without access, you can still invite someone one at a time by phone or email from the add-people screen.",
                    color = TieyohMuted,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TieyohSecondaryButton(text = "Cancel", onClick = onDone)
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search contacts") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "${selectedIds.size} selected", color = TieyohMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (!contactsLoaded) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TieyohAccent)
                }
                Spacer(modifier = Modifier.weight(1f))
            } else if (filteredContacts.isEmpty()) {
                Text(text = "No contacts found.", color = TieyohMuted)
                Spacer(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredContacts, key = { it.contactId }) { contact ->
                        val checked = selectedIds.contains(contact.contactId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TieyohCardBg)
                                .clickable {
                                    selectedIds = if (checked) selectedIds - contact.contactId else selectedIds + contact.contactId
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedIds = if (isChecked) selectedIds + contact.contactId else selectedIds - contact.contactId
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = contact.displayName, fontWeight = FontWeight.Bold, color = TieyohNavy)
                                Text(
                                    text = contact.phone ?: contact.email ?: "",
                                    color = TieyohMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TieyohPrimaryButton(
                text = "Create Invitations (${selectedIds.size})",
                onClick = { createInvitations() },
                enabled = selectedIds.isNotEmpty() && !isSubmitting,
                isLoading = isSubmitting
            )
            Spacer(modifier = Modifier.height(8.dp))
            TieyohSecondaryButton(text = "Cancel", onClick = onDone)
        }
    }
}

// Same intent shape used for every recipient: EXTRA_TEXT/EXTRA_SUBJECT so
// WhatsApp, Messages, AirDrop etc. all get something sensible, plus
// EXTRA_EMAIL so an email app the user picks from the chooser comes up
// pre-addressed. Choosing the phone's own Messages app here is fine --
// it's the user's own app sending it, not Tieyoh calling an SMS gateway.
// Not private -- ContactsInviteScreen's single-contact and manual phone/
// email invite paths share this same intent shape rather than duplicating
// it, now that they also create real Invitation links instead of bare
// PENDING circle entries.
fun shareIntentFor(inviteUrl: String): Intent {
    val body = "Join me on Tieyoh! $inviteUrl"
    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "You're invited to Tieyoh")
        putExtra(Intent.EXTRA_TEXT, body)
        putExtra(Intent.EXTRA_EMAIL, arrayOf<String>())
    }
}

// Bulk address-book read, used only after the user has explicitly granted
// READ_CONTACTS from this screen -- never queried in the background and
// never synced continuously. Phone and email rows are merged by contact id
// so someone with both shows up once; contacts with neither are dropped
// since there'd be nothing to invite them with.
internal fun loadDeviceContacts(context: android.content.Context): List<PickableContact> {
    val byId = LinkedHashMap<String, PickableContact>()
    val resolver = context.contentResolver

    resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (cursor.moveToNext()) {
            val id = if (idIndex >= 0) cursor.getString(idIndex) else null
            val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
            val number = if (numberIndex >= 0) cursor.getString(numberIndex)?.trim()?.takeIf { it.isNotBlank() } else null
            if (id == null || name == null) continue
            val existing = byId[id]
            byId[id] = if (existing == null) {
                PickableContact(contactId = id, displayName = name, phone = number, email = null)
            } else if (existing.phone == null && number != null) {
                existing.copy(phone = number)
            } else {
                existing
            }
        }
    }

    resolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Email.ADDRESS
        ),
        null,
        null,
        null
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
        val addressIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        while (cursor.moveToNext()) {
            val id = if (idIndex >= 0) cursor.getString(idIndex) else null
            val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
            val address = if (addressIndex >= 0) cursor.getString(addressIndex)?.trim()?.takeIf { it.isNotBlank() } else null
            if (id == null) continue
            val existing = byId[id]
            byId[id] = if (existing == null) {
                PickableContact(contactId = id, displayName = name ?: "Contact", phone = null, email = address)
            } else if (existing.email == null && address != null) {
                existing.copy(email = address)
            } else {
                existing
            }
        }
    }

    return byId.values
        .filter { it.phone != null || it.email != null }
        .sortedBy { it.displayName.lowercase() }
}
