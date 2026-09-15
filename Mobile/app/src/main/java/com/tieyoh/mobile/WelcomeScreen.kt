package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tieyoh.mobile.ui.theme.TieyohBrandBg
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal

// The very first screen a logged-out person sees, ahead of LoginScreen --
// per the Sep 2026 onboarding/login redesign reference. Replaces jumping
// straight into the login/signup form: just the brand moment plus a clear
// fork (Create Account vs. Log In), matching the mockup screen-for-screen
// (real logo + gift-box art, not redrawn). onCreateAccount/onLogIn just
// pick which mode LoginScreen opens in -- this screen has no auth logic
// of its own.
@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onLogIn: () -> Unit,
    onNeedHelp: () -> Unit = {}
) {
    // Only the text/buttons get a horizontal inset now -- the logo and
    // gift art are sized against the full screen width (fillMaxWidth
    // fractions below) rather than the padded content width, since "make
    // it bigger" had already hit the padded width's ceiling once. This is
    // close to the physical maximum before either image would start
    // bleeding past the screen edge.
    val contentPadding = Modifier.padding(horizontal = 24.dp)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        Image(
            painter = painterResource(id = R.drawable.tieyoh_logo),
            contentDescription = "Tieyoh",
            modifier = Modifier.fillMaxWidth(0.70f)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Thoughtful gifting.\nStronger connections.",
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 33.sp,
            color = TieyohBrandNavy,
            textAlign = TextAlign.Center,
            modifier = contentPadding
        )

        // The gift art is the hero of this screen -- giving this whole
        // block its own weight (rather than sizing the image bigger still)
        // lets it center in whatever room is actually left above the
        // buttons, on any screen height, instead of just being one more
        // item in a top-loaded stack.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // fillMaxWidth() alone rendered this at close to its raw
            // intrinsic pixel size in practice (the PNG is 273x219) --
            // pinning height to width via aspectRatio forces Compose to
            // actually scale it up to the available width instead, which
            // is what "bigger, more impact" needs here.
            Image(
                painter = painterResource(id = R.drawable.gift_box),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(273f / 219f)
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "Send money with purpose, celebrate moments, and stay close — all in one place.",
                fontSize = 17.sp,
                lineHeight = 25.sp,
                color = TieyohBrandNavy,
                textAlign = TextAlign.Center,
                modifier = contentPadding.widthIn(max = 320.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Both buttons now share the same pill shape and the same brand
        // gradient (blue -> teal): Create Account fills with it, Log In
        // takes it as an outline instead of the old flat-gray fill, so the
        // two read as a matched pair rather than one styled and one not.
        val pillShape = RoundedCornerShape(26.dp)
        val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

        Box(
            modifier = contentPadding
                .fillMaxWidth()
                .height(52.dp)
                .clip(pillShape)
                .background(brandGradient)
                .clickable { onCreateAccount() },
            contentAlignment = Alignment.Center
        ) {
            Text("Create Account", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = contentPadding
                .fillMaxWidth()
                .height(52.dp)
                .clip(pillShape)
                .background(Color.White)
                .border(width = 1.5.dp, brush = brandGradient, shape = pillShape)
                .clickable { onLogIn() },
            contentAlignment = Alignment.Center
        ) {
            Text("Log In", color = TieyohBrandNavy, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Need help?",
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = TieyohBrandBlue,
            modifier = Modifier.clickable { onNeedHelp() }
        )

        Spacer(Modifier.height(16.dp))

        // Step indicator for the account-creation flow this screen kicks
        // off (Welcome -> Let's get started -> Verify code -> profile
        // basics) -- purely decorative here, this screen is step 1 of 4;
        // it doesn't advance on its own.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == 0) 20.dp else 6.dp, 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index == 0) TieyohBrandBlue else TieyohBrandBg)
                )
            }
        }
    }
}
