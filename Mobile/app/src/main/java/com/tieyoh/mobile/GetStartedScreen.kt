package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
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
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohMuted

// Step 2 of the account-creation flow (Welcome -> here -> verify code ->
// profile basics), per the redesign reference screen: first/last name,
// phone, and email are all collected on this one screen now. First/last
// name are stored as separate app_user columns (see AppUser.firstName /
// lastName, migration V25) rather than the old single displayName field --
// that field is untouched for backward compatibility, this screen just
// doesn't feed it (yet). There's still no password on this step -- that
// comes later. onContinue just hands the four values up; nothing past this
// screen is built yet, so the caller decides what "continue" currently does.
@Composable
fun GetStartedScreen(
    initialFirstName: String,
    initialLastName: String,
    initialPhone: String,
    initialEmail: String,
    onFieldsChanged: (firstName: String, lastName: String, phone: String, email: String) -> Unit,
    onBack: () -> Unit,
    onLogIn: () -> Unit,
    onContinue: (firstName: String, lastName: String, phone: String, email: String) -> Unit
) {
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var email by remember { mutableStateOf(initialEmail) }

    // Reports the latest field values up to the caller so they survive this
    // screen leaving and re-entering composition (Continue then Back, or
    // Back then forward again) instead of resetting blank every time.
    LaunchedEffect(firstName, lastName, phone, email) {
        onFieldsChanged(firstName, lastName, phone, email)
    }

    val firstNameValid = firstName.trim().isNotEmpty()
    val lastNameValid = lastName.trim().isNotEmpty()
    val phoneDigits = phone.dropWhile { it == '+' }.filter { it.isDigit() }
    val phoneValid = phoneDigits.length >= 11
    val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    val pillShape = RoundedCornerShape(26.dp)
    val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
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
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.70f)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Let's get started",
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = TieyohBrandDarkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Enter the below details to create an account.",
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = TieyohBrandNavyLight2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("First Name", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TieyohBrandNavy)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    placeholder = { Text("First name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TieyohBrandBlue,
                        focusedLabelColor = TieyohBrandBlue,
                        cursorColor = TieyohBrandBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Last Name", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TieyohBrandNavy)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    placeholder = { Text("Last name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TieyohBrandBlue,
                        focusedLabelColor = TieyohBrandBlue,
                        cursorColor = TieyohBrandBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("Phone Number", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TieyohBrandNavy)
        Spacer(Modifier.height(6.dp))
        Box {
            PhoneNumberField(
                value = phone,
                onValueChange = { phone = it },
                label = "",
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TieyohBrandBlue,
                    focusedLabelColor = TieyohBrandBlue,
                    cursorColor = TieyohBrandBlue
                )
            )
            if (phoneValid) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = TieyohBrandTeal,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("We'll use this to verify your account.", fontSize = 16.sp, color = TieyohBrandNavyLight2)

        Spacer(Modifier.height(20.dp))

        Text("Email Address", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TieyohBrandNavy)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Email, contentDescription = null, tint = TieyohBrandBlue)
            },
            trailingIcon = {
                if (emailValid) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = TieyohBrandTeal,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TieyohBrandBlue,
                focusedLabelColor = TieyohBrandBlue,
                cursorColor = TieyohBrandBlue
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text("We'll use this to send important updates.", fontSize = 16.sp, color = TieyohBrandNavyLight2)

        Spacer(Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(pillShape)
                .background(if (firstNameValid && lastNameValid && phoneValid && emailValid) brandGradient else Brush.horizontalGradient(listOf(TieyohMuted.copy(alpha = 0.35f), TieyohMuted.copy(alpha = 0.35f))))
                .clickable(enabled = firstNameValid && lastNameValid && phoneValid && emailValid) {
                    onContinue(firstName.trim(), lastName.trim(), phone, email.trim())
                },
            contentAlignment = Alignment.Center
        ) {
            Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = TieyohMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("We'll never share your information.", fontSize = 12.sp, color = TieyohMuted)
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Already have an account? ", fontSize = 14.sp, color = TieyohMuted)
            Text(
                "Log In",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TieyohBrandBlue,
                modifier = Modifier.clickable { onLogIn() }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Same step indicator as WelcomeScreen -- this is step 2 of 4.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == 1) 20.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index == 1) TieyohBrandBlue else TieyohBrandBg)
                )
            }
        }
    }
}
