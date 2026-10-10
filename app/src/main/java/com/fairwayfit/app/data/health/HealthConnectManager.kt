package com.fairwayfit.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

data class HealthSummary(
    val weightLb: Double?,
    val restingHr: Long?,
    val sleepMinutes: Long?,
    val avgDailySteps: Long?
)

sealed interface HealthState {
    data object Loading : HealthState
    data object Unavailable : HealthState
    data object NeedsPermission : HealthState
    data class Ready(val summary: HealthSummary) : HealthState
    data class Error(val message: String) : HealthState
}

/**
 * Read-only access to Health Connect. Garmin data reaches Health Connect through the Garmin Connect
 * app (Settings > Connected Apps > Health Connect), so this works with a Forerunner without any
 * Garmin-specific SDK.
 */
class HealthConnectManager(private val context: Context) {

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    private val _state = MutableStateFlow<HealthState>(HealthState.Loading)
    val state: StateFlow<HealthState> = _state.asStateFlow()

    suspend fun refresh() {
        _state.value = try {
            load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            HealthState.Error(e.message ?: "Health Connect error")
        }
    }

    private suspend fun load(): HealthState {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            return HealthState.Unavailable
        }
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.containsAll(permissions)) return HealthState.NeedsPermission
        return HealthState.Ready(readSummary(client))
    }

    private suspend fun readSummary(client: HealthConnectClient): HealthSummary {
        val now = Instant.now()

        val weight = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(90, ChronoUnit.DAYS), now),
                ascendingOrder = false,
                pageSize = 1
            )
        ).records.firstOrNull()?.weight?.inPounds

        val restingHr = client.readRecords(
            ReadRecordsRequest(
                recordType = RestingHeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(14, ChronoUnit.DAYS), now),
                ascendingOrder = false,
                pageSize = 1
            )
        ).records.firstOrNull()?.beatsPerMinute

        val sleepMinutes = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(3, ChronoUnit.DAYS), now),
                ascendingOrder = false,
                pageSize = 1
            )
        ).records.firstOrNull()?.let { Duration.between(it.startTime, it.endTime).toMinutes() }

        val stepsTotal = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(now.minus(7, ChronoUnit.DAYS), now)
            )
        )[StepsRecord.COUNT_TOTAL]

        return HealthSummary(
            weightLb = weight,
            restingHr = restingHr,
            sleepMinutes = sleepMinutes,
            avgDailySteps = stepsTotal?.let { it / 7 }
        )
    }
}
