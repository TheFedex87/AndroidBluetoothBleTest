package it.thefedex87.bletest

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.thefedex87.bletest.bluetooth.BluetoothService
import it.thefedex87.bletest.databinding.DeviceItemBinding
import it.thefedex87.bletest.databinding.ServiceItemBinding

class ServiceAdapter constructor(
        private val listener: OnItemClickListener,
        private val context: Context) : RecyclerView.Adapter<ServiceAdapter.ServiceHolder>() {

    private var services: List<BluetoothGattService> = listOf()

    fun setBtServices(services: List<BluetoothGattService>) {
        this.services = services
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceHolder {
        val view = ServiceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ServiceHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceHolder, position: Int) {
        holder.bind(services[position])
    }

    override fun getItemCount(): Int = services.count()

    inner class ServiceHolder(private val binding: ServiceItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(service: BluetoothGattService) {
            binding.textViewService.text = "${service.uuid}"

            val characteristicAdapter = CharacteristicAdapter(object : CharacteristicAdapter.OnItemClickListener {
                override fun onClick(characteristic: BluetoothGattCharacteristic) {
                    listener.onClick(service, characteristic)
                }
            }, service.characteristics);
            binding.recyclerViewCharacteristics.adapter = characteristicAdapter
            binding.recyclerViewCharacteristics.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

            /*binding.root.setOnClickListener {
                listener.onClick(service)
            }*/
        }
    }

    interface OnItemClickListener {
        fun onClick(service: BluetoothGattService, characteristic: BluetoothGattCharacteristic)
    }
}