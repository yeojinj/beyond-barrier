package com.vd5.beyondb.ui.home

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
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.Fragment
import com.vd5.beyondb.MainActivity
import com.vd5.beyondb.databinding.FragmentHomeBinding
import com.vd5.beyondb.service.BluetoothLeService

private const val TAG = "HOMEFRAGMENT"

class HomeFragment : Fragment(), View.OnClickListener {

    lateinit var _binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListener()
    }

    private fun setOnClickListener() {
        val captionButton = _binding.captionButton
        captionButton.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        if ((activity as MainActivity).connectionState == BluetoothAdapter.STATE_DISCONNECTED){
            (activity as MainActivity).scanLeDevice(true)
            Log.d(TAG, "onClick: 프레그먼트에서 기기 검색 호출")
        } else {
            (activity as MainActivity).captioningRequest()
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: " + intent.action)
            when (intent.action) {
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    Log.d(TAG, "onReceive: captioning 결과")
                    val receivingData = intent.getStringExtra(NfcAdapter.EXTRA_DATA)
                    Log.d(TAG, "captioning 결과 : $receivingData")
                    val textView = _binding.textCaption
                    textView.text = receivingData
                }
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    _binding.textConnection.text = "연결됨"
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    _binding.textConnection.text = "연결 안 됨"
                }
                BluetoothLeService.ACTION_GATT_CAPTIONING -> {
                    Log.d(TAG, "onReceive: captioning 결과")
                    val receivingData = intent.getStringExtra(NfcAdapter.EXTRA_DATA)
                    Log.d(TAG, "captioning 결과 : $receivingData")
                    val textView = _binding.textCaption
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


}