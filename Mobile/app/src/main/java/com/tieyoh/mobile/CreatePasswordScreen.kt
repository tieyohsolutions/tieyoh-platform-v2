package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

// Step 4 of the account-creation flow (Welcome -> Let's get started ->
// Verify it's you -> here). This is the screen whose Continue button
// actually creates the account -- AuthService.signup() on the backend
// requires a recently-verified OTP for the email, which the previous
// screen just satisfied, and won't create an AppUser row before this
// point. Matches the "Create a password" reference image: fonts, colors,
// and sizes follow the same tokens as the rest of the Sep 2026 redesign
// (GetStartedScreen / VerifyCodeScreen / LoginScreen) rather than
// introducing new ones.
@Composable
fun CreatePasswordScreen(
    initialPassword: String,
    initialConfirmPassword: String,
    onFieldsChanged: (password: String, confirmPassword: String) -> Unit,
    onBack: () -> Unit,
    onContinue: (password: String) -> Unit
) {
    var password by remember { mutableStateOf(initialPassword) }
    var confirmPassword by remember { mutableStateOf(initialConfirmPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    // Reports the latest field values up to the caller so they survive this
    // screen leaving and re-entering composition instead of resetting blank
    // every time (e.g. going back to Verify Code and forward again).
    LaunchedEffect(password, confirmPassword) {
        onFieldsChanged(password, confirmPassword)
    }

    val hasMinLength = password.length >= 8
    val hasNumber = password.any { it.isDigit() }
    val hasUppercase = password.any { it.isUpperCase() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword
    val canContinue = hasMinLength && hasNumber && hasUppercase && hasSpecialChar && passwordsMatch

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TieyohBrandBlue,
        focusedLabelColor = TieyohBrandBlue,
        cursorColor = TieyohBrandBlue
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // Decorative wave, bottom of screen, matching the reference image.
        // Two overlapping "hill" shapes rather than an image asset -- no
        // wave art exists in the project yet.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1.4f)
                .height(120.dp)
                .offset(y = 55.dp)
                .clip(RoundedCornerShape(topStart = 600.dp, topEnd = 600.dp))
                .background(TieyohBrandTeal.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1.1f)
                .height(90.dp)
                .offset(y = 40.dp)
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
                text = "Create a password",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Use a strong password to keep your account secure.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            Text("Password", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TieyohBrandNavy, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = TieyohBrandNavyLight2
                        )
                    }
                },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Text("Confirm password", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TieyohBrandNavy, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("Confirm password") },
                singleLine = true,
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            imageVector = if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (confirmVisible) "Hide password" else "Show password",
                            tint = TieyohBrandNavyLight2
                        )
                    }
                },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            PasswordRuleRow("At least 8 characters", hasMinLength)
            Spacer(Modifier.height(10.dp))
            PasswordRuleRow("One number", hasNumber)
            Spacer(Modifier.height(10.dp))
            PasswordRuleRow("One uppercase letter", hasUppercase)
            Spacer(Modifier.height(10.dp))
            PasswordRuleRow("One special character (e.g. !, @, #, \$)", hasSpecialChar)

            Spacer(Modifier.height(28.dp))

            val pillShape = RoundedCornerShape(26.dp)
            val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(if (canContinue) brandGradient else Brush.horizontalGradient(listOf(TieyohMuted.copy(alpha = 0.35f), TieyohMuted.copy(alpha = 0.35f))))
                    .clickable(enabled = canContinue) { onContinue(password) },
                contentAlignment = Alignment.Center
            ) {
                Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.weight(1f))

            // Same step indicator as the rest of the flow -- step 4 of 4.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == 3) 20.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (index == 3) TieyohBrandBlue else TieyohBrandBg)
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordRuleRow(label: String, met: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (met) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (met) TieyohBrandTeal else TieyohMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp, color = TieyohBrandNavy)
    }
}
