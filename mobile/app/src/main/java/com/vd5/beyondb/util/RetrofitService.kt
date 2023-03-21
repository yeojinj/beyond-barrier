package com.vd5.beyondb.util

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RetrofitService {
    @GET("search")
    fun getCat(): Call<List<Cat>>?


    @POST("caption")
    fun getCaption(
        @Body captionRequest: CaptionRequest
    ) : Call<Caption>

}