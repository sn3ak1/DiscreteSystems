package com.example.discretesystems

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
import com.clj.fastble.scan.BleScanRuleConfig
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.type.DateTime
import java.util.Date


class MainActivity : AppCompatActivity() {

    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            //granted
        }else{
            //deny
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }
        }

    var handler: Handler = Handler()
    var runnable: Runnable? = null
    var delay = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT))
        }
        else{
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }

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
        var list  = mutableListOf<BleDevice>()

        checkPermissions()


        val buttonScan = findViewById<Button>(R.id.button_scan)
        buttonScan.setOnClickListener{
        }

        handler.postDelayed(Runnable {
            handler.postDelayed(runnable!!, delay.toLong())
            bleInstance.scan(object : BleScanCallback() {
                override fun onScanStarted(success: Boolean) {}
                override fun onScanning(bleDevice: BleDevice) {
                    val db = Firebase.firestore
                    if(bleDevice.name == "Oclean X"){
                        val instance = hashMapOf(
                            "value" to bleDevice.rssi,
                        )

                        db.collection("OcleanX")
                            .document(System.currentTimeMillis().toString()).
                            set(instance)
//                            .add(instance)
//                            .addOnSuccessListener { documentReference ->
//                                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
//                            }
//                            .addOnFailureListener { e ->
//                                Log.w(TAG, "Error adding document", e)
//                            }
                    }
                    list.add(bleDevice)
                }
                override fun onScanFinished(scanResultList: List<BleDevice>) {
                    var ss = "test\n"
                    for (x in list){
                        val s = x.name + " " + x.rssi + "\n"
                        ss += s
                    }
                    findViewById<TextView>(R.id.textView).text = ss
                    list.removeAll { true }
                }
            })
        }.also { runnable = it }, delay.toLong())

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