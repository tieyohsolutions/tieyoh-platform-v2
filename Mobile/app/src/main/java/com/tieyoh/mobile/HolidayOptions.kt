package com.tieyoh.mobile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tieyoh.mobile.ui.theme.TieyohAccent
import com.tieyoh.mobile.ui.theme.TieyohNavy

// The fixed picklist behind the "holidays you celebrate" selector on both
// the signup profile-setup screen and the Edit Profile screen. A closed
// list of chosen labels (rather than free text) keeps the stored value
// predictable and easy to redisplay -- selections are joined with "," when
// saved (see ProfileSetupRequest.holidaysCelebrated) and split back apart
// when an existing profile is loaded for editing.
//
// This is a deliberately separate concept from the Feed's
// holiday_definition table (general market/calendar occasions, not tied
// to any one person) -- see AppUser.holidaysCelebrated for why. Nothing
// here infers a holiday from a user's name, contacts, or location; it's
// only ever set by the user explicitly picking one.
val HOLIDAY_OPTIONS = listOf(
    "Christmas",
    "Hanukkah",
    "Kwanzaa",
    "Diwali",
    "Eid al-Fitr",
    "Eid al-Adha",
    "Lunar New Year",
    "Easter",
    "Passover",
    "Ramadan",
    "Vesak",
    "Dia de los Muertos"
)

fun holidaysToLabels(stored: String?): Set<String> {
    if (stored.isNullOrBlank()) return emptySet()
    return stored.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}

fun holidaysToStored(labels: Set<String>): String? {
    return if (labels.isEmpty()) null else labels.joinToString(",")
}

// Shared holiday multi-select used by both the signup profile-setup
// screen and the Edit Profile screen, so "holidays you celebrate" looks
// and behaves identically in both places. A one-line dropdown rather than
// a 12-row checkbox list (what this used to be) -- collapsed, it shows a
// short summary of what's picked; tapping it opens a checkable menu that
// stays open across multiple taps, so picking several holidays doesn't
// mean reopening the menu each time.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayCheckboxList(
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val orderedSelected = HOLIDAY_OPTIONS.filter { selected.contains(it) }
    val summary = when {
        orderedSelected.isEmpty() -> "None selected"
        orderedSelected.size <= 2 -> orderedSelected.joinToString(", ")
        else -> "${orderedSelected.take(2).joinToString(", ")} +${orderedSelected.size - 2} more"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = summary,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            HOLIDAY_OPTIONS.forEach { label ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Non-interactive here (onCheckedChange = null) --
                            // the whole row's onClick below handles the tap,
                            // so the checkbox doesn't double-toggle when
                            // someone taps directly on it.
                            Checkbox(
                                checked = selected.contains(label),
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = TieyohAccent)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = label, fontSize = 14.sp, color = TieyohNavy)
                        }
                    },
                    // Deliberately doesn't set expanded = false -- picking
                    // several holidays shouldn't mean reopening the menu
                    // after every tap. It closes via onDismissRequest above
                    // (tapping outside) once the person is done.
                    onClick = { onToggle(label) }
                )
            }
        }
    }
}
