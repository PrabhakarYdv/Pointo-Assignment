package com.prabhakar.pointoassignment

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.prabhakar.pointoassignment.databinding.ActivityBleactivityBinding

class BLEActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBleactivityBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var scanCallback: ScanCallback
    lateinit var bluetoothLeScanner: BluetoothLeScanner


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBleactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        binding.btnScan.setOnClickListener {
            scanDevice()
        }
    }


    private fun scanDevice() {
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)

                runOnUiThread {
                    binding.apply {
                        if (ActivityCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                ActivityCompat.requestPermissions(
                                    this@BLEActivity,
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    ),
                                    1002
                                )
                            }

                        } else {
                            deviceName.text = "Device Name : ${result?.device?.name}"
                            deviceAddress.text = "Device Address : ${result?.device?.address}"
                        }

                    }
                }

                bluetoothGatt = result?.device?.connectGatt(this@BLEActivity, false, gattCallback)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
                for (result in results!!) {
                    onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Scan Failed : $errorCode",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
        bluetoothLeScanner.startScan(scanCallback)


    }

    private var gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                runOnUiThread {
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ActivityCompat.requestPermissions(
                                this@BLEActivity,
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ),
                                1002
                            )
                        }

                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Connected to ${gatt?.device?.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            }
        }


        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            runOnUiThread {
                binding.apply {
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ActivityCompat.requestPermissions(
                                this@BLEActivity,
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ),
                                1002
                            )
                        }

                    } else {
                        for (service in gatt?.services!!) {
                            deviceService.text = "Device Service : ${service.uuid}"
                            for (characteristic in service.characteristics) {
                                deviceCharacteristics.text =
                                    "Device Characteristics :${characteristic.uuid}"
                            }
                        }
                    }

                }
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothGatt?.close()
            bluetoothGatt = null
        }
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
    }

}