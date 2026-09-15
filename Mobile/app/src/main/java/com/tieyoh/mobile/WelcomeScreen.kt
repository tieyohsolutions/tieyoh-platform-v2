package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal

/**
 * Welcome / brand-moment screen — first screen of the logged-out onboarding flow.
 * Rebuilt from the Sep 2026 onboarding/login redesign notes (tieyoh-onboarding-redesign-progress.md,
 * tieyoh-design-tokens.md) after the original implementation was lost (never committed to this repo).
 *
 * Only this screen has been rebuilt so far. onCreateAccount/onLogIn are temporary hooks into the
 * existing dashboard prototype (TieyohApp) until GetStartedScreen/LoginScreen/etc. are rebuilt too.
 */
@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onLogIn: () -> Unit,
    onNeedHelp: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TieyohBrandBg)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Image(
            painter = painterResource(id = R.drawable.tieyoh_logo),
            contentDescription = "Tieyoh",
            modifier = Modifier.height(46.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Thoughtful gifting.\nStronger connections.",
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 33.sp,
            color = TieyohBrandNavy,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(id = R.drawable.welcome_gift_card),
            contentDescription = "Gift box opening to reveal a Tieyoh card",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(220.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Send money with purpose, celebrate moments, and stay close — all in one place.",
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = TieyohBrandNavyLight2,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))
                )
                .clickable { onCreateAccount() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Create Account",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(1.5.dp, TieyohBrandNavy, RoundedCornerShape(26.dp))
                .clickable { onLogIn() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log In",
                color = TieyohBrandNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Need help?",
            color = TieyohBrandBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.clickable { onNeedHelp() }
        )

        Spacer(modifier = Modifier.height(18.dp))

        WelcomeStepDots(activeIndex = 0, stepCount = 4)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun WelcomeStepDots(activeIndex: Int, stepCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until stepCount) {
            val isActive = i == activeIndex
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (isActive) 22.dp else 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isActive) TieyohBrandBlue else Color(0xFFD9DFE5))
            )
        }
    }
}
