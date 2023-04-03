package com.vd5.beyondb.ui.program


import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.bumptech.glide.Glide
import com.vd5.beyondb.MainActivity
import com.vd5.beyondb.R
import com.vd5.beyondb.databinding.FragmentProgramBinding
import com.vd5.beyondb.service.BluetoothLeService
import com.vd5.beyondb.util.Program

private const val TAG = "ProgramFragment"

class ProgramFragment : Fragment() {
    private var programText : TextView? = null
    private var errorText : TextView? = null

    lateinit var binding : FragmentProgramBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramBinding.inflate(inflater,container,false)

        programText = binding.programResult
        errorText = binding.errorMessage

        programText?.text = ""
        errorText!!.visibility = View.INVISIBLE


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


        if ((activity as MainActivity).connectionState == BluetoothAdapter.STATE_DISCONNECTED){
            (activity as MainActivity).scanLeDevice(true)
        } else {
            (activity as MainActivity).programRequest()
        }

        return binding.root
    }





    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: " + intent.action)
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_PROGRAM -> {
                    val program = intent.getSerializableExtra("program") as Program
                    Log.d(TAG, "onReceive: 프로그램 결과 수신 : $program")
                    programText?.text = program.programName

                    val logoView = binding.imageLogo
                    Glide.with(requireActivity()).load(program.programLogoImg).override(700).into(logoView)

                    val programResult = programResult(program)

                    (activity as MainActivity).TTSrun(programResult)
                    binding.loadingImage.isVisible = false
                }
                BluetoothLeService.ACTION_REQUEST_FAIL -> {
                    Log.d(TAG, "onReceive: 로고 인식 실패 fragment에서 받음")
                    val message = intent.getStringExtra(NfcAdapter.EXTRA_DATA)
                    errorText!!.visibility = View.VISIBLE
                    (activity as MainActivity).TTSrun(message.toString())
                    binding.loadingImage.isVisible = false
                }
                BluetoothLeService.ACTION_GATT_PROGRAM_FAIL -> {
                    val message = intent.getStringExtra(NfcAdapter.EXTRA_DATA)
                    errorText!!.visibility = android.view.View.VISIBLE

                    (activity as MainActivity).TTSrun(message.toString())
                    binding.loadingImage.isVisible = false
                }
            }
        }
    }

    fun programResult(program: Program): String {
        var result = "프로그램 이름은 ${program.programName}입니다.\n\n" +
                "${program.programContent} 입니다. "
        if (program.programCasting.isNotEmpty()) {
            var names = ""
            for (i in program.programCasting.indices) {
                if (i > 4) break
                names += (program.programCasting[i] + ", ")
            }
            names = names.dropLast(2)
            result += "주요 출연진은 $names 이 있습니다."
        }
        return result
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
            addAction(BluetoothLeService.ACTION_GATT_PROGRAM)
            addAction(BluetoothLeService.ACTION_GATT_PROGRAM_FAIL)
            addAction(BluetoothLeService.ACTION_REQUEST_FAIL)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).textToSpeech?.stop()
    }
}