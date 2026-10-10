package com.fairwayfit.app.data.sync

import android.content.Context
import com.fairwayfit.app.data.db.AppDatabase
import com.fairwayfit.app.data.db.TestResultEntity
import com.fairwayfit.app.data.db.WorkoutLogEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class SyncStatus(
    val available: Boolean,
    val email: String?,
    val busy: Boolean = false,
    val message: String = ""
)

/**
 * Merge-by-id sync between Room and Firestore. Every record is immutable and has a unique id, so a
 * sync is simply: upload the ids the cloud lacks, download the ids the device lacks.
 *
 * Cloud sync is only available when google-services.json is present in app/ (see README).
 */
class SyncManager(context: Context, private val db: AppDatabase) {

    private val appContext = context.applicationContext
    private val mutex = Mutex()

    val isAvailable: Boolean get() = FirebaseApp.getApps(appContext).isNotEmpty()

    private val _status = MutableStateFlow(
        SyncStatus(
            available = FirebaseApp.getApps(appContext).isNotEmpty(),
            email = null,
            message = if (FirebaseApp.getApps(appContext).isNotEmpty()) "" else "Cloud sync isn't configured yet."
        )
    )
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    init {
        if (isAvailable) {
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                _status.update { it.copy(email = auth.currentUser?.email) }
            }
        }
    }

    suspend fun signIn(email: String, password: String) {
        runReporting("Signing in…") {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            syncInternal()
        }
    }

    suspend fun register(email: String, password: String) {
        runReporting("Creating account…") {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
            syncInternal()
        }
    }

    fun signOut() {
        check(isAvailable) { "Cloud sync is not configured" }
        FirebaseAuth.getInstance().signOut()
        _status.update { it.copy(message = "Signed out.") }
    }

    suspend fun syncNow() {
        runReporting("Syncing…") { syncInternal() }
    }

    /** Background sync after local writes: does nothing unless configured and signed in. */
    suspend fun syncQuietly() {
        if (!isAvailable || FirebaseAuth.getInstance().currentUser == null) return
        syncNow()
    }

    private suspend fun runReporting(startMessage: String, block: suspend () -> Unit) {
        check(isAvailable) { "Cloud sync is not configured" }
        _status.update { it.copy(busy = true, message = startMessage) }
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _status.update { it.copy(busy = false, message = e.message ?: "Sync failed") }
            return
        }
        _status.update { it.copy(busy = false) }
    }

    private suspend fun syncInternal() = mutex.withLock {
        val user = requireNotNull(FirebaseAuth.getInstance().currentUser) { "Not signed in" }
        val userDoc = FirebaseFirestore.getInstance().collection("users").document(user.uid)
        val testsCol = userDoc.collection("testResults")
        val logsCol = userDoc.collection("workoutLogs")

        // ---- test results
        val remoteTests = testsCol.get().await().documents.map { d ->
            TestResultEntity(
                id = d.id,
                sessionId = requireNotNull(d.getString("sessionId")) { "bad cloud record ${d.id}" },
                dateMillis = requireNotNull(d.getLong("dateMillis")) { "bad cloud record ${d.id}" },
                testKey = requireNotNull(d.getString("testKey")) { "bad cloud record ${d.id}" },
                value = requireNotNull(d.getDouble("value")) { "bad cloud record ${d.id}" }
            )
        }
        val remoteTestIds = remoteTests.map { it.id }.toSet()
        val testsToUpload = db.testResultDao().getAll().filter { it.id !in remoteTestIds }
        testsToUpload.chunked(400).forEach { chunk ->
            val batch = FirebaseFirestore.getInstance().batch()
            chunk.forEach { e ->
                batch.set(
                    testsCol.document(e.id),
                    mapOf(
                        "sessionId" to e.sessionId,
                        "dateMillis" to e.dateMillis,
                        "testKey" to e.testKey,
                        "value" to e.value
                    )
                )
            }
            batch.commit().await()
        }
        db.testResultDao().insertAll(remoteTests)

        // ---- workout logs
        val remoteLogs = logsCol.get().await().documents.map { d ->
            WorkoutLogEntity(
                id = d.id,
                dateMillis = requireNotNull(d.getLong("dateMillis")) { "bad cloud record ${d.id}" },
                workoutId = requireNotNull(d.getString("workoutId")) { "bad cloud record ${d.id}" },
                durationSec = requireNotNull(d.getLong("durationSec")) { "bad cloud record ${d.id}" }.toInt()
            )
        }
        val remoteLogIds = remoteLogs.map { it.id }.toSet()
        val logsToUpload = db.workoutLogDao().getAll().filter { it.id !in remoteLogIds }
        logsToUpload.chunked(400).forEach { chunk ->
            val batch = FirebaseFirestore.getInstance().batch()
            chunk.forEach { e ->
                batch.set(
                    logsCol.document(e.id),
                    mapOf(
                        "dateMillis" to e.dateMillis,
                        "workoutId" to e.workoutId,
                        "durationSec" to e.durationSec
                    )
                )
            }
            batch.commit().await()
        }
        db.workoutLogDao().insertAll(remoteLogs)

        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
        _status.update { it.copy(message = "Synced at $time") }
    }
}
