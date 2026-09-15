package com.tieyoh.mobile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import kotlinx.coroutines.launch

// The bell icon's destination. Lists everything the backend has recorded
// for this user (GET .../notifications) -- today that's connection
// requests sent/received and connection-acceptance confirmations; more
// types (e.g. a gift received, once the gift-sending flow itself exists)
// show up here automatically since rendering is driven by "message" plus
// a couple of type-specific affordances, not a fixed enum of screens.
// Each row is only marked read when the user actually taps it (or acts on
// it, for a connection request) -- not the instant this screen opens --
// so bold-vs-not genuinely means unread-vs-read. onUnreadChanged fires
// live as that changes so the bell's red dot can track it in real time,
// not just when the user navigates away.
@Composable
fun NotificationsScreen(
    currentUserId: Long,
    onUnreadChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
    onOpenGift: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<NotificationResult>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    // A connection-request notification doesn't carry a live "did I already
    // respond" flag from the server, so track what's been handled locally
    // during this screen visit and update the row in place.
    var handledRequestIds by remember { mutableStateOf(setOf<Long>()) }
    var acceptedRequestIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(currentUserId) {
        notifications = try {
            RetrofitClient.notificationApi.getNotifications(currentUserId)
        } catch (e: Exception) {
            emptyList()
        }
        loaded = true
    }

    // Recomputed every time the list changes -- including right after the
    // initial load and after each individual mark-read below -- so the
    // bell's dot reflects reality while this screen is open, not just once
    // on the way out. Guarded on `loaded` so the empty initial list isn't
    // mistaken for "nothing unread" before the real data arrives.
    LaunchedEffect(notifications, loaded) {
        if (loaded) {
            onUnreadChanged(notifications.any { !it.read })
        }
    }

    fun markRead(notification: NotificationResult) {
        if (notification.read) return
        // Optimistic -- un-bold immediately; if the network call fails,
        // worst case it shows as unread again next time the list reloads,
        // which isn't worth blocking the tap on.
        notifications = notifications.map {
            if (it.id == notification.id) it.copy(read = true) else it
        }
        coroutineScope.launch {
            try {
                RetrofitClient.notificationApi.markRead(currentUserId, notification.id)
            } catch (e: Exception) {
                // Best-effort, as above.
            }
        }
    }

    // A GIFT_RECEIVED notification opens the recipient's full-screen
    // confirmation directly, rather than just marking itself read like
    // every other notification kind does today.
    fun openIfGift(notification: NotificationResult) {
        markRead(notification)
        if (notification.type == "GIFT_RECEIVED") {
            notification.giftId?.let(onOpenGift)
        }
    }

    fun respond(notification: NotificationResult, accept: Boolean) {
        val requestId = notification.connectionRequestId ?: return
        markRead(notification)
        coroutineScope.launch {
            try {
                RetrofitClient.connectionRequestApi.respond(
                    requestId,
                    ConnectionRequestRespondBody(accept = accept)
                )
                handledRequestIds = handledRequestIds + requestId
                if (accept) {
                    acceptedRequestIds = acceptedRequestIds + requestId
                }
                Toast.makeText(
                    context,
                    if (accept) "You're now connected with ${notification.actorDisplayName}" else "Request declined",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't do that right now", Toast.LENGTH_SHORT).show()
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
            IconButton(onClick = onDone) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TieyohNavy
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!loaded) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TieyohAccent)
            }
        } else if (notifications.isEmpty()) {
            Text(text = "You're all caught up -- no notifications yet.", color = TieyohMuted)
            Spacer(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { n ->
                    NotificationRow(
                        notification = n,
                        isHandled = n.connectionRequestId != null && handledRequestIds.contains(n.connectionRequestId),
                        wasAccepted = n.connectionRequestId != null && acceptedRequestIds.contains(n.connectionRequestId),
                        onRowTap = { openIfGift(n) },
                        onAccept = { respond(n, true) },
                        onDecline = { respond(n, false) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TieyohPrimaryButton(text = "Done", onClick = onDone)
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationResult,
    isHandled: Boolean,
    wasAccepted: Boolean,
    onRowTap: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (notification.read) TieyohCardBg.copy(alpha = 0.6f) else TieyohCardBg)
            .clickable(onClick = onRowTap)
            .padding(12.dp)
    ) {
        Text(
            text = notification.message,
            color = TieyohNavy,
            // Bold marks it unread -- tapping the row (or acting on it,
            // for a connection request) clears both the weight here and
            // eventually the bell's dot, per notification rather than
            // all-at-once.
            fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold
        )

        val canRespond = notification.type == "CONNECTION_REQUEST_RECEIVED" && notification.connectionRequestId != null

        if (canRespond) {
            Spacer(modifier = Modifier.height(8.dp))
            if (isHandled) {
                Text(
                    text = if (wasAccepted) "Accepted" else "Declined",
                    color = TieyohMuted,
                    fontSize = 12.sp
                )
            } else {
                Row {
                    TieyohPrimaryButton(text = "Accept", onClick = onAccept, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    TieyohSecondaryButton(text = "Decline", onClick = onDecline, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
