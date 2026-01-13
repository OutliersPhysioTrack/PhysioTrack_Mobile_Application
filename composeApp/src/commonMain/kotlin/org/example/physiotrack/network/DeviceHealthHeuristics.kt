package org.example.physiotrack.network

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.abs

/**
 * Compute online/offline + needsCalibration from latest readings.
 *
 * Assumptions (testing-friendly):
 * - device is online if the newest sensor ts is within [onlineWindowSec].
 * - metric is "invalid" if value is null, NaN, or exactly 0.0.
 *
 * Limitations:
 * - some metrics may legitimately be zero; adjust per-metric rules if needed.
 */
fun computeDeviceHealth(
    latest: List<LatestReadingDto>,
    onlineWindowSec: Int = 60,
    invalidThresholdRatio: Double = 0.6,
): DeviceHealth {
    val lastSeen: Instant? = latest
        .mapNotNull { runCatching { Instant.parse(it.ts) }.getOrNull() }
        .maxOrNull()

    val now = Clock.System.now()
    val isOnline = lastSeen?.let { (now.epochSeconds - it.epochSeconds) <= onlineWindowSec } ?: false

    val invalidCount = latest.count { r ->
        val v = r.value
        v == null || v.isNaN() || abs(v) == 0.0
    }
    val total = latest.size.coerceAtLeast(1)
    val invalidRatio = invalidCount.toDouble() / total
    val needsCalibration = invalidRatio >= invalidThresholdRatio

    return DeviceHealth(
        isOnline = isOnline,
        needsCalibration = needsCalibration,
        lastSeen = lastSeen,
        invalidRatio = invalidRatio,
    )
}
