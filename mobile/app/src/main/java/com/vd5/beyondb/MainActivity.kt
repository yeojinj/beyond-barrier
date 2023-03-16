package com.vd5.beyondb

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
    private val handler = Handler()

    private val TAG = "MainActivityDebug" // 디버그용 tag

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var scanning: Boolean = false

    private fun scanLeDevice(enable: Boolean) {
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

    var bluetoothGatt: BluetoothGatt? = null
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
            Log.d(TAG, "onScanResult: deviceName = " + result?.device?.name)
            if(result?.device?.name != null && result.device.name.equals("vd5")) {
//                bluetoothGatt = result?.device?.connectGatt(this@MainActivity, false, gattCallback)
                deviceAddress = result.device.address
                Log.d(TAG, "onScanResult: 주소 = $deviceAddress")
                val gattServiceIntent = Intent(this@MainActivity, BluetoothLeService::class.java)
                bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
                // TODO : 장치를 찾았으므로 스캔 종료 필요 !!!
            }
        }
    }

    private var connectionState = STATE_DISCONNECTED

    val gattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> connectionState = STATE_CONNECTED
                BluetoothProfile.STATE_DISCONNECTED -> connectionState = STATE_DISCONNECTED
            }
            Log.d(TAG, "onConnectionStateChange: 현재 연결 상태 = $newState")
        }

    }

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
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connectionState = STATE_CONNECTED
                    Log.d(TAG, "onReceive: 연결됨 방송")
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connectionState = STATE_DISCONNECTED
                    Log.d(TAG, "onReceive: 연결 안됨 방송")
                }
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

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        }
    }

    @SuppressLint("MissingPermission")
    fun bluetoothOn(){
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }
}