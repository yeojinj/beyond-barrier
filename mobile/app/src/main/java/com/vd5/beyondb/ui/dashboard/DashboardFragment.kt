package com.vd5.beyondb.ui.dashboard


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.vd5.beyondb.databinding.FragmentDashboardBinding
import com.vd5.beyondb.util.Cat
import com.vd5.beyondb.util.RetrofitService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class DashboardFragment : Fragment() {


    lateinit var binding : FragmentDashboardBinding

    private val retrofit = Retrofit.Builder().baseUrl("https://api.thecatapi.com/v1/images/")
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
        val dashboardBtn = binding.buttonDashboard
        val dashboardText = binding.textDashboard
        val dashboardImage = binding.imageDashboard
        var url : String? = null
        dashboardBtn.setOnClickListener {



            service.getCat()?.enqueue(object : Callback<List<Cat>> {
                override fun onResponse(call: Call<List<Cat>>, response: Response<List<Cat>>) {
                    if(response.isSuccessful){
                        var result: List<Cat>? = response.body()
                        Log.d("YMC", "onResponse 성공: " + result?.toString());
                        url = response?.body()?.get(0)?.url
                        dashboardText.text = url

                        Glide.with(this@DashboardFragment)
                            .load(url)
                            .override(400,300)
                            .into(dashboardImage)
                    }else{
                        Log.d("YMC", "onResponse 실패")
                    }
                }

                override fun onFailure(call: Call<List<Cat>>, t: Throwable) {
                    Log.d("YMC", "onFailure 에러: " + t.message.toString());
                }
            })

        }


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        _binding = null
    }
}