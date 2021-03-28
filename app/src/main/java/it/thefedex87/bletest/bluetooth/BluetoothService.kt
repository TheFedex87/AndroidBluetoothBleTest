package it.thefedex87.bletest.bluetooth

import android.app.Service
import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.le.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import it.thefedex87.bletest.utils.TAG
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEmpty
import java.util.*


class BluetoothService : Service() {
    private val myBinder = MyBinder()
    override fun onBind(intent: Intent?): IBinder? = myBinder
    inner class MyBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var btLeScanner: BluetoothLeScanner? = null
    private var btGatt: BluetoothGatt? = null

    private var scanning: Boolean = false
    private val scanDelay: Long = 10000



    private val _btConnectionState = MutableLiveData<BtConnectionState>(BtConnectionState.BleNotReady)
    val btConnectionState: LiveData<BtConnectionState> get() = _btConnectionState

    private val _deviceConnectionState = MutableLiveData<DeviceConnectionState>(
        DeviceConnectionState.Disconnected
    )
    val deviceConnectionState: LiveData<DeviceConnectionState> get() = _deviceConnectionState

    private val _btLeDevices = MutableLiveData<HashMap<String, BluetoothDevice>>(hashMapOf())
    val btLeDevices: LiveData<HashMap<String, BluetoothDevice>> get() = _btLeDevices

    private val _btGattServices = MutableLiveData<List<BluetoothGattService>>(listOf())
    val btGattServices: LiveData<List<BluetoothGattService>> get() = _btGattServices

    private val _characteristicReadValue = MutableSharedFlow<String>()
    val characteristicReadValue: Flow<String> get() = _characteristicReadValue

    private val _characteristicUpdated = MutableSharedFlow<Boolean>()
    val characteristicUpdated: Flow<Boolean> get() = _characteristicUpdated

    //var connectDeviceRequested = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate BT Service")
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothManager = getSystemService(BluetoothManager::class.java)
            bluetoothAdapter = bluetoothManager?.adapter
        } else {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        }
        _btConnectionState.value = BtConnectionState.BleReady
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy BT Service")
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter!!.isEnabled
    }

    fun startDevicesScan() {
        Log.d(TAG, "Starting scan of bluetooth devices")
        if(!scanning) {
            scanning = true
            _btLeDevices.value = hashMapOf()
            if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val filter = ScanFilter.Builder().setServiceUuid(
                    ParcelUuid.fromString("00009800-0000-1000-8000-00805f9b34fb")
                ).build()
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                    .build()

                btLeScanner = bluetoothAdapter!!.bluetoothLeScanner
                GlobalScope.launch {
                    delay(scanDelay)
                    if(scanning) {
                        scanning = false
                        btLeScanner!!.stopScan(btLeScanCallbak)
                        withContext(Dispatchers.Main) {
                            _btConnectionState.value = BtConnectionState.BleReady
                        }
                    }
                }
                btLeScanner!!.startScan(
                    listOf(filter),
                    settings,
                    btLeScanCallbak
                )
                _btConnectionState.value = BtConnectionState.ScanningDevices
            } else {
                bluetoothAdapter!!.startLeScan(object : BluetoothAdapter.LeScanCallback {
                    override fun onLeScan(
                        device: BluetoothDevice?,
                        rssi: Int,
                        scanRecord: ByteArray?
                    ) {
                        TODO("Not yet implemented")
                    }
                })
            }
        } else {
            if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                scanning = false
                btLeScanner!!.stopScan(btLeScanCallbak)
                _btConnectionState.value = BtConnectionState.BleReady
            } else {
                bluetoothAdapter!!.stopLeScan(object : BluetoothAdapter.LeScanCallback {
                    override fun onLeScan(
                        device: BluetoothDevice?,
                        rssi: Int,
                        scanRecord: ByteArray?
                    ) {
                        TODO("Not yet implemented")
                    }
                })
            }
        }
    }

    private val btLeScanCallbak: ScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if(result != null) {
                if(!_btLeDevices.value!!.containsKey(result.device.address)) {
                    Log.d(TAG, "Found new device ${result.device.address}")
                    val newMap = _btLeDevices.value!!
                    result.device.bondState
                    newMap[result.device.address] = result.device
                    _btLeDevices.value = newMap
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(TAG, "Scan failed with code $errorCode")
        }
    }

    fun connectDevice(deviceUuid: String) {
        //if(connectDeviceRequested) {
        //    connectDeviceRequested = false
            _deviceConnectionState.value = DeviceConnectionState.Connecting
            val device = btLeDevices.value?.get(deviceUuid)

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device?.connectGatt(this, false, bluetoothGattConnect, BluetoothDevice.TRANSPORT_LE)
            } else {
                device?.connectGatt(this, false, bluetoothGattConnect)
            }
        //} else {
        //    _deviceConnectionState.value = DeviceConnectionState.ServicesRetrieved
        //}
    }

    fun findServices() {
        btGatt!!.discoverServices()
    }

    fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {
        val services = _btGattServices.value
        val characteristic = services!!.firstOrNull {
            it.uuid == UUID.fromString(serviceUuid)
        }!!.getCharacteristic(UUID.fromString(characteristicUuid))

        btGatt!!.readCharacteristic(characteristic)
    }

    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, payload: String) {
        val services = _btGattServices.value
        val characteristic = services!!.firstOrNull {
            it.uuid == UUID.fromString(serviceUuid)
        }!!.getCharacteristic(UUID.fromString(characteristicUuid))

        btGatt?.let { gatt ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.value = payload.decodeHex()
            gatt.writeCharacteristic(characteristic)
        }
    }

    fun registerToCharacteristic(serviceUuid: String, characteristicUuid: String, register: Boolean) {
        val services = _btGattServices.value
        val characteristic = services!!.firstOrNull {
            it.uuid == UUID.fromString(serviceUuid)
        }!!.getCharacteristic(UUID.fromString(characteristicUuid))

        btGatt?.let { gatt ->
            gatt.setCharacteristicNotification(characteristic, register)
            val uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            val descriptor = characteristic.getDescriptor(uuid)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    fun closeGattConnection() {
        if(btGatt != null) {
            GlobalScope.launch(Dispatchers.Main) {
                _deviceConnectionState.value = DeviceConnectionState.Disconnected
            }
            btGatt!!.close()
            btGatt = null
            Log.d(TAG, "BT Gatt connection closed")
        }
    }

    private val bluetoothGattConnect: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "BT connection OK: $status - $newState")
                if(newState == BluetoothProfile.STATE_CONNECTED) {
                    btGatt = gatt
                    GlobalScope.launch(Dispatchers.Main) {
                        _deviceConnectionState.value = DeviceConnectionState.Connected
                    }
                } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if(btGatt != null) {
                        btGatt!!.close()
                    }
                    btGatt = null
                    GlobalScope.launch(Dispatchers.Main) {
                        _deviceConnectionState.value = DeviceConnectionState.Disconnected
                    }
                }
            } else if(status == 133) {
                btGatt!!.close()
                btGatt = null
            } else {
                GlobalScope.launch(Dispatchers.Main) {
                    _deviceConnectionState.value = DeviceConnectionState.Disconnected
                }
                Log.d(TAG, "BT connection error: $status - $newState")
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            GlobalScope.launch(Dispatchers.Main) {
                _btGattServices.value = gatt!!.services
                _deviceConnectionState.value = DeviceConnectionState.ServicesRetrieved
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            when(status) {
                GATT_SUCCESS -> {
                    if (status == GATT_SUCCESS) {
                        Log.d(
                            TAG,
                            "Characteristic with uuid ${characteristic!!.uuid} has a value of: ${characteristic.value.toHexString()}"
                        )
                        GlobalScope.launch(Dispatchers.Main) {
                            Log.d(TAG, "Emitting characteristic value")
                            _characteristicReadValue.emit(characteristic.value.toHexString())
                        }
                    }
                }
                else -> {
                    Log.d(TAG, "Error reading characteristic: $status")
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.d(TAG, "onCharacteristicWrite: $status")
            when(status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.d(TAG, "Characteristic wrote properly")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            Log.d(TAG, "onCharacteristicChanged: ${characteristic!!.uuid}")
            GlobalScope.launch(Dispatchers.Main) {
                Log.d(TAG, "Emitting characteristic update")
                _characteristicUpdated.emit(true)
            }
        }
    }
}

fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

fun String.decodeHex(): ByteArray = chunked(2)
    .map { it.toByte(16) }
    .toByteArray()

sealed class BtConnectionState {
    object BleNotReady : BtConnectionState()
    object BleReady : BtConnectionState()
    object ScanningDevices : BtConnectionState()
}

sealed class DeviceConnectionState {
    object Disconnected : DeviceConnectionState()
    object Connecting : DeviceConnectionState()
    object Connected : DeviceConnectionState()
    object ServicesRetrieved : DeviceConnectionState()
}