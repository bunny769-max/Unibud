package com.unibud.app.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Actions that Android itself performs. Android shows its own pairing dialog and settings UI. */
object BluetoothActions {

    /** Starts pairing. Android shows the system pairing dialog. Returns false if it could not start. */
    @SuppressLint("MissingPermission")
    fun startPairing(context: Context, address: String): Boolean {
        if (!BluetoothPermissions.hasConnect(context)) return false
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return false
        return try {
            adapter.getRemoteDevice(address).createBond()
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    /** Connecting and disconnecting audio is done by Android, so we open its Bluetooth settings. */
    fun openBluetoothSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
