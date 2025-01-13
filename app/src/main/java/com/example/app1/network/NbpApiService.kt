package com.example.app1.network

import com.example.app1.data.CurrencyResponse
import com.example.app1.data.HistoricalRateResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface NbpApiService {
    @GET("api/exchangerates/tables/A/")
    suspend fun getCurrencies(): List<CurrencyResponse>

    @GET("api/exchangerates/rates/A/{code}/last/30/")
    suspend fun getHistoricalRates(@Path("code") currencyCode: String): HistoricalRateResponse

    companion object {
        private const val BASE_URL = "https://api.nbp.pl/"

        fun create(): NbpApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NbpApiService::class.java)
        }
    }
} 