package com.vd5.beyondb.util

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

interface RetrofitService {
    @POST("program")
    fun getProgram(
        @Body programRequest: ProgramRequest
    ) : Call<Program>


    @POST("caption")
    fun getCaption(
        @Body captionRequest: CaptionRequest
    ) : Call<Caption>

    @FormUrlEncoded
    @POST("v1/papago/n2mt")
    fun transferPapago(
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String,
        @Field("source") source: String,
        @Field("target") target: String,
        @Field("text") text: String
    ): Call<ResultTransferPapago>

    @GET("caption/{id}")
    fun getCaption(
        @Path("id") captionNum: String
    ) : Call<Caption>

    @GET("program/{id}")
    fun getProgram(
        @Path("id") programNum: String
    ) : Call<Program>
}