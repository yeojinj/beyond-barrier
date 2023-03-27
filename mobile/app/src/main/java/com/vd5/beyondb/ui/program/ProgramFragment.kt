package com.vd5.beyondb.ui.program


import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.vd5.beyondb.MainActivity
import com.vd5.beyondb.databinding.FragmentProgramBinding
import com.vd5.beyondb.service.BluetoothLeService

private const val TAG = "ProgramFragment"

class ProgramFragment : Fragment() {


    lateinit var _binding : FragmentProgramBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgramBinding.inflate(inflater,container,false)

        val programText = _binding.textProgram
        programText.text = ""

        if ((activity as MainActivity).connectionState == BluetoothAdapter.STATE_DISCONNECTED){
            (activity as MainActivity).scanLeDevice(true)
        } else {
            (activity as MainActivity).captioningRequest()
        }

        return _binding.root
    }


    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: " + intent.action)
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    _binding.textProgram.text = "연결됨"
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    _binding.textProgram.text = "연결 안 됨"
                }
                BluetoothLeService.ACTION_GATT_CAPTIONING -> {
                    Log.d(TAG, "onReceive: captioning 결과")
                    val receivingData = intent.getStringExtra(NfcAdapter.EXTRA_DATA)
                    Log.d(TAG, "captioning 결과 : $receivingData")
                    val textView = _binding.textProgram
                    textView.text = receivingData
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
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_CAPTIONING)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        var textToSpeech = (activity as MainActivity).textToSpeech
        if(textToSpeech != null){
            textToSpeech?.stop()
        }
    }
}