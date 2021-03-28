package it.thefedex87.bletest

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.thefedex87.bletest.databinding.DeviceItemBinding

class DeviceAdapter constructor(private val listener: OnItemClickListener) : RecyclerView.Adapter<DeviceAdapter.DeviceHolder>() {
    private var devices: List<BluetoothDevice> = listOf()

    fun setDevices(devices: List<BluetoothDevice>) {
        this.devices = devices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceHolder {
        val view = DeviceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.count()

    inner class DeviceHolder(private val binding: DeviceItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: BluetoothDevice) {
            binding.textViewDeviceAddress.text = "${device.name} - ${device.address}"
            binding.root.setOnClickListener {
                listener.onClick(device)
            }
        }
    }

    interface OnItemClickListener {
        fun onClick(device: BluetoothDevice)
    }
}