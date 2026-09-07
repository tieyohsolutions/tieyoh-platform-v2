package com.tieyoh.mobile

import android.content.Context
import java.util.Locale

// A country entry drives how a phone number is typed and displayed on the
// UX side: its dial code (stored, with the number, as the leading part of
// the E.164 string Tieyoh actually saves) and its digit grouping (purely
// cosmetic -- how the raw digits get chopped up with dashes while typing,
// e.g. US/Canada as xxx-xxx-xxxx, India as xxxxx-xxxxx). This is a curated
// list covering the most common countries rather than an exhaustive,
// always-correct-for-every-region table (that's what a real phone-number
// library like libphonenumber is for) -- picked deliberately to avoid
// adding a new third-party dependency whose exact available version this
// environment has no way to verify. Anything not in the list still works
// via the generic fallback pattern.
data class Country(
    val isoCode: String,
    val displayName: String,
    val dialCode: String,
    val flagEmoji: String,
    val digitGroups: List<Int>
)

private val GENERIC_GROUPS = listOf(3, 3, 4)

val COUNTRIES: List<Country> = listOf(
    Country("US", "United States", "+1", "🇺🇸", listOf(3, 3, 4)),
    Country("CA", "Canada", "+1", "🇨🇦", listOf(3, 3, 4)),
    Country("GB", "United Kingdom", "+44", "🇬🇧", listOf(4, 6)),
    Country("IE", "Ireland", "+353", "🇮🇪", listOf(2, 3, 4)),
    Country("FR", "France", "+33", "🇫🇷", listOf(1, 2, 2, 2, 2)),
    Country("DE", "Germany", "+49", "🇩🇪", listOf(4, 4, 4)),
    Country("ES", "Spain", "+34", "🇪🇸", listOf(3, 3, 3)),
    Country("IT", "Italy", "+39", "🇮🇹", listOf(3, 3, 4)),
    Country("PT", "Portugal", "+351", "🇵🇹", listOf(3, 3, 3)),
    Country("NL", "Netherlands", "+31", "🇳🇱", listOf(2, 4, 4)),
    Country("BE", "Belgium", "+32", "🇧🇪", listOf(3, 2, 2, 2)),
    Country("CH", "Switzerland", "+41", "🇨🇭", listOf(2, 3, 2, 2)),
    Country("SE", "Sweden", "+46", "🇸🇪", listOf(2, 3, 2, 2)),
    Country("NO", "Norway", "+47", "🇳🇴", listOf(3, 2, 3)),
    Country("DK", "Denmark", "+45", "🇩🇰", listOf(2, 2, 2, 2)),
    Country("PL", "Poland", "+48", "🇵🇱", listOf(3, 3, 3)),
    Country("GR", "Greece", "+30", "🇬🇷", listOf(3, 3, 4)),
    Country("RU", "Russia", "+7", "🇷🇺", listOf(3, 3, 2, 2)),
    Country("UA", "Ukraine", "+380", "🇺🇦", listOf(2, 3, 2, 2)),
    Country("TR", "Turkey", "+90", "🇹🇷", listOf(3, 3, 2, 2)),
    Country("IN", "India", "+91", "🇮🇳", listOf(5, 5)),
    Country("PK", "Pakistan", "+92", "🇵🇰", listOf(3, 7)),
    Country("BD", "Bangladesh", "+880", "🇧🇩", listOf(4, 6)),
    Country("CN", "China", "+86", "🇨🇳", listOf(3, 4, 4)),
    Country("JP", "Japan", "+81", "🇯🇵", listOf(2, 4, 4)),
    Country("KR", "South Korea", "+82", "🇰🇷", listOf(2, 4, 4)),
    Country("PH", "Philippines", "+63", "🇵🇭", listOf(3, 3, 4)),
    Country("VN", "Vietnam", "+84", "🇻🇳", listOf(3, 3, 3)),
    Country("ID", "Indonesia", "+62", "🇮🇩", listOf(3, 4, 4)),
    Country("TH", "Thailand", "+66", "🇹🇭", listOf(2, 3, 4)),
    Country("MY", "Malaysia", "+60", "🇲🇾", listOf(2, 4, 4)),
    Country("SG", "Singapore", "+65", "🇸🇬", listOf(4, 4)),
    Country("AU", "Australia", "+61", "🇦🇺", listOf(3, 3, 3)),
    Country("NZ", "New Zealand", "+64", "🇳🇿", listOf(2, 3, 4)),
    Country("EG", "Egypt", "+20", "🇪🇬", listOf(3, 3, 4)),
    Country("SA", "Saudi Arabia", "+966", "🇸🇦", listOf(2, 3, 4)),
    Country("AE", "United Arab Emirates", "+971", "🇦🇪", listOf(2, 3, 4)),
    Country("QA", "Qatar", "+974", "🇶🇦", listOf(4, 4)),
    Country("JO", "Jordan", "+962", "🇯🇴", listOf(1, 3, 4)),
    Country("LB", "Lebanon", "+961", "🇱🇧", listOf(2, 3, 3)),
    Country("IL", "Israel", "+972", "🇮🇱", listOf(2, 3, 4)),
    Country("MA", "Morocco", "+212", "🇲🇦", listOf(3, 2, 2, 2)),
    Country("DZ", "Algeria", "+213", "🇩🇿", listOf(3, 2, 2, 2)),
    Country("TN", "Tunisia", "+216", "🇹🇳", listOf(2, 3, 3)),
    Country("NG", "Nigeria", "+234", "🇳🇬", listOf(3, 3, 4)),
    Country("KE", "Kenya", "+254", "🇰🇪", listOf(3, 3, 3)),
    Country("GH", "Ghana", "+233", "🇬🇭", listOf(2, 3, 4)),
    Country("ZA", "South Africa", "+27", "🇿🇦", listOf(2, 3, 4)),
    Country("BR", "Brazil", "+55", "🇧🇷", listOf(2, 5, 4)),
    Country("MX", "Mexico", "+52", "🇲🇽", listOf(3, 3, 4)),
    Country("AR", "Argentina", "+54", "🇦🇷", listOf(2, 4, 4)),
    Country("CO", "Colombia", "+57", "🇨🇴", listOf(3, 3, 4))
)

val DEFAULT_COUNTRY: Country = COUNTRIES.first { it.isoCode == "US" }

fun countryByIsoCode(isoCode: String?): Country? = COUNTRIES.firstOrNull { it.isoCode.equals(isoCode, ignoreCase = true) }

fun countryByDialCode(dialCode: String): Country? = COUNTRIES.firstOrNull { it.dialCode == dialCode }

// Best-effort guess at "this device's home country" -- used only to pick a
// sensible starting selection in the country picker, and as the assumed
// region for a phone number that's already in someone's contacts without
// a country code of its own. Never authoritative; the user can always
// change the selected country.
fun defaultCountryForDevice(context: Context): Country {
    val isoCode = try {
        context.resources.configuration.locales[0].country
    } catch (e: Exception) {
        Locale.getDefault().country
    }
    return countryByIsoCode(isoCode) ?: DEFAULT_COUNTRY
}

// Groups raw digits per the country's pattern, e.g. "3141234567" with
// [3,3,4] -> "314-123-4567". Stops once every group is full; leftover
// digits beyond the pattern's total are simply not shown (the caller is
// expected to have already capped input at maxDigits()).
fun formatNationalNumber(digits: String, country: Country): String {
    val groups = if (country.digitGroups.isEmpty()) GENERIC_GROUPS else country.digitGroups
    val parts = mutableListOf<String>()
    var index = 0
    for (groupSize in groups) {
        if (index >= digits.length) break
        val end = minOf(index + groupSize, digits.length)
        parts.add(digits.substring(index, end))
        index = end
    }
    if (index < digits.length) {
        // More digits than the pattern accounts for -- append the rest
        // ungrouped rather than silently dropping them.
        parts.add(digits.substring(index))
    }
    return parts.joinToString("-")
}

fun maxDigits(country: Country): Int {
    val groups = if (country.digitGroups.isEmpty()) GENERIC_GROUPS else country.digitGroups
    return groups.sum()
}

// Turns a raw device-contact phone number (already-punctuated, whatever
// format Android's Contacts app happened to store) into an E.164-ish
// string for the backend. If it's already international (starts with a
// "+"), just strips formatting; otherwise assumes it's a local number in
// the given default country and prepends that country's dial code. This
// is a heuristic, not real phone-number parsing -- good enough for an
// existing contact, since most address-book entries are for people in the
// same country as their owner, but not a substitute for a real library.
fun toE164OrRaw(rawPhone: String, defaultCountry: Country): String? {
    val trimmed = rawPhone.trim()
    if (trimmed.isEmpty()) return null
    val digitsOnly = trimmed.filter { it.isDigit() }
    if (digitsOnly.isEmpty()) return null
    return if (trimmed.startsWith("+")) {
        "+$digitsOnly"
    } else {
        defaultCountry.dialCode + digitsOnly
    }
}
