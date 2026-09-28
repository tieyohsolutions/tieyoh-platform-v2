package com.tieyoh.mobile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandDarkBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandNavy
import com.tieyoh.mobile.ui.theme.TieyohNavy
import com.tieyoh.mobile.ui.theme.TieyohBrandNavyLight2
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohMuted

private data class PrivacyPoint(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

private val privacyPoints = listOf(
    PrivacyPoint(
        Icons.Filled.Lock,
        "Your Information is Protected",
        "We use industry-standard encryption to keep your data safe."
    ),
    PrivacyPoint(
        Icons.Filled.Person,
        "You're in Control",
        "You choose what to share and can update your preferences anytime."
    ),
    PrivacyPoint(
        Icons.Filled.Visibility,
        "We Don't Share Without Your Consent",
        "We never sell your personal information and only share it as needed to provide our services."
    )
)

// Step in profile setup right after choosing an @t id (Choose your @t id
// -> here -> the rest of profile setup). Matches the "Your Privacy is Our
// Priority" reference image: fonts, colors, and sizes follow the same
// tokens as the rest of the Sep 2026 redesign. Continue stays disabled
// until the consent checkbox is checked. The Terms of Service / Privacy
// Policy links both open the same placeholder page for now -- real legal
// copy is coming later from the user, so that page just shows the two
// titles.
@Composable
fun PrivacyExplainedScreen(
    initialAgreed: Boolean,
    onAgreedChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var agreed by remember { mutableStateOf(initialAgreed) }
    var showLegalPage by remember { mutableStateOf(false) }

    // Reports the checkbox state up to the caller so it survives this screen
    // leaving and re-entering composition instead of resetting unchecked.
    LaunchedEffect(agreed) {
        onAgreedChanged(agreed)
    }

    if (showLegalPage) {
        LegalPlaceholderScreen(onBack = { showLegalPage = false })
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Same decorative wave as the other post-signup screens.
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
                    text = "Choose ID",
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
                text = "Your Privacy is Our Priority",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = TieyohBrandDarkBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "We're committed to keeping your information secure, private, and in your control.",
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = TieyohBrandNavyLight2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            privacyPoints.forEachIndexed { index, point ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(TieyohBrandBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = point.icon,
                            contentDescription = null,
                            tint = TieyohBrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = point.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TieyohBrandNavy
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = point.description,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            color = TieyohBrandNavyLight2
                        )
                    }
                }
                if (index != privacyPoints.lastIndex) {
                    Spacer(Modifier.height(18.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TieyohMuted.copy(alpha = 0.25f))
            )

            Spacer(Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = agreed,
                    onCheckedChange = { agreed = it },
                    colors = CheckboxDefaults.colors(checkedColor = TieyohBrandBlue),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))

                val annotatedText = buildAnnotatedString {
                    append("I have read and agree to Tieyoh's ")
                    pushStringAnnotation(tag = "LEGAL", annotation = "legal")
                    withStyle(style = SpanStyle(color = TieyohBrandBlue, fontWeight = FontWeight.SemiBold)) {
                        append("Terms of Service")
                    }
                    pop()
                    append(" and ")
                    pushStringAnnotation(tag = "LEGAL", annotation = "legal")
                    withStyle(style = SpanStyle(color = TieyohBrandBlue, fontWeight = FontWeight.SemiBold)) {
                        append("Privacy Policy")
                    }
                    pop()
                    append(".")
                }

                ClickableText(
                    text = annotatedText,
                    style = TextStyle(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TieyohBrandNavyLight2
                    ),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "LEGAL", start = offset, end = offset)
                            .firstOrNull()?.let {
                                showLegalPage = true
                            }
                    },
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            val pillShape = RoundedCornerShape(26.dp)
            val brandGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(pillShape)
                    .background(if (agreed) brandGradient else Brush.horizontalGradient(listOf(TieyohMuted.copy(alpha = 0.35f), TieyohMuted.copy(alpha = 0.35f))))
                    .clickable(enabled = agreed) { onContinue() },
                contentAlignment = Alignment.Center
            ) {
                Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// Placeholder for the Terms of Service / Privacy Policy links above --
// real legal copy comes later from the user; this just shows the two
// titles with room between them, per their explicit note.
@Composable
fun LegalPlaceholderScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
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
                text = "Privacy",
                color = TieyohNavy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Terms of Service",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = TieyohBrandDarkBlue
        )

        Spacer(Modifier.height(40.dp))

        Text(
            text = "Privacy Policy",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = TieyohBrandDarkBlue
        )
    }
}
