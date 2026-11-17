package com.unibud.app.core.capability

import com.unibud.app.core.model.Brand
import com.unibud.app.core.model.BrandSource
import com.unibud.app.core.model.Classification
import com.unibud.app.core.model.Confidence
import com.unibud.app.core.model.DeviceInfoDetails
import com.unibud.app.core.model.DeviceKind
import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.FeatureStatus
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.core.model.SupportSource
import com.unibud.app.core.model.Transport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun device(brand: Brand?) = DiscoveredDevice(
    "AA:BB", "Zorbo Buds", -50, 0x0418, true, true, setOf(Transport.BONDED), emptySet(), emptySet(), brand,
    Classification(DeviceKind.TWS_EARBUDS, Confidence.MEDIUM, "test"),
)

class CapabilityMergeTest {
    @Test fun infoFromSeveralIntegrationsIsMerged() {
        val gatt = IntegrationFindings(emptyList(), DeviceInfoDetails(modelNumber = "M1", firmwareRevision = "1.0"))
        val audio = IntegrationFindings(emptyList(), DeviceInfoDetails(audioOutputActive = true, audioChannels = listOf(2)))
        val info = CapabilityResolver.resolve(device(null), listOf(gatt, audio)).info
        assertEquals("M1", info?.modelNumber)
        assertEquals(true, info?.audioOutputActive)
        assertEquals(listOf(2), info?.audioChannels)
    }

    @Test fun mergeKeepsExistingValues() {
        val a = DeviceInfoDetails(modelNumber = "A")
        val b = DeviceInfoDetails(modelNumber = "B", firmwareRevision = "2")
        assertEquals(DeviceInfoDetails(modelNumber = "A", firmwareRevision = "2"), a.mergedWith(b))
    }

    @Test fun makerNameFromTheDeviceUpgradesAGuessedBrand() {
        val guess = Brand("guess:zorbo", "Zorbo", BrandSource.GUESS)
        val gatt = IntegrationFindings(emptyList(), DeviceInfoDetails(manufacturerName = "Samsung Electronics"))
        val brand = CapabilityResolver.resolve(device(guess), listOf(gatt)).device.brand
        assertEquals("samsung", brand?.id)
        assertEquals(BrandSource.DEVICE_INFO, brand?.source)
    }

    @Test fun userBrandIsNeverOverridden() {
        val mine = Brand("user:mine", "Mine", BrandSource.USER)
        val gatt = IntegrationFindings(emptyList(), DeviceInfoDetails(manufacturerName = "Samsung Electronics"))
        assertEquals(mine, CapabilityResolver.resolve(device(mine), listOf(gatt)).device.brand)
    }

    @Test fun volumeAndMediaCommandsTargetTheirOwnFeatures() {
        assertEquals(FeatureId.VOLUME, ControlCommand.SetVolume(40).feature)
        assertEquals(FeatureId.MEDIA_PLAYBACK, ControlCommand.MediaAction(MediaKey.NEXT).feature)
    }

    @Test fun volumeIsUnavailableUnlessAnIntegrationReportsIt() {
        val report = CapabilityResolver.resolve(device(null), emptyList())
        assertEquals(SupportLevel.UNAVAILABLE, report.status(FeatureId.VOLUME).level)
        val findings = IntegrationFindings(
            listOf(FeatureStatus(FeatureId.VOLUME, SupportLevel.SUPPORTED, SupportSource.ANDROID_STANDARD, "ok")),
        )
        assertTrue(CapabilityResolver.resolve(device(null), listOf(findings)).status(FeatureId.VOLUME).level == SupportLevel.SUPPORTED)
    }
}
