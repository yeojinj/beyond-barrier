package com.vd5.beyondb.util

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RetrofitService {
    @GET("search")
    fun getCat(): Call<List<Cat>>?


    @POST("caption")
    fun getCaption(
        @Body captionRequest: CaptionRequest
    ) : Call<Caption>

    @GET("caption/{id}")
    fun getCaption(
        @Path("id") captionNum: String
    ) : Call<Caption>

    @GET("program/{id}")
    fun getProgram(
        @Path("id") programNum: String
    ) : Call<ProgramDetail>
}