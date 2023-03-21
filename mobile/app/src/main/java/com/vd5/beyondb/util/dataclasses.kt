package com.vd5.beyondb.util
import com.google.gson.annotations.SerializedName



data class Cat(
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String,
)

data class Caption(
    @SerializedName("result")
    val result: String,
)

data class CaptionRequest(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("imgPath")
    val imgPath: String,
    @SerializedName("captureTime")
    val captureTime: String,
)