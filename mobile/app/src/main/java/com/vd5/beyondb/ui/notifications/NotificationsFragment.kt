package com.vd5.beyondb.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.vd5.beyondb.databinding.FragmentNotificationsBinding
import com.vd5.beyondb.util.Caption
import com.vd5.beyondb.util.CaptionRequest
import com.vd5.beyondb.util.RetrofitService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NotificationsFragment : Fragment() {

    lateinit var binding : FragmentNotificationsBinding

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
        binding = FragmentNotificationsBinding.inflate(inflater,container,false)


        val notificationsBtn = binding.buttonNotifications
        val notificationText = binding.textNotifications

        val captionRequest = CaptionRequest( deviceId = "", imgPath = "https://i.ytimg.com/vi/cKTa2Is52qI/maxresdefault.jpg",   captureTime = "2023-03-20T16:25:00" )

        notificationsBtn.setOnClickListener {

            service.getCaption(captionRequest)?.enqueue(object : Callback<Caption> {
                override fun onResponse(call: Call<Caption>, response: Response<Caption>) {
                    if(response.isSuccessful){
                        var result: Caption? = response.body()
                        Log.d("YMC", "onResponse 성공: " + result?.toString());
                        notificationText.text = response.body()?.result

                    }else{
                        Log.d("YMC", "onResponse 실패")
                    }
                }
                override fun onFailure(call: Call<Caption>, t: Throwable) {
                    Log.d("YMC", "onFailure 에러: " + t.message.toString());
                }
            })
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}