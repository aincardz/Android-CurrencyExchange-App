package com.example.app1.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app1.data.Rate
import com.example.app1.data.HistoricalRate
import com.example.app1.network.NbpApiService
import kotlinx.coroutines.launch

class CurrencyViewModel : ViewModel() {
    var currencies by mutableStateOf<List<Rate>>(emptyList())
        private set
    
    var error by mutableStateOf<String?>(null)
        private set
        
    var fromCurrency by mutableStateOf<Rate?>(null)
        private set
        
    var toCurrency by mutableStateOf<Rate?>(null)
        private set

    var amount by mutableStateOf("1.0")
        private set

    var convertedAmount by mutableStateOf(0.0)
        private set

    var historicalRates by mutableStateOf<List<HistoricalRate>>(emptyList())
        private set
    
    var isLoadingHistory by mutableStateOf(false)
        private set

    var selectedDate by mutableStateOf("Select a point")
        private set
    
    var selectedValue by mutableStateOf(0.0)
        private set

    private val apiService = NbpApiService.create()

    init {
        fetchCurrencies()
    }

    val canConvert: Boolean
        get() = fromCurrency != null && toCurrency != null && amount.toDoubleOrNull() != null

    fun updateAmount(newAmount: String) {
        amount = newAmount
        convertAmount()
    }

    fun updateFromCurrency(currency: Rate) {
        fromCurrency = currency
        convertAmount()
        fetchHistoricalRates(currency.code)
    }

    fun updateToCurrency(currency: Rate) {
        toCurrency = currency
        convertAmount()
    }

    fun convert() {
        convertAmount()
    }

    private fun convertAmount() {
        val fromRate = fromCurrency?.mid ?: return
        val toRate = toCurrency?.mid ?: return
        
        val amountValue = amount.toDoubleOrNull() ?: 0.0
        convertedAmount = amountValue * (fromRate / toRate)
    }

    private fun fetchCurrencies() {
        viewModelScope.launch {
            try {
                val response = apiService.getCurrencies()
                currencies = response.firstOrNull()?.rates ?: emptyList()
                // Add PLN as a currency option
                currencies = listOf(
                    Rate("Polish Złoty", "PLN", 1.0)
                ) + currencies
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun selectRandomCurrencies() {
        if (currencies.size < 2) return
        
        val availableCurrencies = currencies.toMutableList()
        val randomFrom = availableCurrencies.random()
        availableCurrencies.remove(randomFrom)
        val randomTo = availableCurrencies.random()
        
        fromCurrency = randomFrom
        toCurrency = randomTo
        convertAmount()
        fetchHistoricalRates(randomFrom.code)
    }

    private fun fetchHistoricalRates(currencyCode: String) {
        historicalRates = emptyList() // Clear previous data
        if (currencyCode == "PLN") {
            // If PLN is selected, fetch rates for the other currency
            val otherCode = toCurrency?.code
            if (otherCode != null && otherCode != "PLN") {
                viewModelScope.launch {
                    isLoadingHistory = true
                    try {
                        val response = apiService.getHistoricalRates(otherCode)
                        historicalRates = response.rates.map { rate ->
                            HistoricalRate(
                                effectiveDate = rate.effectiveDate,
                                mid = 1.0 / rate.mid // Invert the rate for correct relationship
                            )
                        }
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        isLoadingHistory = false
                    }
                }
            }
            return
        }
        
        viewModelScope.launch {
            isLoadingHistory = true
            try {
                val response = apiService.getHistoricalRates(currencyCode)
                historicalRates = if (toCurrency?.code == "PLN") {
                    response.rates.map { rate ->
                        HistoricalRate(
                            effectiveDate = rate.effectiveDate,
                            mid = 1.0 / rate.mid // Invert for PLN
                        )
                    }
                } else {
                    // Convert rates to show the correct exchange rate between selected currencies
                    response.rates.map { rate ->
                        HistoricalRate(
                            effectiveDate = rate.effectiveDate,
                            mid = (fromCurrency?.mid ?: 1.0) / (rate.mid * (toCurrency?.mid ?: 1.0))
                        )
                    }
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoadingHistory = false
            }
        }
    }

    fun updateSelectedPoint(date: String, value: Double) {
        selectedDate = date
        selectedValue = value
    }
} 