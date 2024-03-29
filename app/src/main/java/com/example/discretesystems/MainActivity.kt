package com.example.discretesystems

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanRecord
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.EditText
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date
import kotlin.math.pow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



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

    var dsUserAppBLENamePrefix = "DsClient";

    private lateinit var fusedLocationClient: FusedLocationProviderClient
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null
                Log.d("aaaa",location.toString())
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

        fun getSignalFalloff(rssi: Int): Double {
            var signalFalloff = 3.0
            when (rssi) {
                in 0..-70 -> signalFalloff = 2.0
                in -70..-80 -> signalFalloff = 2.5
                in -80..-90 -> signalFalloff = 3.0
                in -90..-95 -> signalFalloff = 3.5
                else -> {
                    signalFalloff = 6.0
                }
            }
            return signalFalloff
        }

        class TrackableBLEDevice constructor(
            value_rssi: Int = 0,
            roomID: String = "",
            beaconID: String = "",
            foundName: String = "",
            time: Long = 0,
            meters: Double = 0.0,
            x: Double = 0.0,
            y: Double = 0.0,
            Tx: Int = 0,
        ) {
            val value_rssi: Int = value_rssi
            val roomID: String = roomID
            val beaconID: String = beaconID
            val foundName: String = foundName
            val time: Long = time
            val meters: Double = meters
            val x: Double = x
            val y: Double = y
        }

        val foundDevices = mutableListOf<TrackableBLEDevice>()
        checkPermissions()

        val button = findViewById<Button>(R.id.button)

        button.setOnClickListener{
            handler.postDelayed(Runnable {
                handler.postDelayed(runnable!!, delay.toLong())

                val db = Firebase.firestore
                var seenDeviceNames : MutableList<String> = ArrayList()

                bleInstance.scan(object : BleScanCallback() {
                    override fun onScanStarted(success: Boolean) {}
                    override fun onScanning(bleDevice: BleDevice) {
                        if(seenDeviceNames.indexOf(bleDevice.name) == -1 && bleDevice.name != null
                            && bleDevice.name.contains(dsUserAppBLENamePrefix, ignoreCase=false) ){

                            val currTime: Long = System.currentTimeMillis()

                            val rssi_to_meters: Double = (10.0).pow(((
                                        -69.0 -(bleDevice.rssi))
                                        /(10.0 * getSignalFalloff(bleDevice.rssi))
                                    )
                            )
                            var currDevice: TrackableBLEDevice = TrackableBLEDevice(
                                value_rssi = bleDevice.rssi,
                                roomID = findViewById<EditText>(R.id.roomId).text.toString(),
                                beaconID = findViewById<EditText>(R.id.referenceBeacon).text.toString(),
                                foundName = bleDevice.name,
                                time =  currTime,
                                meters = rssi_to_meters,
                                x = findViewById<EditText>(R.id.yBeaconCoordinate).text.toString().toDouble(),
                                y = findViewById<EditText>(R.id.xBeaconCoordinate).text.toString().toDouble()
                            )

                            foundDevices.add(currDevice)
                            val instance = hashMapOf(
                                "value" to currDevice.value_rssi,
                                "roomID" to currDevice.roomID,
                                "beaconID" to currDevice.beaconID,
                                "time" to currDevice.time.toString(),
                                "meters" to currDevice.meters,
                                "y" to currDevice.y.toString(),
                                "x" to currDevice.x.toString()
                            )

                            seenDeviceNames.add(bleDevice.name)

                            db.collection("Devices").document("Bluetooth").collection(bleDevice.name)
                                .document(currTime.toString()).
                                set(instance)

//
                        }
                    }
                    override fun onScanFinished(scanResultList: List<BleDevice>) {
                        var debugInfo = "test | ID:      | RSSI:      | DISTANCE (m):      \n"
                        for (currBLEDevice in foundDevices){
                            val slug = "ID: " + currBLEDevice.foundName +
                                    " || RSSI: " + currBLEDevice.value_rssi.toString() +
                                    " || DISTANCE (m): " + currBLEDevice.meters.toString() +"\n"
                            debugInfo += slug
                        }
                        findViewById<TextView>(R.id.textView).text = debugInfo
//                        list.removeAll { true }
                        foundDevices.removeAll { true }
                        seenDeviceNames.removeAll { true }
                    }
                })
            }.also { runnable = it }, delay.toLong())
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
        if (permissionDeniedList.isNotEmpty()) {
            val deniedPermissions = permissionDeniedList.toTypedArray()
            ActivityCompat.requestPermissions(
                this,
                deniedPermissions,
                2
            )
        }
    }
}