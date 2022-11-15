package com.example.discretesystems

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var beaconMac = ""

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
                        var s = x.name + " " + x.rssi + "\n"
                        ss += s
                    }
                    findViewById<TextView>(R.id.textView).text = ss
                    list.removeAll { true }
                }
            })
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