package com.vd5.beyondb.util
import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class ProgramRequest(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("imgPath")
    val imgPath: String,
    @SerializedName("captureTime")
    val captureTime: String,
)
data class Program(
    @SerializedName("programId")
    val programId: String,
    @SerializedName("programName")
    val programName: String,
    @SerializedName("programContent")
    val programContent: String,
    @SerializedName("programCasting")
    val programCasting: List<String>,
    @SerializedName("programLogoImg")
    val programLogoImg: String,
) : Serializable




data class CaptionRequest(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("imgPath")
    val imgPath: String,
    @SerializedName("captureTime")
    val captureTime: String,
)
data class Caption(
    @SerializedName("result")
    val result: String,
    @SerializedName("names")
    val names: String,
    @SerializedName("imgPath")
    val imgPath: String,
) : Serializable

// papago data class
data class ResultTransferPapago (
    var message: Message
)

data class Message(
    var result: Result
)

data class Result (
    var srcLangType: String = "",
    var tarLangType: String = "",
    var translatedText: String = ""
)