package com.vd5.beyondb.util
import com.google.gson.annotations.SerializedName



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
)




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
)

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
////////////////////////////////////