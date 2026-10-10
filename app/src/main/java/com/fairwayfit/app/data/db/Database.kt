package com.fairwayfit.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** One measured value for one test inside one test session. Rows are immutable once written. */
@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val dateMillis: Long,
    val testKey: String,
    val value: Double
)

/** A completed workout. Immutable once written. */
@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey val id: String,
    val dateMillis: Long,
    val workoutId: String,
    val durationSec: Int
)

@Dao
interface TestResultDao {
    @Query("SELECT * FROM test_results ORDER BY dateMillis ASC")
    fun observeAll(): Flow<List<TestResultEntity>>

    @Query("SELECT * FROM test_results")
    suspend fun getAll(): List<TestResultEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<TestResultEntity>)
}

@Dao
interface WorkoutLogDao {
    @Query("SELECT * FROM workout_logs ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_logs")
    suspend fun getAll(): List<WorkoutLogEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<WorkoutLogEntity>)
}

@Database(
    entities = [TestResultEntity::class, WorkoutLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun testResultDao(): TestResultDao
    abstract fun workoutLogDao(): WorkoutLogDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "fairwayfit.db").build()
    }
}
