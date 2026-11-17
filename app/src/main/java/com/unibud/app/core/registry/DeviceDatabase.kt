package com.unibud.app.core.registry

import com.unibud.app.core.model.Brand
import com.unibud.app.core.model.BrandSource

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
        entry("apple", "Apple / Beats", "airpods|beats|\\bpowerbeats", setOf(0x004C)),
        entry("samsung", "Samsung", "galaxy buds|gear ?iconx|\\bsamsung\\b", setOf(0x0075)),
        entry("sony", "Sony", "\\bwf-|\\bwh-|linkbuds|\\bsony\\b", setOf(0x012D)),
        entry("google", "Google", "pixel buds", setOf(0x00E0)),
        entry("bose", "Bose", "\\bbose\\b|quietcomfort", setOf(0x009E)),
        entry("nothing", "Nothing / CMF", "nothing ear|cmf buds|cmf by nothing|\\bnothing\\b"),
        entry("oneplus", "OnePlus", "oneplus|\\bnord buds"),
        entry("realme", "realme", "realme|\\brma\\d"),
        entry("xiaomi", "Xiaomi / Redmi", "redmi|xiaomi|\\bmi true wireless|poco buds", setOf(0x038F)),
        entry("oppo", "OPPO", "oppo|\\benco\\b"),
        entry("vivo", "vivo / iQOO", "\\bvivo\\b|iqoo|vivo tws"),
        entry("motorola", "Motorola", "moto buds|motorola|\\bmoto\\b"),
        entry("honor", "Honor", "\\bhonor\\b|honor choice"),
        entry("huawei", "Huawei", "freebuds|huawei|freeclip", setOf(0x027D)),
        entry("soundcore", "Anker Soundcore", "soundcore|\\bliberty\\b|\\banker\\b|space a40"),
        entry("jbl", "JBL", "\\bjbl\\b|\\bwave \\d|tune \\d+ ?tws"),
        entry("harman", "Harman Kardon", "harman|\\bakg\\b"),
        entry("jabra", "Jabra", "jabra|\\belite \\d"),
        entry("sennheiser", "Sennheiser", "sennheiser|momentum true wireless|\\bcx plus|\\bcx true"),
        entry("marshall", "Marshall", "marshall|\\bmotif\\b|\\bminor iv"),
        entry("beoplay", "Bang & Olufsen", "beoplay|bang ?& ?olufsen"),
        entry("technics", "Technics / Panasonic", "technics|panasonic"),
        entry("audiotechnica", "Audio-Technica", "audio-technica|\\bath-"),
        entry("philips", "Philips", "philips|\\btat\\d"),
        entry("edifier", "Edifier", "edifier|\\bneobuds|\\bw\\d{3}nb"),
        entry("qcy", "QCY", "\\bqcy\\b|\\bht0\\d|melobuds"),
        entry("haylou", "Haylou", "haylou"),
        entry("tozo", "TOZO", "\\btozo\\b"),
        entry("lenovo", "Lenovo", "lenovo|thinkplus|\\blp\\d{2}\\b"),
        entry("boat", "boAt", "\\bboat\\b|airdopes|rockerz|bassheads|nirvana"),
        entry("noise", "Noise", "noise buds|noise air|noise pods|noise master|\\bnoise\\b"),
        entry("boult", "Boult", "\\bboult\\b"),
        entry("ptron", "pTron", "\\bptron\\b|bassbuds"),
        entry("mivi", "Mivi", "\\bmivi\\b|duopods|collar"),
        entry("zebronics", "Zebronics", "zebronics|\\bzeb[- ]"),
        entry("fireboltt", "Fire-Boltt", "fire-?boltt"),
        entry("portronics", "Portronics", "portronics|harmonics"),
        entry("ubon", "UBON", "\\bubon\\b"),
        entry("crossbeats", "Crossbeats", "crossbeats"),
        entry("gizmore", "Gizmore", "gizmore"),
        entry("skullcandy", "Skullcandy", "skullcandy|\\bindy\\b|\\bsesh\\b|dime"),
        entry("microsoft", "Microsoft", "surface earbuds", setOf(0x0006)),
    )

    fun identifyByName(name: String?): Brand? =
        if (name.isNullOrBlank()) null else entries.firstOrNull { it.namePattern.containsMatchIn(name) }?.brand

    fun identifyByCompanyId(manufacturerIds: Set<Int>): Brand? =
        entries.firstOrNull { e -> e.companyIds.any { it in manufacturerIds } }
            ?.brand?.copy(source = BrandSource.MANUFACTURER_DATA)

    fun identify(name: String?, manufacturerIds: Set<Int> = emptySet()): Brand? =
        identifyByName(name) ?: identifyByCompanyId(manufacturerIds)

    fun integrationFor(brand: Brand?): String? =
        brand?.let { b -> entries.firstOrNull { it.brand.id == b.id }?.integrationId }
}
