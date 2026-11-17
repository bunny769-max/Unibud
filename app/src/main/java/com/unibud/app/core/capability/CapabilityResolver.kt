package com.unibud.app.core.capability

import com.unibud.app.core.model.CapabilityReport
import com.unibud.app.core.model.DiscoveredDevice
import com.unibud.app.core.model.FeatureId
import com.unibud.app.core.model.FeatureStatus
import com.unibud.app.core.model.SupportLevel
import com.unibud.app.core.model.SupportSource
import com.unibud.app.core.registry.BrandResolver
import kotlin.coroutines.cancellation.CancellationException

/**
 * Merges findings from all layers into one report. Every [FeatureId] always appears, and
 * anything no integration reported is UNAVAILABLE with an explanation. Nothing is guessed.
 */
object CapabilityResolver {

    fun resolve(
        device: DiscoveredDevice,
        findings: List<IntegrationFindings>,
        failures: List<String> = emptyList(),
    ): CapabilityReport {
        val reported = findings.flatMap { it.features }
        val features = FeatureId.entries.map { feature ->
            reported.filter { it.feature == feature }
                .maxWithOrNull(compareBy<FeatureStatus>({ it.level.rank }, { it.source.priority }))
                ?: FeatureStatus(feature, SupportLevel.UNAVAILABLE, SupportSource.NONE, defaultReason(device))
        }
        val hasManufacturerSupport = features.any {
            it.source == SupportSource.MANUFACTURER && it.level != SupportLevel.UNAVAILABLE
        }
        val limitations = buildList {
            if (!hasManufacturerSupport) {
                add(
                    "No manufacturer integration is active for this device, so advanced controls " +
                        "(noise modes, equalizer, touch controls, per-earbud battery) are unavailable. " +
                        "Nothing is simulated.",
                )
            }
            addAll(findings.flatMap { it.notes })
            addAll(failures)
        }
        val info = findings.mapNotNull { it.info }.reduceOrNull { a, b -> a.mergedWith(b) }
        val brand = BrandResolver.refine(device.brand, device.name, device.manufacturerIds, info?.manufacturerName)
        return CapabilityReport(device.copy(brand = brand), features, info, limitations)
    }

    private fun defaultReason(device: DiscoveredDevice): String {
        val brand = device.brand?.displayName
        val suffix = if (brand != null) " (no $brand integration is installed yet)" else ""
        return "This feature lives in the earbuds' own firmware and needs the maker's protocol$suffix."
    }
}

/** Runs integrations in order (cheap Android-standard first) and resolves the result. */
class CapabilityDiscovery(private val integrations: List<DeviceIntegration>) {
    suspend fun discover(device: DiscoveredDevice): CapabilityReport {
        val findings = mutableListOf<IntegrationFindings>()
        val failures = mutableListOf<String>()
        for (integration in integrations) {
            try {
                findings += integration.discover(device)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failures += "${integration.id} failed: ${e.message ?: e::class.simpleName}"
            }
        }
        return CapabilityResolver.resolve(device, findings, failures)
    }
}
