package com.unibud.app.core.registry

import com.unibud.app.core.model.Confidence
import com.unibud.app.core.model.DeviceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegistryTest {
    private val noServices = emptySet<String>()

    @Test fun earbudsNamePlusAudioClassIsTwsButNeverHighConfidence() {
        val c = DeviceClassifier.classify("Galaxy Buds2 Pro", 0x0418, noServices)
        assertEquals(DeviceKind.TWS_EARBUDS, c.kind)
        assertEquals(Confidence.MEDIUM, c.confidence)
    }

    @Test fun headbandHeadphonesAreNotCalledTws() {
        assertEquals(DeviceKind.HEADSET, DeviceClassifier.classify("WH-1000XM5", 0x0418, noServices).kind)
    }

    @Test fun speakerClassIsSpeaker() {
        assertEquals(DeviceKind.SPEAKER, DeviceClassifier.classify("Living room", 0x0414, noServices).kind)
    }

    @Test fun phoneClassIsNotAudio() {
        assertEquals(DeviceKind.NOT_AUDIO, DeviceClassifier.classify("Pixel", 0x0204, noServices).kind)
    }

    @Test fun nameOnlyHintIsLowConfidence() {
        val c = DeviceClassifier.classify("boAt Airdopes 141", null, noServices)
        assertEquals(DeviceKind.TWS_EARBUDS, c.kind)
        assertEquals(Confidence.LOW, c.confidence)
    }

    @Test fun nothingKnownIsUnknown() {
        assertEquals(DeviceKind.UNKNOWN, DeviceClassifier.classify(null, null, noServices).kind)
    }

    @Test fun a2dpServiceCountsAsAudioEvidence() {
        val c = DeviceClassifier.classify("OnePlus Buds Pro", null, setOf("0000110B-0000-1000-8000-00805F9B34FB"))
        assertEquals(Confidence.MEDIUM, c.confidence)
    }

    @Test fun brandFromName() {
        assertEquals("boat", DeviceDatabase.identify("boAt Airdopes 141")?.id)
        assertEquals("samsung", DeviceDatabase.identify("Galaxy Buds2 Pro")?.id)
    }

    @Test fun brandFromCompanyIdWhenNameMissing() {
        assertEquals("apple", DeviceDatabase.identify(null, setOf(0x004C))?.id)
    }

    @Test fun unknownBrandIsNull() {
        assertNull(DeviceDatabase.identify("Random Gadget", setOf(0x9999)))
    }

    @Test fun noBrandHasAnIntegrationYet() {
        DeviceDatabase.entries.forEach { assertNull(it.integrationId) }
    }
}
