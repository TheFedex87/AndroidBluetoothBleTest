package it.thefedex87.bletest

import android.bluetooth.BluetoothGattCharacteristic
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.thefedex87.bletest.databinding.CharacteristicItemBinding
import it.thefedex87.bletest.databinding.DeviceItemBinding

class CharacteristicAdapter constructor(private val listener: OnItemClickListener,
                                        private val characteristics: List<BluetoothGattCharacteristic>) : RecyclerView.Adapter<CharacteristicAdapter.CharacteristicHolder>() {

    /*fun setBtCharacteristics(characteristics: List<BluetoothGattCharacteristic>) {
        this.characteristics = services
        notifyDataSetChanged()
    }*/

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacteristicHolder {
        val view = CharacteristicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CharacteristicHolder(view)
    }

    override fun onBindViewHolder(holder: CharacteristicHolder, position: Int) {
        holder.bind(characteristics[position])
    }

    override fun getItemCount(): Int = characteristics.count()

    inner class CharacteristicHolder(private val binding: CharacteristicItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(characteristic: BluetoothGattCharacteristic) {
            binding.textViewCharacteristic.setOnClickListener {
                listener.onClick(characteristic)
            }
            binding.textViewCharacteristic.text = characteristic.uuid.toString()
        }
    }

    interface OnItemClickListener {
        fun onClick(characteristic: BluetoothGattCharacteristic)
    }
}