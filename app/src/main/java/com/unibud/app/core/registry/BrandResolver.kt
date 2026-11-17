package com.unibud.app.core.registry

import com.unibud.app.core.model.Brand
import com.unibud.app.core.model.BrandSource

/**
 * Works out a brand from every signal available, strongest first:
 * the user's own choice, the device name, BLE manufacturer data, the maker name the device reports
 * itself, and finally a guess from the first word of the name. Returns null only when nothing helps.
 */
object BrandResolver {

    private val GENERIC_WORDS = setOf(
        "tws", "bt", "bluetooth", "earbuds", "earbud", "buds", "bud", "headset", "headphones", "headphone",
        "speaker", "audio", "wireless", "ear", "earphone", "earphones", "pods", "neckband", "mini", "pro",
        "max", "air", "lite", "plus", "sport", "sports", "stereo", "true", "music", "gaming", "device", "my",
    )

    fun resolve(
        name: String?,
        manufacturerIds: Set<Int>,
        gattManufacturer: String? = null,
        userBrand: String? = null,
    ): Brand? {
        userBrand?.trim()?.takeIf { it.isNotEmpty() }?.let {
            return Brand("user:${slug(it)}", it, BrandSource.USER)
        }
        DeviceDatabase.identifyByName(name)?.let { return it.copy(source = BrandSource.DEVICE_NAME) }
        DeviceDatabase.identifyByCompanyId(manufacturerIds)?.let { return it }
        gattManufacturer?.trim()?.takeIf { it.isNotEmpty() }?.let { maker ->
            val known = DeviceDatabase.identifyByName(maker)
            return (known ?: Brand("info:${slug(maker)}", maker)).copy(source = BrandSource.DEVICE_INFO)
        }
        return guessFromName(name)
    }

    /** Keeps a trustworthy brand; upgrades a guess or a missing brand when better information arrives. */
    fun refine(current: Brand?, name: String?, manufacturerIds: Set<Int>, gattManufacturer: String?): Brand? {
        if (current != null && current.source != BrandSource.GUESS) return current
        return resolve(name, manufacturerIds, gattManufacturer) ?: current
    }

    fun guessFromName(name: String?): Brand? {
        val first = name?.trim()?.split(Regex("[\\s_\\-]+"))?.firstOrNull() ?: return null
        if (first.length !in 3..16 || !first.all { it.isLetter() }) return null
        if (first.lowercase() in GENERIC_WORDS) return null
        return Brand("guess:${slug(first)}", first, BrandSource.GUESS)
    }

    private fun slug(text: String) = text.lowercase().filter { it.isLetterOrDigit() }
}
