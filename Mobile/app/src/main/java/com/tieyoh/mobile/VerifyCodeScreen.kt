package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohBrandBg
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohMuted
import kotlinx.coroutines.delay

// Step 3 of the account-creation flow (Welcome -> Let's get started -> here
// -> profile basics). Code entry is a plain text field using the system
// keyboard now -- the reference's six boxed digits + custom on-screen
// keypad were replaced per feedback in favor of standard keyboard entry
// plus an explicit Verify button, matching Continue's style from
// GetStartedScreen.
//
// NOTE: the backend only ever sends verification codes by email today
// (see AuthController.otp/request + OtpService/LoggingOtpMailSender --
// there's no SMS provider wired up anywhere in tieyoh-api). This screen
// is built to the reference's phone-based copy ("We sent a 6-digit code
// to (314) 555-1234"), but nothing yet actually sends a text message to
// that number -- onVerify/onResend are hooks for the caller, currently
// wired to a placeholder upstream. Getting a real code here requires
// either switching this step back to email, or adding SMS delivery
// server-side.
@Composable
fun VerifyCodeScreen(
    phone: String,
    onBack: () -> Unit,
    onVerify: (code: String) -> Unit,
    onResend: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableStateOf(30) }

    val codeValid = code.length == 6

    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

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
            text = "Verify it's you",
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = TieyohBrandDarkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "We sent a 6-digit code to",
            fontSize = 15.sp,
            color = TieyohBrandNavyLight2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = formatPhoneForDisplay(phone),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TieyohBrandDarkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        Text("Verification Code", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TieyohBrandNavy, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { new -> code = new.filter { it.isDigit() }.take(6) },
            placeholder = { Text("6-digit code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TieyohBrandBlue,
                focusedLabelColor = TieyohBrandBlue,
                cursorColor = TieyohBrandBlue
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        if (secondsLeft > 0) {
            val mm = secondsLeft / 60
            val ss = secondsLeft % 60
            Text(
                text = "Resend code in " + "%02d:%02d".format(mm, ss),
                fontSize = 14.sp,
                color = TieyohMuted
            )
        } else {
            Text(
                text = "Resend code",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TieyohBrandBlue,
                modifier = Modifier.clickable {
                    onResend()
                    secondsLeft = 30
                }
            )
        }

        Spacer(Modifier.height(28.dp))

        // Same gradient pill, size, and colors as GetStartedScreen's
        // Continue button.
        val pillShape = RoundedCornerShape(26.dp)
        val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(pillShape)
                .background(if (codeValid) brandGradient else Brush.horizontalGradient(listOf(TieyohMuted.copy(alpha = 0.35f), TieyohMuted.copy(alpha = 0.35f))))
                .clickable(enabled = codeValid) { onVerify(code) },
            contentAlignment = Alignment.Center
        ) {
            Text("Verify", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(TieyohBrandBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = TieyohBrandBlue,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Didn't receive the code?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TieyohBrandNavy)
                Text("Check your messages or try resending.", fontSize = 12.sp, color = TieyohMuted)
            }
        }

        // Same step indicator as WelcomeScreen/GetStartedScreen -- this is
        // step 3 of 4.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == 2) 20.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index == 2) TieyohBrandBlue else TieyohBrandBg)
                )
            }
        }
    }
}

// Best-effort US display formatting ("+13143451234" / "3143451234" ->
// "(314) 345-1234"). Falls back to the raw string for anything that
// doesn't look like a plain 10/11-digit US number rather than guessing.
private fun formatPhoneForDisplay(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    val national = if (digits.length == 11 && digits.startsWith("1")) digits.substring(1) else digits
    if (national.length != 10) return phone
    return "(${national.substring(0, 3)}) ${national.substring(3, 6)}-${national.substring(6)}"
}
