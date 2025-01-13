package com.example.app1.data

data class HistoricalRateResponse(
    val rates: List<HistoricalRate>,
    val code: String
)

data class HistoricalRate(
    val effectiveDate: String,
    val mid: Double
) 