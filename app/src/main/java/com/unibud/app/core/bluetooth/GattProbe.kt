package com.unibud.app.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.unibud.app.core.capability.GattProbeResult
import com.unibud.app.core.capability.SigUuids
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Layer 2: opens a short-lived BLE GATT connection and reads only *standard* Bluetooth SIG
 * characteristics (Battery Level, Device Information). It never writes and never sends
 * vendor-specific commands. Returns null if no GATT connection could be made.
 */
class GattProbe(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun probe(address: String, timeoutMs: Long = 10_000L): GattProbeResult? {
        if (!BluetoothPermissions.hasConnect(context)) return null
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            return null
        }

        val done = CompletableDeferred<Unit>()
        val connectedOnce = AtomicBoolean(false)
        val services = ConcurrentHashMap.newKeySet<UUID>()
        val values = ConcurrentHashMap<UUID, ByteArray>()
        val pending = ConcurrentLinkedQueue<BluetoothGattCharacteristic>()

        // Reads are strictly sequential: Android allows one outstanding GATT operation at a time.
        fun readNext(gatt: BluetoothGatt) {
            while (true) {
                val next = pending.poll() ?: run {
                    done.complete(Unit)
                    return
                }
                if (gatt.readCharacteristic(next)) return
            }
        }

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedOnce.set(true)
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> done.complete(Unit)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    done.complete(Unit)
                    return
                }
                gatt.services.forEach { services.add(it.uuid) }
                for ((service, characteristic) in WANTED) {
                    gatt.getService(service)?.getCharacteristic(characteristic)?.let { pending.add(it) }
                }
                readNext(gatt)
            }

            // Android 13+
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int,
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) values[characteristic.uuid] = value
                readNext(gatt)
            }

            // Android 12 and below
            @Deprecated("Used on API < 33")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
                @Suppress("DEPRECATION")
                val value = characteristic.value
                if (status == BluetoothGatt.GATT_SUCCESS && value != null) values[characteristic.uuid] = value
                readNext(gatt)
            }
        }

        var gatt: BluetoothGatt? = null
        try {
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            withTimeoutOrNull(timeoutMs) { done.await() }
        } finally {
            gatt?.disconnect()
            gatt?.close()
        }
        return if (connectedOnce.get()) GattProbeResult(services.toSet(), values.toMap()) else null
    }

    private companion object {
        val WANTED: List<Pair<UUID, UUID>> = listOf(
            SigUuids.BATTERY_SERVICE to SigUuids.BATTERY_LEVEL,
            SigUuids.DEVICE_INFO_SERVICE to SigUuids.MANUFACTURER_NAME,
            SigUuids.DEVICE_INFO_SERVICE to SigUuids.MODEL_NUMBER,
            SigUuids.DEVICE_INFO_SERVICE to SigUuids.HARDWARE_REVISION,
            SigUuids.DEVICE_INFO_SERVICE to SigUuids.FIRMWARE_REVISION,
            SigUuids.DEVICE_INFO_SERVICE to SigUuids.SOFTWARE_REVISION,
        )
    }
}
