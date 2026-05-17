package com.sqldpass.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "mock_exams")
data class MockExamEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val examType: String?,
    val sequence: Int,
    val visibility: String?,
    val kind: String?,
    val examYear: Int?,
    val examRound: Int?,
    val examDate: String?,
    val totalQuestions: Int,
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: Long,
    val mockExamId: Long,
    val displayOrder: Int,
    val subjectName: String?,
    val content: String,
    val questionType: String?,
    val correctOption: Int?,
    val answer: String?,
    val explanation: String?,
)

@Entity(tableName = "metadata")
data class MetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(tableName = "pending_solves")
data class PendingSolveEntity(
    @PrimaryKey val clientSubmissionId: String,
    val mockExamId: Long,
    val answersJson: String,
    val createdAtMillis: Long,
    val synced: Boolean = false,
    val serverSolveId: Long? = null,
)

@Dao
interface OfflineDao {
    @Query("SELECT * FROM mock_exams ORDER BY examType ASC, sequence DESC")
    suspend fun mockExams(): List<MockExamEntity>

    @Query("SELECT * FROM mock_exams WHERE id = :id")
    suspend fun mockExam(id: Long): MockExamEntity?

    @Query("SELECT * FROM questions WHERE mockExamId = :mockExamId ORDER BY displayOrder ASC")
    suspend fun questionsForExam(mockExamId: Long): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMockExams(items: List<MockExamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(items: List<QuestionEntity>)

    @Query("DELETE FROM mock_exams")
    suspend fun clearMockExams()

    @Query("DELETE FROM questions")
    suspend fun clearQuestions()

    @Query("SELECT value FROM metadata WHERE `key` = :key")
    suspend fun metadata(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(entity: MetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPendingSolve(entity: PendingSolveEntity)

    @Query("SELECT * FROM pending_solves WHERE synced = 0 ORDER BY createdAtMillis ASC")
    suspend fun unsyncedSolves(): List<PendingSolveEntity>

    @Query("UPDATE pending_solves SET synced = 1, serverSolveId = :serverSolveId WHERE clientSubmissionId = :clientSubmissionId")
    suspend fun markSolveSynced(clientSubmissionId: String, serverSolveId: Long)
}

@Database(
    entities = [
        MockExamEntity::class,
        QuestionEntity::class,
        MetadataEntity::class,
        PendingSolveEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SqldpassDatabase : RoomDatabase() {
    abstract fun offlineDao(): OfflineDao
}
