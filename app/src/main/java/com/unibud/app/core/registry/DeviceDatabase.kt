package com.unibud.app.core.registry

import com.unibud.app.core.model.Brand

/**
 * Layer 4: expandable device compatibility database.
 *
 * [integrationId] is null until a real manufacturer integration module exists for the brand.
 * Adding a brand here only improves identification; it never enables controls by itself.
 */
data class DeviceDbEntry(
    val brand: Brand,
    val namePattern: Regex,
    val companyIds: Set<Int> = emptySet(),
    val integrationId: String? = null,
)

object DeviceDatabase {

    private fun entry(id: String, display: String, pattern: String, companyIds: Set<Int> = emptySet()) =
        DeviceDbEntry(Brand(id, display), Regex(pattern, RegexOption.IGNORE_CASE), companyIds)

    // Company IDs are Bluetooth SIG assigned numbers.
    val entries: List<DeviceDbEntry> = listOf(
        entry("apple", "Apple / Beats", "airpods|beats", setOf(0x004C)),
        entry("samsung", "Samsung", "galaxy buds|gear ?iconx", setOf(0x0075)),
        entry("sony", "Sony", "\\bwf-|\\bwh-|linkbuds|\\bsony\\b", setOf(0x012D)),
        entry("google", "Google", "pixel buds", setOf(0x00E0)),
        entry("bose", "Bose", "\\bbose\\b|quietcomfort", setOf(0x009E)),
        entry("nothing", "Nothing / CMF", "nothing ear|cmf buds|cmf by nothing"),
        entry("oneplus", "OnePlus", "oneplus buds"),
        entry("realme", "realme", "realme buds"),
        entry("xiaomi", "Xiaomi / Redmi", "redmi buds|xiaomi buds|mi true wireless"),
        entry("oppo", "OPPO", "oppo enco|\\benco\\b"),
        entry("soundcore", "Anker Soundcore", "soundcore|\\bliberty\\b"),
        entry("jbl", "JBL", "\\bjbl\\b"),
        entry("jabra", "Jabra", "jabra|\\belite \\d"),
        entry("sennheiser", "Sennheiser", "sennheiser|momentum true wireless"),
        entry("huawei", "Huawei", "freebuds|huawei"),
        entry("boat", "boAt", "\\bboat\\b|airdopes"),
        entry("noise", "Noise", "noise buds|noise air"),
        entry("boult", "Boult", "\\bboult\\b"),
        entry("ptron", "pTron", "\\bptron\\b"),
        entry("skullcandy", "Skullcandy", "skullcandy"),
    )

    fun identify(name: String?, manufacturerIds: Set<Int> = emptySet()): Brand? {
        if (!name.isNullOrBlank()) {
            entries.firstOrNull { it.namePattern.containsMatchIn(name) }?.let { return it.brand }
        }
        return entries.firstOrNull { e -> e.companyIds.any { it in manufacturerIds } }?.brand
    }

    fun integrationFor(brand: Brand?): String? =
        brand?.let { b -> entries.firstOrNull { it.brand.id == b.id }?.integrationId }
}
