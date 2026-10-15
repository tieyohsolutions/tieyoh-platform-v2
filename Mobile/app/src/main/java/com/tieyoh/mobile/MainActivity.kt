package com.tieyoh.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.util.Log as AndroidLog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Base64
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohCardFaceStart
import com.tieyoh.mobile.ui.theme.TieyohCardFaceMid
import com.tieyoh.mobile.ui.theme.TieyohCardFaceEnd
import com.tieyoh.mobile.ui.theme.TieyohChooseHowBg
import com.tieyoh.mobile.ui.theme.TieyohMintTileBg
import com.tieyoh.mobile.ui.theme.TieyohMintIconBg
import com.tieyoh.mobile.ui.theme.TieyohBlueTileBg
import com.tieyoh.mobile.ui.theme.TieyohBlueIconBg
import com.tieyoh.mobile.ui.theme.TieyohBorder
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohGiftRed
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavInactive
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohSponsoredBg
import com.tieyoh.mobile.ui.theme.TieyohSponsoredBody
import com.tieyoh.mobile.ui.theme.TieyohSponsoredHeading
import com.tieyoh.mobile.ui.theme.TieyohSponsoredLabel
import com.tieyoh.mobile.ui.theme.TieyohSponsoredLogoBg
import com.tieyoh.mobile.ui.theme.TieyohTealGradient
import com.tieyoh.mobile.ui.theme.TieyohTheme
import com.tieyoh.mobile.ui.theme.TieyohTrack
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.time.LocalDate

// Tracks where the person is in the sign-up journey: logged out, mid-onboarding
// (choosing a Tieyoh ID / adding merchants), or fully into the app.
// Which screen a logged-out person is on, within the Sep 2026
// onboarding redesign: Welcome (brand moment) -> Get Started (phone +
// email) -> Login/Signup (the existing form). Local to the LoggedOut
// branch below, not part of SessionState, since none of it survives
// past actually authenticating.
private enum class LoggedOutStep { WELCOME, GET_STARTED, VERIFY_CODE, CREATE_PASSWORD, LOGIN }
private enum class ProfileStep { CHOOSE_ID, PRIVACY, DETAILS, FIND_FRIENDS, CONTACTS_PERMISSION, PEOPLE_YOU_MAY_KNOW }

private sealed class SessionState {
    object LoggedOut : SessionState()
    object NeedsBiometricUnlock : SessionState()
    data class NeedsProfile(val user: AuthResponse) : SessionState()
    data class NeedsMerchants(val user: AuthResponse) : SessionState()
    data class NeedsPaymentMethod(val user: AuthResponse) : SessionState()
    data class NeedsSendGiftSetup(val user: AuthResponse) : SessionState()
    data class NeedsBiometricEnroll(val user: AuthResponse, val isNewSignup: Boolean) : SessionState()
    data class OnboardingComplete(val user: AuthResponse) : SessionState()
    data class LoggedIn(val user: AuthResponse) : SessionState()
}

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TieyohTheme {
                val context = LocalContext.current

                // "Keep me logged in" is opt-in on the login screen; when
                // chosen, we remember the signed-in user locally so the next
                // app launch skips straight past the password prompt.
                var rememberSession by remember { mutableStateOf(true) }

                var sessionState by remember {
                    mutableStateOf<SessionState>(
                        when {
                            BiometricSessionStore.isEnrolled(context) -> SessionState.NeedsBiometricUnlock
                            else -> SessionStore.load(context)?.let { SessionState.LoggedIn(it) }
                                ?: SessionState.LoggedOut
                        }
                    )
                }

                // Hoisted out of the NeedsProfile branch below (was local to
                // it) so it survives sessionState moving on to
                // NeedsMerchants / NeedsPaymentMethod / OnboardingComplete
                // and back -- otherwise leaving NeedsProfile disposed this
                // state entirely, and coming back via a Back arrow reset
                // profileStep to CHOOSE_ID instead of returning to whatever
                // step the person actually left from.
                var profileStep by remember { mutableStateOf(ProfileStep.CHOOSE_ID) }
                var pendingTieyohId by remember { mutableStateOf<String?>(null) }
                var profileCompleteUser by remember { mutableStateOf<AuthResponse?>(null) }
                var pendingHandle by remember { mutableStateOf("") }
                var pendingHandleAvailable by remember { mutableStateOf(false) }
                var pendingPrivacyAgreed by remember { mutableStateOf(false) }
                var pendingBirthDate by remember { mutableStateOf<LocalDate?>(null) }
                var pendingGender by remember { mutableStateOf<String?>(null) }
                var pendingHolidaysCelebrated by remember { mutableStateOf(setOf<String>()) }
                var pendingAddress by remember { mutableStateOf("") }

                // A fresh SessionState.NeedsProfile (a brand-new signup, or
                // a returning login whose profile setup never finished)
                // must always start this walkthrough from scratch. The
                // fields above are hoisted here -- outside the NeedsProfile
                // branch -- so Back arrows inside NeedsProfile/NeedsMerchants
                // /etc. don't lose progress, but that same hoisting means
                // they silently carry over from whichever account last went
                // through this screen in the same app session. Without this
                // reset, starting a second signup (or logging in as a
                // different account that still needs profile setup) after
                // an earlier one completed this flow would skip straight to
                // whatever step -- and whichever user's data -- the
                // previous account left behind: profileCompleteUser in
                // particular would still hold the PREVIOUS user's
                // AuthResponse, which is how a brand-new signup could end
                // up finishing into someone else's account on the Feed.
                fun resetProfileSetupState() {
                    profileStep = ProfileStep.CHOOSE_ID
                    pendingTieyohId = null
                    profileCompleteUser = null
                    pendingHandle = ""
                    pendingHandleAvailable = false
                    pendingPrivacyAgreed = false
                    pendingBirthDate = null
                    pendingGender = null
                    pendingHolidaysCelebrated = setOf()
                    pendingAddress = ""
                }

                // isNewSignup distinguishes "just finished creating an
                // account" (NeedsSendGiftSetup's onDone, below) from a
                // normal returning login (LoginScreen's onAuthenticated,
                // below). The biometric-enroll offer still shows either way
                // the same as it always has (first time this device hasn't
                // enrolled yet) -- only the new "You're all set!" screen is
                // gated to the signup path, so a plain login lands straight
                // on the Feed instead of seeing it too.
                fun enterApp(user: AuthResponse, isNewSignup: Boolean) {
                    if (rememberSession) {
                        SessionStore.save(context, user)
                    }
                    val activity = context as? FragmentActivity
                    if (activity != null &&
                        !BiometricSessionStore.isEnrolled(context) &&
                        BiometricAuthHelper.isBiometricAvailable(activity)
                    ) {
                        sessionState = SessionState.NeedsBiometricEnroll(user, isNewSignup)
                    } else if (isNewSignup) {
                        sessionState = SessionState.OnboardingComplete(user)
                    } else {
                        sessionState = SessionState.LoggedIn(user)
                    }
                }

                when (val state = sessionState) {
                    is SessionState.NeedsBiometricUnlock -> {
                        BiometricUnlockScreen(
                            activity = this@MainActivity,
                            onUnlocked = { user ->
                                sessionState = SessionState.LoggedIn(user)
                            },
                            onUseFallback = {
                                sessionState = SessionStore.load(context)?.let { SessionState.LoggedIn(it) }
                                    ?: SessionState.LoggedOut
                            }
                        )
                    }

                    is SessionState.LoggedOut -> {
                        // The brand-moment Welcome screen (Sep 2026 redesign)
                        // is the actual first screen now -- LoginScreen only
                        // shows once the person has picked Create Account or
                        // Log In, in the matching mode. Local to this branch
                        // since it resets naturally on sign-out (a fresh
                        // SessionState.LoggedOut recomposition).
                        var loggedOutStep by remember { mutableStateOf(LoggedOutStep.WELCOME) }
                        var startInSignupMode by remember { mutableStateOf(false) }
                        // Carries what GetStartedScreen collected forward to
                        // VerifyCodeScreen -- nothing here is submitted
                        // anywhere yet (see VerifyCodeScreen's own note on
                        // the backend having no SMS delivery), this is just
                        // local state for the screen-to-screen handoff.
                        var pendingFirstName by remember { mutableStateOf("") }
                        var pendingLastName by remember { mutableStateOf("") }
                        var pendingPhone by remember { mutableStateOf("") }
                        var pendingEmail by remember { mutableStateOf("") }

                        // Same reasoning as pendingFirstName/etc. above --
                        // hoisted so Create Password and Log In don't reset
                        // to blank if the user backs out and comes back.
                        // Verification codes are the one deliberate
                        // exception (VerifyCodeScreen, and LoginScreen's own
                        // in-progress signup-code step) -- those stay local
                        // to their screens, never hoisted here.
                        var pendingPassword by remember { mutableStateOf("") }
                        var pendingConfirmPassword by remember { mutableStateOf("") }
                        var pendingLoginEmail by remember { mutableStateOf("") }
                        var pendingLoginPassword by remember { mutableStateOf("") }
                        var pendingLoginDisplayName by remember { mutableStateOf("") }
                        var pendingLoginRememberMe by remember { mutableStateOf(true) }

                        val loggedOutScope = rememberCoroutineScope()
                        // The backend only has email-based OTP delivery
                        // (LoggingOtpMailSender prints the code to the
                        // server console in dev rather than actually
                        // emailing it -- see OtpService/AuthController).
                        // There's no SMS provider, so this new flow's
                        // verification step is wired to that same
                        // email-based endpoint even though the screen's
                        // copy talks about the phone number -- the phone
                        // is display-only here for now.
                        fun requestSignupOtp(email: String, phone: String? = null, onSuccess: () -> Unit = {}) {
                            loggedOutScope.launch {
                                try {
                                    val response = RetrofitClient.authApi.requestOtp(
                                        OtpRequestRequest(email = email.trim(), purpose = "SIGNUP", phone = phone)
                                    )
                                    if (response.isSuccessful) {
                                        onSuccess()
                                    } else {
                                        val message = if (response.code() == 409) {
                                            // Email and phone are checked together server-side, so a 409
                                            // here could be either -- this stays generic rather than
                                            // guessing which one.
                                            "An account with this email or phone number already exists -- try logging in instead."
                                        } else {
                                            "Couldn't send a verification code (code ${response.code()}). Please try again."
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Couldn't reach the server. Is tieyoh-api running?", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        when (loggedOutStep) {
                            LoggedOutStep.WELCOME -> {
                                WelcomeScreen(
                                    onCreateAccount = {
                                        loggedOutStep = LoggedOutStep.GET_STARTED
                                    },
                                    onLogIn = {
                                        startInSignupMode = false
                                        loggedOutStep = LoggedOutStep.LOGIN
                                    }
                                )
                            }

                            LoggedOutStep.GET_STARTED -> {
                                GetStartedScreen(
                                    initialFirstName = pendingFirstName,
                                    initialLastName = pendingLastName,
                                    initialPhone = pendingPhone,
                                    initialEmail = pendingEmail,
                                    onFieldsChanged = { firstName, lastName, phone, email ->
                                        pendingFirstName = firstName
                                        pendingLastName = lastName
                                        pendingPhone = phone
                                        pendingEmail = email
                                    },
                                    onBack = { loggedOutStep = LoggedOutStep.WELCOME },
                                    onLogIn = {
                                        startInSignupMode = false
                                        loggedOutStep = LoggedOutStep.LOGIN
                                    },
                                    onContinue = { firstName, lastName, phone, email ->
                                        pendingFirstName = firstName
                                        pendingLastName = lastName
                                        pendingPhone = phone
                                        pendingEmail = email
                                        requestSignupOtp(email, phone) {
                                            loggedOutStep = LoggedOutStep.VERIFY_CODE
                                        }
                                    }
                                )
                            }

                            LoggedOutStep.VERIFY_CODE -> {
                                VerifyCodeScreen(
                                    phone = pendingPhone,
                                    onBack = { loggedOutStep = LoggedOutStep.GET_STARTED },
                                    onVerify = { code ->
                                        loggedOutScope.launch {
                                            try {
                                                val response = RetrofitClient.authApi.verifyOtp(
                                                    OtpVerifyRequest(
                                                        email = pendingEmail.trim(),
                                                        purpose = "SIGNUP",
                                                        code = code.trim()
                                                    )
                                                )
                                                if (response.isSuccessful) {
                                                    loggedOutStep = LoggedOutStep.CREATE_PASSWORD
                                                } else {
                                                    val message = when (response.code()) {
                                                        400 -> "That code isn't right or has expired -- try again or resend it."
                                                        429 -> "Too many attempts -- request a new code."
                                                        else -> "Something went wrong (code ${response.code()}). Please try again."
                                                    }
                                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Couldn't reach the server. Is tieyoh-api running?", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    onResend = {
                                        requestSignupOtp(pendingEmail, pendingPhone) {
                                            Toast.makeText(context, "Code resent.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }

                            LoggedOutStep.CREATE_PASSWORD -> {
                                CreatePasswordScreen(
                                    initialPassword = pendingPassword,
                                    initialConfirmPassword = pendingConfirmPassword,
                                    onFieldsChanged = { password, confirmPassword ->
                                        pendingPassword = password
                                        pendingConfirmPassword = confirmPassword
                                    },
                                    onBack = { loggedOutStep = LoggedOutStep.VERIFY_CODE },
                                    onContinue = { password ->
                                        loggedOutScope.launch {
                                            try {
                                                val displayName = listOf(pendingFirstName, pendingLastName)
                                                    .filter { it.isNotBlank() }
                                                    .joinToString(" ")
                                                    .ifBlank { pendingEmail.substringBefore("@") }
                                                val response = RetrofitClient.authApi.signup(
                                                    SignupRequest(
                                                        email = pendingEmail.trim(),
                                                        password = password,
                                                        displayName = displayName,
                                                        firstName = pendingFirstName.ifBlank { null },
                                                        lastName = pendingLastName.ifBlank { null },
                                                        phone = pendingPhone.ifBlank { null }
                                                    )
                                                )
                                                rememberSession = true
                                                resetProfileSetupState()
                                                sessionState = SessionState.NeedsProfile(response)
                                            } catch (e: HttpException) {
                                                val message = when (e.code()) {
                                                    409 -> "An account with this email already exists -- try logging in instead."
                                                    400 -> "Please verify your email again before creating an account."
                                                    else -> "Something went wrong (code ${e.code()}). Please try again."
                                                }
                                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Couldn't reach the server. Is tieyoh-api running?", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                )
                            }

                            LoggedOutStep.LOGIN -> {
                                LoginScreen(
                                    initialEmail = pendingLoginEmail,
                                    initialPassword = pendingLoginPassword,
                                    initialDisplayName = pendingLoginDisplayName,
                                    initialRememberMe = pendingLoginRememberMe,
                                    onFieldsChanged = { email, password, displayName, rememberMe ->
                                        pendingLoginEmail = email
                                        pendingLoginPassword = password
                                        pendingLoginDisplayName = displayName
                                        pendingLoginRememberMe = rememberMe
                                    },
                                    startInSignupMode = startInSignupMode,
                                    onBack = { loggedOutStep = LoggedOutStep.WELCOME },
                                    onAuthenticated = { response, remember, isSignup ->
                                        rememberSession = remember
                                        // Route by whether profile setup (@t id /
                                        // phone / sex) has actually been completed,
                                        // not just by whether this was a fresh
                                        // signup -- a returning user whose account
                                        // never finished that screen (backed out,
                                        // app was killed mid-setup, etc.) still
                                        // needs to go through it, the same as right
                                        // after signing up. Checking isSignup alone
                                        // let those accounts straight into the home
                                        // feed with no Tieyoh ID, ever, on every
                                        // later login.
                                        if (isSignup || response.tieyohId.isNullOrBlank()) {
                                            resetProfileSetupState()
                                            sessionState = SessionState.NeedsProfile(response)
                                        } else {
                                            // A normal returning login --
                                            // isNewSignup = false sends this
                                            // straight to the Feed, skipping
                                            // the biometric-enroll offer
                                            // (already handled, if needed)
                                            // and skipping "You're all set!"
                                            // entirely, since that's a
                                            // one-time signup moment, not
                                            // something every login should
                                            // show.
                                            enterApp(response, isNewSignup = false)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    is SessionState.NeedsProfile -> {
                        // Split across three screens now (@t id, then the
                        // privacy/consent step, then phone/gender/etc.)
                        // instead of one long form -- same backend calls as
                        // before (check-id, then saveProfile), just spread
                        // the collection out to match the new step-by-step
                        // onboarding UX.
                        // profileStep and the pendingX fields below are now
                        // hoisted to the top level (see above enterApp) so
                        // they survive a round trip out to NeedsMerchants /
                        // NeedsPaymentMethod and back via those screens'
                        // Back arrows.
                        when (profileStep) {
                            ProfileStep.CHOOSE_ID -> {
                                ChooseIdScreen(
                                    initialHandle = pendingHandle,
                                    initialAvailable = pendingHandleAvailable,
                                    onHandleStateChanged = { handle, available ->
                                        pendingHandle = handle
                                        pendingHandleAvailable = available
                                    },
                                    // No real "back" target exists yet this early
                                    // in profile setup (the account already
                                    // exists at this point) -- falling back to
                                    // the logged-out flow is a safe default since
                                    // a PENDING_PROFILE account can always log
                                    // back in and pick up here again.
                                    onBack = { sessionState = SessionState.LoggedOut },
                                    onContinue = { id ->
                                        pendingTieyohId = id
                                        profileStep = ProfileStep.PRIVACY
                                    }
                                )
                            }

                            ProfileStep.PRIVACY -> {
                                PrivacyExplainedScreen(
                                    initialAgreed = pendingPrivacyAgreed,
                                    onAgreedChanged = { pendingPrivacyAgreed = it },
                                    onBack = { profileStep = ProfileStep.CHOOSE_ID },
                                    onContinue = { profileStep = ProfileStep.DETAILS }
                                )
                            }

                            ProfileStep.DETAILS -> {
                                ProfileSetupScreen(
                                    tieyohId = pendingTieyohId ?: "",
                                    currentUser = state.user,
                                    initialBirthDate = pendingBirthDate,
                                    initialGender = pendingGender,
                                    initialHolidaysCelebrated = pendingHolidaysCelebrated,
                                    initialAddress = pendingAddress,
                                    onFieldsChanged = { birthDate, gender, holidaysCelebrated, address ->
                                        pendingBirthDate = birthDate
                                        pendingGender = gender
                                        pendingHolidaysCelebrated = holidaysCelebrated
                                        pendingAddress = address
                                    },
                                    onBack = { profileStep = ProfileStep.PRIVACY },
                                    onProfileComplete = { updated ->
                                        profileCompleteUser = updated
                                        profileStep = ProfileStep.FIND_FRIENDS
                                    }
                                )
                            }

                            ProfileStep.FIND_FRIENDS -> {
                                // profileCompleteUser is always set by the time this
                                // step is reached (DETAILS always sets it right before
                                // advancing here) -- state.user is the fallback only
                                // for the type system, never actually used in practice.
                                val updatedUser = profileCompleteUser ?: state.user
                                FindFriendsScreen(
                                    onBack = { profileStep = ProfileStep.DETAILS },
                                    onFindFriends = { profileStep = ProfileStep.CONTACTS_PERMISSION },
                                    onMaybeLater = { sessionState = SessionState.NeedsMerchants(updatedUser) }
                                )
                            }

                            ProfileStep.CONTACTS_PERMISSION -> {
                                val updatedUser = profileCompleteUser ?: state.user
                                ContactsPermissionScreen(
                                    onBack = { profileStep = ProfileStep.FIND_FRIENDS },
                                    onAllowed = { profileStep = ProfileStep.PEOPLE_YOU_MAY_KNOW },
                                    // Don't Allow, Maybe Later, or backing out of the
                                    // real OS permission dialog all skip the contacts
                                    // step altogether and go straight to merchant
                                    // selection -- same destination "Maybe Later" on
                                    // the previous screen already uses.
                                    onSkip = { sessionState = SessionState.NeedsMerchants(updatedUser) }
                                )
                            }

                            ProfileStep.PEOPLE_YOU_MAY_KNOW -> {
                                val updatedUser = profileCompleteUser ?: state.user
                                PeopleYouMayKnowScreen(
                                    ownerTieyohId = updatedUser.tieyohId ?: "",
                                    onBack = { profileStep = ProfileStep.CONTACTS_PERMISSION },
                                    onDone = { sessionState = SessionState.NeedsMerchants(updatedUser) }
                                )
                            }
                        }
                    }

                    is SessionState.NeedsMerchants -> {
                        MerchantSelectionScreen(
                            currentUser = state.user,
                            onBack = {
                                // profileStep is still PEOPLE_YOU_MAY_KNOW
                                // from before -- it's hoisted now, so
                                // re-entering NeedsProfile lands back on
                                // the same step instead of resetting to
                                // CHOOSE_ID.
                                sessionState = SessionState.NeedsProfile(state.user)
                            },
                            onDone = {
                                sessionState = SessionState.NeedsPaymentMethod(state.user)
                            }
                        )
                    }

                    is SessionState.NeedsPaymentMethod -> {
                        PaymentMethodSetupScreen(
                            onBack = {
                                sessionState = SessionState.NeedsMerchants(state.user)
                            },
                            onDone = {
                                sessionState = SessionState.NeedsSendGiftSetup(state.user)
                            },
                            backLabel = "Merchant Selection"
                        )
                    }

                    is SessionState.NeedsSendGiftSetup -> {
                        SendGiftPromptScreen(
                            currentUserId = state.user.id,
                            ownerTieyohId = state.user.tieyohId.orEmpty(),
                            onDone = {
                                // The tail end of a brand-new signup --
                                // this is the one path that should still
                                // see "You're all set!" (via enterApp).
                                enterApp(state.user, isNewSignup = true)
                            },
                            onBack = {
                                sessionState = SessionState.NeedsPaymentMethod(state.user)
                            }
                        )
                    }

                    is SessionState.NeedsBiometricEnroll -> {
                        BiometricEnrollScreen(
                            activity = this@MainActivity,
                            user = state.user,
                            onDone = {
                                sessionState = if (state.isNewSignup) {
                                    SessionState.OnboardingComplete(state.user)
                                } else {
                                    SessionState.LoggedIn(state.user)
                                }
                            }
                        )
                    }

                    is SessionState.OnboardingComplete -> {
                        OnboardingCompleteScreen(
                            onBack = {
                                // SendGiftPromptScreen (the last regular
                                // step before the enter-app/biometric
                                // machinery) has no Back arrow of its own
                                // yet -- this at least stops the arrow here
                                // from skipping straight into the Feed.
                                sessionState = SessionState.NeedsSendGiftSetup(state.user)
                            },
                            onGoToFeed = {
                                sessionState = SessionState.LoggedIn(state.user)
                            }
                        )
                    }

                    is SessionState.LoggedIn -> {
                        TieyohApp(
                            currentUser = state.user,
                            onLogout = {
                                SessionStore.clear(context)
                                BiometricSessionStore.clear(context)
                                BiometricAuthHelper.deleteKey()
                                sessionState = SessionState.LoggedOut
                            },
                            onUserUpdated = { updated ->
                                // Edit Profile can change email now, which
                                // this screen (and the header on Profile)
                                // reads straight off currentUser -- persist
                                // and swap in the fresh copy so neither goes
                                // stale until the next login.
                                SessionStore.save(context, updated)
                                sessionState = SessionState.LoggedIn(updated)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Shown at launch when a biometric-gated session exists (see
// BiometricSessionStore). Immediately prompts Face ID / Touch ID /
// fingerprint to decrypt the cached session; falls back to the normal
// login flow (or a remembered plain session) if the user cancels or it
// fails for any reason.
@Composable
private fun BiometricUnlockScreen(
    activity: FragmentActivity,
    onUnlocked: (AuthResponse) -> Unit,
    onUseFallback: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val stored = BiometricSessionStore.readCiphertext(context)
        if (stored == null) {
            onUseFallback()
            return@LaunchedEffect
        }
        val (ciphertext, iv) = stored
        try {
            val json = BiometricAuthHelper.decryptWithBiometrics(
                activity = activity,
                title = "Unlock Tieyoh",
                subtitle = "Use your biometric to sign in",
                ciphertextB64 = ciphertext,
                ivB64 = iv
            )
            val user = BiometricSessionStore.parseUser(json)
            if (user != null) {
                onUnlocked(user)
            } else {
                onUseFallback()
            }
        } catch (e: Exception) {
            onUseFallback()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.tieyoh_logo),
            contentDescription = "Tieyoh",
            modifier = Modifier.height(40.dp)
        )
        Spacer(Modifier.height(24.dp))
        CircularProgressIndicator(color = TieyohAccent)
        Spacer(Modifier.height(16.dp))
        Text("Waiting for biometric unlock…", color = TieyohMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "Use password instead",
            color = TieyohAccent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onUseFallback() }
        )
    }
}

// Offered right after a successful signup or password login on a device
// that hasn't enrolled biometrics yet (see MainActivity.enterApp()).
// Skippable — declining just leaves the existing plain SessionStore
// session in place, unchanged from before this feature existed.
@Composable
private fun BiometricEnrollScreen(
    activity: FragmentActivity,
    user: AuthResponse,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val gson = remember { Gson() }

    fun enable() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val json = gson.toJson(user)
                val (ciphertext, iv) = BiometricAuthHelper.encryptWithBiometrics(
                    activity = activity,
                    title = "Set up faster sign-in",
                    subtitle = "Confirm with your biometric to enable this",
                    plaintext = json
                )
                BiometricSessionStore.save(context, ciphertext, iv)
                SessionStore.clear(context)
                onDone()
            } catch (e: Exception) {
                errorMessage = "Couldn't enable biometric sign-in. You can try again later from Profile."
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Set up faster sign-in",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TieyohNavy
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Use Face ID, Touch ID, or your fingerprint to open Tieyoh next time — no password needed on this device.",
            color = TieyohMuted,
            fontSize = 14.sp
        )

        errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        TieyohPrimaryButton(
            text = "Enable biometric sign-in",
            onClick = { enable() },
            enabled = !isLoading,
            isLoading = isLoading
        )

        Spacer(Modifier.height(14.dp))

        TieyohSecondaryButton(text = "Skip for now", onClick = onDone)
    }
}

// Matches the four tabs + center Send action from the interactive prototype
// (Feed / My list / My card / Profile, with the Send FAB in the middle).
private enum class TieyohScreen {
    FEED,
    MY_LIST,
    MY_CARD,
    PROFILE,
    SEND_GIFT,
    SEND_GIFT_MERCHANT_SEARCH,
    SEARCH_MERCHANT,
    INVITE_CONTACT,
    CHOOSE_RECIPIENT,
    INVITE_FRIENDS,
    PAYMENT_METHODS,
    WEBSITE,
    NOTIFICATIONS,
    EDIT_PROFILE,
    PRIVACY_SETTINGS,
    GIFT_SENT,
    GIFT_RECEIVED,
    CONFIRM_CONNECTION,
    ALL_CIRCLE,
    MY_QR_CODE,
    PAY_WITH_QR,
    PAY_WITH_CARD,
    PARTNERED_MERCHANT_SEARCH,
    WALLET_OVERVIEW,
    TRANSACTION_HISTORY,
    ACTIVITY_INSIGHTS
}

private data class WishTarget(
    val merchant: String,
    val current: Int,
    val goal: Int
)

@Composable
fun TieyohApp(
    currentUser: AuthResponse,
    onLogout: () -> Unit,
    onUserUpdated: (AuthResponse) -> Unit = {}
) {
    val feedContext = LocalContext.current

    var currentScreen by remember {
        mutableStateOf(TieyohScreen.FEED)
    }
    // Who the "Send" flow (or an active circle member tapped from the add
    // screen) is currently sending a gift to -- set right before jumping
    // into SEND_GIFT so that screen can show who it's for.
    var giftRecipient by remember { mutableStateOf<FriendResult?>(null) }
    // Tracks which screen "View My QR Code" was opened from (Profile or
    // My Card) so the QR screen's back button returns there instead of
    // always going to Profile.
    var qrCodeOrigin by remember { mutableStateOf(TieyohScreen.PROFILE) }
    // The merchant chosen (either from the recipient's favorites or via the
    // 'Other' round trip through SEND_GIFT_MERCHANT_SEARCH) for the gift
    // currently being composed in SEND_GIFT.
    var giftMerchant by remember { mutableStateOf<MerchantDto?>(null) }

    // Info for the sender's full-screen "You sent a gift!" confirmation,
    // and the gift id for the recipient's "You received a gift!" one --
    // set right before jumping into GIFT_SENT / GIFT_RECEIVED so those
    // screens have what they need without extra plumbing through the
    // TieyohScreen enum itself.
    var sentGiftInfo by remember { mutableStateOf<SentGiftInfo?>(null) }
    var receivedGiftId by remember { mutableStateOf<Long?>(null) }

    // Set when the recipient taps "Thank <sender>" on GIFT_RECEIVED --
    // that button drops them back on the Feed with this quick-reply
    // overlay showing, rather than a separate screen (see ThankSenderSheet).
    var pendingThank by remember { mutableStateOf<PendingThank?>(null) }

    // Set when a name tapped inside a feed item belongs to someone who
    // isn't a direct circle member yet -- routes to CONFIRM_CONNECTION
    // instead of straight into the gift flow. See FeedScreen's
    // classifyAndRoute.
    var pendingConnectionTarget by remember { mutableStateOf<PendingConnectionTarget?>(null) }

    var hasUnreadNotifications by remember { mutableStateOf(false) }

    // Cheap stand-in for a real push/badge system: just check once when the
    // app comes up whether there's anything unread, so the bell shows a dot
    // without the user having to open it first. While the notifications
    // screen itself is open, NotificationsScreen's onUnreadChanged keeps
    // this in sync live as individual notifications get tapped/read --
    // this initial check only covers before that screen has ever loaded.
    LaunchedEffect(currentUser.id) {
        try {
            hasUnreadNotifications = RetrofitClient.notificationApi
                .getNotifications(currentUser.id)
                .any { !it.read }
        } catch (e: Exception) {
            // Leave it as-is -- no badge is better than a wrong one.
        }
    }

    Scaffold(
        topBar = {
            TieyohTopBar(
                onLogoClick = {
                    currentScreen = TieyohScreen.WEBSITE
                },
                onAddClick = {
                    currentScreen = TieyohScreen.INVITE_CONTACT
                },
                onBellClick = {
                    currentScreen = TieyohScreen.NOTIFICATIONS
                },
                hasUnreadNotifications = hasUnreadNotifications
            )
        },
        bottomBar = {
            TieyohBottomBar(
                currentScreen = currentScreen,
                onNavigate = { screen ->
                    currentScreen = screen
                },
                onSendClick = { currentScreen = TieyohScreen.CHOOSE_RECIPIENT }
            )
        }
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Scaffold's innerPadding only accounts for the bottom
                // bar's own laid-out height -- it has no way to know that
                // the circular Send FAB inside TieyohBottomBar pokes up
                // ~38dp above the bar's top edge via a paint-only offset
                // (offset() doesn't change measured/layout bounds). Every
                // screen below effectively got 38dp less real clearance
                // than innerPadding promised, which is what let the FAB
                // sit on top of whatever a screen put at its own bottom
                // edge (seen on the QR page and the payment-method page).
                // Reserving that space once, here, for every screen is
                // the actual fix, instead of patching each screen's own
                // bottom spacing one at a time as this keeps coming up.
                .padding(bottom = 52.dp),
            color = MaterialTheme.colorScheme.background
        ) {

            when (currentScreen) {

                TieyohScreen.FEED -> {
                    FeedScreen(
                        currentUser = currentUser,
                        onGiftPerson = { person ->
                            if (person.userId == currentUser.id) {
                                Toast.makeText(feedContext, "You can't send a gift to yourself", Toast.LENGTH_SHORT).show()
                                return@FeedScreen
                            }
                            giftMerchant = null
                            giftRecipient = FriendResult(
                                id = 0L,
                                contactUserId = person.userId,
                                displayName = person.displayName,
                                tieyohId = person.tieyohId,
                                phone = null,
                                email = null,
                                status = "ACTIVE",
                                profilePictureBase64 = person.profilePictureBase64
                            )
                            currentScreen = TieyohScreen.SEND_GIFT
                        },
                        onGiftFeedPerson = { person ->
                            if (person.userId == currentUser.id) {
                                Toast.makeText(feedContext, "You can't send a gift to yourself", Toast.LENGTH_SHORT).show()
                                return@FeedScreen
                            }
                            giftMerchant = null
                            giftRecipient = FriendResult(
                                id = 0L,
                                contactUserId = person.userId,
                                displayName = person.displayName,
                                tieyohId = person.tieyohId,
                                phone = null,
                                email = null,
                                status = "ACTIVE",
                                profilePictureBase64 = person.profilePictureBase64
                            )
                            currentScreen = TieyohScreen.SEND_GIFT
                        },
                        onConnectFeedPerson = { target ->
                            pendingConnectionTarget = target
                            currentScreen = TieyohScreen.CONFIRM_CONNECTION
                        },
                        onSendGiftClick = { currentScreen = TieyohScreen.CHOOSE_RECIPIENT },
                        onSeeAllCircle = { currentScreen = TieyohScreen.ALL_CIRCLE },
                        onAddToCircle = { currentScreen = TieyohScreen.INVITE_CONTACT },
                        pendingThank = pendingThank,
                        onDismissThank = { pendingThank = null }
                    )
                }

                TieyohScreen.ALL_CIRCLE -> {
                    AllCircleScreen(
                        currentUserId = currentUser.id,
                        onBack = { currentScreen = TieyohScreen.FEED },
                        onAddToCircle = { currentScreen = TieyohScreen.INVITE_CONTACT },
                        onGiftPerson = { person ->
                            if (person.userId == currentUser.id) {
                                Toast.makeText(feedContext, "You can't send a gift to yourself", Toast.LENGTH_SHORT).show()
                                return@AllCircleScreen
                            }
                            giftMerchant = null
                            giftRecipient = FriendResult(
                                id = 0L,
                                contactUserId = person.userId,
                                displayName = person.displayName,
                                tieyohId = person.tieyohId,
                                phone = null,
                                email = null,
                                status = "ACTIVE",
                                profilePictureBase64 = person.profilePictureBase64
                            )
                            currentScreen = TieyohScreen.SEND_GIFT
                        },
                        onConnectPerson = { target ->
                            pendingConnectionTarget = target
                            currentScreen = TieyohScreen.CONFIRM_CONNECTION
                        }
                    )
                }

                TieyohScreen.MY_LIST -> MyListScreen(currentUser = currentUser)

                TieyohScreen.MY_CARD -> MyCardScreen(
                    currentUser = currentUser,
                    onNavigateToPayWithQr = { currentScreen = TieyohScreen.PAY_WITH_QR },
                    onNavigateToPayWithCard = { currentScreen = TieyohScreen.PAY_WITH_CARD },
                    onNavigateToWalletOverview = { currentScreen = TieyohScreen.WALLET_OVERVIEW },
                    onNavigateToTransactionHistory = { currentScreen = TieyohScreen.TRANSACTION_HISTORY },
                    onNavigateToActivityInsights = { currentScreen = TieyohScreen.ACTIVITY_INSIGHTS }
                )

                TieyohScreen.WALLET_OVERVIEW -> {
                    WalletOverviewScreen(
                        currentUser = currentUser,
                        onBack = { currentScreen = TieyohScreen.MY_CARD }
                    )
                }

                TieyohScreen.TRANSACTION_HISTORY -> {
                    TransactionHistoryScreen(
                        currentUser = currentUser,
                        onBack = { currentScreen = TieyohScreen.MY_CARD }
                    )
                }

                TieyohScreen.ACTIVITY_INSIGHTS -> {
                    ActivityInsightsScreen(
                        currentUser = currentUser,
                        onBack = { currentScreen = TieyohScreen.MY_CARD }
                    )
                }

                TieyohScreen.SEND_GIFT -> {
                    val recipient = giftRecipient
                    if (recipient == null) {
                        // Shouldn't happen -- SEND_GIFT is only ever entered with a
                        // recipient already chosen -- but bounce back to the feed
                        // rather than crash if it does.
                        LaunchedEffect(Unit) { currentScreen = TieyohScreen.FEED }
                    } else {
                        SendGiftScreen(
                            currentUserId = currentUser.id,
                            recipient = recipient,
                            selectedMerchant = giftMerchant,
                            onSelectMerchant = { giftMerchant = it },
                            onSearchOtherMerchant = { currentScreen = TieyohScreen.SEND_GIFT_MERCHANT_SEARCH },
                            onSent = { info ->
                                giftMerchant = null
                                giftRecipient = null
                                sentGiftInfo = info
                                currentScreen = TieyohScreen.GIFT_SENT
                            },
                            onCancel = {
                                giftMerchant = null
                                giftRecipient = null
                                currentScreen = TieyohScreen.FEED
                            }
                        )
                    }
                }

                TieyohScreen.SEND_GIFT_MERCHANT_SEARCH -> {
                    MerchantSearchScreen(
                        title = "Search for a Merchant",
                        subtitle = "Find another merchant for this gift.",
                        actionLabel = "Search Merchants",
                        onMerchantSelected = { merchant ->
                            giftMerchant = merchant
                            currentScreen = TieyohScreen.SEND_GIFT
                        },
                        onBack = { currentScreen = TieyohScreen.SEND_GIFT },
                        backLabel = "Send a Gift"
                    )
                }

                TieyohScreen.SEARCH_MERCHANT -> {
                    MerchantSearchScreen(
                        title = "Search for a Merchant",
                        subtitle = "Search the Tieyoh merchant registry.",
                        actionLabel = "Search Merchants",
                        onBack = { currentScreen = TieyohScreen.FEED }
                    )
                }

                TieyohScreen.PROFILE -> ProfileScreen(
                    currentUser = currentUser,
                    onLogout = onLogout,
                    onManagePaymentMethods = { currentScreen = TieyohScreen.PAYMENT_METHODS },
                    onEditProfile = { currentScreen = TieyohScreen.EDIT_PROFILE },
                    onPrivacySettings = { currentScreen = TieyohScreen.PRIVACY_SETTINGS },
                    onViewQrCode = {
                        qrCodeOrigin = TieyohScreen.PROFILE
                        currentScreen = TieyohScreen.MY_QR_CODE
                    },
                    onProfileUpdated = onUserUpdated
                )

                TieyohScreen.MY_QR_CODE -> {
                    MyQrCodeScreen(
                        currentUser = currentUser,
                        onBack = { currentScreen = qrCodeOrigin }
                    )
                }

                TieyohScreen.PAY_WITH_QR -> {
                    PayWithQrScreen(
                        currentUser = currentUser,
                        onBack = { currentScreen = TieyohScreen.MY_CARD },
                        onFindMerchants = { currentScreen = TieyohScreen.PARTNERED_MERCHANT_SEARCH }
                    )
                }

                TieyohScreen.PAY_WITH_CARD -> {
                    PayWithCardScreen(
                        currentUser = currentUser,
                        onBack = { currentScreen = TieyohScreen.MY_CARD }
                    )
                }

                TieyohScreen.PARTNERED_MERCHANT_SEARCH -> {
                    // Its own dedicated screen (not the generic
                    // MerchantSearchScreen used elsewhere) -- it needs the
                    // Search/Map toggle and only ever calls the
                    // partnered-only search/nearby endpoints, never the
                    // general merchant registry search.
                    PartneredMerchantSearchScreen(
                        onBack = { currentScreen = TieyohScreen.MY_CARD }
                    )
                }

                TieyohScreen.INVITE_CONTACT -> {
                    ContactsInviteScreen(
                        currentUserId = currentUser.id,
                        ownerTieyohId = currentUser.tieyohId.orEmpty(),
                        onDone = { currentScreen = TieyohScreen.FEED },
                        onSelectRecipient = { friend ->
                            giftMerchant = null
                            giftRecipient = friend
                            currentScreen = TieyohScreen.SEND_GIFT
                        },
                        onInviteFriendsClick = { currentScreen = TieyohScreen.INVITE_FRIENDS }
                    )
                }

                TieyohScreen.INVITE_FRIENDS -> {
                    InviteFriendsScreen(
                        ownerTieyohId = currentUser.tieyohId.orEmpty(),
                        onDone = { currentScreen = TieyohScreen.INVITE_CONTACT }
                    )
                }

                TieyohScreen.CHOOSE_RECIPIENT -> {
                    ContactsInviteScreen(
                        currentUserId = currentUser.id,
                        ownerTieyohId = currentUser.tieyohId.orEmpty(),
                        onDone = { currentScreen = TieyohScreen.FEED },
                        onSelectRecipient = { friend ->
                            giftMerchant = null
                            giftRecipient = friend
                            currentScreen = TieyohScreen.SEND_GIFT
                        },
                        title = "Send a gift to",
                        subtitle = "Pick someone from your circle, or invite them to join Tieyoh.",
                        doneLabel = "Cancel"
                    )
                }

                TieyohScreen.PAYMENT_METHODS -> {
                    PaymentMethodSetupScreen(
                        onDone = { currentScreen = TieyohScreen.PROFILE },
                        doneButtonLabel = "Done"
                    )
                }

                TieyohScreen.WEBSITE -> {
                    TieyohWebsiteScreen(
                        onDone = { currentScreen = TieyohScreen.FEED }
                    )
                }

                TieyohScreen.NOTIFICATIONS -> {
                    NotificationsScreen(
                        currentUserId = currentUser.id,
                        onUnreadChanged = { stillUnread -> hasUnreadNotifications = stillUnread },
                        onDone = { currentScreen = TieyohScreen.FEED },
                        onOpenGift = { giftId ->
                            receivedGiftId = giftId
                            currentScreen = TieyohScreen.GIFT_RECEIVED
                        }
                    )
                }

                TieyohScreen.EDIT_PROFILE -> {
                    EditProfileScreen(
                        currentUserId = currentUser.id,
                        onDone = { currentScreen = TieyohScreen.PROFILE },
                        onProfileUpdated = onUserUpdated
                    )
                }

                TieyohScreen.PRIVACY_SETTINGS -> {
                    PrivacySettingsScreen(
                        currentUserId = currentUser.id,
                        onDone = { currentScreen = TieyohScreen.PROFILE }
                    )
                }

                TieyohScreen.GIFT_SENT -> {
                    val info = sentGiftInfo
                    if (info == null) {
                        // Shouldn't happen -- GIFT_SENT is only ever
                        // entered right after a successful send -- but
                        // bounce back to the feed rather than crash if it
                        // does (matches the SEND_GIFT null-recipient guard
                        // above).
                        LaunchedEffect(Unit) { currentScreen = TieyohScreen.FEED }
                    } else {
                        GiftSentScreen(
                            info = info,
                            onSendToSomeoneElse = {
                                sentGiftInfo = null
                                currentScreen = TieyohScreen.CHOOSE_RECIPIENT
                            },
                            onBackToFeed = {
                                sentGiftInfo = null
                                currentScreen = TieyohScreen.FEED
                            }
                        )
                    }
                }

                TieyohScreen.GIFT_RECEIVED -> {
                    val giftId = receivedGiftId
                    if (giftId == null) {
                        LaunchedEffect(Unit) { currentScreen = TieyohScreen.FEED }
                    } else {
                        GiftReceivedScreen(
                            giftId = giftId,
                            onThankSender = { senderId, senderDisplayName ->
                                pendingThank = PendingThank(
                                    giftId = giftId,
                                    senderId = senderId,
                                    senderDisplayName = senderDisplayName
                                )
                                receivedGiftId = null
                                currentScreen = TieyohScreen.FEED
                            },
                            onBackToFeed = {
                                receivedGiftId = null
                                currentScreen = TieyohScreen.FEED
                            }
                        )
                    }
                }

                TieyohScreen.CONFIRM_CONNECTION -> {
                    val target = pendingConnectionTarget
                    if (target == null) {
                        // Shouldn't happen -- this screen is only ever
                        // entered right after tapping a non-circle name in
                        // the feed -- but bounce back rather than crash.
                        LaunchedEffect(Unit) { currentScreen = TieyohScreen.FEED }
                    } else {
                        ConfirmConnectionScreen(
                            currentUserId = currentUser.id,
                            target = target,
                            onCancel = {
                                pendingConnectionTarget = null
                                currentScreen = TieyohScreen.FEED
                            },
                            onRequestSent = {
                                pendingConnectionTarget = null
                                currentScreen = TieyohScreen.FEED
                            }
                        )
                    }
                }
            }
        }
    }
}

// Tapping the Tieyoh wordmark opens this instead of just jumping back to
// the feed -- it hands off to the real Tieyoh website. Doubles right now
// as a clean way to test whether taps on the LEFT side of the top bar
// (far from the invite icon's spot near the camera cutout) are reliable.
@Composable
private fun TieyohWebsiteScreen(
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val websiteUrl = "https://www.tieyoh.com"

    LaunchedEffect(Unit) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
        context.startActivity(intent)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(top = 8.dp),
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Opening tieyoh.com",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Taking you to the Tieyoh website in your browser.",
            color = TieyohMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        TieyohPrimaryButton(
            text = "Open tieyoh.com",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TieyohSecondaryButton(text = "Back to feed", onClick = onDone)
    }
    }
}

@Composable
private fun TieyohTopBar(
    onLogoClick: () -> Unit,
    onAddClick: () -> Unit,
    onBellClick: () -> Unit,
    hasUnreadNotifications: Boolean = false
) {

    Surface(
        color = Color.White,
        shadowElevation = 0.dp
    ) {

        // The app targets a modern SDK where edge-to-edge is enforced by the
        // system -- our content draws all the way to the top of the screen,
        // behind the status bar and the front-camera cutout, whether we ask
        // for that or not. Without this padding the logo/icon row started
        // right at y=0: part of it sat visually behind the cutout (the
        // "head chopped off" clipping) and touches in that same top band
        // never reached our clickable() at all (the "only clicks under the
        // camera" dead zone) -- true for BOTH the logo on the left and the
        // invite icon on the right, since it's a full-width band, not a
        // left/right issue. Pushing the row below the status bar + cutout
        // safe area fixes both. The white Surface background above still
        // extends under the status bar for a seamless look.
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.statusBars.union(WindowInsets.displayCutout)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box {
                    Text(
                        text = "\uD83D\uDD14",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .size(22.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .clickable { onBellClick() }
                    )
                    if (hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TieyohGiftRed)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Left-aligned (not centered) so the front camera cutout on
                // notched phones never sits on top of the wordmark.
                Image(
                    painter = painterResource(id = R.drawable.tieyoh_logo),
                    contentDescription = "Tieyoh",
                    modifier = Modifier
                        .height(26.dp)
                        .clickable { onLogoClick() }
                )

                Spacer(modifier = Modifier.weight(1f))

                InvitePeopleIcon(
                    onClick = onAddClick
                )
            }

            HorizontalDivider(color = TieyohBorder, thickness = 1.dp)
        }
    }
}

// The person+plus glyph is a real drawable (ic_invite_person.png) rather
// than something drawn by hand in Canvas -- communicates "add someone to
// your circle" more clearly than a bare plus sign, since this same button
// doubles as the general invite entry point (top bar + bottom-nav Send
// flow both eventually funnel here).
@Composable
private fun InvitePeopleIcon(
    onClick: () -> Unit
) {
    // A single, explicitly-sized Box is the ONLY thing that determines the
    // clickable area -- deliberately not IconButton. Testing showed
    // IconButton's own internal sizing layers put the real clickable
    // region somewhere different from where it visually rendered. A plain
    // Box with one .size() and .clickable() has no hidden internal layers
    // to fight with, and the Image fills that exact same Box, so the
    // drawn pixels and the tappable area are, by construction, the same
    // rectangle.
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_invite_person),
            contentDescription = "Invite someone to Tieyoh",
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        )
    }
}

@Composable
private fun TieyohBottomBar(
    currentScreen: TieyohScreen,
    onNavigate: (TieyohScreen) -> Unit,
    onSendClick: () -> Unit
) {

    Box(modifier = Modifier.fillMaxWidth()) {

        NavigationBar(
            containerColor = Color.White,
            contentColor = TieyohNavInactive,
            modifier = Modifier.padding(top = 16.dp)
        ) {

            TieyohNavItem(
                screen = TieyohScreen.FEED,
                currentScreen = currentScreen,
                onNavigate = onNavigate,
                glyph = "▤",
                label = "Feed"
            )

            TieyohNavItem(
                screen = TieyohScreen.MY_LIST,
                currentScreen = currentScreen,
                onNavigate = onNavigate,
                icon = { color -> NavListGlyph(color = color, size = 22.dp) },
                label = "My Picks"
            )

            TieyohNavItem(
                screen = TieyohScreen.MY_CARD,
                currentScreen = currentScreen,
                onNavigate = onNavigate,
                icon = { color -> NavCardGlyph(color = color, size = 22.dp) },
                label = "My Card"
            )

            TieyohNavItem(
                screen = TieyohScreen.PROFILE,
                currentScreen = currentScreen,
                onNavigate = onNavigate,
                icon = { color -> NavProfileGlyph(color = color, size = 22.dp) },
                label = "Profile"
            )
        }

        // Gift-bag Send FAB, nudged up so it pokes above the bar.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-41.8).dp)
                .size(88.dp)
                .clickable { onSendClick() }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_send_fab),
                contentDescription = "Send a gift",
                modifier = Modifier.fillMaxSize()
            )
        }

        // A perfectly flat, horizontal black line at the same baseline
        // height the whole way across -- under Feed/My list, My card/
        // Profile, AND right up to the Send icon, with no rise or curve
        // beforehand. It meets the icon's own true circle (same measured
        // center/radius from ic_send_fab.png, so the seam still lands
        // exactly on the icon's real edge and stays hidden) at a point
        // below its equator rather than at the very left/right tip, then
        // sweeps under it in a shallower-than-180-degree dip before
        // returning to the same flat baseline -- shallower on purpose, so
        // it doesn't eat as much vertical space as a full half-moon would.
        // Small rounded curls stay at each far end (unchanged). Drawn LAST
        // so it flows above (in front of) both the nav bar and the icon.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(83.6.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-44).dp)
        ) {
            val leftPad = 24.2.dp.toPx()
            val rightPad = 24.2.dp.toPx()
            val baseline = 61.6.dp.toPx()
            val centerX = size.width / 2f
            // Measured from ic_send_fab.png at its original 80.dp size
            // (center ~41.dp / radius ~35.dp, 44% of the box), then scaled
            // by the same 1.1x the FAB box grew by (80.dp -> 88.dp) so the
            // arc still lands exactly on the icon's real edge.
            val ringCenterY = 45.1.dp.toPx()
            val ringRadius = 38.5.dp.toPx()
            // Small rounded "drop" at each tip of the line instead of a
            // flat stroke-cap ending.
            val hookRun = 11.dp.toPx()
            val hookDrop = 13.2.dp.toPx()

            // Find where the icon's TRUE circle (same measured radius)
            // crosses the flat baseline height, below the circle's own
            // equator -- that's where the flat line hands off to the arc.
            // Using the real radius keeps the seam exactly on the icon's
            // own edge (hidden there) while letting the flat run stay
            // level all the way in, and the resulting sweep is naturally
            // less than a full 180 semicircle.
            val refAngleRad = asin(((baseline - ringCenterY) / ringRadius).coerceIn(-1f, 1f))
            val refAngleDeg = Math.toDegrees(refAngleRad.toDouble()).toFloat()
            val leftAngleDeg = 180f - refAngleDeg
            val rightAngleDeg = refAngleDeg
            val sweepDeg = rightAngleDeg - leftAngleDeg

            val leftAttach = Offset(
                centerX + ringRadius * cos(Math.toRadians(leftAngleDeg.toDouble())).toFloat(),
                baseline
            )
            val rightAttach = Offset(
                centerX + ringRadius * cos(Math.toRadians(rightAngleDeg.toDouble())).toFloat(),
                baseline
            )

            val path = Path().apply {
                // Rounded drop at the very start of the line.
                moveTo(leftPad - hookRun, baseline + hookDrop)
                quadraticTo(leftPad - hookRun, baseline, leftPad, baseline)
                // Perfectly flat and level all the way to the icon -- no
                // rise, no curve.
                lineTo(leftAttach.x, baseline)
                // A shallower-than-half-circle dip -- still exactly the
                // icon's own true edge, just not swept all the way around.
                arcTo(
                    rect = Rect(
                        left = centerX - ringRadius,
                        top = ringCenterY - ringRadius,
                        right = centerX + ringRadius,
                        bottom = ringCenterY + ringRadius
                    ),
                    startAngleDegrees = leftAngleDeg,
                    sweepAngleDegrees = sweepDeg,
                    forceMoveTo = false
                )
                // Flat and level again on the way out.
                lineTo(size.width - rightPad, baseline)
                // Rounded drop at the very end of the line.
                quadraticTo(
                    size.width - rightPad + hookRun, baseline,
                    size.width - rightPad + hookRun, baseline + hookDrop
                )
            }

            drawPath(
                path = path,
                color = TieyohNavy,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
private fun RowScope.TieyohNavItem(
    screen: TieyohScreen,
    currentScreen: TieyohScreen,
    onNavigate: (TieyohScreen) -> Unit,
    glyph: String? = null,
    icon: (@Composable (Color) -> Unit)? = null,
    label: String
) {
    NavigationBarItem(
        selected = currentScreen == screen,
        onClick = { onNavigate(screen) },
        icon = {
            if (icon != null) {
                icon(LocalContentColor.current)
            } else {
                Text(glyph.orEmpty(), fontSize = 20.sp)
            }
        },
        label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = TieyohAccent,
            selectedTextColor = TieyohAccent,
            unselectedIconColor = TieyohNavInactive,
            unselectedTextColor = TieyohNavInactive,
            indicatorColor = Color.Transparent
        )
    )
}

// -- Bottom nav glyphs -------------------------------------------------
// Hand-drawn with Canvas, same convention as every other custom icon in
// this file (QrCodeGlyph, VirtualCardGlyph, etc.) rather than a Material
// ImageVector, so the bottom bar keeps drawing its own icons instead of
// mixing in a different icon language. Shapes were matched from the
// reference images the product owner sent for this pass (a credit card
// for My Card, a person-in-a-circle for Profile, list rows for My
// picks) -- Feed's original glyph ("▤") is untouched since no reference
// was given for it. `color` is driven by LocalContentColor from
// TieyohNavItem above, so these automatically switch between
// TieyohAccent (selected) and TieyohNavInactive (unselected) exactly
// like the unicode glyphs did.
@Composable
private fun NavCardGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 24f
        val strokeW = 1.8f * s
        drawRoundRect(
            color = color,
            topLeft = Offset(2f * s, 5f * s),
            size = Size(20f * s, 14f * s),
            cornerRadius = CornerRadius(3f * s, 3f * s),
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Magnetic-stripe bar near the top of the card face.
        drawLine(
            color = color,
            start = Offset(2f * s, 10f * s),
            end = Offset(22f * s, 10f * s),
            strokeWidth = 2.2f * s,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun NavProfileGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 24f
        val strokeW = 1.8f * s
        val center = Offset(12f * s, 12f * s)
        val ringRadius = 10f * s
        // Outer ring, then head + shoulders clipped to it so the
        // shoulder arc's flat top edge doesn't poke outside the circle.
        drawCircle(color = color, radius = ringRadius, center = center, style = Stroke(width = strokeW))
        clipPath(Path().apply { addOval(Rect(center = center, radius = ringRadius - strokeW / 2f)) }) {
            drawCircle(color = color, radius = 3.3f * s, center = Offset(12f * s, 9.6f * s))
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(12f * s - 6.4f * s, 13.4f * s),
                size = Size(12.8f * s, 10f * s)
            )
        }
    }
}

@Composable
private fun NavListGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 24f
        val rowYs = listOf(6.5f, 12f, 17.5f)
        rowYs.forEach { y ->
            drawRoundRect(
                color = color,
                topLeft = Offset(2f * s, (y - 1.5f) * s),
                size = Size(3f * s, 3f * s),
                cornerRadius = CornerRadius(0.8f * s, 0.8f * s)
            )
            drawLine(
                color = color,
                start = Offset(8f * s, y * s),
                end = Offset(22f * s, y * s),
                strokeWidth = 2f * s,
                cap = StrokeCap.Round
            )
        }
    }
}

// Sample social-feed content. There's no feed backend yet, so this stands
// in for what a real "activity from people you follow" endpoint will
// eventually return. Intended ranking criteria for the real feed (more
// conditions to be layered on later):
//   - message-exchange history with the viewer's contacts on Tieyoh
//   - sponsored/ad content relevant to the viewer and their area
//   - activity where someone has set a target for a gift amount
// The feed is a single continuously scrollable list — no "see more" step.
// Kept only so WishTargetCard/SponsoredCard below stay available as
// reusable components (per the Interactive Feed spec: don't delete UI
// pieces just because the data source changed) even though nothing
// constructs these anymore now that the Feed is backend-driven. Renamed
// from the old bare "FeedItem" -- that name now belongs to the real
// backend-driven item shape in FeedApi.kt, and both types living in the
// same package would otherwise collide.
private sealed class LegacyFeedItem {
    data class Wish(
        val person: String,
        val merchant: String,
        val current: Int,
        val goal: Int,
        val timeAgo: String,
        val supporters: Int
    ) : LegacyFeedItem()

    data class Transaction(
        val initials: String,
        val actor: String,
        val amount: Int,
        val recipient: String,
        val merchant: String,
        val timeAgo: String
    ) : LegacyFeedItem()

    data class Sponsored(
        val initials: String,
        val merchant: String,
        val offer: String
    ) : LegacyFeedItem()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FeedScreen(
    currentUser: AuthResponse,
    onGiftPerson: (FeedPersonResult) -> Unit,
    onGiftFeedPerson: (FeedPerson) -> Unit,
    // A name tapped inside a feed item (GiftActivityCard, BirthdayCard)
    // that isn't a direct circle member -- see classifyAndRoute below.
    // Opens a dedicated confirm screen instead of firing a connection
    // request immediately.
    onConnectFeedPerson: (PendingConnectionTarget) -> Unit = {},
    onSendGiftClick: () -> Unit,
    // "See all" opens the full circle grid (AllCircleScreen); the "+" chip
    // at the end of the row reuses the same add-to-circle flow as the top
    // bar's own "+" icon.
    onSeeAllCircle: () -> Unit = {},
    onAddToCircle: () -> Unit = {},
    pendingThank: PendingThank? = null,
    onDismissThank: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = currentUser.id

    var network by remember { mutableStateOf<List<FeedPersonResult>>(emptyList()) }
    var networkLoaded by remember { mutableStateOf(false) }
    var networkLoadFailed by remember { mutableStateOf(false) }

    // Real, backend-driven Feed state (Interactive Social/Gifting Feed spec,
    // sections 14-16). isLoadingFeed covers the very first load; isRefreshing
    // covers pull-to-refresh; isLoadingMore covers the "Load more" footer --
    // kept separate so each shows its own spinner without hiding the list.
    var feedItems by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    var feedPage by remember { mutableStateOf(0) }
    var hasMoreFeed by remember { mutableStateOf(false) }
    var isLoadingFeed by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var feedError by remember { mutableStateOf<String?>(null) }

    suspend fun loadFirstPage(showSpinner: Boolean) {
        if (showSpinner) isLoadingFeed = true
        feedError = null
        when (val result = FeedRepository.loadPage(currentUserId, page = 0)) {
            is FeedPageResult.Success -> {
                feedItems = result.items
                hasMoreFeed = result.hasMore
                feedPage = 0
            }
            is FeedPageResult.Error -> {
                feedError = result.message
            }
        }
        isLoadingFeed = false
    }

    fun loadMore() {
        if (isLoadingMore || !hasMoreFeed) return
        coroutineScope.launch {
            isLoadingMore = true
            val nextPage = feedPage + 1
            when (val result = FeedRepository.loadPage(currentUserId, page = nextPage)) {
                is FeedPageResult.Success -> {
                    feedItems = feedItems + result.items
                    hasMoreFeed = result.hasMore
                    feedPage = nextPage
                }
                is FeedPageResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
            isLoadingMore = false
        }
    }

    LaunchedEffect(currentUserId) {
        network = try {
            RetrofitClient.friendApi.getNetwork(currentUserId)
        } catch (e: Exception) {
            networkLoadFailed = true
            emptyList()
        }
        networkLoaded = true
        loadFirstPage(showSpinner = true)
    }

    // "Your circle" shows direct members plus anyone with a connection
    // request pending in either direction, so sending a request from the
    // new confirm screen is immediately visible here (with a pending
    // badge) rather than the person just disappearing from the row. Pure
    // friend-of-a-friend contacts with no request yet are still excluded.
    val circleRow = network.filter { it.relationship == "DIRECT" || it.connectionRequestStatus != null }

    // A name tapped inside a feed item (sender/recipient on a gift card, a
    // birthday card's person) routes differently depending on whether
    // they're already a direct circle member: DIRECT -> straight into the
    // gift flow, same as always. Anyone else (a friend-of-a-friend surfaced
    // by the feed's visibility rules, or someone reachable via a PUBLIC
    // gift) -> a dedicated confirm-connection screen instead of silently
    // firing a request on a single tap.
    fun classifyAndRoute(person: FeedPerson) {
        // A feed item can name the current user themselves (e.g. Hannah
        // shows up as the sender/recipient of her own past gift) -- that's
        // not someone to gift or connect with, so this has to be checked
        // before the network lookup below, which won't find a viewer's own
        // id in their own network list anyway and would otherwise fall
        // through to "not connected yet, send a request".
        if (person.userId == currentUserId) return
        val match = network.firstOrNull { it.userId == person.userId }
        if (match?.relationship == "DIRECT") {
            onGiftFeedPerson(person)
        } else {
            onConnectFeedPerson(
                PendingConnectionTarget(
                    userId = person.userId,
                    displayName = person.displayName,
                    tieyohId = person.tieyohId,
                    profilePictureBase64 = person.profilePictureBase64,
                    connectionRequestStatus = match?.connectionRequestStatus
                )
            )
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                loadFirstPage(showSpinner = false)
                isRefreshing = false
            }
        },
        state = rememberPullToRefreshState(),
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Text(
                    text = "Hi ${currentUser.displayName}, here's what your circle's up to",
                    fontSize = 13.sp,
                    color = TieyohMuted,
                    lineHeight = 15.sp
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Matches the reference image: larger/bolder than
                    // the section headers used to be, same navy as the
                    // rest of the app's headings.
                    Text(
                        text = "Your circle",
                        fontWeight = FontWeight.Bold,
                        color = TieyohNavy,
                        fontSize = 17.sp
                    )
                    if (circleRow.isNotEmpty()) {
                        Text(
                            text = "See all >",
                            fontWeight = FontWeight.SemiBold,
                            color = TieyohBrandBlue,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onSeeAllCircle() }
                        )
                    }
                }
            }
            item {
                when {
                    !networkLoaded -> {
                        Box(modifier = Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.CenterStart) {
                            CircularProgressIndicator(color = TieyohAccent, modifier = Modifier.size(20.dp))
                        }
                    }
                    networkLoadFailed -> {
                        Text("Couldn't load your circle", color = TieyohMuted, fontSize = 13.sp)
                    }
                    else -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(circleRow) { person ->
                                NetworkPersonChip(
                                    person = person,
                                    onClick = {
                                        if (person.relationship == "DIRECT") {
                                            onGiftPerson(person)
                                        } else {
                                            onConnectFeedPerson(
                                                PendingConnectionTarget(
                                                    userId = person.userId,
                                                    displayName = person.displayName,
                                                    tieyohId = person.tieyohId,
                                                    profilePictureBase64 = person.profilePictureBase64,
                                                    connectionRequestStatus = person.connectionRequestStatus
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                            item {
                                AddCircleChip(onClick = onAddToCircle)
                            }
                        }
                    }
                }
            }

            when {
                isLoadingFeed -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TieyohAccent)
                        }
                    }
                }

                feedError != null -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Couldn't load your feed",
                                fontWeight = FontWeight.Bold,
                                color = TieyohNavy
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = feedError ?: "",
                                color = TieyohMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            TieyohSecondaryButton(
                                text = "Try again",
                                onClick = { coroutineScope.launch { loadFirstPage(showSpinner = true) } },
                                fillWidth = false
                            )
                        }
                    }
                }

                feedItems.isEmpty() -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nothing new yet.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TieyohNavy
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Send someone a gift and make their day.",
                                color = TieyohMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            TieyohPrimaryButton(
                                text = "Send a Gift",
                                onClick = onSendGiftClick,
                                fillWidth = false
                            )
                        }
                    }
                }

                else -> {
                    // Static placeholder ad -- there's no backend ad system
                    // yet (see SponsoredCard/LegacyFeedItem.Sponsored), so
                    // this is fixed sample content until one exists. Shown
                    // once, right after the first real item, matching where
                    // the old sample feed placed its own sponsored card.
                    itemsIndexed(feedItems, key = { _, entry -> entry.eventId }) { index, entry ->
                        FeedEventCard(
                            item = entry,
                            onPersonTapped = { person -> classifyAndRoute(person) },
                            onSendGiftClick = onSendGiftClick
                        )
                        if (index == 0) {
                            Spacer(Modifier.height(16.dp))
                            SponsoredCard(
                                item = LegacyFeedItem.Sponsored(
                                    initials = "BL",
                                    merchant = "Blue Bottle Coffee",
                                    offer = "20% off this week with Tieyoh"
                                )
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasMoreFeed) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        color = TieyohAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    TieyohSecondaryButton(
                                        text = "Load more",
                                        onClick = { loadMore() },
                                        fillWidth = false
                                    )
                                }
                            } else {
                                Text("You're all caught up", color = TieyohMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Rendered as a sibling overlay, not part of the LazyColumn above --
    // this is what "Thank <sender>" on GiftReceivedScreen leads to (see
    // ThankSenderSheet), matching the reference mockup's third screen
    // ("where Thank leads"): a quick-reply surface over the live Feed,
    // not a separate navigation destination.
    pendingThank?.let { thank ->
        ThankSenderSheet(
            currentUserId = currentUser.id,
            pendingThank = thank,
            onDismiss = onDismissThank,
            onSent = { coroutineScope.launch { loadFirstPage(showSpinner = false) } }
        )
    }
}

// Dispatches a single backend Feed item (Interactive Social/Gifting Feed
// spec, section 8) to the right card by eventType. An event type this
// build doesn't know about yet renders nothing rather than crashing, so
// the backend can introduce new types (FRIEND_JOINED_TIEYOH, GIFT_RECEIVED,
// etc.) without requiring an app update first.
@Composable
private fun FeedEventCard(
    item: FeedItem,
    onPersonTapped: (FeedPerson) -> Unit,
    onSendGiftClick: () -> Unit
) {
    when (item.eventType) {
        "GIFT_ACTIVITY" -> {
            val sender = item.sender
            val recipient = item.recipient
            if (sender != null && recipient != null) {
                GiftActivityCard(
                    sender = sender,
                    recipient = recipient,
                    gift = item.gift,
                    merchant = item.merchant,
                    occurredAt = item.occurredAt,
                    onPersonTapped = onPersonTapped
                )
            }
        }

        "UPCOMING_BIRTHDAY" -> {
            val person = item.person
            if (person != null) {
                BirthdayCard(
                    person = person,
                    daysUntil = item.daysUntil ?: 0,
                    onGiftPerson = onPersonTapped
                )
            }
        }

        "UPCOMING_HOLIDAY" -> {
            HolidayCard(
                holidayName = item.holidayName ?: "Upcoming holiday",
                displayMessage = item.displayMessage ?: "",
                onSendGiftClick = onSendGiftClick
            )
        }

        else -> {
            // Unrecognized/future event type -- nothing to render yet.
        }
    }
}

// Restored to the original feed's "gift activity" card look (avatar +
// one wrapped sentence + a merchant/time subtitle line) per the user's
// request to bring the feed's visual style back to how it looked before
// the Sep 2, 2026 backend-driven rebuild -- this is the same layout
// TransactionCard (below) already had for the old sample data, just fed
// from the real backend item now. Sender and recipient still each need
// to be their own tap target (spec section 9) even inside one wrapped
// sentence, so this uses ClickableText with string annotations marking
// each name's character range, rather than two separate Text composables
// (which was correct but is what made this look different from the
// original card in the first place).
@Composable
private fun GiftActivityCard(
    sender: FeedPerson,
    recipient: FeedPerson,
    gift: FeedGiftInfo?,
    merchant: FeedMerchantSummary?,
    occurredAt: Long?,
    onPersonTapped: (FeedPerson) -> Unit
) {
    val amountText = gift?.let { "\$${formatGiftAmount(it.amount)}" }
    val timeText = occurredAt?.let {
        DateUtils.getRelativeTimeSpanString(
            it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
    val subtitle = listOfNotNull(merchant?.brandName, timeText).joinToString(" · ")

    val sentence = remember(sender.displayName, recipient.displayName, amountText) {
        buildAnnotatedString {
            pushStringAnnotation(tag = "SENDER", annotation = "sender")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TieyohNavy)) {
                append(sender.displayName)
            }
            pop()
            append(" sent ")
            if (amountText != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = TieyohAccent)) {
                    append("$amountText gift")
                }
            } else {
                append("a gift")
            }
            append(" to ")
            pushStringAnnotation(tag = "RECIPIENT", annotation = "recipient")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TieyohNavy)) {
                append(recipient.displayName)
            }
            pop()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohCardBg)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(TieyohBlue, TieyohTealGradient))),
            contentAlignment = Alignment.Center
        ) {
            Text(personInitials(sender.displayName), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ClickableText(
                text = sentence,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TieyohNavy),
                onClick = { offset ->
                    sentence.getStringAnnotations(tag = "SENDER", start = offset, end = offset)
                        .firstOrNull()?.let { onPersonTapped(sender) }
                    sentence.getStringAnnotations(tag = "RECIPIENT", start = offset, end = offset)
                        .firstOrNull()?.let { onPersonTapped(recipient) }
                }
            )
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 13.sp, color = TieyohMuted)
            }
            gift?.comments?.forEach { comment ->
                Spacer(Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${comment.authorDisplayName ?: "Someone"}: ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TieyohMuted
                    )
                    Text(
                        text = comment.body,
                        fontSize = 12.sp,
                        color = TieyohMuted
                    )
                }
            }
        }
    }
}

// "Marcus Lee" -> "ML" -- first letter of up to the first two words,
// matching the two-letter avatar initials the original feed cards used
// (NetworkAvatar/SearchResultAvatar elsewhere in the app use just a
// single first-letter initial instead; this is deliberately the one spot
// that mimics the old two-letter style since that's the card being
// restored here).
internal fun personInitials(displayName: String): String {
    val words = displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return words.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        .ifBlank { "?" }
}

private fun formatGiftAmount(amount: Double): String {
    return if (amount == Math.floor(amount)) amount.toLong().toString() else "%.2f".format(amount)
}

@Composable
private fun BirthdayCard(person: FeedPerson, daysUntil: Int, onGiftPerson: (FeedPerson) -> Unit) {
    val whenText = when (daysUntil) {
        0 -> "today"
        1 -> "tomorrow"
        else -> "in $daysUntil days"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohCardBg)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🎂", fontSize = 30.sp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${person.displayName}'s birthday is $whenText",
                fontWeight = FontWeight.Bold,
                color = TieyohNavy
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(TieyohAccent)
                    .clickable { onGiftPerson(person) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Send ${person.displayName} a Gift",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// No specific recipient for a holiday, so the CTA is a generic hand-off
// into the same "choose a recipient" flow the center Send button uses.
@Composable
private fun HolidayCard(holidayName: String, displayMessage: String, onSendGiftClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohCardBg)
            .padding(20.dp)
    ) {
        Text(holidayName, fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(displayMessage, color = TieyohMuted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(TieyohNavy)
                .clickable(onClick = onSendGiftClick)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text("Send a Gift", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// Reached via "See all >" on the Feed's "Your circle" row. Same circle
// membership rules as that row (DIRECT members plus anyone with a
// connection request pending either direction) laid out as a grid instead
// of a single scrolling row, with the same "+" add tile always last. The
// bottom nav bar stays visible on this screen the same way it does on
// every other TieyohScreen, so tapping Feed there works too -- the back
// arrow just gives an explicit way back without needing it.
@Composable
private fun AllCircleScreen(
    currentUserId: Long,
    onBack: () -> Unit,
    onAddToCircle: () -> Unit,
    onGiftPerson: (FeedPersonResult) -> Unit,
    onConnectPerson: (PendingConnectionTarget) -> Unit
) {
    var network by remember { mutableStateOf<List<FeedPersonResult>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        network = try {
            RetrofitClient.friendApi.getNetwork(currentUserId)
        } catch (e: Exception) {
            loadFailed = true
            emptyList()
        }
        loaded = true
    }

    val circleRow = network.filter { it.relationship == "DIRECT" || it.connectionRequestStatus != null }
    val columns = 4

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
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
                text = "Feed",
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = "Your circle",
                fontWeight = FontWeight.Bold,
                color = TieyohNavy,
                fontSize = 20.sp
            )
        }

        when {
            !loaded -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TieyohAccent)
                }
            }
            loadFailed -> {
                Text(
                    "Couldn't load your circle",
                    color = TieyohMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
            else -> {
                val rowCount = (circleRow.size + 1 + columns - 1) / columns
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
                ) {
                    items(rowCount) { rowIndex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until columns) {
                                val index = rowIndex * columns + col
                                when {
                                    index < circleRow.size -> {
                                        val person = circleRow[index]
                                        NetworkPersonChip(
                                            person = person,
                                            onClick = {
                                                if (person.relationship == "DIRECT") {
                                                    onGiftPerson(person)
                                                } else {
                                                    onConnectPerson(
                                                        PendingConnectionTarget(
                                                            userId = person.userId,
                                                            displayName = person.displayName,
                                                            tieyohId = person.tieyohId,
                                                            profilePictureBase64 = person.profilePictureBase64,
                                                            connectionRequestStatus = person.connectionRequestStatus
                                                        )
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    index == circleRow.size -> {
                                        AddCircleChip(onClick = onAddToCircle)
                                    }
                                    else -> {
                                        Spacer(modifier = Modifier.width(72.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// One person in the "Your circle" row -- a direct circle member (full
// color, tap to gift) or someone with a connection request pending
// between the viewer and them (dimmed, badge shows which direction;
// tap reopens the confirm screen to see status or send/accept there).
@Composable
private fun NetworkPersonChip(person: FeedPersonResult, onClick: () -> Unit) {
    val isDirect = person.relationship == "DIRECT"

    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            NetworkAvatar(
                profilePictureBase64 = person.profilePictureBase64,
                displayName = person.displayName,
                dimmed = !isDirect
            )
            if (!isDirect) {
                val badge = when (person.connectionRequestStatus) {
                    "PENDING_SENT" -> "⏳"
                    "PENDING_RECEIVED" -> "❓"
                    else -> "+"
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (badge == "+") TieyohAccent else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = badge, fontSize = 11.sp, color = if (badge == "+") Color.White else TieyohNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = person.displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDirect) TieyohNavy else TieyohMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// The "+" chip at the end of the "Your circle" row -- same footprint as
// NetworkPersonChip so it lines up in the LazyRow, but a dashed-outline
// circle instead of an avatar. Reuses the same add-to-circle destination
// as the top bar's own "+" icon (ContactsInviteScreen).
@Composable
private fun AddCircleChip(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(TieyohBrandBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Add",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NetworkAvatar(
    profilePictureBase64: String?,
    displayName: String,
    dimmed: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(profilePictureBase64) {
        profilePictureBase64?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(TieyohAccent.copy(alpha = if (dimmed) 0.08f else 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "$displayName's profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                text = displayName.firstOrNull()?.uppercase() ?: "?",
                color = if (dimmed) TieyohMuted else TieyohAccent,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.32f).sp
            )
        }
    }
}

@Composable
private fun WishTargetCard(item: LegacyFeedItem.Wish) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohCardBg)
            .padding(20.dp)
    ) {
        Text("${item.person} set a wish target", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
        Spacer(Modifier.height(4.dp))
        Text("${item.merchant} · ${item.timeAgo}", fontSize = 14.sp, color = TieyohMuted)
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text("\$${item.current}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
            Text(" / \$${item.goal}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TieyohMuted)
        }
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(TieyohTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        if (item.goal > 0) (item.current.toFloat() / item.goal).coerceIn(0f, 1f) else 0f
                    )
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(TieyohBlue, TieyohTealGradient)))
            )
        }
    }
}

@Composable
private fun SponsoredCard(item: LegacyFeedItem.Sponsored) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohSponsoredBg)
            .padding(20.dp)
    ) {
        Text(
            "SPONSORED",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TieyohSponsoredLabel,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TieyohSponsoredLogoBg),
                contentAlignment = Alignment.Center
            ) {
                Text(item.initials, fontWeight = FontWeight.Bold, color = TieyohSponsoredHeading)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(item.merchant, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TieyohSponsoredHeading)
                Text(item.offer, fontSize = 13.sp, color = TieyohSponsoredBody)
            }
        }
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TieyohNavy)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("View offer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun TransactionCard(
    initials: String,
    headlinePrefix: String,
    giftLabel: String,
    headlineSuffix: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohCardBg)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(TieyohBlue, TieyohTealGradient))),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                buildAnnotatedString {
                    append(headlinePrefix)
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = TieyohAccent)) {
                        append(giftLabel)
                    }
                    append(headlineSuffix)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TieyohNavy
            )
            Text(subtitle, fontSize = 13.sp, color = TieyohMuted)
        }
    }
}

private enum class PicksSortOption(val label: String) {
    NAME("Name (A-Z)"),
    TARGET("Highest target"),
    RECEIVED("Highest received")
}

private fun formatPickAmount(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.2f", value)
    }
}

@Composable
private fun MyListScreen(currentUser: AuthResponse) {

    var picks by remember { mutableStateOf<List<MerchantDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(PicksSortOption.NAME) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTarget by remember { mutableStateOf<MerchantDto?>(null) }

    val coroutineScope = rememberCoroutineScope()

    suspend fun reload() {
        try {
            picks = RetrofitClient.userApi.getMerchantSelections(currentUser.id)
            loadError = null
        } catch (e: Exception) {
            loadError = "Couldn't load your picks right now."
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(currentUser.id) {
        isLoading = true
        reload()
    }

    val sortedPicks = remember(picks, sortOption) {
        when (sortOption) {
            PicksSortOption.NAME -> picks.sortedBy { it.brandName.lowercase() }
            PicksSortOption.TARGET -> picks.sortedByDescending { it.targetAmount ?: -1.0 }
            PicksSortOption.RECEIVED -> picks.sortedByDescending { it.amountReceived ?: 0.0 }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Text("My Picks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TieyohNavy)
            Spacer(Modifier.height(6.dp))
            Text(
                "Merchants you'd love a gift from. Set a wish target and your circle can chip in toward it.",
                color = TieyohMuted
            )
            Spacer(Modifier.height(16.dp))
            TieyohPrimaryButton(
                text = "+ Add a merchant to your list",
                onClick = { showAddSheet = true }
            )
        }

        when {
            isLoading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TieyohAccent)
                }
            }

            loadError != null -> item {
                Text(loadError.orEmpty(), color = TieyohGiftRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            picks.isEmpty() -> item {
                Text(
                    "You haven't added any picks yet. Tap \"Add a merchant\" above to get started.",
                    color = TieyohMuted,
                    fontSize = 14.sp
                )
            }

            else -> {
                item {
                    Column {
                        Text("Sort by", fontSize = 12.sp, color = TieyohMuted, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PicksSortOption.values().forEach { option ->
                                val isActive = option == sortOption
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isActive) TieyohAccent else Color.Transparent)
                                        .border(1.dp, TieyohAccent, RoundedCornerShape(12.dp))
                                        .clickable { sortOption = option }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        option.label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color.White else TieyohAccent
                                    )
                                }
                            }
                        }
                    }
                }

                items(sortedPicks, key = { it.id }) { pick ->
                    MerchantPickCard(
                        pick = pick,
                        onEditTarget = { editingTarget = pick }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddMerchantSheet(
            currentUser = currentUser,
            existingIds = picks.map { it.id }.toSet(),
            onDismiss = { showAddSheet = false },
            onAdded = {
                showAddSheet = false
                coroutineScope.launch { reload() }
            }
        )
    }

    editingTarget?.let { pick ->
        TargetEditDialog(
            merchantName = pick.brandName,
            currentTarget = pick.targetAmount,
            onDismiss = { editingTarget = null },
            onSave = { newTarget ->
                coroutineScope.launch {
                    try {
                        RetrofitClient.userApi.updateMerchantTarget(
                            userId = currentUser.id,
                            merchantId = pick.id,
                            request = MerchantTargetRequest(targetAmount = newTarget)
                        )
                        editingTarget = null
                        reload()
                    } catch (e: Exception) {
                        // Leave the dialog open with what they typed rather
                        // than silently discarding it -- no toast/error
                        // surface wired up here yet.
                    }
                }
            }
        )
    }
}

// One pick's card -- shows real progress (amountReceived, derived from
// completed gifts) against the target once one is set, or an invitation
// to set one when it isn't. The pill in the top-right is the only
// difference between those two states: "Edit target" (outline) vs.
// "Set target" (filled), both opening the same TargetEditDialog.
// Per spec, only a Tieyoh-partnered merchant gets its logo shown in a
// search or list row -- everyone else is represented by plain name
// alone, no icon. Shared here across MerchantPickCard, AddMerchantSheet's
// results, and MerchantResultCard (below) rather than duplicated three
// times, since all three live in this same file.
@Composable
private fun MerchantLogoThumb(merchant: MerchantDto) {
    if (merchant.partnered && !merchant.logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = merchant.logoUrl,
            contentDescription = merchant.brandName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(TieyohMuted.copy(alpha = 0.08f))
        )
        Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun MerchantPickCard(pick: MerchantDto, onEditTarget: () -> Unit) {
    val target = pick.targetAmount
    val hasTarget = target != null && target > 0
    val received = pick.amountReceived ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TieyohCardBg)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            MerchantLogoThumb(pick)
            Column(modifier = Modifier.weight(1f)) {
                Text(pick.brandName, fontWeight = FontWeight.Bold, color = TieyohNavy, fontSize = 16.sp)
                pick.category?.let {
                    Text(it, fontSize = 12.sp, color = TieyohMuted)
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .then(
                        if (hasTarget) {
                            Modifier.border(1.5.dp, TieyohAccent, RoundedCornerShape(15.dp))
                        } else {
                            Modifier.background(TieyohAccent)
                        }
                    )
                    .clickable { onEditTarget() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    if (hasTarget) "✎ Edit target" else "+ Set target",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasTarget) TieyohAccent else Color.White
                )
            }
        }

        if (hasTarget && target != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("\$${formatPickAmount(received)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TieyohNavy)
                Text(" / \$${formatPickAmount(target)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TieyohMuted)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(TieyohTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            if (target > 0) (received / target).toFloat().coerceIn(0f, 1f) else 0f
                        )
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.horizontalGradient(listOf(TieyohBlue, TieyohTealGradient)))
                )
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                "No wish target set yet — tap \"Set target\" to add one anytime.",
                fontSize = 13.sp,
                color = TieyohMuted
            )
        }
    }
}

// Shared by "Set target" (currentTarget == null) and "Edit target"
// (currentTarget != null) -- same dialog, the title and starting value
// are the only difference.
@Composable
private fun TargetEditDialog(
    merchantName: String,
    currentTarget: Double?,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit
) {
    var amountText by remember {
        mutableStateOf(currentTarget?.let { formatPickAmount(it) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (currentTarget != null) "Edit target" else "Set target",
                fontWeight = FontWeight.Bold,
                color = TieyohNavy
            )
        },
        text = {
            Column {
                Text(merchantName, color = TieyohMuted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Wish target") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(amountText.toDoubleOrNull()) }) {
                Text("Save", color = TieyohAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TieyohMuted)
            }
        }
    )
}

// The "Add a merchant to your list" flow from My picks -- a single search
// + select step, same real search MerchantSelectionScreen uses, but
// additive (RetrofitClient.userApi.addMerchantToList) so it never touches
// any other pick already on the list. A wish target is optional here: it
// can be set in this same step, or skipped and added later from the
// "Set target" pill on the new card.
//
// Search mode's single field covers business name, full or partial street
// address, city, and ZIP all in one box -- the backend ORs the query
// across name and every location field (MerchantRepository.
// searchEligibleMerchants) instead of the caller having to pick a field
// first, so "Starbucks", "123 Main St", "St. Louis", and "63101" all work
// from the same input. Map mode stays separate since it's a different
// interaction (browse/pan a map), not a text query.
private enum class MerchantSearchMode(val label: String) {
    SEARCH("Search"),
    MAP("Map")
}

private fun formatDistance(miles: Double): String {
    return if (miles < 0.1) "Nearby" else "%.1f mi away".format(miles)
}

@Composable
private fun AddMerchantSheet(
    currentUser: AuthResponse,
    existingIds: Set<Long>,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    val context = LocalContext.current
    var searchMode by remember { mutableStateOf(MerchantSearchMode.SEARCH) }
    var searchText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MerchantDto>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MerchantDto?>(null) }
    var targetText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var locationServicesDisabled by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    // Map mode's own map/list sub-toggle -- "user can switch from a map to
    // a list and see all merchants in the area."
    var showMapView by remember { mutableStateOf(true) }
    var showSearchThisArea by remember { mutableStateOf(false) }
    var lastSearchedCenter by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState()

    val coroutineScope = rememberCoroutineScope()

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(LocationManager::class.java)
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    // Shared by both "locate me" (fetchNearby, below) and the map's
    // "Search this area" button -- the only difference is whether the
    // camera should jump to the new center or stay put because the user
    // just put it there themselves by panning.
    suspend fun fetchNearbyAt(lat: Double, lng: Double, moveCamera: Boolean) {
        isSearching = true
        hasSearched = true
        errorMessage = null
        try {
            results = RetrofitClient.merchantApi.getNearbyMerchants(lat = lat, lng = lng)
            lastSearchedCenter = LatLng(lat, lng)
            showSearchThisArea = false
            if (moveCamera) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 14f)
            }
        } catch (e: Exception) {
            AndroidLog.e("AddMerchantSheet", "Failed to fetch nearby merchants", e)
            results = emptyList()
            errorMessage = "Unable to find merchants near you right now."
        } finally {
            isSearching = false
        }
    }

    fun switchMode(mode: MerchantSearchMode) {
        if (searchMode == mode) return
        searchMode = mode
        searchText = ""
        results = emptyList()
        hasSearched = false
        selected = null
        errorMessage = null
        locationServicesDisabled = false
        showSearchThisArea = false
        lastSearchedCenter = null
        if (mode == MerchantSearchMode.MAP) {
            showMapView = true
        }
    }

    fun search() {
        if (searchText.isBlank()) return
        coroutineScope.launch {
            isSearching = true
            hasSearched = true
            errorMessage = null
            try {
                // One combined query, OR'd server-side across name and
                // every location field -- no more guessing which mode a
                // typed string belongs to.
                results = RetrofitClient.merchantApi.searchMerchants(q = searchText.trim())
            } catch (e: Exception) {
                results = emptyList()
                errorMessage = "Unable to search places right now."
            } finally {
                isSearching = false
            }
        }
    }

    fun fetchNearby() {
        // Checked up front so a device with Location switched off entirely
        // (common on a fresh emulator, but a real possibility for real
        // users too) gets a direct link to the right settings screen
        // instead of a generic "couldn't find your location" dead end --
        // the fused client would otherwise just return null with no way
        // to tell "off" apart from "no fix yet".
        if (!isLocationEnabled()) {
            hasSearched = true
            results = emptyList()
            locationServicesDisabled = true
            errorMessage = "Location is turned off on this device."
            return
        }
        locationServicesDisabled = false

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        coroutineScope.launch {
            isSearching = true
            hasSearched = true
            errorMessage = null
            try {
                // lastLocation resolves immediately from whatever fix the provider
                // already has -- on an emulator, that's exactly what Extended
                // Controls' "Set location" just pushed. getCurrentLocation asks
                // for a brand-new fix instead, which can sit unresolved on an
                // emulator's simulated GPS provider even after a location has
                // been set, so it's only the fallback here, not the primary path.
                var location = try {
                    fusedClient.lastLocation.await()
                } catch (e: Exception) {
                    AndroidLog.w("AddMerchantSheet", "lastLocation failed, falling back to getCurrentLocation", e)
                    null
                }

                if (location == null) {
                    location = fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                }

                if (location == null) {
                    results = emptyList()
                    errorMessage = "Couldn't get your location -- make sure location is turned on."
                    isSearching = false
                } else {
                    fetchNearbyAt(location.latitude, location.longitude, moveCamera = true)
                }
            } catch (e: Exception) {
                AndroidLog.e("AddMerchantSheet", "Failed to fetch nearby merchants", e)
                results = emptyList()
                errorMessage = "Unable to find merchants near you right now."
                isSearching = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            fetchNearby()
        } else {
            errorMessage = "Location permission is needed to find merchants near you."
        }
    }

    fun useMyLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasLocationPermission = hasPermission

        if (hasPermission) {
            fetchNearby()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Auto-locate the moment Map mode is entered -- "the page will populate
    // a map and will be set close to my location" -- rather than waiting on
    // an extra tap. switchMode() only fires this once per entry (it early-
    // returns if the mode doesn't actually change), so re-tapping Map after
    // it's already active won't spam relocate; leaving and coming back will.
    LaunchedEffect(searchMode) {
        if (searchMode == MerchantSearchMode.MAP && lastSearchedCenter == null) {
            useMyLocation()
        }
    }

    // Offers "Search this area" once the user pans away from wherever was
    // last searched and lets go -- not while the map is still moving under
    // their finger, and not for a jitter-sized nudge.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            val last = lastSearchedCenter
            if (last != null) {
                val distanceMeters = FloatArray(1)
                Location.distanceBetween(last.latitude, last.longitude, center.latitude, center.longitude, distanceMeters)
                if (distanceMeters[0] > 400) {
                    showSearchThisArea = true
                }
            }
        }
    }

    fun save() {
        val merchant = selected ?: return
        coroutineScope.launch {
            isSaving = true
            errorMessage = null
            try {
                RetrofitClient.userApi.addMerchantToList(
                    userId = currentUser.id,
                    merchantId = merchant.id,
                    request = MerchantTargetRequest(targetAmount = targetText.toDoubleOrNull())
                )
                onAdded()
            } catch (e: Exception) {
                errorMessage = "Couldn't add that merchant right now."
            } finally {
                isSaving = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = TieyohNavy
                        )
                    }
                    Text(
                        text = "My Picks",
                        color = TieyohNavy,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Add a merchant",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TieyohNavy
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Search the Tieyoh merchant registry to add a pick to your list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TieyohMuted
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(TieyohMuted.copy(alpha = 0.10f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MerchantSearchMode.values().forEach { mode ->
                        val isActive = searchMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isActive) Color.White else Color.Transparent)
                                .clickable { switchMode(mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                mode.label,
                                fontSize = 13.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) TieyohNavy else TieyohMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (searchMode == MerchantSearchMode.MAP) {
                    if (locationServicesDisabled) {
                        TieyohSecondaryButton(
                            text = "Turn on Location",
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(TieyohMuted.copy(alpha = 0.10f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Map" to true, "List" to false).forEach { (label, isMap) ->
                            val isActive = showMapView == isMap
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isActive) Color.White else Color.Transparent)
                                    .clickable { showMapView = isMap }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) TieyohNavy else TieyohMuted
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search by name, address, city, or zip") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(4.dp))

                    // Spelled out explicitly -- not obvious from the
                    // placeholder alone once it's scrolled out of view
                    // while typing that this one box covers all four.
                    Text(
                        text = "Try a business name like \"Starbucks,\" a full or partial street address, a city, or a ZIP code.",
                        fontSize = 11.5.sp,
                        color = TieyohMuted
                    )

                    Spacer(Modifier.height(8.dp))

                    TieyohPrimaryButton(
                        text = "Search",
                        onClick = { search() }
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (searchMode == MerchantSearchMode.MAP && showMapView) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
                        ) {
                            results.forEach { merchant ->
                                val lat = merchant.latitude
                                val lng = merchant.longitude
                                if (lat != null && lng != null) {
                                    val alreadyAdded = merchant.id in existingIds
                                    Marker(
                                        state = MarkerState(position = LatLng(lat, lng)),
                                        title = merchant.brandName,
                                        snippet = if (alreadyAdded) "Already on your list" else merchant.category,
                                        onClick = {
                                            if (!alreadyAdded) selected = merchant
                                            true
                                        }
                                    )
                                }
                            }
                        }

                        if (isSearching) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = TieyohAccent)
                            }
                        }

                        if (hasSearched && results.isEmpty() && !isSearching) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("No merchants found in this area.", fontSize = 12.sp, color = TieyohMuted)
                            }
                        }

                        if (showSearchThisArea) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp, vertical = 12.dp)
                            ) {
                                TieyohSecondaryButton(
                                    text = "Search this area",
                                    onClick = {
                                        val target = cameraPositionState.position.target
                                        coroutineScope.launch {
                                            fetchNearbyAt(target.latitude, target.longitude, moveCamera = false)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    when {
                        isSearching -> Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = TieyohAccent)
                        }

                        hasSearched && results.isEmpty() -> Text(
                            if (searchMode == MerchantSearchMode.MAP)
                                "No merchants found near you."
                            else
                                "No places found -- try a different search.",
                            color = TieyohMuted,
                            fontSize = 13.sp
                        )

                        else -> LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(results, key = { it.id }) { merchant ->
                                val alreadyAdded = merchant.id in existingIds
                                val isSelected = selected?.id == merchant.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) TieyohAccent.copy(alpha = 0.10f) else Color.White)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) TieyohAccent else TieyohMuted.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable(enabled = !alreadyAdded) { selected = merchant }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MerchantLogoThumb(merchant)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(merchant.brandName, fontWeight = FontWeight.Bold, color = TieyohNavy)
                                        val subtitle = listOfNotNull(
                                            merchant.category,
                                            merchant.distanceMiles?.let { formatDistance(it) }
                                        ).joinToString("  ·  ")
                                        if (subtitle.isNotEmpty()) {
                                            Text(subtitle, fontSize = 12.sp, color = TieyohMuted)
                                        }
                                    }
                                    when {
                                        alreadyAdded -> Text("Already on your list", fontSize = 11.sp, color = TieyohMuted)
                                        isSelected -> Text("Selected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TieyohAccent)
                                    }
                                }
                            }
                        }
                    }
                }

                selected?.let { merchant ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Set a wish target for ${merchant.brandName}? (optional)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TieyohNavy
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { input -> targetText = input.filter { it.isDigit() || it == '.' } },
                        placeholder = { Text("Add now, or skip and set it later") },
                        prefix = { Text("$") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = TieyohGiftRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(14.dp))

                TieyohPrimaryButton(
                    text = "Add to My Picks",
                    onClick = { save() },
                    enabled = selected != null && !isSaving,
                    isLoading = isSaving
                )
            }
        }
    }
}
@Composable
private fun MyCardScreen(
    currentUser: AuthResponse,
    onNavigateToPayWithQr: () -> Unit,
    onNavigateToPayWithCard: () -> Unit,
    onNavigateToWalletOverview: () -> Unit,
    onNavigateToTransactionHistory: () -> Unit,
    onNavigateToActivityInsights: () -> Unit
) {
    // Tapping the small avatar next to the name blows it up full-size in
    // a dismissible overlay -- stays a small 44dp circle inline otherwise.
    var showFullPicture by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Left-justified name + @handle in place of the old "My Card" title --
        // same fonts/sizes as the name + handle on MyQrCodeScreen (17sp bold /
        // 14sp), start-aligned here instead of centered, full name
        // (currentUser.displayName is first + last). Per the screen's color
        // pass: this name and "Choose how to pay" are the two labels that
        // stay black; every other word on the screen is navy blue.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Same NetworkAvatar used for every other person's picture in
            // the app (friends list, invites, etc.) -- circular photo with
            // an initial-letter fallback, just sized down a touch (44dp)
            // to sit comfortably next to the name instead of the 56dp used
            // in a list row. Clickable here (and only here) to open the
            // full-size view below.
            NetworkAvatar(
                profilePictureBase64 = currentUser.profilePictureBase64,
                displayName = currentUser.displayName,
                size = 44.dp,
                modifier = Modifier.clickable { showFullPicture = true }
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = currentUser.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black
                )
                currentUser.tieyohId?.takeIf { it.isNotBlank() }?.let { tieyohId ->
                    // tieyohId is already stored with its official "@t-" prefix
                    // (see ChooseIdScreen's TIEYOH_ID_PREFIX) -- show it as-is
                    // rather than stripping down to a bare "@handle".
                    Text(
                        text = tieyohId,
                        color = TieyohBrandDarkBlue,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // New: opens a page listing every balance and gift card received
        // from the user's circle -- sits above "Choose how to pay" per the
        // approved mockup (white top strip with the Tieyoh logo + "My
        // Cards" label, brand-gradient lower half with a wave overlay).
        WalletOverviewCard(onClick = onNavigateToWalletOverview)

        Spacer(Modifier.height(20.dp))

        ChooseHowToPaySection(
            onPayWithQr = { onNavigateToPayWithQr() },
            onUsePrepaidCard = { onNavigateToPayWithCard() }
        )

        Spacer(Modifier.height(24.dp))

        MyCardActionsSection(
            onNavigateToTransactionHistory = onNavigateToTransactionHistory,
            onNavigateToActivityInsights = onNavigateToActivityInsights
        )
    }

    if (showFullPicture) {
        Dialog(
            onDismissRequest = { showFullPicture = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            // Full-bleed dark scrim, tap anywhere to dismiss -- same idea
            // as a standard photo-viewer overlay. The picture itself sits
            // centered at a much larger size (260dp) than the 44dp inline
            // version, same NetworkAvatar component so a missing photo
            // still falls back to the same big initial letter instead of
            // a broken image.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showFullPicture = false },
                contentAlignment = Alignment.Center
            ) {
                NetworkAvatar(
                    profilePictureBase64 = currentUser.profilePictureBase64,
                    displayName = currentUser.displayName,
                    size = 260.dp
                )
            }
        }
    }
}

// New entry point on the My Card screen: opens a page listing every
// balance and gift card the user has received from their circle. Matches
// the approved mockup -- white top strip (official Tieyoh logo + "My
// Cards" label) over a brand-gradient lower half (headline, subtitle and
// a chevron), with a visible border around the whole card and a wave
// overlay on the gradient half echoing the wave already used on the
// prepaid card face (PayWithCardScreen's CardWaveOverlay).
@Composable
private fun WalletOverviewCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(width = 1.dp, color = TieyohNavy.copy(alpha = 0.14f), shape = RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(id = R.drawable.tieyoh_logo_transparent),
                contentDescription = "Tieyoh",
                modifier = Modifier.height(34.dp)
            )
            Text(
                text = "MY CARDS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = TieyohMuted
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(TieyohBrandBlue, TieyohBrandTeal)))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            WalletOverviewCardWave(modifier = Modifier.matchParentSize())

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "See all your balances & gift cards",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "View money and gift cards you've received from your circle \u2014 all in one place.",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    WalletOverviewChevronGlyph(color = Color.White, size = 16.dp)
                }
            }
        }
    }
}

// Two soft translucent waves on the gradient half -- same cubic-Bezier
// approach as PayWithCardScreen's CardWaveOverlay, redrawn here since that
// one is private to its own file.
@Composable
private fun WalletOverviewCardWave(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val backWave = Path().apply {
            moveTo(0f, h * 0.55f)
            cubicTo(w * 0.25f, h * 0.85f, w * 0.55f, h * 0.35f, w, h * 0.65f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = backWave, color = Color.White.copy(alpha = 0.10f))

        val frontWave = Path().apply {
            moveTo(0f, h * 0.75f)
            cubicTo(w * 0.35f, h * 0.45f, w * 0.65f, h * 0.95f, w, h * 0.60f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(path = frontWave, color = Color.White.copy(alpha = 0.16f))
    }
}

// Simple chevron (">") stroke glyph -- consistent with this file's own
// convention of hand-drawn Canvas glyphs rather than Material icons.
@Composable
internal fun WalletOverviewChevronGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 16f
        val path = Path().apply {
            moveTo(6f * s, 3f * s)
            lineTo(11f * s, 8f * s)
            lineTo(6f * s, 13f * s)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// "Registered/partnered merchant" QR pay vs. "everywhere else" prepaid
// card -- the two ways to pay with Tieyoh, side by side. Both are
// placeholders (coming-soon) until the actual payment flows are built.
@Composable
private fun ChooseHowToPaySection(
    onPayWithQr: () -> Unit,
    onUsePrepaidCard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(TieyohChooseHowBg)
            .padding(vertical = 18.dp, horizontal = 14.dp)
    ) {
        Text(
            "Choose how to pay",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            PayOptionTile(
                modifier = Modifier.weight(1f),
                tileBg = TieyohMintTileBg,
                iconBg = TieyohMintIconBg,
                iconInk = TieyohNavy,
                title = "Partnered\nMerchants",
                subtitle = "Use your Tieyoh\nQR code",
                buttonLabel = "Pay with QR",
                icon = { color -> QrCodeGlyph(ink = color, bg = TieyohMintIconBg, size = 40.dp) },
                onClick = onPayWithQr
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 6.dp)
                    .width(1.dp)
                    .background(TieyohNavy.copy(alpha = 0.10f))
            )
            PayOptionTile(
                modifier = Modifier.weight(1f),
                tileBg = TieyohBlueTileBg,
                iconBg = TieyohBlueIconBg,
                iconInk = TieyohBlue,
                title = "All Other\nMerchants",
                subtitle = "Use your Tieyoh\nprepaid card",
                buttonLabel = "Pay with Card",
                buttonGradient = listOf(TieyohAccent, TieyohBlue),
                icon = { color -> BroadcastGlyph(color = color, size = 40.dp) },
                onClick = onUsePrepaidCard
            )
        }
    }
}

// The buttons here match the app's one shared button standard
// (TieyohPrimaryButton from TieyohButtons.kt -- same pill shape, height,
// and white/bold text as every other screen's buttons), not a bespoke
// pill. "Pay with QR" uses that standard component as-is. "Pay with Card"
// needs a different gradient (green -> blue instead of the app-wide
// blue -> teal) for this button only, so it draws the identical pill
// shape/size by hand with a custom Brush -- buttonGradient stays null
// everywhere else, which keeps every other button on the real
// TieyohPrimaryButton component.
@Composable
private fun PayOptionTile(
    modifier: Modifier = Modifier,
    tileBg: Color,
    iconBg: Color,
    iconInk: Color,
    title: String,
    subtitle: String,
    buttonLabel: String,
    buttonGradient: List<Color>? = null,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(tileBg)
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            icon(iconInk)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TieyohBrandDarkBlue,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        Text(
            subtitle,
            fontSize = 13.sp,
            color = TieyohBrandDarkBlue,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(10.dp))
        if (buttonGradient != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TieyohButtonHeight)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.horizontalGradient(buttonGradient))
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(buttonLabel, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        } else {
            TieyohPrimaryButton(text = buttonLabel, onClick = onClick, modifier = Modifier.fillMaxWidth())
        }
    }
}

// Everything else you can do with the card -- transaction history and
// spending insights, both full width. "Add to Wallet" was dropped
// entirely (PayWithCardScreen already has its own "Add to Wallet"
// button, so this was a duplicate) and "One-Time Virtual Card" moved
// into PayWithCardScreen's own action list, since generating a virtual
// card number is something you do with the prepaid card specifically,
// not a generic My Card action. Both remaining rows are coming-soon
// placeholders until their own screens are built. ("View My QR Code" was
// dropped from here too -- it duplicated the "Pay with QR" option
// above.)
@Composable
private fun MyCardActionsSection(
    onNavigateToTransactionHistory: () -> Unit,
    onNavigateToActivityInsights: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Full width rather than squeezed into a half-width column -- root
        // cause of the earlier "Transactio / n" wrap: at this font size
        // "Transaction" (11 letters, no natural break point) is simply
        // wider than a half-width tile can ever offer once you subtract
        // the icon circle, its spacing, and the arrow -- so no choice of
        // manual line break or font tweak inside that width fixes it
        // reliably across devices/font scales, only more width does. Full
        // width gives every device comfortable room for the whole word, so
        // this doesn't depend on screen size or system font scale to look
        // right.
        MyCardActionRow(
            modifier = Modifier.fillMaxWidth(),
            title = "Transaction History",
            subtitle = "View and search",
            icon = { color -> HistoryGlyph(color = color, size = 26.dp) },
            onClick = onNavigateToTransactionHistory
        )
        MyCardActionRow(
            modifier = Modifier.fillMaxWidth(),
            title = "Activity & Insights",
            subtitle = "See your spending and impact",
            icon = { color -> InsightsGlyph(color = color, size = 26.dp) },
            onClick = onNavigateToActivityInsights
        )
    }
}

@Composable
internal fun MyCardActionRow(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(TieyohBlueTileBg),
            contentAlignment = Alignment.Center
        ) {
            icon(TieyohBlue)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = TieyohBrandDarkBlue, lineHeight = 19.sp)
            Text(subtitle, fontSize = 13.sp, color = TieyohBrandDarkBlue)
        }
        Spacer(Modifier.width(4.dp))
        Text("›", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TieyohBrandDarkBlue)
    }
}

// -- "My Card" glyphs -------------------------------------------------
// Hand-drawn with Canvas rather than a drawable/vector asset, matching how
// the rest of the app draws its custom icons (see the send-FAB seam
// above). `bg` on QrCodeGlyph is the color behind the icon circle it's
// drawn on -- used to punch the finder-pattern "holes" so they read as
// hollow squares rather than solid ones.

@Composable
private fun QrCodeGlyph(ink: Color, bg: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 24f
        fun sq(x: Float, y: Float, w: Float, color: Color) {
            drawRect(color = color, topLeft = Offset(x * s, y * s), size = Size(w * s, w * s))
        }
        fun finder(x: Float, y: Float) {
            sq(x, y, 7f, ink)
            sq(x + 1.4f, y + 1.4f, 4.2f, bg)
            sq(x + 2.6f, y + 2.6f, 1.8f, ink)
        }
        finder(1f, 1f)
        finder(16f, 1f)
        finder(1f, 16f)
        sq(16f, 16f, 2.6f, ink)
        sq(19.4f, 16f, 2.6f, ink)
        sq(16f, 19.4f, 2.6f, ink)
        sq(19.4f, 19.4f, 2.6f, bg)
        sq(11.5f, 1f, 2.4f, ink)
        sq(1f, 11.5f, 2.4f, ink)
        sq(11f, 11f, 2.2f, ink)
        sq(16f, 11f, 2.2f, ink)
        sq(11f, 16f, 2.2f, ink)
    }
}

@Composable
private fun BroadcastGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 24f
        val stroke = Stroke(width = 2.2f * s, cap = StrokeCap.Round)
        fun arc(r: Float) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset((6f - r) * s, (18f - r) * s),
                size = Size(2 * r * s, 2 * r * s),
                style = stroke
            )
        }
        arc(4f)
        arc(7.5f)
        arc(11f)
        drawCircle(color = color, radius = 1.5f * s, center = Offset(6f * s, 18f * s))
    }
}

@Composable
internal fun VirtualCardGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 24f
        val strokeW = 1.8f * s
        drawRoundRect(
            color = color,
            topLeft = Offset(2f * s, 5f * s),
            size = Size(20f * s, 14f * s),
            cornerRadius = CornerRadius(2.5f * s, 2.5f * s),
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = color,
            start = Offset(2f * s, 9.5f * s),
            end = Offset(22f * s, 9.5f * s),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(6f * s, 14.5f * s),
            end = Offset(10f * s, 14.5f * s),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun HistoryGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 24f
        val strokeW = 1.8f * s
        drawCircle(
            color = color,
            radius = 9f * s,
            center = Offset(12f * s, 12f * s),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        val hands = Path().apply {
            moveTo(12f * s, 7f * s)
            lineTo(12f * s, 12f * s)
            lineTo(15.5f * s, 14f * s)
        }
        drawPath(
            path = hands,
            color = color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun InsightsGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 24f
        val strokeW = 1.8f * s
        drawLine(color = color, start = Offset(5f * s, 21f * s), end = Offset(5f * s, 13f * s), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(12f * s, 21f * s), end = Offset(12f * s, 9f * s), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(19f * s, 21f * s), end = Offset(19f * s, 5f * s), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

@Composable
private fun MerchantSearchScreen(
    title: String,
    subtitle: String,
    actionLabel: String,
    onMerchantSelected: ((MerchantDto) -> Unit)? = null,
    onBack: () -> Unit = {},
    // Static per-call-site label, same reasoning as PaymentMethodSetupScreen's
    // backLabel above -- this composable is shared by two call sites
    // (SEARCH_MERCHANT and SEND_GIFT_MERCHANT_SEARCH) that return to
    // genuinely different screens.
    backLabel: String = "Feed"
) {

    // One combined field for name/address/city/zip, same shape
    // RetrofitClient.merchantApi.searchMerchants takes now -- matches the
    // single-box search used by MerchantSelectionScreen and the "Add a
    // merchant" sheet elsewhere in the app. Merchant type stays its own
    // field since it's a separate, independently-ANDed category filter.
    var searchText by remember { mutableStateOf("") }
    var merchantType by remember { mutableStateOf("") }

    var merchants by remember {
        mutableStateOf<List<MerchantDto>>(emptyList())
    }

    var selectedMerchant by remember {
        mutableStateOf<MerchantDto?>(null)
    }

    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
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
                text = backLabel,
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TieyohMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Name, address, city, or ZIP") },
            placeholder = { Text("e.g. \"Starbucks\", a street address, a city, or a ZIP code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = merchantType,
            onValueChange = { merchantType = it },
            label = { Text("Merchant type (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        TieyohPrimaryButton(
            text = actionLabel,
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    hasSearched = true
                    errorMessage = null
                    selectedMerchant = null

                    try {
                        merchants = RetrofitClient.merchantApi.searchMerchants(
                            q = searchText.trim().ifBlank { null },
                            merchantType = merchantType.trim().ifBlank { null }
                        )
                    } catch (e: Exception) {
                        merchants = emptyList()
                        errorMessage = e.message ?: "Unable to search merchants"
                    } finally {
                        isLoading = false
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        selectedMerchant?.let { merchant ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TieyohCardBg)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Selected merchant",
                        style = MaterialTheme.typography.labelMedium,
                        color = TieyohMuted
                    )

                    Text(
                        text = merchant.brandName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TieyohNavy
                    )

                    merchant.category?.let { category ->
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodySmall,
                            color = TieyohMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TieyohAccent)
                }
            }

            errorMessage != null -> {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error
                )
            }

            hasSearched && merchants.isEmpty() -> {
                Text("No eligible merchants found.", color = TieyohMuted)
            }

            merchants.isNotEmpty() -> {
                Text(
                    text = "${merchants.size} merchant(s) found",
                    style = MaterialTheme.typography.labelLarge,
                    color = TieyohNavy
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = merchants,
                        key = { merchant -> merchant.id }
                    ) { merchant ->
                        MerchantResultCard(
                            merchant = merchant,
                            selected = selectedMerchant?.id == merchant.id,
                            onSelect = {
                                selectedMerchant = merchant
                                onMerchantSelected?.invoke(merchant)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MerchantResultCard(
    merchant: MerchantDto,
    selected: Boolean,
    onSelect: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) TieyohCardBg else Color.White)
            .border(1.dp, TieyohBorder, RoundedCornerShape(16.dp))
            .clickable { onSelect() }
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            MerchantLogoThumb(merchant)

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = merchant.brandName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TieyohNavy
                )

                merchant.category?.let { category ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = TieyohMuted
                    )
                }

                merchant.tmid?.let { tmid ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tmid,
                        style = MaterialTheme.typography.labelSmall,
                        color = TieyohMuted
                    )
                }
            }

            Text(
                text = if (selected) "Selected" else "›",
                style = MaterialTheme.typography.labelLarge,
                color = TieyohAccent
            )
        }
    }
}

@Composable
private fun ProfileScreen(
    currentUser: AuthResponse,
    onLogout: () -> Unit,
    onManagePaymentMethods: () -> Unit,
    onEditProfile: () -> Unit,
    onPrivacySettings: () -> Unit,
    onViewQrCode: () -> Unit = {},
    onProfileUpdated: (AuthResponse) -> Unit = {}
) {

    val context = LocalContext.current
    val profileCoroutineScope = rememberCoroutineScope()
    var photoVersion by remember { mutableStateOf(0) }
    // Prefer the just-picked local cache (instant, no network wait), but
    // it's keyed by this account's id so switching accounts on the same
    // device never shows a leftover cache from someone else -- see
    // ProfilePhotoStore. Falling back to currentUser.profilePictureBase64
    // (the backend's per-account copy) covers a user who has a photo set
    // from another device but hasn't picked one on this device yet.
    val photoBitmap = remember(photoVersion, currentUser.id, currentUser.profilePictureBase64) {
        ProfilePhotoStore.load(context, currentUser.id)
            ?: currentUser.profilePictureBase64?.let { decodeBase64ToBitmap(it) }
    }

    // Auto-accept circle invites (and the newer "share gifting with your
    // circle" setting) now live in Edit Profile's Privacy section instead
    // of here -- see EditProfileScreen -- so this screen no longer needs
    // to hold that toggle's state itself.

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (ProfilePhotoStore.save(context, currentUser.id, uri)) {
                photoVersion++
                // Sync it to the server too, in the background, so this
                // picture shows up for other people (e.g. in the invite
                // search results) -- not just on this device.
                profileCoroutineScope.launch {
                    try {
                        val bytes = ProfilePhotoStore.readBytes(context, currentUser.id)
                        if (bytes != null) {
                            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            val updated = RetrofitClient.userApi.updateProfilePicture(
                                userId = currentUser.id,
                                request = ProfilePictureRequest(imageBase64 = encoded)
                            )
                            onProfileUpdated(updated)
                        }
                    } catch (e: Exception) {
                        // Best-effort sync -- the picture still shows locally
                        // even if this fails, so just skip it.
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(TieyohCardBg)
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoBitmap != null) {
                            Image(
                                bitmap = photoBitmap.asImageBitmap(),
                                contentDescription = "Profile picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = currentUser.displayName.firstOrNull()?.uppercase() ?: "?",
                                color = TieyohAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            )
                        }

                        // Small camera badge signaling the avatar is tappable.
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(TieyohNavy),
                            contentAlignment = Alignment.Center
                    ) {
                        Text(text = "\uD83D\uDCF7", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = currentUser.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TieyohNavy
                )
            }

                Spacer(modifier = Modifier.height(12.dp))

                currentUser.tieyohId?.let { tieyohId ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tieyohId,
                                color = TieyohAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Scan to add me",
                                color = TieyohMuted,
                                fontSize = 12.sp
                            )
                        }

                        val qrBitmap = remember(tieyohId) { QrCodeGenerator.generate(tieyohId) }
                        qrBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR code for $tieyohId",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clickable { onViewQrCode() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = currentUser.email,
                    color = TieyohMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TieyohCardBg)
                .clickable(onClick = onEditProfile)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Edit profile", fontWeight = FontWeight.Bold, color = TieyohNavy)
                Text(
                    text = "Email, phone, address, birthday, and holidays",
                    color = TieyohMuted,
                    fontSize = 13.sp
                )
            }
            Text(text = "Edit", color = TieyohAccent, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TieyohCardBg)
                .clickable(onClick = onPrivacySettings)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Privacy", fontWeight = FontWeight.Bold, color = TieyohNavy)
                Text(
                    text = "Auto-accept circle invites, share gifting with your circle",
                    color = TieyohMuted,
                    fontSize = 13.sp
                )
            }
            Text(text = "Edit", color = TieyohAccent, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val savedPaymentMethods = remember { PaymentMethodStore.load(context) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(TieyohCardBg)
                .clickable(onClick = onManagePaymentMethods)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Payment methods", fontWeight = FontWeight.Bold, color = TieyohNavy)
                Text(
                    text = if (savedPaymentMethods.isEmpty()) {
                        "None added yet"
                    } else {
                        savedPaymentMethods.joinToString { "${it.label} •••• ${it.last4}" }
                    },
                    color = TieyohMuted,
                    fontSize = 13.sp
                )
            }
            Text(
                text = if (savedPaymentMethods.isEmpty()) "Add" else "Edit",
                color = TieyohAccent,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TieyohSecondaryButton(text = "Log out", onClick = onLogout)

        Spacer(modifier = Modifier.height(16.dp))

        // App version (REQ-002) -- last item on the screen, read from
        // BuildConfig, never typed here. Plain Text: not clickable, and
        // TalkBack reads it as-is ("Version 1.x").
        Text(
            text = AppVersion.displayLabel,
            fontWeight = FontWeight.Bold,
            color = TieyohNavy
        )
    }
}
