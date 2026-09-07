package com.tieyoh.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Tieyoh brand palette — sampled directly from the tieyoh logo (navy body,
// blue-to-teal gradient on "yoh" and the dot over the "i"). White header,
// navy headings, blue/teal accents and gradients app-wide.
val TieyohNavy = Color(0xFF0A1F3D)        // headings, dark surfaces (My Card face)
val TieyohBlue = Color(0xFF0A66D6)        // gradient start (progress bars, avatars)
val TieyohTealGradient = Color(0xFF12C4AE) // gradient end
val TieyohAccent = Color(0xFF0F9D86)      // solid teal accent (active nav, links, highlights)

val TieyohFabRing = Color(0xFF0C7C6B)     // unused by the current image-based FAB; kept for reference
val TieyohGiftRed = Color(0xFFE0473E)     // unused by the current image-based FAB; kept for reference
val TieyohPersonGreen = Color(0xFF2E9E5B)  // person glyph in the invite icon (top bar)

val TieyohCardBg = Color(0xFFF5F7FA)      // soft card background
val TieyohTrack = Color(0xFFE4E9F0)       // progress bar track / avatar bg
val TieyohMuted = Color(0xFF8B96A5)       // muted body text
val TieyohBorder = Color(0xFFEEF1F5)      // hairline borders

val TieyohSponsoredBg = Color(0xFFFEF3E1)
val TieyohSponsoredLogoBg = Color(0xFFF6CE7E)
val TieyohSponsoredHeading = Color(0xFF7A4E0A)
val TieyohSponsoredBody = Color(0xFF9C7526)
val TieyohSponsoredLabel = Color(0xFFB57A12)

val TieyohNavInactive = Color(0xFFA6AEB9)

// Exact hex values from the Sep 2026 onboarding/login redesign reference
// (login/signup flow modernization). Kept alongside the approximated
// tokens above rather than replacing them -- retiring those would recolor
// every existing screen at once, not just the new onboarding screens this
// palette is for.
val TieyohBrandNavy = Color(0xFF0D1B2A)
val TieyohBrandBlue = Color(0xFF1565FF)
val TieyohBrandTeal = Color(0xFF00B3B3)
val TieyohBrandTealLight = Color(0xFF2DD4BF)
val TieyohBrandBg = Color(0xFFF2F6F9)
// Based on the "Tie" glyphs in the real wordmark asset (which sample at a
// near-black 0xFF041738) but lightened for use as body/copy text -- the raw
// sampled shade read as too dark on screen.
val TieyohBrandDarkBlue = Color(0xFF1E3A5F)
// Two lightening steps up from the raw "Tie" sample (0xFF041738) instead of
// one -- for hint/helper copy that should read as a lighter navy than
// TieyohBrandDarkBlue above.
val TieyohBrandNavyLight2 = Color(0xFF385D86)

// Kept for reference / dark-theme fallback
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
