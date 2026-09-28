package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.R
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohMuted
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val OTP_PURPOSE_SIGNUP = "SIGNUP"

@Composable
fun LoginScreen(
    initialEmail: String,
    initialPassword: String,
    initialDisplayName: String,
    initialRememberMe: Boolean,
    onFieldsChanged: (email: String, password: String, displayName: String, rememberMe: Boolean) -> Unit,
    onAuthenticated: (AuthResponse, Boolean, Boolean) -> Unit,
    onBack: () -> Unit,
    startInSignupMode: Boolean = false
) {
    // Which mode this opens in is now decided one screen earlier, by which
    // button the person tapped on WelcomeScreen (Create Account vs. Log
    // In) -- startInSignupMode just seeds this screen's own toggle so
    // "Already have an account? Log in" / "New to Tieyoh? Create an
    // account" still work exactly as before once they're in here.
    var isSignupMode by remember { mutableStateOf(startInSignupMode) }

    var displayName by remember { mutableStateOf(initialDisplayName) }
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf(initialPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(initialRememberMe) }

    // Reports email/password/displayName/rememberMe up to the caller so
    // they survive this screen leaving and re-entering composition (e.g.
    // Back to Welcome, then Log In again) instead of resetting blank every
    // time. The in-progress signup-code fields below are deliberately left
    // out of this -- same as VerifyCodeScreen, a one-time code shouldn't be
    // remembered/prefilled.
    LaunchedEffect(email, password, displayName, rememberMe) {
        onFieldsChanged(email, password, displayName, rememberMe)
    }

    // Signup only: once we've sent a code, the form below swaps for a code
    // entry step. The account isn't actually created until that code is
    // verified — see verifyAndCreateAccount().
    var awaitingOtp by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // If the user backgrounds or closes the app mid-signup -- e.g. they
    // requested a code but never entered it -- don't leave that attempt's
    // email/password sitting in memory for next time. The account was
    // never actually created (that only happens once the OTP is
    // verified), so reopening the app and starting over should show a
    // blank form, not whatever was typed into the abandoned attempt. Plain
    // remember{} would already reset this on a genuine fresh process, but
    // Android doesn't necessarily kill the process just because the app
    // was backgrounded -- ON_STOP covers that case too.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && isSignupMode) {
                isSignupMode = false
                displayName = ""
                email = ""
                password = ""
                otpCode = ""
                awaitingOtp = false
                errorMessage = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun verifyAndCreateAccount() {
        errorMessage = null

        if (otpCode.isBlank()) {
            errorMessage = "Enter the code we sent to your email."
            return
        }

        coroutineScope.launch {
            isLoading = true
            try {
                RetrofitClient.authApi.verifyOtp(
                    OtpVerifyRequest(
                        email = email.trim(),
                        purpose = OTP_PURPOSE_SIGNUP,
                        code = otpCode.trim()
                    )
                )

                val response = RetrofitClient.authApi.signup(
                    SignupRequest(
                        email = email.trim(),
                        password = password,
                        displayName = displayName.trim()
                    )
                )
                onAuthenticated(response, rememberMe, true)
            } catch (e: HttpException) {
                errorMessage = when (e.code()) {
                    400 -> "That code isn't right or has expired — try again or resend it."
                    429 -> "Too many attempts — request a new code."
                    409 -> "An account with this email already exists — try logging in instead."
                    else -> "Something went wrong (code ${e.code()}). Please try again."
                }
            } catch (e: Exception) {
                errorMessage = "Couldn't reach the server. Is tieyoh-api running?"
            } finally {
                isLoading = false
            }
        }
    }

    fun resendCode() {
        errorMessage = null
        coroutineScope.launch {
            isLoading = true
            try {
                RetrofitClient.authApi.requestOtp(
                    OtpRequestRequest(email = email.trim(), purpose = OTP_PURPOSE_SIGNUP)
                )
            } catch (e: Exception) {
                errorMessage = "Couldn't resend the code. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    fun submit() {
        errorMessage = null

        if (isSignupMode && awaitingOtp) {
            verifyAndCreateAccount()
            return
        }

        if (email.isBlank() || password.isBlank() || (isSignupMode && displayName.isBlank())) {
            errorMessage = "Please fill in all fields."
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            errorMessage = "That doesn't look like a valid email address."
            return
        }

        if (isSignupMode && password.length < 8) {
            errorMessage = "Password must be at least 8 characters."
            return
        }

        coroutineScope.launch {
            isLoading = true
            try {
                if (isSignupMode) {
                    // Don't create the account yet — first prove the email
                    // is real. AuthService.signup() on the backend won't
                    // succeed until this code is verified.
                    RetrofitClient.authApi.requestOtp(
                        OtpRequestRequest(email = email.trim(), purpose = OTP_PURPOSE_SIGNUP)
                    )
                    awaitingOtp = true
                } else {
                    val response = RetrofitClient.authApi.login(
                        LoginRequest(
                            email = email.trim(),
                            password = password
                        )
                    )
                    onAuthenticated(response, rememberMe, false)
                }
            } catch (e: HttpException) {
                errorMessage = when (e.code()) {
                    401 -> "Incorrect email or password."
                    409 -> "An account with this email already exists — try logging in instead."
                    403 -> "This account isn't active yet."
                    400 -> "Please double-check your details — make sure your email looks right and your password is at least 8 characters."
                    else -> "Something went wrong (code ${e.code()}). Please try again."
                }
            } catch (e: Exception) {
                errorMessage = "Couldn't reach the server. Is tieyoh-api running?"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Same edge-to-edge issue as the feed's top bar (see TieyohTopBar
        // in MainActivity.kt): this screen's Column has no top inset
        // padding of its own, so without this the back arrow sits right at
        // y=0 -- partly hidden behind the status bar/camera cutout, and
        // taps in that same top band never reach the IconButton's
        // clickable area at all. Styled to match GetStartedScreen's back
        // row. Its action depends on where we are: mid-signup with a code
        // already sent, it steps back to the email/password form; anywhere
        // else it leaves this screen entirely, back to Welcome.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (isSignupMode && awaitingOtp) {
                    awaitingOtp = false
                    otpCode = ""
                    errorMessage = null
                } else {
                    onBack()
                }
            }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TieyohNavy
                )
            }
            // Matches the IconButton's own conditional above: mid-OTP this
            // arrow steps back into the email/password form rather than
            // leaving the screen, so the label says "Login" (where you're
            // actually landing) instead of always claiming "Welcome".
            Text(
                text = if (isSignupMode && awaitingOtp) "Login" else "Welcome",
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        Image(
            painter = painterResource(id = R.drawable.tieyoh_logo),
            contentDescription = "Tieyoh",
            modifier = Modifier.fillMaxWidth(0.70f)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = when {
                isSignupMode && awaitingOtp -> "Verify your email"
                isSignupMode -> "Create your account"
                else -> "Welcome back"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = TieyohBrandDarkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        if (isSignupMode && awaitingOtp) {
            Text(
                "We sent a 6-digit code to ${email.trim()}. Enter it below to finish creating your account.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text("Verification Code", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TieyohBrandNavy)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                placeholder = { Text("6-digit code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TieyohBrandBlue,
                    focusedLabelColor = TieyohBrandBlue,
                    cursorColor = TieyohBrandBlue
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "Resend code",
                    color = TieyohBrandBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { resendCode() }
                )
            }
        } else {
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TieyohBrandBlue,
                focusedLabelColor = TieyohBrandBlue,
                cursorColor = TieyohBrandBlue
            )

            if (isSignupMode) {
                Text("Full Name", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TieyohBrandNavy)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    placeholder = { Text("Full name") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
            }

            Text(
                "Email Address",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TieyohBrandNavy,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Password",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TieyohBrandNavy,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(
                            text = if (passwordVisible) "Hide" else "Show",
                            color = TieyohBrandBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { rememberMe = !rememberMe }
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = TieyohBrandBlue)
                )
                Text("Keep me logged in", fontSize = 14.sp, color = TieyohMuted)
            }
        }

        errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))

        // Matches GetStartedScreen's Continue button -- brand gradient pill
        // instead of the old flat teal Material button.
        val pillShape = RoundedCornerShape(26.dp)
        val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(pillShape)
                .background(brandGradient)
                .clickable(enabled = !isLoading) { submit() },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Text(
                    text = when {
                        isSignupMode && awaitingOtp -> "Verify & create account"
                        isSignupMode -> "Send verification code"
                        else -> "Log in"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        if (!awaitingOtp) {
            Text(
                text = if (isSignupMode) "Already have an account? Log in" else "New to Tieyoh? Create an account",
                color = TieyohBrandBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable {
                        isSignupMode = !isSignupMode
                        awaitingOtp = false
                        otpCode = ""
                        errorMessage = null
                    }
                    .padding(bottom = 32.dp)
            )
        } else {
            Spacer(Modifier.height(32.dp))
        }
    }
}
