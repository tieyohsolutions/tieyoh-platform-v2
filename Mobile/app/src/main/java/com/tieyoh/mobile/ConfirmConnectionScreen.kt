package com.tieyoh.mobile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import kotlinx.coroutines.launch

// What tapping a non-circle person's name in the Feed hands off to this
// screen: just enough to show who they are and, if a request already
// exists between the viewer and them, which direction it's pending in.
// Mirrors FeedPersonResult's connectionRequestStatus contract (null,
// "PENDING_SENT", or "PENDING_RECEIVED") rather than inventing a new one.
data class PendingConnectionTarget(
    val userId: Long,
    val displayName: String,
    val tieyohId: String?,
    val profilePictureBase64: String?,
    val connectionRequestStatus: String?
)

// The "clean and some confirmations" page requested for connecting to
// someone seen in the Feed but not yet in the viewer's circle -- a tap on
// their name no longer fires a request immediately, it lands here first.
// Self-contained: sends the request itself (rather than delegating back
// to FeedScreen) since this is a standalone destination reachable from
// more than one tap point (gift activity sender/recipient, birthday card).
@Composable
fun ConfirmConnectionScreen(
    currentUserId: Long,
    target: PendingConnectionTarget,
    onCancel: () -> Unit,
    onRequestSent: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSending by remember(target) { mutableStateOf(false) }
    var errorMessage by remember(target) { mutableStateOf<String?>(null) }

    val bitmap = remember(target.profilePictureBase64) {
        target.profilePictureBase64?.let { decodeBase64ToBitmap(it) }
    }
    val firstName = target.displayName.trim().substringBefore(" ").ifBlank { target.displayName }

    fun sendRequest() {
        if (isSending) return
        isSending = true
        errorMessage = null
        coroutineScope.launch {
            try {
                RetrofitClient.connectionRequestApi.sendRequest(
                    ConnectionRequestBody(senderId = currentUserId, recipientId = target.userId)
                )
                Toast.makeText(
                    context,
                    "Connection request sent to ${target.displayName}",
                    Toast.LENGTH_SHORT
                ).show()
                onRequestSent()
            } catch (e: Exception) {
                errorMessage = "Couldn't send that request right now."
                isSending = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.size(40.dp)) {
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

        // Fixed, not weight(1f) -- this Column scrolls now (see the
        // header note above), and weight() needs a bounded parent.
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(88.dp)
                .clip(CircleShape)
                .background(TieyohCardBg),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = target.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = target.displayName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = TieyohNavy
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = target.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        target.tieyohId?.let { id ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = id,
                color = TieyohMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val (message, showSend) = when (target.connectionRequestStatus) {
            "PENDING_SENT" -> "You already sent $firstName a connection request. We'll let you know when they respond." to false
            "PENDING_RECEIVED" -> "$firstName already sent you a connection request -- check your notifications to accept it." to false
            else -> "$firstName isn't in your circle yet. Send a connection request so you can gift each other directly." to true
        }

        Text(
            text = message,
            color = TieyohMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (showSend) {
            // Standardized button styling (app-wide pill shape/size/colors)
            // -- wording unchanged.
            TieyohPrimaryButton(
                text = if (isSending) "Sending..." else "Send Connection Request",
                onClick = { sendRequest() },
                enabled = !isSending
            )
            Spacer(modifier = Modifier.height(8.dp))
            TieyohSecondaryButton(
                text = "Not Now",
                onClick = onCancel,
                enabled = !isSending
            )
        } else {
            TieyohPrimaryButton(
                text = "Done",
                onClick = onCancel
            )
        }
    }
}
