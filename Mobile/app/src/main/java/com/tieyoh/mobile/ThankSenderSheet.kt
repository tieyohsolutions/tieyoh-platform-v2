package com.tieyoh.mobile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBorder
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohTrack
import kotlinx.coroutines.launch

// What GiftReceivedScreen's "Thank <name>" button hands off to the Feed:
// enough to post a comment on that gift and notify the right person, and
// a display name for the sheet's own copy ("Thank Marcus for the gift").
data class PendingThank(
    val giftId: Long,
    val senderId: Long,
    val senderDisplayName: String
)

// The overlay the reference mockup calls "where Thank leads" -- rendered
// by FeedScreen on top of the live feed whenever there's a PendingThank,
// so tapping "Thank Sender" genuinely drops the recipient back onto the
// Feed with a lightweight reply surface, rather than a separate screen.
// One tap on an emoji sends it immediately; typing a message needs the
// explicit Send button. Either way this is just a GiftComment -- there's
// no separate "reaction" concept on the backend.
@Composable
fun ThankSenderSheet(
    currentUserId: Long,
    pendingThank: PendingThank,
    onDismiss: () -> Unit,
    onSent: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var messageText by remember(pendingThank) { mutableStateOf("") }
    var isSending by remember(pendingThank) { mutableStateOf(false) }

    fun sendReply(body: String) {
        val trimmed = body.trim()
        if (trimmed.isEmpty() || isSending) return
        isSending = true
        coroutineScope.launch {
            try {
                RetrofitClient.giftApi.addComment(
                    pendingThank.giftId,
                    GiftCommentRequest(authorUserId = currentUserId, body = trimmed)
                )
                Toast.makeText(
                    context,
                    "Sent to ${pendingThank.senderDisplayName}",
                    Toast.LENGTH_SHORT
                ).show()
                // Lets the Feed refetch so the new reply shows up under
                // the gift card right away, without waiting on the
                // recipient to pull-to-refresh themselves.
                onSent()
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't send that right now", Toast.LENGTH_SHORT).show()
                isSending = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color.White)
                .clickable(onClick = {})
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TieyohTrack)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Thank ${pendingThank.senderDisplayName.substringBefore(" ")} for the gift",
                fontWeight = FontWeight.Bold,
                color = TieyohNavy,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("🙏", "❤️", "🎉", "😍", "👏").forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TieyohCardBg)
                            .border(1.dp, TieyohBorder, CircleShape)
                            .clickable(enabled = !isSending) { sendReply(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Say thanks...") },
                    singleLine = true,
                    enabled = !isSending,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                TieyohPrimaryButton(
                    text = "Send",
                    onClick = { sendReply(messageText) },
                    enabled = messageText.isNotBlank() && !isSending,
                    fillWidth = false
                )
            }
        }
    }
}
