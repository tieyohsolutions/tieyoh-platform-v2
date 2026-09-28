package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohGiftRed
import com.tieyoh.mobile.ui.theme.TieyohMuted
import kotlinx.coroutines.launch

// "@t-" is a fixed label, same convention as the pre-existing
// ProfileSetupScreen -- only the part after the dash is the user's to
// choose. Kept as its own constant here rather than sharing that
// screen's private one.
private const val TIEYOH_ID_PREFIX = "@t-"
private val HANDLE_REGEX = Regex("^[A-Za-z0-9_]{2,15}$")

private enum class IdCheckState { IDLE, AVAILABLE, TAKEN }

// First screen of profile setup, right after account creation (Welcome ->
// Let's get started -> Verify it's you -> Create a password -> here).
// Matches the "Choose your unique @t ID" reference image: fonts, colors,
// and sizes follow the same tokens as the rest of the Sep 2026 redesign.
//
// Checking availability is its own explicit "Create ID" step now, separate
// from Continue -- tapping Create ID runs the check and shows the result
// inline; Continue stays disabled until that check has come back
// available, and re-disables itself if the handle is edited afterward.
// This only validates and hands the chosen id back via onContinue -- it
// does not itself call saveProfile, since that endpoint still requires
// phone/gender together with the id in one request (see the later steps
// in the reference flow -- Privacy Explained, Disclosure & Consent, etc.
// -- which aren't built yet).
@Composable
fun ChooseIdScreen(
    initialHandle: String,
    initialAvailable: Boolean,
    onHandleStateChanged: (handle: String, available: Boolean) -> Unit,
    onBack: () -> Unit,
    onContinue: (fullTieyohId: String) -> Unit
) {
    var handle by remember { mutableStateOf(initialHandle) }
    var checkState by remember { mutableStateOf(if (initialAvailable) IdCheckState.AVAILABLE else IdCheckState.IDLE) }
    var isChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Reports the latest handle/availability up to the caller so it survives
    // this screen leaving and re-entering composition (e.g. Continue then
    // Back) instead of resetting to blank every time.
    LaunchedEffect(handle, checkState) {
        onHandleStateChanged(handle, checkState == IdCheckState.AVAILABLE)
    }

    fun onHandleChanged(new: String) {
        handle = new.filter { it.isLetterOrDigit() || it == '_' }
        checkState = IdCheckState.IDLE
        errorMessage = null
    }

    fun checkAvailability() {
        errorMessage = null

        if (!handle.matches(HANDLE_REGEX)) {
            checkState = IdCheckState.IDLE
            errorMessage = "Your ID must be 2-15 characters (letters, numbers, underscore)."
            return
        }

        scope.launch {
            isChecking = true
            try {
                val result = RetrofitClient.userApi.checkIdAvailable(TIEYOH_ID_PREFIX + handle)
                if (result.available) {
                    checkState = IdCheckState.AVAILABLE
                } else {
                    checkState = IdCheckState.TAKEN
                    errorMessage = "ID is used, choose another one"
                }
            } catch (e: Exception) {
                checkState = IdCheckState.IDLE
                errorMessage = "Couldn't reach the server. Is tieyoh-api running?"
            } finally {
                isChecking = false
            }
        }
    }

    val canCheck = handle.isNotBlank() && !isChecking
    val canContinue = checkState == IdCheckState.AVAILABLE

    Box(modifier = Modifier.fillMaxSize()) {

        // Same decorative wave as CreatePasswordScreen.
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
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = TieyohNavy
                    )
                }
                Text(
                    text = "Welcome",
                    color = TieyohNavy,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            Image(
                painter = painterResource(id = R.drawable.tieyoh_logo),
                contentDescription = "Tieyoh",
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Choose your unique @t ID",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "This is your Tieyoh handle. Friends can find and connect with you.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = handle,
                onValueChange = { onHandleChanged(it) },
                prefix = {
                    Text(
                        text = TIEYOH_ID_PREFIX,
                        fontSize = 16.sp,
                        color = TieyohBrandNavy
                    )
                },
                singleLine = true,
                trailingIcon = {
                    if (checkState == IdCheckState.AVAILABLE) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Available",
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

            Spacer(Modifier.height(14.dp))

            val pillShape = RoundedCornerShape(26.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(pillShape)
                    .border(
                        width = 1.5.dp,
                        color = if (canCheck) TieyohBrandBlue else TieyohMuted.copy(alpha = 0.35f),
                        shape = pillShape
                    )
                    .clickable(enabled = canCheck) { checkAvailability() },
                contentAlignment = Alignment.Center
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TieyohBrandBlue)
                } else {
                    Text(
                        text = "Create ID",
                        color = if (canCheck) TieyohBrandBlue else TieyohMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            if (checkState == IdCheckState.AVAILABLE) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Great! This @t ID is available.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TieyohBrandTeal,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TieyohGiftRed,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.weight(1f))

            val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(if (canContinue) brandGradient else Brush.horizontalGradient(listOf(TieyohMuted.copy(alpha = 0.35f), TieyohMuted.copy(alpha = 0.35f))))
                    .clickable(enabled = canContinue) { onContinue(TIEYOH_ID_PREFIX + handle) },
                contentAlignment = Alignment.Center
            ) {
                Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
