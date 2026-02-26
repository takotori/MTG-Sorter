package com.example.mtg_sorter.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MoxfieldRetrofit {
    private const val BASE_URL = "https://api2.moxfield.com/v2/"

    val instance: MoxfieldApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(MoxfieldApi::class.java)
    }
}