package com.prabhakar.pointoassignment

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.prabhakar.pointoassignment.databinding.ActivityBleactivityBinding

class BLEActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBleactivityBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>

    private val requestBluetoothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true &&
            permissions[android.Manifest.permission.BLUETOOTH] == true &&
            permissions[android.Manifest.permission.BLUETOOTH_SCAN] == true &&
            permissions[android.Manifest.permission.BLUETOOTH_ADMIN] == true &&
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            startScanningForDevices()
        } else {
            requestPermissions()
            Toast.makeText(this, "Permissions required for BLE", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startScanningForDevices()
            } else {

                Toast.makeText(
                    this,
                    "Bluetooth is required to scan for devices",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        if (!bluetoothAdapter.isEnabled || bluetoothAdapter == null) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(intent)
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions()
            }
            if (scanCallback != null) {
                bluetoothLeScanner?.startScan(scanCallback)
            }
        }

        requestPermissions()
        binding.btnScan.setOnClickListener {
            requestPermissions()
            startScanningForDevices()
        }
    }


    private fun requestPermissions() {
        requestBluetoothPermissions.launch(
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }


    private fun startScanningForDevices() {
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val device = result.device
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()

                } else {
                    connectToDevice(device)
                    Log.d("BLE", "Device found: ${device.name}, ${device.address}")
                    runOnUiThread {
                        binding.apply {
                            deviceName.text = "Device Name: ${device.name}"
                            deviceAddress.text = "Device Address: ${device.address}"
                        }
                    }
                }
            }
        }

        bluetoothLeScanner?.startScan(scanCallback)
    }

    private fun stopScanningForDevices(scanCallback: ScanCallback) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        requestPermissions()
                    }
                    Log.d("BLE", "Connected to ${device.name}")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BLE", "Disconnected from ${device.name}")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                for (service in gatt.services) {
                    Log.d("BLE", "Service discovered: ${service.uuid}")
                    runOnUiThread {
                        binding.apply {
                            deviceService.text = "Device Service: ${service.uuid}"
                        }
                    }
                    for (characteristic in service.characteristics) {
                        Log.d("BLE", "Service discovered: ${characteristic.uuid}")
                        runOnUiThread {
                            binding.apply {
                                deviceCharacteristics.text =
                                    "Characteristics: ${characteristic.uuid}"
                            }
                        }
                    }
                }
            }
        }

        device.connectGatt(this, false, gattCallback)
    }

}