package com.vd5.beyondb.util
import com.google.gson.annotations.SerializedName



data class Cat(
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String,
)