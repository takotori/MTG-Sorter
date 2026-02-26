package com.example.mtg_sorter.data.api

import com.example.mtg_sorter.data.model.ScryfallCard
import com.example.mtg_sorter.data.model.ScryfallSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ScryfallApi {
    @GET("cards/named")
    suspend fun getCardNamedFuzzy(
        @Query("fuzzy") name: String
    ): Response<ScryfallCard>

    @GET("cards/search")
    suspend fun searchCards(
        @Query("q") query: String
    ): Response<ScryfallSearchResponse>
}
