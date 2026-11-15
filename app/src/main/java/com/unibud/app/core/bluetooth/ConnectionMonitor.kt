package com.unibud.app.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Live set of device addresses currently connected over the A2DP (media) or Headset (calls)
 * profile. Event driven, with no polling.
 */
class ConnectionMonitor(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun connectedAddresses(): Flow<Set<String>> = callbackFlow {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !BluetoothPermissions.hasConnect(context)) {
            trySend(emptySet())
            awaitClose { }
            return@callbackFlow
        }

        val a2dpSet = ConcurrentHashMap.newKeySet<String>()
        val headsetSet = ConcurrentHashMap.newKeySet<String>()
        var a2dpProxy: BluetoothProfile? = null
        var headsetProxy: BluetoothProfile? = null

        fun emit() {
            trySend((a2dpSet + headsetSet).toSet())
        }

        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                val target = when (profile) {
                    BluetoothProfile.A2DP -> { a2dpProxy = proxy; a2dpSet }
                    BluetoothProfile.HEADSET -> { headsetProxy = proxy; headsetSet }
                    else -> return
                }
                target.clear()
                proxy.connectedDevices.forEach { target.add(it.address) }
                emit()
            }

            override fun onServiceDisconnected(profile: Int) {
                when (profile) {
                    BluetoothProfile.A2DP -> { a2dpProxy = null; a2dpSet.clear() }
                    BluetoothProfile.HEADSET -> { headsetProxy = null; headsetSet.clear() }
                }
                emit()
            }
        }
        adapter.getProfileProxy(context, listener, BluetoothProfile.A2DP)
        adapter.getProfileProxy(context, listener, BluetoothProfile.HEADSET)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                        a2dpSet.clear(); headsetSet.clear(); emit()
                    }
                    return
                }
                val device = intent.bluetoothDevice() ?: return
                val target = if (intent.action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) a2dpSet else headsetSet
                when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                    BluetoothProfile.STATE_CONNECTED -> target.add(device.address)
                    BluetoothProfile.STATE_DISCONNECTED -> target.remove(device.address)
                }
                emit()
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
            a2dpProxy?.let { runCatching { adapter.closeProfileProxy(BluetoothProfile.A2DP, it) } }
            headsetProxy?.let { runCatching { adapter.closeProfileProxy(BluetoothProfile.HEADSET, it) } }
        }
    }
}
