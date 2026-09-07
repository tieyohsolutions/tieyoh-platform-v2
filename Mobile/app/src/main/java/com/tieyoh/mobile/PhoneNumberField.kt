package com.tieyoh.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohCardBg
import com.tieyoh.mobile.ui.theme.TieyohMuted
import com.tieyoh.mobile.ui.theme.TieyohNavy

// A phone entry field standardized across every screen that collects one
// (profile setup, the "invite by phone" flow): a tappable country chip
// (flag + dial code) next to a text field that only accepts digits, grouped
// and dashed the way that country's numbers are normally written (see
// Countries.kt). The value this exposes via onValueChange is always the
// full "+<dialcode><digits>" string -- what actually gets sent to and
// stored by the backend -- never the punctuated display text.
@Composable
fun PhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Phone number",
    // Optional focus/border color override -- defaults to the field's
    // original (theme-default) look so every existing call site renders
    // exactly as before. The onboarding redesign screens pass brand blue
    // here explicitly instead of this shared field silently inheriting
    // whatever MaterialTheme.colorScheme.primary happens to be.
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    val context = LocalContext.current
    // The country picker chip used to sit in front of this field, but a
    // visible "+1" was making phone screens look cluttered. Tieyoh only
    // supports US/Canada numbers for now, and both format identically
    // (+1, xxx-xxx-xxxx), so there's nothing to actually choose between --
    // the dial code is applied silently instead. This still resolves
    // through defaultCountryForDevice so a future location-based lookup
    // (or more countries) can slot in here without touching call sites.
    val country = remember { defaultCountryForDevice(context) }

    fun digitsFrom(raw: String, c: Country): String =
        if (raw.startsWith(c.dialCode)) {
            raw.removePrefix(c.dialCode).filter { it.isDigit() }
        } else {
            raw.filter { it.isDigit() }
        }

    // The field's actual (untransformed) text is always plain digits --
    // dashes are added only for display, via PhoneNumberVisualTransformation
    // below, never written into the text being edited. Previously this
    // field reformatted the real text with dashes on every keystroke, which
    // desynced the field's cursor from where the user was actually typing
    // and scrambled digits as more were typed (e.g. typing "314-134-5678"
    // could come out as "314-345-7861"). Keeping the underlying text
    // digit-only and letting VisualTransformation own the punctuation is
    // what keeps the cursor -- and the digit order -- correct.
    var fieldValue by remember {
        val initialDigits = digitsFrom(value, country)
        mutableStateOf(TextFieldValue(text = initialDigits, selection = TextRange(initialDigits.length)))
    }

    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            Text(text = label, fontSize = 12.sp, color = TieyohMuted)
            Spacer(modifier = Modifier.height(4.dp))
        }
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val maxLen = maxDigits(country)
                // newValue.text/selection are already in "raw" (untransformed)
                // coordinates -- Compose maps visual clicks/edits back to the
                // real text for us, so we only need to strip anything that
                // isn't a digit and re-count the cursor in digit terms.
                val digitsBeforeCursor = newValue.text.take(newValue.selection.end).count { it.isDigit() }
                val digits = newValue.text.filter { it.isDigit() }.take(maxLen)
                val cursor = digitsBeforeCursor.coerceAtMost(digits.length)
                fieldValue = TextFieldValue(text = digits, selection = TextRange(cursor))
                onValueChange(country.dialCode + digits)
            },
            visualTransformation = PhoneNumberVisualTransformation(country),
            placeholder = { Text("Phone number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = colors,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Formats a plain digit string for *display only*, per the country's
// grouping pattern (e.g. US 314-345-7861), while leaving the text field's
// actual/editable content as plain digits -- see the comment in
// PhoneNumberField above for why that split matters.
private class PhoneNumberVisualTransformation(private val country: Country) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = formatNationalNumber(digits, country)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var digitsSeen = 0
                for (i in formatted.indices) {
                    if (formatted[i].isDigit()) {
                        digitsSeen++
                        if (digitsSeen == offset) return i + 1
                    }
                }
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                return formatted.substring(0, clamped).count { it.isDigit() }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

