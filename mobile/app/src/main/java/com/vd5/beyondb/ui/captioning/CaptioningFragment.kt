package com.vd5.beyondb.ui.captioning

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.nfc.NfcAdapter
import android.os.Bundle
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import androidx.preference.PreferenceManager
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.vd5.beyondb.MainActivity
import com.vd5.beyondb.R
import com.vd5.beyondb.databinding.FragmentCaptioningBinding
import com.vd5.beyondb.service.BluetoothLeService
import com.vd5.beyondb.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Math.sqrt
import java.util.*

class CaptioningFragment : Fragment(), SensorEventListener {

    lateinit var binding : FragmentCaptioningBinding

    private val TAG = "captionFragment"
    private var captioning_lang : String = "ko"
    private var captioning_cast : Boolean = true
    private var captionFlag : Boolean = true


    //view
    private var notificationText : TextView? = null
    private var errorText : TextView? = null



    // PAPAGO API
    val CLIENT_ID = "tQ1IC34NWA_W2eKoRO3p"
    val CLIENT_SECRET = "LJdUj3JuDW"
    val BASE_URL_NAVER_API = "https://openapi.naver.com/"

    private val retrofit2 = Retrofit.Builder().baseUrl(BASE_URL_NAVER_API)
        .addConverterFactory(GsonConverterFactory.create()).build()
    val papagoService = retrofit2.create(RetrofitService::class.java)

    // https://hwanine.github.io/android/Retrofit/

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        captionFlag = preferences.getBoolean("captioning_auto", true)
        captioning_lang = preferences.getString("captioning_lang", "ko").toString()
        captioning_cast = preferences.getBoolean("captioning_showcast", true)


        binding = FragmentCaptioningBinding.inflate(inflater,container,false)

        sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        notificationText = binding.captioningResult
        errorText = binding.errorMessage

        errorText!!.visibility = android.view.View.INVISIBLE
        notificationText?.text = ""




        Log.d(TAG, "onCreateView: ${(activity as MainActivity).textToSpeech!!.availableLanguages}")
        when (captioning_lang) {
            "ko" -> (activity as MainActivity).textToSpeech!!.language = Locale.KOREAN
            "en" -> (activity as MainActivity).textToSpeech!!.language = Locale.ENGLISH
            "ja" -> (activity as MainActivity).textToSpeech!!.language = Locale.JAPAN
            "zh-CN" -> (activity as MainActivity).textToSpeech!!.language = Locale.CHINA
            "fr" -> (activity as MainActivity).textToSpeech!!.language = Locale.FRANCE
        }

        val loadingImage = binding.loadingImage
        val animated = AnimatedVectorDrawableCompat.create(requireContext(), R.drawable.progress_bar)
        animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                loadingImage.post { animated.start() }
            }

        })
        loadingImage.setImageDrawable(animated)
        animated?.start()
        loadingImage.isVisible = true

        (activity as MainActivity).textToSpeech!!.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(p0: String?) {
                }

                // 완료 시점 마다 신호
                override fun onDone(utteranceId: String) {
                    if (captionFlag) {
                        (activity as MainActivity).captioningRequest()
                    }
                }

                override fun onError(p0: String?) {
                }
            })

        if ((activity as MainActivity).connectionState == BluetoothAdapter.STATE_DISCONNECTED){
            (activity as MainActivity).scanLeDevice(true)
        } else {
            (activity as MainActivity).captioningRequest()
        }

        return binding.root
    }


    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {


        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: " + intent.action)
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CAPTIONING -> {
                    val caption = intent.getSerializableExtra("caption") as Caption
                    Log.d(TAG, "onReceive: captionFragment에서의 caption결과 수신 : $caption")
                    var captionResult = caption.result
                    if (captioning_lang != "en") {
                        papagoService.transferPapago(
                            CLIENT_ID,
                            CLIENT_SECRET,
                            "en",
                            captioning_lang,
                            captionResult
                        )
                            .enqueue(object : Callback<ResultTransferPapago> {
                                override fun onResponse(
                                    call: Call<ResultTransferPapago>,
                                    response: Response<ResultTransferPapago>
                                ) {
                                    captionResult =
                                        response.body()?.message?.result?.translatedText.toString()
                                    Log.d("http", "papago api 통신 성공 : $captionResult")
                                    val names = caption.names



                                    if (captioning_cast && captioning_lang == "ko" && names != "") {
                                        captionResult += "\n\n 현재 화면에 보이는 인물은 ${names.dropLast(2)}입니다."
                                    }
                                    val captureView: ImageView = binding.captureView
                                    Glide.with(requireActivity()).load(caption.imgPath).override(1000).into(captureView)
                                    notificationText?.text = captionResult
                                    (activity as MainActivity).TTSrun(captionResult, "captioning")
                                    binding.loadingImage.isVisible = false
                                }

                                override fun onFailure(
                                    call: Call<ResultTransferPapago>,
                                    t: Throwable
                                ) {
                                    Log.d("http", "papago api 통신 성공 실패 : $t")
                                }
                            })
                    } else {
                        val captureView: ImageView = binding.captureView
                        Glide.with(requireActivity()).load(caption.imgPath).override(1000).into(captureView)
                        notificationText?.text = captionResult
                        (activity as MainActivity).TTSrun(captionResult, "captioning")
                        binding.loadingImage.isVisible = false
                    }
                }
                BluetoothLeService.ACTION_GATT_CAPTIONING_FAIL -> {
                    val failMessage = intent.getStringExtra(NfcAdapter.EXTRA_DATA)
                    binding.loadingImage.isVisible = false
                    errorText!!.visibility = android.view.View.VISIBLE
                    (activity as MainActivity).TTSrun(failMessage!!, "captioning")

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        activity?.unregisterReceiver(gattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CAPTIONING)
            addAction(BluetoothLeService.ACTION_GATT_CAPTIONING_FAIL)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).textToSpeech!!.language = Locale.KOREAN
        (activity as MainActivity).textToSpeech?.stop()
    }

    private var lastShakeTime = 0L

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt((x * x + y * y + z * z).toDouble())
            val currentShakeTime = System.currentTimeMillis()

            val shakeThresholdGravity = 1.7F
            val shakeInterval = 500

            if (acceleration / SensorManager.GRAVITY_EARTH > shakeThresholdGravity) {
                if (currentShakeTime - lastShakeTime > shakeInterval) {
                    lastShakeTime = currentShakeTime
                    if (captionFlag) Toast.makeText(context, "연속 화면 해설이 중단 되었습니다.", Toast.LENGTH_SHORT).show()
                    captionFlag = false

                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle the change in the accuracy of the accelerometer sensor
    }

}