package com.unibud.app.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.unibud.app.core.model.DeviceKind
import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.Transport
import com.unibud.app.core.registry.DeviceFactory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Layers 1 and 2 discovery: bonded devices + Bluetooth Classic discovery + BLE scan, merged
 * by address. Scanning is time-boxed ([durationMs]) to avoid background battery drain.
 * Emits a sorted snapshot list every time something new is learned.
 */
class BluetoothScanner(private val context: Context) {

    /** Paired devices only. No radio scan, so it is cheap and safe to call on every app resume. */
    @SuppressLint("MissingPermission")
    fun bonded(): List<DiscoveredDevice> {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
        if (!BluetoothPermissions.hasConnect(context)) return emptyList()
        return try {
            adapter.bondedDevices.orEmpty().map { d ->
                DeviceFactory.merge(
                    d.address, null, d.name, null, d.bluetoothClass?.deviceClass, true, Transport.BONDED,
                    emptySet(), d.uuids.orEmpty().map { it.uuid.toString() }.toSet(),
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun scan(durationMs: Long = 12_000L): Flow<List<DiscoveredDevice>> = callbackFlow {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null) {
            close(IllegalStateException("This device has no Bluetooth adapter."))
            return@callbackFlow
        }
        if (!BluetoothPermissions.hasAll(context)) {
            close(SecurityException("Bluetooth permissions have not been granted."))
            return@callbackFlow
        }
        if (!adapter.isEnabled) {
            close(IllegalStateException("Bluetooth is turned off."))
            return@callbackFlow
        }

        val found = ConcurrentHashMap<String, DiscoveredDevice>()

        // Radios can report dozens of advertisements per second. Collapse them into at most one
        // list update every PUBLISH_INTERVAL_MS so the UI is not recomposed on every packet.
        val dirty = AtomicBoolean(false)

        fun flush() {
            if (!dirty.getAndSet(false)) return
            val sorted = found.values.sortedWith(
                compareBy<DiscoveredDevice>(
                    { it.classification.kind != DeviceKind.TWS_EARBUDS },
                    { !it.isBonded },
                    { -(it.rssi ?: -127) },
                ),
            )
            trySend(sorted)
        }

        fun publish() {
            dirty.set(true)
        }

        fun upsert(
            address: String,
            name: String?,
            rssi: Int?,
            deviceClass: Int?,
            bonded: Boolean,
            transport: Transport,
            manufacturerIds: Set<Int>,
            services: Set<String>,
        ) {
            found.compute(address) { _, old ->
                DeviceFactory.merge(
                    address, old, name, rssi, deviceClass, bonded, transport, manufacturerIds, services,
                )
            }
            publish()
        }

        // 1) Already-paired devices.
        adapter.bondedDevices.orEmpty().forEach { d ->
            upsert(
                d.address, d.name, null, d.bluetoothClass?.deviceClass, true, Transport.BONDED,
                emptySet(), d.uuids.orEmpty().map { it.uuid.toString() }.toSet(),
            )
        }

        // 2) Bluetooth Classic discovery.
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action != BluetoothDevice.ACTION_FOUND) return
                val d = intent.bluetoothDevice() ?: return
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    .takeIf { it != Short.MIN_VALUE }?.toInt()
                upsert(
                    d.address,
                    intent.getStringExtra(BluetoothDevice.EXTRA_NAME) ?: d.name,
                    rssi,
                    d.bluetoothClass?.deviceClass,
                    d.bondState == BluetoothDevice.BOND_BONDED,
                    Transport.CLASSIC,
                    emptySet(),
                    d.uuids.orEmpty().map { it.uuid.toString() }.toSet(),
                )
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(BluetoothDevice.ACTION_FOUND), ContextCompat.RECEIVER_EXPORTED,
        )
        adapter.startDiscovery()

        // 3) BLE scan (also surfaces manufacturer company IDs used for brand detection).
        val leScanner = adapter.bluetoothLeScanner
        val leCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = handle(result)

            override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach(::handle)

            private fun handle(result: ScanResult) {
                val record = result.scanRecord
                val mfrData = record?.manufacturerSpecificData
                val mfrIds = buildSet { if (mfrData != null) for (i in 0 until mfrData.size()) add(mfrData.keyAt(i)) }
                val d = result.device
                upsert(
                    d.address,
                    record?.deviceName ?: d.name,
                    result.rssi,
                    d.bluetoothClass?.deviceClass,
                    d.bondState == BluetoothDevice.BOND_BONDED,
                    Transport.BLE,
                    mfrIds,
                    record?.serviceUuids.orEmpty().map { it.uuid.toString() }.toSet(),
                )
            }
        }
        leScanner?.startScan(
            null,
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build(),
            leCallback,
        )

        publish()
        flush()
        launch {
            while (true) {
                delay(PUBLISH_INTERVAL_MS)
                flush()
            }
        }
        launch {
            delay(durationMs)
            flush()
            close()
        }

        awaitClose {
            runCatching { if (adapter.isDiscovering) adapter.cancelDiscovery() }
            runCatching { leScanner?.stopScan(leCallback) }
            runCatching { context.unregisterReceiver(receiver) }
        }
    }.conflate()

    private companion object {
        const val PUBLISH_INTERVAL_MS = 300L
    }
}
