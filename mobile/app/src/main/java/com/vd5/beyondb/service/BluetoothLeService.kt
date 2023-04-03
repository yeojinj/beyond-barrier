package com.vd5.beyondb.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter.EXTRA_DATA
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.vd5.beyondb.BuildConfig
import com.vd5.beyondb.R
import com.vd5.beyondb.util.Caption
import com.vd5.beyondb.util.Program
import com.vd5.beyondb.util.RetrofitService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


private const val TAG = "BluetoothLeService"

class BluetoothLeService: Service() {
    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    inner class LocalBinder: Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    fun initialize(): Boolean {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    private var bluetoothGatt: BluetoothGatt? = null

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                Log.d(TAG, "connect: $address")
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.")
                return false
            }
            // connect to the GATT server on the device
        } ?: run {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
    }
    private var connectionState = STATE_DISCONNECTED

    @SuppressLint("MissingPermission")
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "onConnectionStateChange: 정상적으로 연결됨")
                // successfully connected to the GATT Server
                connectionState = STATE_CONNECTED
                broadcastUpdate(ACTION_GATT_CONNECTED)
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "onConnectionStateChange: 정상적으로 연결 안됨")
                // disconnected from the GATT Server
                connectionState = STATE_DISCONNECTED
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "onServicesDiscovered: " + gatt?.services)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered: !!!")
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        private val baseUrl = "http://18.191.139.106:5000/api/"

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val value = characteristic.value
            Log.d(TAG, "readValue: ${String(value)}")
            if (String(value) != requestDefault) {
                if (String(value) != resultDefault) {
                    pollingState = false
                    //
                    if (String(value) == programFail) {
                        val failMessage = "로고 인식에 실패하였습니다.\n화면에 로고가 없을 수 도 있습니다."
                        broadcastUpdate(ACTION_REQUEST_FAIL, failMessage)
                    } else {
                        val charaUuid = characteristic.uuid.toString()
                        Log.d(TAG, "onCharacteristicRead: $charaUuid")
                        if (charaUuid == UUID_CAPTION_RESULT) {
                            Log.d(TAG, "onCharacteristicRead: 캡션 결과 http 요청")
                            val retrofit = Retrofit.Builder().baseUrl(baseUrl)
                                .addConverterFactory(GsonConverterFactory.create()).build()
                            val service = retrofit.create(RetrofitService::class.java)
                            val captionNum = String(value)
                            service.getCaption(captionNum).enqueue(object : Callback<Caption> {
                                override fun onResponse(call: Call<Caption>, response: Response<Caption>) {
                                    if(response.isSuccessful){
                                        val result: Caption? = response.body()
                                        Log.d(TAG, "onResponse 성공: " + result?.result)
                                        broadcastUpdate(ACTION_GATT_CAPTIONING, result!!)
                                    }else{
                                        Log.d(TAG, "onResponse 실패")
                                    }
                                }

                                override fun onFailure(call: Call<Caption>, t: Throwable) {
                                    Log.d(TAG, "onFailure 에러: " + t.message.toString())
                                }
                            })

                        } else if (charaUuid == UUID_PROGRAM_RESULT) {
                            // TODO 받은 프로그램 번호를 서버에 요청하기
                            val retrofit = Retrofit.Builder().baseUrl(baseUrl)
                                .addConverterFactory(GsonConverterFactory.create()).build()
                            val service = retrofit.create(RetrofitService::class.java)
                            val programNum = String(value)
                            service.getProgram(programNum).enqueue(object : Callback<Program> {
                                override fun onResponse(call: Call<Program>, response: Response<Program>) {
                                    if(response.isSuccessful){
                                        val result: Program? = response.body()
                                        Log.d(TAG, "onResponse 성공: ${result?.programId}")
                                        broadcastUpdate(ACTION_GATT_PROGRAM, result!!)
                                    }else{
                                        Log.d(TAG, "onResponse 실패")
                                    }
                                }

                                override fun onFailure(call: Call<Program>, t: Throwable) {
                                    Log.d(TAG, "onFailure 에러: " + t.message.toString())
                                }
                            })
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }

    }


    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, content: String) {
        val intent = Intent(action)
        intent.putExtra(EXTRA_DATA, content)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)
        Log.d(TAG, "broadcastUpdate: $action")
        val data: ByteArray? = characteristic.value
        if (data?.isNotEmpty() == true) {
            val stringData = String(data)
            intent.putExtra(EXTRA_DATA, stringData)
        }
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, program: Program) {
        val intent = Intent(action)
        intent.putExtra("program", program as java.io.Serializable)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, caption: Caption) {
        val intent = Intent(action)
        intent.putExtra("caption", caption as java.io.Serializable)
        sendBroadcast(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    @SuppressLint("MissingPermission")
    private fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    private val UUID_CAPTION_RESULT = BuildConfig.UUID_CAPTION_RESULT
    private val UUID_PROGRAM_RESULT = BuildConfig.UUID_PROGRAM_RESULT

    private val requestDefault = "0"
    private val resultDefault = "-1"
    private val programFail = "-2"

    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return bluetoothGatt?.services
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            Log.d(TAG, "readCharacteristic: 읽기 시작")
            gatt.readCharacteristic(characteristic)
        } ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
            return
        }
    }


    private var pollingState = false

    @SuppressLint("MissingPermission")
    fun resultPolling(gattCharacteristic: BluetoothGattCharacteristic) {
        pollingState = true
        if (bluetoothGatt == null) return
        //타이머 객체 선언
        val t_timer = Timer()
        // 반복 횟수
        var count = 0
        //타이머 동작 시간 지정 및 작업 내용 지정
        t_timer.schedule(object : TimerTask(){
            override fun run(){
                println("${count}")
                //카운트 값 증가
                count++
                //카운트 값이 15초가되면 타이머 종료
                if(count > 60) {
                    val charaUuid = gattCharacteristic.uuid.toString()
                    if (charaUuid == UUID_CAPTION_RESULT) broadcastUpdate(ACTION_GATT_CAPTIONING_FAIL, "요청에 실패하였습니다. 재요청 해주세요.")
                    if (charaUuid == UUID_PROGRAM_RESULT) broadcastUpdate(ACTION_GATT_PROGRAM_FAIL, "요청에 실패하였습니다. 재요청 해주세요.")
                    println("[polling 요청 초과]")
                    t_timer.cancel()
                } else if (!pollingState){
                    println("[polling 종료]")
                    t_timer.cancel()
                } else {
                    bluetoothGatt?.readCharacteristic(gattCharacteristic)
                    Log.d(TAG, "run: ${gattCharacteristic.uuid.toString()}")
                    Log.d(TAG, "run: read 요청!!!")
                }
            }
        },0, 250) // 바로 실행, 1초에 4회 요청
        println("[polling 시작]")
    }

    companion object {
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val ACTION_GATT_CAPTIONING =
            "com.vd5.bluetooth.le.ACTION_GATT_CAPTIONING"
        const val ACTION_GATT_PROGRAM =
            "com.vd5.bluetooth.le.ACTION_GATT_PROGRAM"
        const val ACTION_REQUEST_FAIL =
            "com.vd5.bluetooth.le.ACTION_REQUEST_FAIL"
        const val ACTION_GATT_CAPTIONING_FAIL =
            "com.vd5.bluetooth.le.ACTION_GATT_CAPTIONING_FAIL"
        const val ACTION_GATT_PROGRAM_FAIL =
            "com.vd5.bluetooth.le.ACTION_GATT_PROGRAM_FAIL"



        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2
    }
}