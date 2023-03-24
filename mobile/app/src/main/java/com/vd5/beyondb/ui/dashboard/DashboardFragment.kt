package com.vd5.beyondb.ui.dashboard


import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import com.vd5.beyondb.MainActivity
import com.vd5.beyondb.databinding.ActivityMainBinding
import com.vd5.beyondb.databinding.FragmentDashboardBinding
import com.vd5.beyondb.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


class DashboardFragment : Fragment() {


    lateinit var binding : FragmentDashboardBinding
    lateinit var binding2: ActivityMainBinding

    private var textToSpeech: TextToSpeech? = null

    private val retrofit = Retrofit.Builder().baseUrl("http://18.191.139.106:5000/api/")
        .addConverterFactory(GsonConverterFactory.create()).build();
    val service = retrofit.create(RetrofitService::class.java);

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        binding = FragmentDashboardBinding.inflate(inflater,container,false)
        binding2 = ActivityMainBinding.inflate(layoutInflater)
        TTSinit()

        val dashboardBtn : Button = binding.buttonDashboard
        val dashboardText = binding.textDashboard




        var url : String? = null

        dashboardText.text = ""
        dashboardBtn.isEnabled = false
        dashboardBtn.text = "processing.."

        val programRequest = ProgramRequest( deviceId = "", imgPath = "https://beyondb-bucket.s3.ap-northeast-2.amazonaws.com/capture/live.png",   captureTime = "2023-03-20T16:25:00" )

        service.getProgram(programRequest)?.enqueue(object : Callback<Program> {
            override fun onResponse(call: Call<Program>, response: Response<Program>) {
                if(response.isSuccessful){
                    var programText: Program? = response.body()
                    Log.d("http", "onResponse 성공: ${programText}")

                    var returnText : String = "프로그램 이름은 "+ programText?.programName + " 입니다."

                    TTSrun(returnText)
                    dashboardText.text = returnText


                }else{
                    Log.d("http", "onResponse 실패")
                }


                dashboardBtn.isEnabled = true
                dashboardBtn.text = "PROGRAM"

            }
            override fun onFailure(call: Call<Program>, t: Throwable) {
                Log.d("http", "onFailure 에러: " + t.message.toString());
            }
        })




//        dashboardBtn.setOnClickListener {
//            dashboardText.text = ""
//            dashboardBtn.isEnabled = false
//            dashboardBtn.text = "processing.."
//
//            val programRequest = ProgramRequest( deviceId = "", imgPath = "https://beyondb-bucket.s3.ap-northeast-2.amazonaws.com/capture/live.png",   captureTime = "2023-03-20T16:25:00" )
//
//            service.getProgram(programRequest)?.enqueue(object : Callback<Program> {
//                override fun onResponse(call: Call<Program>, response: Response<Program>) {
//                    if(response.isSuccessful){
//                        var programText: Program? = response.body()
//                        Log.d("http", "onResponse 성공: ${programText}")
//
//                        var returnText : String = "프로그램 이름은 "+ programText?.programName + " 입니다."
//
//                        TTSrun(returnText)
//                        dashboardText.text = returnText
//
//
//                    }else{
//                        Log.d("http", "onResponse 실패")
//                    }
//
//
//                    dashboardBtn.isEnabled = true
//                    dashboardBtn.text = "PROGRAM"
//
//                }
//                override fun onFailure(call: Call<Program>, t: Throwable) {
//                    Log.d("http", "onFailure 에러: " + t.message.toString());
//                }
//            })
//        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if(textToSpeech != null){
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
//        _binding = null
    }

    fun TTSrun(string: String) {
        textToSpeech?.speak(string, TextToSpeech.QUEUE_FLUSH, null, null)
        textToSpeech?.playSilentUtterance(750, TextToSpeech.QUEUE_ADD,null) // deley시간 설정
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