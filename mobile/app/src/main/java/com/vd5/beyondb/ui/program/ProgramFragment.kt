package com.vd5.beyondb.ui.program


import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.vd5.beyondb.MainActivity
import com.vd5.beyondb.databinding.FragmentProgramBinding
import com.vd5.beyondb.service.BluetoothLeService
import com.vd5.beyondb.util.Program

private const val TAG = "ProgramFragment"

class ProgramFragment : Fragment() {


    lateinit var _binding : FragmentProgramBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgramBinding.inflate(inflater,container,false)

        programText = _binding.textProgram
        programText?.text = ""

        programBtn = _binding.buttonProgram
        programBtn?.isEnabled = false
        programBtn?.text = "processing.."

        if ((activity as MainActivity).connectionState == BluetoothAdapter.STATE_DISCONNECTED){
            (activity as MainActivity).scanLeDevice(true)
        } else {
            (activity as MainActivity).programRequest()
        }

        return _binding.root
    }

    private var programBtn : Button? = null
    private var programText : TextView? = null



    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: " + intent.action)
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_PROGRAM -> {
                    Log.d(TAG, "onReceive: 프로그램 결과 수신")
                    val program = intent.getSerializableExtra("program") as Program
                    Log.d(TAG, "program 결과 : $program")
                    val programName = "프로그램 이름은 ${program?.programName}입니다."
                    programText?.text = programName
                    (activity as MainActivity).TTSrun(programName)
                    programBtn?.text = "READY"
                }
                BluetoothLeService.ACTION_REQUEST_FAIL -> {
                    Log.d(TAG, "onReceive: 로고 인식 실패 fragment에서 받음")
                    val message = intent.getStringExtra(NfcAdapter.EXTRA_DATA)
                    programText?.text = message
                    (activity as MainActivity).TTSrun(message.toString())
                    programBtn?.text = "READY"
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
            addAction(BluetoothLeService.ACTION_GATT_PROGRAM)
            addAction(BluetoothLeService.ACTION_REQUEST_FAIL)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as MainActivity).textToSpeech?.stop()
    }
}