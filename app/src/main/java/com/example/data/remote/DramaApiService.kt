package com.example.data.remote

import com.example.data.model.Drama
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface DramaApiService {
    @GET("dramas/catalog.json")
    suspend fun getDramasCatalog(): Response<List<Drama>>

    @GET
    suspend fun getCustomCatalogUrl(@Url url: String): Response<List<Drama>>
}
