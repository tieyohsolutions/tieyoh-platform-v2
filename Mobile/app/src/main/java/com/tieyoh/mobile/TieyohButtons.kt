package com.tieyoh.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohBrandBlue
import com.tieyoh.mobile.ui.theme.TieyohBrandTeal
import com.tieyoh.mobile.ui.theme.TieyohMuted

// The one button standard for the whole app: same pill shape, height, and
// color scheme everywhere a button appears, matching what the redesigned
// onboarding/profile/payment screens already settled on (Continue, Go to
// Feed, Skip for Now, Maybe Later, Don't Allow, and so on). Every other
// screen's buttons are being switched over to these two so the whole app
// uses one visual language for buttons -- wording is never touched by that
// switch, only the look.
//
// TieyohPrimaryButton = the gradient pill (main/confirming action).
// TieyohSecondaryButton = the white pill with a blue outline (secondary/
// skip/cancel action). Height and shape are the same everywhere, always --
// that's the "same size schema" part. Width defaults to filling the row
// (the normal case: a screen's main CTA), but a caller placing one inline
// -- next to a name in a list row, say -- can pass fillWidth = false to get
// a compact, content-hugging pill instead, still the same height/shape/
// colors as every other button. modifier still applies either way, e.g.
// Modifier.weight(1f) for two buttons side by side.
private val TieyohButtonShape = RoundedCornerShape(26.dp)
val TieyohButtonHeight = 52.dp
private val TieyohButtonGradient = Brush.horizontalGradient(listOf(TieyohBrandBlue, TieyohBrandTeal))
private val TieyohButtonDisabledGradient = Brush.horizontalGradient(
    listOf(TieyohMuted.copy(alpha = 0.35f), TieyohMuted.copy(alpha = 0.35f))
)

@Composable
fun TieyohPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    fillWidth: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    // Overrides the default blue->teal gradient with a solid fill (e.g.
    // a dark-navy CTA that still needs to read as this same button
    // standard -- same pill/height/font, just a different color for that
    // one screen). Null keeps the normal gradient. Ignored when disabled,
    // which always shows the muted gray regardless.
    backgroundColor: Color? = null,
    // Escape hatch for a caller that needs one of the app's own
    // hand-drawn Canvas glyphs (e.g. WalletGlyph) instead of a plain
    // Material ImageVector as the leading icon -- takes over the leading
    // icon slot entirely when set (icon is ignored), same 18dp-ish sizing
    // expected of whatever's passed in.
    leadingContent: (@Composable () -> Unit)? = null,
    // Per-instance override for a button that needs to read as bigger/
    // more prominent than the app's normal pill (e.g. a hero CTA).
    // Defaults to the standard height everywhere else.
    height: Dp = TieyohButtonHeight
) {
    Box(
        modifier = modifier
            .let { if (fillWidth) it.fillMaxWidth() else it }
            .height(height)
            .clip(TieyohButtonShape)
            .let {
                when {
                    !enabled -> it.background(TieyohButtonDisabledGradient)
                    backgroundColor != null -> it.background(backgroundColor)
                    else -> it.background(TieyohButtonGradient)
                }
            }
            .clickable(enabled = enabled && !isLoading) { onClick() }
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
        } else if (leadingContent != null || icon != null || trailingIcon != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingContent != null) {
                    leadingContent()
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = trailingIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun TieyohSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .let { if (fillWidth) it.fillMaxWidth() else it }
            .height(TieyohButtonHeight)
            .clip(TieyohButtonShape)
            .background(Color.White)
            .border(width = 1.5.dp, color = TieyohBrandBlue, shape = TieyohButtonShape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null || trailingIcon != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, tint = TieyohBrandBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text, color = TieyohBrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = trailingIcon, contentDescription = null, tint = TieyohBrandBlue, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            Text(text, color = TieyohBrandBlue, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

// A plain-text link styled like the button labels (e.g. "Maybe Later" when
// it's a bare link rather than a bordered pill in a given layout) -- same
// color and weight as TieyohSecondaryButton's text, no box around it.
@Composable
fun TieyohTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Text(
        text = text,
        color = TieyohBrandBlue,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        modifier = modifier.clickable { onClick() }
    )
}
