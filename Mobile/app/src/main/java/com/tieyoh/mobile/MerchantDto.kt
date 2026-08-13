package com.tieyoh.mobile

data class MerchantDto(
    val id: Long,
    val pmid: String?,
    val tmid: String?,
    val brandName: String,
    val logoUrl: String?,
    val category: String?,
    val status: String?
)
