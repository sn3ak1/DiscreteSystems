package com.example.discretesystems

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice


class MainActivity : AppCompatActivity() {

    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
        }else{
            //deny
        }
    }
    lateinit var n:String;

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE))
        }
        else{
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }

        val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        //bluetoothAdapter.name = "gosiaaa"

        val bleInstance =  BleManager.getInstance()
        bleInstance.init(getApplication())
        if(!BleManager.getInstance().isSupportBle)
            throw Exception("Ble not supported")


        bleInstance.enableLog(true)
            .setReConnectCount(1, 5000)
            .setSplitWriteNum(20)
            .setConnectOverTime(10000)
            .setOperateTimeout(5000);

        bleInstance.enableBluetooth()
        bleInstance.enableLog(true)

//        val scanRuleConfig = BleScanRuleConfig.Builder()
//            .setServiceUuids(null)
//            .setDeviceName(true, "names")
//            .setAutoConnect(false)
//            .setScanTimeOut(10000)
//            .build()
//
//        bleInstance.initScanRule(scanRuleConfig)

        var list  = mutableListOf<BleDevice>()
        checkPermissions()


        val buttonScan = findViewById<Button>(R.id.button_scan)
        buttonScan.setOnClickListener{

            bleInstance.scan(object : BleScanCallback() {
                override fun onScanStarted(success: Boolean) {}
                override fun onScanning(bleDevice: BleDevice) {

                    list.add(bleDevice)
                }
                override fun onScanFinished(scanResultList: List<BleDevice>) {

                    var ss = "test\n"
                    for (x in list){
//                        if(x.name == "buu"){
//                            var s  = "aa" + " " + x.rssi + "\n";
//                            ss += s
//                            continue
//                        }
                        var s  =x.name + " " + x.rssi + "\n"
                        ss += s
                    }
                    findViewById<TextView>(R.id.textView).text = ss
                    list.removeAll { true }
                }
            })


            val advSettings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build()

            val advData = AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
//                .addServiceUuid(mCurrentServiceFragment.getServiceUUID())
                .build()

            val advScanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build()

            val advCallback: AdvertiseCallback = object : AdvertiseCallback() {
                override fun onStartFailure(errorCode: Int) {
                    super.onStartFailure(errorCode)
                    Log.e(TAG, "Not broadcasting: $errorCode")
                    var statusText: Int
                    when (errorCode) {
                        ADVERTISE_FAILED_ALREADY_STARTED -> Log.w(
                            TAG,
                            "ADVERTISE_FAILED_ALREADY_STARTED"
                        )
                        ADVERTISE_FAILED_DATA_TOO_LARGE -> Log.w(
                            TAG,
                            "ADVERTISE_FAILED_DATA_TOO_LARGE"
                        )
                        ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> Log.w(
                            TAG,
                            "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
                        )
                        ADVERTISE_FAILED_INTERNAL_ERROR -> Log.w(
                            TAG,
                            "ADVERTISE_FAILED_INTERNAL_ERROR"
                        )
                        ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> Log.w(
                            TAG,
                            "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
                        )
                        else -> Log.wtf(TAG, "Unhandled error: $errorCode")
                    }
                }

                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    super.onStartSuccess(settingsInEffect)
                    Log.v(TAG, "Advertising started")
                }
            }

            bluetoothAdapter.bluetoothLeAdvertiser
                .startAdvertising(advSettings, advData, advScanResponse, advCallback)
        }


        val buttonBeacon = findViewById<Button>(R.id.button_beacon)
        buttonBeacon.setOnClickListener{

        }

    }
    private fun checkPermissions() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "nie dziala", Toast.LENGTH_LONG).show()
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        //bluetoothAdapter.name = "buu";
        val tv = findViewById<TextView>(R.id.textView)
        tv.text=bluetoothAdapter.name
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val permissionDeniedList: MutableList<String> = ArrayList()
        for (permission in permissions) {
            val permissionCheck = ContextCompat.checkSelfPermission(this, permission)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
//                onPermissionGranted(permission)
            } else {
                permissionDeniedList.add(permission)
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            val deniedPermissions = permissionDeniedList.toTypedArray()
            ActivityCompat.requestPermissions(
                this,
                deniedPermissions,
                2
            )
        }
    }

}