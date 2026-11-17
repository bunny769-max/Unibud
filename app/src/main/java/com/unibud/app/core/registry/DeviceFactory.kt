package com.unibud.app.core.registry

import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.Transport

/** Builds or merges a [DiscoveredDevice] from one new observation. Pure, so it is unit-tested. */
object DeviceFactory {
    fun merge(
        address: String,
        old: DiscoveredDevice?,
        name: String?,
        rssi: Int?,
        deviceClass: Int?,
        bonded: Boolean,
        transport: Transport,
        manufacturerIds: Set<Int>,
        services: Set<String>,
    ): DiscoveredDevice {
        val mergedName = name?.takeIf { it.isNotBlank() } ?: old?.name
        val mergedClass = deviceClass ?: old?.deviceClass
        val mergedServices = (old?.advertisedServices ?: emptySet()) + services
        val mergedMfr = (old?.manufacturerIds ?: emptySet()) + manufacturerIds
        return DiscoveredDevice(
            address = address,
            name = mergedName,
            rssi = rssi ?: old?.rssi,
            deviceClass = mergedClass,
            isBonded = bonded || old?.isBonded == true,
            isConnected = old?.isConnected ?: false,
            seenOver = (old?.seenOver ?: emptySet()) + transport,
            manufacturerIds = mergedMfr,
            advertisedServices = mergedServices,
            brand = BrandResolver.resolve(mergedName, mergedMfr),
            classification = DeviceClassifier.classify(mergedName, mergedClass, mergedServices),
        )
    }
}
