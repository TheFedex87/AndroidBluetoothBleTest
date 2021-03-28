package it.thefedex87.bletest.bluetooth

import android.content.Context
import android.content.pm.PackageManager

fun checkIfDeviceHasBle(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
}