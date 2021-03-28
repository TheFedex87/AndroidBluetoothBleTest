package it.thefedex87.bletest

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import it.thefedex87.bletest.bluetooth.BluetoothService
import it.thefedex87.bletest.databinding.ActivityCharacteristicBinding
import it.thefedex87.bletest.utils.INTENT_EXTRA_CHARACTERISTIC_UUID
import it.thefedex87.bletest.utils.TAG
import it.thefedex87.bletest.utils.INTENT_EXTRA_DEVICE_ID
import it.thefedex87.bletest.utils.INTENT_EXTRA_SERVICE_UUID
import kotlinx.coroutines.flow.collect

class CharacteristicActivity : AppCompatActivity() {
    private lateinit var serviceUUID: String
    private lateinit var characteristicUUID: String

    private var btService: BluetoothService? = null
    private var isBtServiceBound: Boolean = false

    private lateinit var binding: ActivityCharacteristicBinding

    private val btServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothService.MyBinder
            btService = binder.getService()
            isBtServiceBound = true

            readCharacteristic()
            setupObservers()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBtServiceBound = false
            btService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacteristicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        if (intent != null && intent.extras != null) {
            if (intent!!.extras!!.containsKey(INTENT_EXTRA_SERVICE_UUID) &&
                intent!!.extras!!.containsKey(INTENT_EXTRA_CHARACTERISTIC_UUID)
            ) {
                serviceUUID = intent!!.extras!![INTENT_EXTRA_SERVICE_UUID] as String
                characteristicUUID = intent!!.extras!![INTENT_EXTRA_CHARACTERISTIC_UUID] as String
                setupUi(characteristicUUID)
            }
        } else {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    override fun onStop() {
        super.onStop()
        if(isBtServiceBound) {
            btService!!.registerToCharacteristic(serviceUUID, characteristicUUID, false)
            unbindService(btServiceConnection)
        }
    }

    private fun bindService() {
        Intent(this, BluetoothService::class.java).also {
            //startService(intent)
            bindService(it, btServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun setupUi(characteristicUUID: String) {
        supportActionBar!!.title = characteristicUUID

        binding.buttonWriteValue.setOnClickListener {
            writeCharacteristic()
        }
    }

    private fun writeCharacteristic() {
        val value = binding.editTextCharacteristicValue.text.toString()
        btService!!.writeCharacteristic(serviceUUID, characteristicUUID, value)
    }

    private fun readCharacteristic() {
        btService!!.readCharacteristic(serviceUUID, characteristicUUID)
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            btService!!.characteristicReadValue.collect {
                Log.d(TAG, "Received characteristic value")
                Toast.makeText(this@CharacteristicActivity, "Reade value: $it", Toast.LENGTH_LONG)
                    .show()
                binding.textViewCharacteristicValue.text = it

                btService!!.registerToCharacteristic(serviceUUID, characteristicUUID, true)
            }
        }

        lifecycleScope.launchWhenStarted {
            btService!!.characteristicUpdated.collect {
                btService!!.readCharacteristic(serviceUUID, characteristicUUID)
            }
        }
    }
}