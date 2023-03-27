package com.vd5.beyondb.ui.captioning

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.vd5.beyondb.R
import com.vd5.beyondb.databinding.FragmentCaptioningBinding
import com.vd5.beyondb.util.Caption
import com.vd5.beyondb.util.CaptionRequest
import com.vd5.beyondb.util.ResultTransferPapago
import com.vd5.beyondb.util.RetrofitService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class CaptioningFragment : Fragment() {

    lateinit var binding : FragmentCaptioningBinding
    private var textToSpeech: TextToSpeech? = null

    private val retrofit = Retrofit.Builder().baseUrl("http://18.191.139.106:5000/api/")
        .addConverterFactory(GsonConverterFactory.create()).build();
    val service = retrofit.create(RetrofitService::class.java);


    // PAPAGO API
    val CLIENT_ID = "tQ1IC34NWA_W2eKoRO3p"
    val CLIENT_SECRET = "LJdUj3JuDW"
    val BASE_URL_NAVER_API = "https://openapi.naver.com/"

    private val retrofit2 = Retrofit.Builder().baseUrl(BASE_URL_NAVER_API)
        .addConverterFactory(GsonConverterFactory.create()).build();
    val papagoService = retrofit2.create(RetrofitService::class.java);

    // https://hwanine.github.io/android/Retrofit/

    ////////////////////////

    //시연을 위한 버튼 정보

    private var imgNum : Int = 0
    private val imgUrl : Array<String> = arrayOf("https://beyondb-bucket.s3.ap-northeast-2.amazonaws.com/capture/live.png","https://beyondb-bucket.s3.ap-northeast-2.amazonaws.com/capture/captiontest_walk.png")


    ///////////////////////





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCaptioningBinding.inflate(inflater,container,false)
        TTSinit()

        val notificationsBtn = binding.buttonNotifications
        val notificationText = binding.textNotifications


        notificationsBtn.setOnClickListener {
            notificationText.text = ""
            notificationsBtn.isEnabled = false
            notificationsBtn.text = "processing.."

            val captionRequest = CaptionRequest( deviceId = "", imgPath = imgUrl[imgNum],   captureTime = "2023-03-20T16:25:00" )
            service.getCaption(captionRequest)?.enqueue(object : Callback<Caption> {
                override fun onResponse(call: Call<Caption>, response: Response<Caption>) {
                    if(response.isSuccessful){
                        var captionText: String = response.body()?.result.toString()
                        var returnText : String = ""
                        Log.d("http", "onResponse 성공: ${captionText}")

                        papagoService.transferPapago(CLIENT_ID,CLIENT_SECRET,"en","ko",captionText)
                            .enqueue(object : Callback<ResultTransferPapago> {
                                override fun onResponse(call: Call<ResultTransferPapago>, response: Response<ResultTransferPapago>
                                ) {
                                    returnText = response.body()?.message?.result?.translatedText.toString()
                                    Log.d("http", "papago api 통신 성공 : ${returnText}")
                                    notificationText.text = returnText
                                    TTSrun(returnText)
                                }

                                override fun onFailure(call: Call<ResultTransferPapago>, t: Throwable) {
                                    Log.d("http", "papago api 통신 성공 실패 : $t")
                                }
                            })
                        notificationsBtn.isEnabled = true
                        notificationsBtn.text = "CAPTION"
                    }else{
                        Log.d("http", "onResponse 실패")
                    }
                }
                override fun onFailure(call: Call<Caption>, t: Throwable) {
                    Log.d("http", "onFailure 에러: " + t.message.toString());
                }
            })
        }


        val notificationImageView = binding.imageView
        val notificationsBtn2 = binding.button2Notifications
        notificationsBtn2.setOnClickListener{
            imgNum = 1 - imgNum
            if(imgNum == 0){
                notificationImageView.setImageResource(R.drawable.captiontest_live)
            }
            else{
                notificationImageView.setImageResource(R.drawable.captiontest_walk)
            }
        }



        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if(textToSpeech != null){
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }

    fun TTSrun(string: String) {
        textToSpeech?.speak(string, TextToSpeech.QUEUE_FLUSH, null, null)
        textToSpeech?.playSilentUtterance(750,TextToSpeech.QUEUE_ADD,null) // deley시간 설정
    }

    fun TTSinit() {
        textToSpeech = TextToSpeech(this.context, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS) {
                val result = textToSpeech!!.setLanguage(Locale.KOREAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS","해당언어는 지원되지 않습니다.")
                    return@OnInitListener
                }
                textToSpeech!!.setSpeechRate(1.0f)
            }
        })
    }



}