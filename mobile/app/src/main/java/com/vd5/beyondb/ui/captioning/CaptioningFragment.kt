package com.vd5.beyondb.ui.captioning

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Build.VERSION_CODES.S
import android.os.Bundle
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.vd5.beyondb.MainActivity
import com.vd5.beyondb.databinding.FragmentCaptioningBinding
import com.vd5.beyondb.service.BluetoothLeService
import com.vd5.beyondb.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Math.sqrt

class CaptioningFragment : Fragment(), SensorEventListener {

    lateinit var binding : FragmentCaptioningBinding

    private val TAG = "captionFragment"

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

    private var captionFlag = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        // TODO 자동 화면 해설 preference key 넣기
        captionFlag = preferences.getBoolean("captioning", true)

        binding = FragmentCaptioningBinding.inflate(inflater,container,false)

        sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        notificationsBtn = binding.buttonNotifications
        notificationText = binding.textNotifications

        notificationText?.text = ""
        notificationsBtn?.isEnabled = false
        notificationsBtn?.text = "processing.."


        if ((activity as MainActivity).connectionState == BluetoothAdapter.STATE_DISCONNECTED){
            (activity as MainActivity).scanLeDevice(true)
        } else {
            (activity as MainActivity).captioningRequest()
        }

        return binding.root
    }

    private var notificationsBtn : Button? = null
    private var notificationText : TextView? = null

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: " + intent.action)
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CAPTIONING -> {
                    Log.d(TAG, "onReceive: captionFragment에서의 caption결과 수신")
                    val caption = intent.getSerializableExtra("caption") as Caption
                    Log.d(TAG, "onReceive: $caption")
                    var captionResult = caption?.result!!
                    papagoService.transferPapago(CLIENT_ID,CLIENT_SECRET,"en","ko",captionResult)
                        .enqueue(object : Callback<ResultTransferPapago> {
                            override fun onResponse(call: Call<ResultTransferPapago>, response: Response<ResultTransferPapago>
                            ) {
                                captionResult = response.body()?.message?.result?.translatedText.toString()
                                Log.d("http", "papago api 통신 성공 : ${captionResult}")
                                notificationText?.text = captionResult
                                // TODO 설정에서 가져와서 이어서 말할 지 결정
                                (activity as MainActivity).textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
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
                                (activity as MainActivity).TTSrun(captionResult, "captioning")
                                notificationsBtn?.text = "READY"
                            }

                            override fun onFailure(call: Call<ResultTransferPapago>, t: Throwable) {
                                Log.d("http", "papago api 통신 성공 실패 : $t")
                            }
                        })
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
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