package com.vd5.beyondb.util

import retrofit2.Call
import retrofit2.http.GET

interface RetrofitService {
    @GET("search")
        fun getCat(): Call<List<Cat>>?
}