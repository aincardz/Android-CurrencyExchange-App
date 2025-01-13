package com.example.app1.data

data class CurrencyResponse(
    val table: String,
    val no: String,
    val effectiveDate: String,
    val rates: List<Rate>
)

data class Rate(
    val currency: String,
    val code: String,
    val mid: Double
) 