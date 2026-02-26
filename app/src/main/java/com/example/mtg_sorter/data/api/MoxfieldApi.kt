package com.example.mtg_sorter.data.api

import com.example.mtg_sorter.data.model.MoxfieldDeckResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface MoxfieldApi {
    @GET("decks/all/{id}")
    suspend fun getDeck(@Path("id") id: String): Response<MoxfieldDeckResponse>
}