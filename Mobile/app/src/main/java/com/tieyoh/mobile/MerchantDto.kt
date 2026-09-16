package com.tieyoh.mobile

data class MerchantDto(
    val id: Long,
    val pmid: String?,
    val tmid: String?,
    val brandName: String,
    val logoUrl: String?,
    val category: String?,
    val status: String?,
    // Only populated when this came from "My picks" (getMerchantSelections)
    // -- null for plain search results, where they're simply unused.
    val targetAmount: Double? = null,
    val amountReceived: Double? = null,
    // Only populated on "Near me" search results (getNearbyMerchants) --
    // null everywhere else.
    val distanceMiles: Double? = null
)
