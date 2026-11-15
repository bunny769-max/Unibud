package com.unibud.app.core.audio

import com.unibud.app.core.capability.ControlCommand
import com.unibud.app.core.capability.SoundMode
import com.unibud.app.core.model.Confidence
import com.unibud.app.core.model.DeviceKind
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.SavedDevice
import com.unibud.app.core.model.ThemeMode
import com.unibud.app.core.model.Transport
import com.unibud.app.core.registry.DeviceFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerTest {
    @Test fun codecRoundTrips() {
        val gains = listOf(-3f, 0f, 1.5f, 6f, 12f)
        assertEquals(gains, EqCodec.decode(EqCodec.encode(gains)))
    }

    @Test fun decodeRejectsWrongBandCountAndGarbage() {
        assertNull(EqCodec.decode("1,2,3"))
        assertNull(EqCodec.decode("a,b,c,d,e"))
    }

    @Test fun decodeClampsOutOfRangeValues() {
        assertEquals(listOf(12f, -12f, 0f, 0f, 0f), EqCodec.decode("99,-99,0,0,0"))
    }

    @Test fun presetNameRules() {
        assertEquals("My mix", EqPresetRules.cleanName("  My mix  "))
        assertNull(EqPresetRules.cleanName("   "))
        assertNull(EqPresetRules.cleanName("x".repeat(31)))
    }

    @Test fun builtInPresetsAreValid() {
        assertTrue(BuiltInPresets.all.all { it.builtIn && it.id < 0 && it.gainsDb.size == EqBands.COUNT })
        assertTrue(BuiltInPresets.all.all { p -> p.gainsDb.all { it in EqBands.MIN_DB..EqBands.MAX_DB } })
        assertEquals(List(EqBands.COUNT) { 0f }, BuiltInPresets.all.first().gainsDb)
        assertEquals(BuiltInPresets.all.size, BuiltInPresets.all.map { it.id }.toSet().size)
    }

    @Test fun equalizerCommandTargetsTheEqualizerFeature() {
        assertEquals(FeatureId.EQUALIZER, ControlCommand.SetEqualizer(listOf(0f, 0f, 0f, 0f, 0f)).feature)
        assertEquals(FeatureId.ANC, ControlCommand.SetSoundMode(SoundMode.ANC).feature)
    }

    @Test fun bandLabels() {
        assertEquals("60 Hz", EqBands.label(60))
        assertEquals("3.6 kHz", EqBands.label(3600))
        assertEquals("14 kHz", EqBands.label(14000))
    }
}

class ModelsTest {
    @Test fun themeModeParsesAndFallsBack() {
        assertEquals(ThemeMode.DARK, ThemeMode.parse("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse("nonsense"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse(null))
    }

    @Test fun savedDeviceNamePrefersCustomName() {
        fun device(custom: String?, system: String?) = SavedDevice(
            "AA", system, custom, null, DeviceKind.TWS_EARBUDS, Confidence.MEDIUM, null, null, 0L,
        )
        assertEquals("Gym buds", device("Gym buds", "Galaxy Buds").displayName)
        assertEquals("Galaxy Buds", device(null, "Galaxy Buds").displayName)
        assertEquals("Galaxy Buds", device("  ", "Galaxy Buds").displayName)
        assertEquals("AA", device(null, null).displayName)
    }

    @Test fun deviceFactoryMergesObservations() {
        val ble = DeviceFactory.merge("AA", null, null, -60, null, false, Transport.BLE, setOf(0x0075), emptySet())
        assertNull(ble.name)
        assertEquals("samsung", ble.brand?.id)

        val classic = DeviceFactory.merge("AA", ble, "Galaxy Buds2", -55, 0x0418, false, Transport.CLASSIC, emptySet(), emptySet())
        assertEquals("Galaxy Buds2", classic.name)
        assertEquals(setOf(Transport.BLE, Transport.CLASSIC), classic.seenOver)
        assertEquals(DeviceKind.TWS_EARBUDS, classic.classification.kind)
        assertEquals(setOf(0x0075), classic.manufacturerIds)

        val bonded = DeviceFactory.merge("AA", classic, null, null, null, true, Transport.BONDED, emptySet(), emptySet())
        val later = DeviceFactory.merge("AA", bonded, null, null, null, false, Transport.BLE, emptySet(), emptySet())
        assertTrue(later.isBonded)
        assertEquals("Galaxy Buds2", later.name)
        assertEquals(-55, later.rssi)
    }
}
