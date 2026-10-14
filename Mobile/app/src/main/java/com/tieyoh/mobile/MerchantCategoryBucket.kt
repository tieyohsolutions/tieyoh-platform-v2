package com.tieyoh.mobile

import androidx.compose.ui.graphics.Color

// Buckets a merchant's real `category` field (MerchantDto.category --
// free text from the Outscraper/Google-Places import, e.g. "Nail salon",
// "Fast food restaurant", "Used car dealer") into a small, fixed set of
// spending categories for charts like Activity & Insights' "By Category"
// breakdown. There is no bucket field anywhere in the backend -- this is
// purely a client-side grouping over the real `category` string, built by
// checking every one of the 154 distinct raw values actually present in
// the real import data (Backend/merchant-imports/Ballwin_Data_Set.xlsx),
// not guessed. If a spend-ledger endpoint returns transactions with a
// merchant category the rules below don't recognize, it falls into
// OTHER rather than crashing or leaving a chart segment unlabeled.
//
// Colors are validated for colorblind-safe separation (CVD delta-E,
// chroma floor, and lightness-band checks via the dataviz skill's
// validator) as a set of 6 -- don't add a 7th bucket without
// re-validating the whole set, and don't hand-pick a new color for just
// one bucket in isolation.
enum class MerchantCategoryBucket(val label: String, val color: Color) {
    DINING("Dining", Color(0xFF226CD3)),
    GROCERIES("Groceries", Color(0xFF22A080)),
    PERSONAL_CARE("Personal Care", Color(0xFFA572CA)),
    AUTO("Auto", Color(0xFFB47722)),
    RETAIL("Retail", Color(0xFFBE3764)),
    OTHER("Other", Color(0xFF814418))
}

// Ordered, first-match-wins keyword rules -- checked in this order
// specifically because a few raw values are substrings of each other
// across buckets (e.g. "Bar & grill" is Dining but "Barber shop" must
// NOT match on "bar", so short/ambiguous tokens use \b word-boundary
// regex rather than a plain "contains" check). Case-insensitive.
private val DINING_PATTERN = Regex(
    "restaurant|\\bcafe\\b|coffee|bakery|\\bdiner\\b|\\bbar\\b|bar & grill|" +
        "pizza|sandwich|\\bdonut\\b|ice cream|steak house|sushi|\\bbbq\\b|" +
        "barbecue|\\bdeli\\b|\\bpub\\b|brewery|winery|\\bjuice\\b|smoothie|" +
        "cake shop|buffet|chicken restaurant|hamburger|food truck|cater",
    RegexOption.IGNORE_CASE
)
private val GROCERIES_PATTERN = Regex(
    "grocery|supermarket|\\bmarket\\b|butcher|farmers market",
    RegexOption.IGNORE_CASE
)
private val PERSONAL_CARE_PATTERN = Regex(
    "salon|\\bspa\\b|massage|barber|\\bnail\\b|dentist|dental|beauty|" +
        "esthetician|tanning|waxing|\\bgym\\b|fitness|\\bhair|orthodont",
    RegexOption.IGNORE_CASE
)
private val AUTO_PATTERN = Regex(
    "car dealer|auto repair|\\btire\\b|car rental|\\bauto\\b|mechanic|car wash|" +
        "gas station|\\bdealer(ship)?\\b",
    RegexOption.IGNORE_CASE
)
private val RETAIL_PATTERN = Regex(
    "\\bstore\\b|boutique|\\bmall\\b|thrift|clothing|furniture|appliance|\\bshop\\b",
    RegexOption.IGNORE_CASE
)

fun bucketForMerchantCategory(rawCategory: String?): MerchantCategoryBucket {
    val value = rawCategory?.trim().orEmpty()
    if (value.isEmpty()) return MerchantCategoryBucket.OTHER

    return when {
        DINING_PATTERN.containsMatchIn(value) -> MerchantCategoryBucket.DINING
        PERSONAL_CARE_PATTERN.containsMatchIn(value) -> MerchantCategoryBucket.PERSONAL_CARE
        GROCERIES_PATTERN.containsMatchIn(value) -> MerchantCategoryBucket.GROCERIES
        AUTO_PATTERN.containsMatchIn(value) -> MerchantCategoryBucket.AUTO
        RETAIL_PATTERN.containsMatchIn(value) -> MerchantCategoryBucket.RETAIL
        else -> MerchantCategoryBucket.OTHER
    }
}
