package it.thefedex87.bletest

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import it.thefedex87.bletest.bluetooth.BluetoothService
import it.thefedex87.bletest.bluetooth.DeviceConnectionState
import it.thefedex87.bletest.databinding.ActivityDeviceDetailsBinding
import it.thefedex87.bletest.utils.INTENT_EXTRA_CHARACTERISTIC_UUID
import it.thefedex87.bletest.utils.INTENT_EXTRA_DEVICE_ID
import it.thefedex87.bletest.utils.INTENT_EXTRA_SERVICE_UUID
import it.thefedex87.bletest.utils.TAG
import java.util.*

class DeviceDetailsActivity : AppCompatActivity(), ServiceAdapter.OnItemClickListener {
    private lateinit var deviceAddress: String

    private var btService: BluetoothService? = null
    private var isBtServiceBound: Boolean = false

    private lateinit var binding: ActivityDeviceDetailsBinding

    private lateinit var adapter: ServiceAdapter

    private val btServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothService.MyBinder
            btService = binder.getService()
            isBtServiceBound = true

            observeDeviceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBtServiceBound = false
            btService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        if(intent != null && intent.extras != null) {
            if(intent!!.extras!!.containsKey(INTENT_EXTRA_DEVICE_ID)) {
                deviceAddress = intent!!.extras!![INTENT_EXTRA_DEVICE_ID] as String
                setupUi()
                bindService()
            }
        } else {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(isBtServiceBound) {
            Log.d(TAG,"Unbinding service")
            btService!!.closeGattConnection()
            unbindService(btServiceConnection)
        }
    }

    private fun setupUi() {
        adapter = ServiceAdapter(this, this)
        binding.apply {
            recyclerViewDeviceServices.adapter = adapter
            recyclerViewDeviceServices.layoutManager = LinearLayoutManager(this@DeviceDetailsActivity, LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun observeDeviceState() {
        btService!!.deviceConnectionState.observe(this) {
            when(it) {
                DeviceConnectionState.Disconnected -> {
                    binding.progressBarLoading.visibility = View.GONE
                    binding.textViewOperation.visibility = View.GONE
                    binding.recyclerViewDeviceServices.visibility = View.GONE


                    connectDevice()
                }
                DeviceConnectionState.Connecting -> {
                    binding.progressBarLoading.visibility = View.VISIBLE
                    binding.textViewOperation.visibility = View.VISIBLE
                    binding.textViewOperation.text = "Connecting..."
                    binding.recyclerViewDeviceServices.visibility = View.GONE
                }
                DeviceConnectionState.Connected -> {
                    binding.progressBarLoading.visibility = View.VISIBLE
                    binding.textViewOperation.visibility = View.VISIBLE
                    binding.recyclerViewDeviceServices.visibility = View.GONE
                    binding.textViewOperation.text = "Connected...Finding Services..."
                    btService!!.findServices()
                }
                DeviceConnectionState.ServicesRetrieved -> {
                    binding.progressBarLoading.visibility = View.GONE
                    binding.textViewOperation.visibility = View.GONE
                    binding.recyclerViewDeviceServices.visibility = View.VISIBLE
                    binding.textViewOperation.text = ""
                }
            }
        }

        btService!!.btGattServices.observe(this) {
            Log.d(TAG, it.toString())
            adapter.setBtServices(it)
        }
    }

    private fun bindService() {
        Intent(this, BluetoothService::class.java).also {
            //startService(intent)
            bindService(it, btServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun connectDevice() {
        btService!!.connectDevice(deviceAddress)
        /*btService!!.btLeDevices.observe(this) {
            val device = it[deviceAddress]
            Log.d(TAG, "Details device: ${device!!.address}")

        }*/
    }

    override fun onClick(service: BluetoothGattService, characteristic: BluetoothGattCharacteristic) {
        // btService!!.readCharacteristic(characteristic)
        val intent = Intent(this, CharacteristicActivity::class.java).apply {
            putExtra(INTENT_EXTRA_CHARACTERISTIC_UUID, characteristic.uuid.toString()).apply {
                putExtra(INTENT_EXTRA_SERVICE_UUID, service.uuid.toString())
            }
        }
        startActivity(intent)
    }
}