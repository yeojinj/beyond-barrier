package com.vd5.beyondb

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.registerReceiver
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.vd5.beyondb.databinding.ActivityMainBinding
import com.vd5.beyondb.service.BluetoothLeService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bluetooth 관련 코드
            // 권한 요청하기
        ActivityCompat.requestPermissions(this, PERMISSIONS_BT, REQUEST_ALL_PERMISSION)
            // bluetooth가 꺼져있는 경우 켜기
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter?.isEnabled == false) {
                bluetoothOn()
            }
        } else {
            // 블루투스를 지원하지 않는 경우
            Log.d("bluetoothAdapter","Device doesn't support Bluetooth")
            // TODO : 블루투스를 지원하지 않는 기기는 사용이 불가하다는 UI 필요
        }

        // TODO : 장치와 연결되어 있지 않고 실행할 때마다 호출 필요
        scanLeDevice(true)

        ///////////////////////////////////

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private val PERMISSIONS_BT = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
    private val REQUEST_ENABLE_BT=1
    private val REQUEST_ALL_PERMISSION= 2
    private val SCAN_PERIOD: Long = 10000 // BLE 스캔 시간
    private val handler = Handler(Looper.getMainLooper())

    private val TAG = "MainActivityDebug" // 디버그용 tag

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var scanning: Boolean = false

    fun scanLeDevice(enable: Boolean) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_BT, REQUEST_ALL_PERMISSION)
            return
        }
        when (enable) {
            true -> {
                handler.postDelayed({
                    scanning = false
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
                }, SCAN_PERIOD)
                scanning = true
                bluetoothAdapter ?. bluetoothLeScanner ?. startScan (leScanCallback)
            }
            else -> {
                scanning = false
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
            }
        }
    }

    var deviceAddress: String? = null

    private val leScanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this@MainActivity, PERMISSIONS_BT, REQUEST_ALL_PERMISSION)
                return
            }
            if(result?.device?.name != null)
                Log.d(TAG, "onScanResult: deviceName = " + result.device?.name)
            if(result?.device?.name != null && result.device.name.equals("vd5")) {
                deviceAddress = result.device.address
                val gattServiceIntent = Intent(this@MainActivity, BluetoothLeService::class.java)
                bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                // TODO : 장치를 찾았으므로 스캔 종료 필요 !!!
                scanning = false
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(stopScancallback as ScanCallback?)
            }
        }
    }
    private val stopScancallback = object: ScanCallback() {}

    var connectionState = STATE_DISCONNECTED

    private var bluetoothService: BluetoothLeService? = null

    private val serviceConnection: ServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth")
                    finish()
                }
                bluetooth.connect(deviceAddress!!)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            bluetoothService = null
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: " + intent.action)
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connectionState = STATE_CONNECTED
                    Log.d(TAG, "onReceive: 연결됨 방송")
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connectionState = STATE_DISCONNECTED
                    Log.d(TAG, "onReceive: 연결 안됨 방송")
                    Log.d(TAG, "onReceive: 연결 재시도")
                    scanLeDevice(true)
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Log.d(TAG, "onReceive: 서비스 발견 방송")
                    Log.d(TAG, "onReceive: " + bluetoothService?.getSupportedGattServices())
                    accessGattServices(bluetoothService?.getSupportedGattServices())
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    Log.d(TAG, "onReceive: READ or NOTIFY")
                    val receivingData = intent.getStringExtra(NfcAdapter.EXTRA_DATA)
                    Log.d(TAG, "onReceive: $receivingData")
                    // TODO receivingData fragment로 전달
                }
            }
        }
    }

//    private val UUID_READ = "00001234-0000-1000-8000-00805f9b34fb"
//    private val UUID_READ = "24c33316-87b2-4159-9dbd-87d730e27745"
    private val UUID_READ = "5d3f4a23-cab6-4123-8c3a-71b75bf63fb2"

    // TODO UUID 설정 필요
//    private val UUID_CAPTION = "00005555-0000-1000-8000-00805f9b34fb"
//    private val UUID_PROGRAM = "00005555-0000-1000-8000-00805f9b34fb"

    private val UUID_CAPTION = "4a3bb1de-9fb9-4237-ab33-ae6ebebc643f"
    private val UUID_PROGRAM = "4a3bb1de-9fb9-4237-ab33-ae6ebebc643f"

    private fun accessGattServices(gattServices: List<BluetoothGattService?>?) {
        Log.d(TAG, "accessGattServices: 서비스 접근 메소드")
        if (gattServices == null) return
        var uuid: String?
        gattServices.forEach { gattService ->
            Log.d(TAG, "accessGattServices: service UUID ${gattService?.uuid}")
            ///////
            var serviceUuid: String = gattService?.uuid.toString()
            if (serviceUuid == UUID_CAPTION) {
                captionService = gattService
                Log.d(TAG, "captionService: ${captionService?.uuid}")
            } else if (serviceUuid == UUID_PROGRAM){
                programService = gattService
                Log.d(TAG, "programService: ${programService?.uuid}")
            }
            ///////

//            uuid = gattService?.uuid.toString()
//            Log.d(TAG, "accessGattServices: $uuid")
//            val gattCharacteristics = gattService?.characteristics
//            gattCharacteristics?.forEach { gattCharacteristic ->
//                uuid = gattCharacteristic.uuid.toString()
//                Log.d(TAG, "accessGattServices: characteristic $uuid")
//                if (uuid.equals(UUID_READ)) {
//                    Log.d(TAG, "accessGattServices: READ 요청")
//                    bluetoothService?.readCharacteristic(gattCharacteristic)
//                    Log.d(TAG, "accessGattServices: NOTIFY 시작")
//                    bluetoothService?.setCharacteristicNotification(gattCharacteristic, true)
//                }
//            }
        }
    }

    private var captionService: BluetoothGattService? = null
    private var programService: BluetoothGattService? = null


    /**
     * 캡셔닝 요청 메소드
     * captioning fragment 에서 호출
     */
    fun captioningRequest() {
        Log.d(TAG, "captioning Service UUID: ${captionService?.uuid}")
        val gattCharacteristics = captionService?.characteristics
        var uuid: String? = null
        gattCharacteristics?.forEach { gattCharacteristic ->
            uuid = gattCharacteristic.uuid.toString()
            Log.d(TAG, "accessGattServices: characteristic $uuid")
            if (uuid.equals(UUID_READ)) {
                Log.d(TAG, "accessGattServices: READ 요청")
                bluetoothService?.readCharacteristic(gattCharacteristic)
                Log.d(TAG, "accessGattServices: NOTIFY 시작")
                bluetoothService?.setCharacteristicNotification(gattCharacteristic, true)
            }
        }
    }

    /**
     * 프로그램 정보 요청 메소드
     * program fragment 에서 호출
     */
    fun programRequest() {
        Log.d(TAG, "program Service UUID: ${captionService?.uuid}")
        val gattCharacteristics = programService?.characteristics
        var uuid: String? = null
        gattCharacteristics?.forEach { gattCharacteristic ->
            uuid = gattCharacteristic.uuid.toString()
            Log.d(TAG, "accessGattServices: characteristic $uuid")
            if (uuid.equals(UUID_READ)) {
                Log.d(TAG, "accessGattServices: READ 요청")
                bluetoothService?.readCharacteristic(gattCharacteristic)
                Log.d(TAG, "accessGattServices: NOTIFY 시작")
                bluetoothService?.setCharacteristicNotification(gattCharacteristic, true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bluetoothService != null) {
            val result = bluetoothService!!.connect(deviceAddress!!)
            Log.d(TAG, "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
    }

    @SuppressLint("MissingPermission")
    fun bluetoothOn(){
        val enableBtIntent = Intent(ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }
}