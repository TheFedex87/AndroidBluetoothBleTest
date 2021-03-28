package it.thefedex87.bletest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import it.thefedex87.bletest.bluetooth.BtConnectionState
import it.thefedex87.bletest.bluetooth.BluetoothService
import it.thefedex87.bletest.bluetooth.checkIfDeviceHasBle
import it.thefedex87.bletest.databinding.ActivityMainBinding
import it.thefedex87.bletest.utils.INTENT_EXTRA_DEVICE_ID
import it.thefedex87.bletest.utils.REQUEST_ENABLE_BT
import it.thefedex87.bletest.utils.REQUEST_FINE_LOCATION_PERM
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, DeviceAdapter.OnItemClickListener {
    private var isBleSupported = false

    private lateinit var btService: BluetoothService
    private var isBtServiceBound: Boolean = false

    private lateinit var devicesAdapter: DeviceAdapter

    private lateinit var binding: ActivityMainBinding

    private val btServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothService.MyBinder
            btService = binder.getService()
            isBtServiceBound = true

            observeBtState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBtServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        if(!checkIfDeviceHasBle(this)) {
            Toast.makeText(this, "Device does not support BLE", Toast.LENGTH_LONG).show()
        } else {
            isBleSupported = true
        }

        devicesAdapter = DeviceAdapter(this)
        binding.recyclerViewDevices.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerViewDevices.adapter = devicesAdapter

        binding.buttonScanBleDevices.setOnClickListener {
            if(isBtServiceBound) {
                btService.startDevicesScan()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if(isBleSupported) {
            if(checkFineLocationPermissionGranted()) {
                startBtService()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if(isBtServiceBound) {
            unbindService(btServiceConnection)
            isBtServiceBound = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_ENABLE_BT -> {
                if(resultCode == RESULT_OK) {
                    binding.apply {
                        buttonScanBleDevices.isEnabled = true
                        buttonScanBleDevices.text = "Start Scan"
                        progressBarScanning.visibility = View.GONE
                    }
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        startBtService()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        checkFineLocationPermissionGranted()
    }

    private fun startBtService() {
        Intent(this, BluetoothService::class.java).also {
            startService(it)
            bindService(it, btServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun observeBtState() {
        btService.btConnectionState.observe(this) {
            when(it) {
                BtConnectionState.BleNotReady -> {
                    binding.apply {
                        buttonScanBleDevices.isEnabled = false
                        buttonScanBleDevices.text = "Start Scan"
                        progressBarScanning.visibility = View.GONE
                    }
                }
                BtConnectionState.BleReady -> {
                    // Bluetooth adapter is ready, check if bt is on
                    if(!btService.isBluetoothEnabled()) {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                    } else {
                        // btService.startDevicesScan()
                        binding.apply {
                            buttonScanBleDevices.isEnabled = true
                            buttonScanBleDevices.text = "Start Scan"
                            progressBarScanning.visibility = View.GONE
                        }
                    }
                }
                BtConnectionState.ScanningDevices -> {
                    binding.apply {
                        buttonScanBleDevices.text = "Stop Scan"
                        progressBarScanning.visibility = View.VISIBLE
                    }
                }
            }
        }

        btService.btLeDevices.observe(this) {
            val devices: ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()
            for (device in it) {
                devices.add(device.value)
            }
            devicesAdapter.setDevices(devices)
        }
    }

    private fun checkFineLocationPermissionGranted(): Boolean {
        return if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            true
        } else {
            val perms = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            EasyPermissions.requestPermissions(this,
                "To use bluetooth you have to give fine location permission",
                REQUEST_FINE_LOCATION_PERM,
                *perms)
            false
        }
    }

    override fun onClick(device: BluetoothDevice) {
        val intent = Intent(this, DeviceDetailsActivity::class.java).apply {
            putExtra(INTENT_EXTRA_DEVICE_ID, device.address)
        }
        //btService!!.connectDeviceRequested = true
        startActivity(intent)
    }
}