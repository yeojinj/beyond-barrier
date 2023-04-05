package com.vd5.beyondb

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
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
import android.nfc.NfcAdapter.EXTRA_DATA
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationBarView
import com.vd5.beyondb.databinding.ActivityMainBinding
import com.vd5.beyondb.service.BluetoothLeService
import com.vd5.beyondb.ui.captioning.CaptioningFragment
import com.vd5.beyondb.ui.home.HomeFragment
import com.vd5.beyondb.ui.program.ProgramFragment
import com.vd5.beyondb.ui.settings.SettingsFragment
import java.util.*


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
            Toast.makeText(this, "기기가 블루투스를 지원하지 않습니다.", Toast.LENGTH_LONG).show()
            finish()
        }

        // TODO : 장치와 연결되어 있지 않고 실행할 때마다 호출 필요
        scanLeDevice(true)

        ///////////////////////////////////

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val navigationBarView = findViewById<NavigationBarView>(R.id.nav_view)
        navigationBarView.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment_activity_main, HomeFragment()).setTransition(
                        FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit()
                    return@OnItemSelectedListener true
                }
                R.id.navigation_program -> {
                    supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment_activity_main, ProgramFragment()).setTransition(
                        FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit()
                    return@OnItemSelectedListener true
                }
                R.id.navigation_captioning -> {
                    supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment_activity_main, CaptioningFragment()).setTransition(
                        FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit()
                    return@OnItemSelectedListener true
                }
                R.id.navigation_settings -> {
                    supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment_activity_main, SettingsFragment()).setTransition(
                        FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit()
                    return@OnItemSelectedListener true
                }
            }
            false
        })

        // TTS 객체 생성
        TTSinit()



//        val navView: BottomNavigationView = binding.navView
//
//        val navController = findNavController(R.id.nav_host_fragment_activity_main)
//        // Passing each menu ID as a set of Ids because each
//        // menu should be considered as top level destinations.
////        val appBarConfiguration = AppBarConfiguration(
////            setOf(
////                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_settings
////            )
////        )
////        setupActionBarWithNavController(navController, appBarConfiguration)
////
//        navView.setupWithNavController(navController)
    }

    private val PERMISSIONS_BT = arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_ALL_PERMISSION= 2
    private val SCAN_PERIOD: Long = 20000 // BLE 최대 스캔 시간
    private val handler = Handler(Looper.getMainLooper())

    private val TAG = "MainActivityDebug" // 디버그용 tag

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var scanning: Boolean = false

    fun scanLeDevice(enable: Boolean) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_BT, REQUEST_ALL_PERMISSION)
            return
        }
        when (enable) {
            true -> {
                handler.postDelayed({
                    if(scanning) {
                        Toast.makeText(this, "기기 검색에 실패하였습니다. 재시도 해주세요.", Toast.LENGTH_SHORT).show()
                        scanning = false
                        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
                    }
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
            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@MainActivity, PERMISSIONS_BT, REQUEST_ALL_PERMISSION)
                return
            }
            if(result?.device?.name != null)
                Log.d(TAG, "onScanResult: deviceName = " + result.device?.name)
            if(result?.device?.name != null && result.device.name.equals(deviceName)) {
                deviceAddress = result.device.address
                val gattServiceIntent = Intent(this@MainActivity, BluetoothLeService::class.java)
                bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                scanning = false
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
            }
        }
    }

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
                    bluetoothService?.connect(deviceAddress!!)
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Log.d(TAG, "onReceive: 서비스 발견 방송")
                    accessGattServices(bluetoothService?.getSupportedGattServices())
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    Log.d(TAG, "onReceive: READ or NOTIFY")
                    val receivingData = intent.getStringExtra(EXTRA_DATA)
                    Log.d(TAG, "onReceive: $receivingData")
                    // TODO receivingData fragment로 전달
                }
            }
        }
    }


    private val deviceName = BuildConfig.DEVICENAME
    private val SERVICE_UUID = BuildConfig.SERVICE_UUID
    private val UUID_CAPTION_REQUEST = BuildConfig.UUID_CAPTION_REQUEST
    private val UUID_CAPTION_RESULT = BuildConfig.UUID_CAPTION_RESULT
    private val UUID_PROGRAM_REQUEST = BuildConfig.UUID_PROGRAM_REQUEST
    private val UUID_PROGRAM_RESULT = BuildConfig.UUID_PROGRAM_RESULT


    private fun accessGattServices(gattServices: List<BluetoothGattService?>?) {
        Log.d(TAG, "accessGattServices: 서비스 접근 메소드")
        if (gattServices == null) return
        gattServices.forEach { gattService ->
            Log.d(TAG, "accessGattServices: service UUID ${gattService?.uuid}")
            val serviceUuid: String = gattService?.uuid.toString()
            if (serviceUuid == SERVICE_UUID) {
                beyondService = gattService
                Log.d(TAG, "captionService: ${beyondService?.uuid}")
                beyondService?.characteristics?.forEach { gattCharacteristic ->
                    Log.d(TAG, "accessGattServices: ${gattCharacteristic.uuid}")
                    val characteristicUuid: String = gattCharacteristic.uuid.toString()
                    if (characteristicUuid == UUID_CAPTION_REQUEST) captionRequestCharacteristic = gattCharacteristic
                    else if (characteristicUuid == UUID_CAPTION_RESULT) captionResultCharacteristic = gattCharacteristic
                    else if (characteristicUuid == UUID_PROGRAM_REQUEST) programRequestCharacteristic = gattCharacteristic
                    else if (characteristicUuid == UUID_PROGRAM_RESULT) programResultCharacteristic = gattCharacteristic
                }
                Toast.makeText(this, "TV와 연결되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var beyondService: BluetoothGattService? = null

    private var captionRequestCharacteristic: BluetoothGattCharacteristic? = null
    private var captionResultCharacteristic: BluetoothGattCharacteristic? = null
    private var programRequestCharacteristic: BluetoothGattCharacteristic? = null
    private var programResultCharacteristic: BluetoothGattCharacteristic? = null


    /**
     * 캡셔닝 요청 메소드
     * captioning fragment 에서 호출
     */
    fun captioningRequest() {
        // 일단 read 요청 한 번 보내기
        // result read 나올 때까지 계속 읽기
        if (captionRequestCharacteristic == null) return
        bluetoothService?.readCharacteristic(captionRequestCharacteristic!!)
        if (captionResultCharacteristic == null) return
        Log.d(TAG, "captioningRequest: polling 시작")
        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothService?.resultPolling(captionResultCharacteristic!!)
        }, 500)
    }

    /**
     * 프로그램 정보 요청 메소드
     * program fragment 에서 호출
     */
    fun programRequest() {
        // 일단 read 요청 한 번 보내기
        // result read 나올 때까지 계속 읽기
        if (programRequestCharacteristic == null) return
        bluetoothService?.readCharacteristic(programRequestCharacteristic!!)
        if (programResultCharacteristic == null) return
        Log.d(TAG, "captioningRequest: polling 시작")
        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothService?.resultPolling(programResultCharacteristic!!)
        }, 500)
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
            addAction(BluetoothLeService.ACTION_GATT_CAPTIONING)
            addAction(BluetoothLeService.ACTION_GATT_PROGRAM)
        }
    }

    @SuppressLint("MissingPermission")
    fun bluetoothOn(){
        val enableBtIntent = Intent(ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    /**
     * TTS 관련 메소드
     */
    var textToSpeech: TextToSpeech? = null

    fun TTSrun(string: String) {
        textToSpeech?.speak(string, TextToSpeech.QUEUE_FLUSH, null, null)
        textToSpeech?.playSilentUtterance(750, TextToSpeech.QUEUE_ADD,null) // deley시간 설정
    }

    fun TTSrun(string: String, utteranceId: String) {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)

        if (enabledServices.isNotEmpty()) {
            am.interrupt()
        }
        textToSpeech?.speak(string, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        textToSpeech?.playSilentUtterance(750, TextToSpeech.QUEUE_ADD,null) // deley시간 설정
    }

    fun TTSinit() {
        textToSpeech = TextToSpeech(this, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS) {
                val result = textToSpeech!!.setLanguage(Locale.KOREAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS","해당언어는 지원되지 않습니다.")
                    return@OnInitListener
                }
                textToSpeech!!.setSpeechRate(1.0f)
            }
        })
    }
}