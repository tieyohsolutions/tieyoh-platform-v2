package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal

// The very last screen of onboarding -- shown once, right after payment
// method setup (and biometric enrollment, when the device offers it),
// immediately before landing on the Feed. Matches the "You're all set!"
// reference image: fonts, colors, sizes, and the celebratory checkmark
// illustration follow the same tokens as the rest of the redesign.
//
// Wired in MainActivity via a new SessionState.OnboardingComplete step
// that both the "no biometric enrollment" branch of enterApp() and
// NeedsBiometricEnroll's onDone now route through on their way to
// LoggedIn, so this shows up last regardless of which path got the user
// here. onBack has nothing meaningful to return to at this final step --
// same judgment call as the other screens with no clean prior step --
// so it defaults to onGoToFeed rather than being left broken.
@Composable
fun OnboardingCompleteScreen(
    onGoToFeed: () -> Unit,
    onBack: () -> Unit = onGoToFeed
) {
    val pillShape = RoundedCornerShape(26.dp)
    val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

    Box(modifier = Modifier.fillMaxSize()) {

        // Same decorative wave as the other redesigned screens.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1.4f)
                .height(120.dp)
                .offset(y = 70.dp)
                .clip(RoundedCornerShape(topStart = 600.dp, topEnd = 600.dp))
                .background(TieyohBrandTeal.copy(alpha = 0.10f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(1.1f)
                .height(90.dp)
                .offset(y = 55.dp)
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

            Spacer(Modifier.weight(1f))

            Text(
                text = "You're all set!",
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Thanks for joining Tieyoh. You're now ready to explore, connect, and share.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            // The actual reference artwork (checkmark badge + confetti),
            // background removed -- not a Compose approximation. The
            // confetti burst isn't symmetric around the artwork's own crop
            // box, so the drawable is padded with transparent margin to
            // put the checkmark circle itself at the exact center of the
            // image (verified against a crosshair test render) -- centering
            // the *frame* alone left the circle visibly off to one side.
            Image(
                painter = painterResource(id = R.drawable.onboarding_complete_icon),
                contentDescription = "You're all set",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .aspectRatio(351f / 225f)
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "A happier, more connected\nyou starts here.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(brandGradient)
                    .clickable { onGoToFeed() },
                contentAlignment = Alignment.Center
            ) {
                Text("Go to Feed", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
