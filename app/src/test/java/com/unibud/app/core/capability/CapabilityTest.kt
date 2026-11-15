package com.unibud.app.core.capability

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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

private val device = DiscoveredDevice(
    address = "AA:BB:CC:DD:EE:FF", name = "Test Buds", rssi = -50, deviceClass = 0x0418,
    isBonded = true, isConnected = true, seenOver = setOf(Transport.BONDED),
    manufacturerIds = emptySet(), advertisedServices = emptySet(), brand = null,
    classification = Classification(DeviceKind.TWS_EARBUDS, Confidence.MEDIUM, "test"),
)

class CapabilityResolverTest {

    @Test fun everyFeatureIsListedAndNothingIsAvailableWithoutASource() {
        val report = CapabilityResolver.resolve(device, emptyList())
        assertEquals(FeatureId.entries.size, report.features.size)
        assertTrue(report.features.all { it.level == SupportLevel.UNAVAILABLE })
        assertTrue(report.limitations.any { it.contains("Nothing is simulated") })
    }

    @Test fun supportedWithoutASourceIsRejected() {
        try {
            FeatureStatus(FeatureId.ANC, SupportLevel.SUPPORTED, SupportSource.NONE, "x")
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test fun reportedFeaturesWinOverDefaults() {
        val findings = IntegrationFindings(
            listOf(FeatureStatus(FeatureId.BATTERY_OVERALL, SupportLevel.PARTIAL, SupportSource.BLE_STANDARD_GATT, "ble")),
        )
        val report = CapabilityResolver.resolve(device, listOf(findings))
        assertEquals(SupportLevel.PARTIAL, report.status(FeatureId.BATTERY_OVERALL).level)
        assertEquals(SupportLevel.UNAVAILABLE, report.status(FeatureId.BATTERY_LEFT).level)
        assertEquals(SupportLevel.UNAVAILABLE, report.status(FeatureId.ANC).level)
    }

    @Test fun higherSupportLevelWinsAcrossSources() {
        val low = IntegrationFindings(listOf(FeatureStatus(FeatureId.CODEC_INFO, SupportLevel.UNAVAILABLE, SupportSource.ANDROID_STANDARD, "no")))
        val high = IntegrationFindings(listOf(FeatureStatus(FeatureId.CODEC_INFO, SupportLevel.SUPPORTED, SupportSource.MANUFACTURER, "yes")))
        val report = CapabilityResolver.resolve(device, listOf(low, high))
        assertEquals(SupportLevel.SUPPORTED, report.status(FeatureId.CODEC_INFO).level)
        assertTrue(report.limitations.none { it.contains("No manufacturer integration") })
    }

    @Test fun failingIntegrationIsReportedNotHidden() = runTest {
        val broken = object : DeviceIntegration {
            override val id = "broken"
            override suspend fun discover(device: DiscoveredDevice): IntegrationFindings = error("boom")
        }
        val report = CapabilityDiscovery(listOf(broken)).discover(device)
        assertTrue(report.limitations.any { it.contains("broken failed") })
    }
}

class GattFindingsInterpreterTest {
    private fun result(vararg pairs: Pair<UUID, ByteArray>) = GattProbeResult(emptySet(), mapOf(*pairs))

    @Test fun parsesDeviceInfoAndBattery() {
        val info = GattFindingsInterpreter.details(
            result(
                SigUuids.MODEL_NUMBER to "XYZ-1".toByteArray(),
                SigUuids.FIRMWARE_REVISION to "1.2.3\u0000".toByteArray(),
                SigUuids.BATTERY_LEVEL to byteArrayOf(87),
            ),
        )
        assertEquals(DeviceInfoDetails(modelNumber = "XYZ-1", firmwareRevision = "1.2.3", batteryPercent = 87), info)
    }

    @Test fun outOfRangeBatteryIsIgnored() {
        assertNull(GattFindingsInterpreter.details(result(SigUuids.BATTERY_LEVEL to byteArrayOf(0xFA.toByte()))))
    }

    @Test fun nullProbeMeansNoInfoAndUnavailableFeatures() {
        assertNull(GattFindingsInterpreter.details(null))
        assertTrue(GattFindingsInterpreter.features(null).all { it.level == SupportLevel.UNAVAILABLE })
    }

    @Test fun firmwareFeatureOnlySupportedWhenActuallyRead() {
        val info = DeviceInfoDetails(firmwareRevision = "2.0")
        val firmware = GattFindingsInterpreter.features(info).first { it.feature == FeatureId.FIRMWARE_INFO }
        assertEquals(SupportLevel.SUPPORTED, firmware.level)
    }
}

class ControlGatewayTest {
    @Test fun noControllersMeansNotSupportedNeverSuccess() = runTest {
        val result = ControlGateway().execute(device, ControlCommand.SetSoundMode(SoundMode.ANC))
        assertTrue(result is ControlResult.NotSupported)
    }

    @Test fun controllerThatCannotControlTheFeatureIsSkipped() = runTest {
        val picky = object : DeviceController {
            override val integrationId = "picky"
            override fun canControl(device: DiscoveredDevice, feature: FeatureId) = feature == FeatureId.LOW_LATENCY
            override suspend fun execute(device: DiscoveredDevice, command: ControlCommand): ControlResult =
                ControlResult.Success("read-back")
        }
        val gateway = ControlGateway(listOf(picky))
        assertTrue(gateway.execute(device, ControlCommand.SetSoundMode(SoundMode.ANC)) is ControlResult.NotSupported)
        assertTrue(gateway.execute(device, ControlCommand.SetLowLatency(true)) is ControlResult.Success)
    }
}
