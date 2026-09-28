package com.tieyoh.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy

// Opened from ContactsInviteScreen's "no one on Tieyoh matches" state --
// one simple, focused screen for inviting someone who isn't on Tieyoh yet:
// a single name field (first + last together, since that's how most people
// type it), a Phone/Email toggle, and one field underneath that swaps with
// it. onSend hands back exactly what createInvitationAndShare already
// expects (name, phone-or-null, email-or-null); this screen doesn't know
// or care how the invite is actually delivered.
@Composable
fun InviteToTieyohScreen(
    prefillName: String,
    onCancel: () -> Unit,
    onSend: (name: String, phone: String?, email: String?) -> Unit
) {
    var name by remember { mutableStateOf(prefillName) }
    var usePhone by remember { mutableStateOf(true) }
    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun handleSend() {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            errorMessage = "Enter their name."
            return
        }
        if (usePhone) {
            val digitsAfterDialCode = phoneInput.dropWhile { it == '+' }.filter { it.isDigit() }
            if (digitsAfterDialCode.length < 6) {
                errorMessage = "Enter a valid phone number."
                return
            }
            errorMessage = null
            onSend(trimmedName, phoneInput, null)
        } else {
            val trimmedEmail = emailInput.trim()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                errorMessage = "That doesn't look like a valid email address."
                return
            }
            errorMessage = null
            onSend(trimmedName, null, trimmedEmail)
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
            IconButton(onClick = onCancel, modifier = Modifier.size(40.dp)) {
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

        val firstName = name.trim().substringBefore(" ").ifBlank { null }
        Text(
            text = if (firstName != null) "Invite $firstName to Tieyoh" else "Invite to Tieyoh",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "We'll send a one-time link to join Tieyoh and connect with you.",
            color = TieyohMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Name", fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                errorMessage = null
            },
            placeholder = { Text("First and last name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TieyohCardBg)
                .padding(4.dp)
        ) {
            InviteToggleOption(
                text = "Phone",
                selected = usePhone,
                onClick = {
                    usePhone = true
                    errorMessage = null
                },
                modifier = Modifier.weight(1f)
            )
            InviteToggleOption(
                text = "Email",
                selected = !usePhone,
                onClick = {
                    usePhone = false
                    errorMessage = null
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (usePhone) {
            PhoneNumberField(
                value = phoneInput,
                onValueChange = {
                    phoneInput = it
                    errorMessage = null
                },
                label = "Phone number"
            )
        } else {
            Text(text = "Email address", fontSize = 12.sp, color = TieyohMuted)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = emailInput,
                onValueChange = {
                    emailInput = it
                    errorMessage = null
                },
                placeholder = { Text("Email address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        TieyohPrimaryButton(text = "Send Invite", onClick = { handleSend() })

        Spacer(modifier = Modifier.height(8.dp))

        TieyohSecondaryButton(text = "Cancel", onClick = onCancel)
    }
}

@Composable
private fun InviteToggleOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) TieyohNavy else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TieyohMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
