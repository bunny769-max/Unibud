package com.unibud.app.core.registry

import com.unibud.app.core.model.Brand
import com.unibud.app.core.model.BrandSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrandResolverTest {
    @Test fun userChoiceWinsOverEverything() {
        val b = BrandResolver.resolve("Galaxy Buds", setOf(0x0075), "Samsung", userBrand = "My Brand")
        assertEquals(BrandSource.USER, b?.source)
        assertEquals("My Brand", b?.displayName)
    }

    @Test fun deviceNameIsUsedFirst() {
        val b = BrandResolver.resolve("realme Buds Air 5", emptySet())
        assertEquals("realme", b?.id)
        assertEquals(BrandSource.DEVICE_NAME, b?.source)
    }

    @Test fun manufacturerDataIsUsedWhenNameIsUnknown() {
        val b = BrandResolver.resolve("X1", setOf(0x0075))
        assertEquals("samsung", b?.id)
        assertEquals(BrandSource.MANUFACTURER_DATA, b?.source)
    }

    @Test fun makerNameFromTheDeviceMapsToAKnownBrand() {
        val b = BrandResolver.resolve("TWS-1234", emptySet(), gattManufacturer = "Samsung Electronics")
        assertEquals("samsung", b?.id)
        assertEquals(BrandSource.DEVICE_INFO, b?.source)
    }

    @Test fun unknownMakerNameIsKeptAsText() {
        val b = BrandResolver.resolve("A1", emptySet(), gattManufacturer = "Acme Audio Ltd")
        assertEquals("Acme Audio Ltd", b?.displayName)
        assertEquals(BrandSource.DEVICE_INFO, b?.source)
    }

    @Test fun firstWordOfTheNameIsAGuessOfLastResort() {
        val b = BrandResolver.resolve("Zorbo Buds 3", emptySet())
        assertEquals("Zorbo", b?.displayName)
        assertEquals(BrandSource.GUESS, b?.source)
    }

    @Test fun genericOrNumericNamesAreNotGuessed() {
        assertNull(BrandResolver.resolve("TWS-BT", emptySet()))
        assertNull(BrandResolver.resolve("A3-5B2C", emptySet()))
        assertNull(BrandResolver.resolve("Earbuds", emptySet()))
        assertNull(BrandResolver.resolve(null, emptySet()))
    }

    @Test fun refineUpgradesAGuessButKeepsTrustedBrands() {
        val guess = Brand("guess:zorbo", "Zorbo", BrandSource.GUESS)
        assertEquals("samsung", BrandResolver.refine(guess, "Zorbo", emptySet(), "Samsung")?.id)
        val trusted = Brand("user:mine", "Mine", BrandSource.USER)
        assertEquals(trusted, BrandResolver.refine(trusted, "Zorbo", emptySet(), "Samsung"))
        assertEquals(guess, BrandResolver.refine(guess, "Zorbo", emptySet(), null)?.copy(source = BrandSource.GUESS))
    }

    @Test fun commonIndianMarketNamesAreRecognised() {
        listOf(
            "boAt Airdopes 141" to "boat", "Noise Buds VS104" to "noise", "Boult Z40" to "boult",
            "pTron Bassbuds Duo" to "ptron", "Mivi DuoPods A25" to "mivi", "OnePlus Nord Buds 2" to "oneplus",
            "Redmi Buds 5" to "xiaomi", "OPPO Enco Air3" to "oppo", "Soundcore Liberty 4" to "soundcore",
        ).forEach { (name, id) -> assertEquals(name, id, DeviceDatabase.identifyByName(name)?.id) }
    }
}
