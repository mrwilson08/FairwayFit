package com.fairwayfit.app.data

import com.fairwayfit.app.data.db.AppDatabase
import com.fairwayfit.app.data.db.TestResultEntity
import com.fairwayfit.app.data.db.WorkoutLogEntity
import com.fairwayfit.app.data.sync.SyncManager
import com.fairwayfit.app.domain.TestSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class Repository(
    db: AppDatabase,
    private val sync: SyncManager,
    private val scope: CoroutineScope
) {
    private val testDao = db.testResultDao()
    private val logDao = db.workoutLogDao()

    /** Test sessions, oldest first. The first one is the baseline. */
    val sessions: Flow<List<TestSession>> = testDao.observeAll().map { rows ->
        rows.groupBy { it.sessionId }
            .map { (id, list) ->
                TestSession(
                    id = id,
                    dateMillis = list.minOf { it.dateMillis },
                    values = list.associate { it.testKey to it.value }
                )
            }
            .sortedBy { it.dateMillis }
    }

    /** Completed workouts, newest first. */
    val workoutLogs: Flow<List<WorkoutLogEntity>> = logDao.observeAll()

    suspend fun saveSession(values: Map<String, Double>): String {
        require(values.isNotEmpty()) { "Cannot save a test session with no results" }
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        testDao.insertAll(
            values.map { (key, value) ->
                TestResultEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    dateMillis = now,
                    testKey = key,
                    value = value
                )
            }
        )
        scope.launch { sync.syncQuietly() }
        return sessionId
    }

    suspend fun logWorkout(workoutId: String, durationSec: Int) {
        logDao.insertAll(
            listOf(
                WorkoutLogEntity(
                    id = UUID.randomUUID().toString(),
                    dateMillis = System.currentTimeMillis(),
                    workoutId = workoutId,
                    durationSec = durationSec
                )
            )
        )
        scope.launch { sync.syncQuietly() }
    }
}
