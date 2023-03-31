package com.vd5.beyondb.ui.captioning

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
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

class CaptioningFragment : Fragment() {

    lateinit var binding : FragmentCaptioningBinding

    private val TAG = "captionFragment"
    private var captioning_lang : String = "ko"



    // PAPAGO API
    val CLIENT_ID = "tQ1IC34NWA_W2eKoRO3p"
    val CLIENT_SECRET = "LJdUj3JuDW"
    val BASE_URL_NAVER_API = "https://openapi.naver.com/"

    private val retrofit2 = Retrofit.Builder().baseUrl(BASE_URL_NAVER_API)
        .addConverterFactory(GsonConverterFactory.create()).build()
    val papagoService = retrofit2.create(RetrofitService::class.java)

    // https://hwanine.github.io/android/Retrofit/

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCaptioningBinding.inflate(inflater,container,false)

        notificationText = binding.textCaptioning
        notificationText?.text = ""

//        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
//        captioning_lang = sharedPref.getString("captioning_lang_values", "ko").toString()

        Glide.with(this).load(R.drawable.loading).into(binding.loadingImage)
        binding.loadingImage.isVisible = true


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
                    val caption = intent.getSerializableExtra("caption") as Caption
                    Log.d(TAG, "onReceive: captionFragment에서의 caption결과 수신 : $caption")

                    var captionResult = caption?.result!!
                    papagoService.transferPapago(CLIENT_ID,CLIENT_SECRET,"en","ko",captionResult)
                        .enqueue(object : Callback<ResultTransferPapago> {
                            override fun onResponse(call: Call<ResultTransferPapago>, response: Response<ResultTransferPapago>
                            ) {
                                captionResult = response.body()?.message?.result?.translatedText.toString()
                                Log.d("http", "papago api 통신 성공 : ${captionResult}")
                                notificationText?.text = captionResult
                                (activity as MainActivity).TTSrun(captionResult)
                                binding.loadingImage.isVisible = false
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
    }

    override fun onPause() {
        super.onPause()
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

}