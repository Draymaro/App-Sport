package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // Pectoraux, Jambes, Dos, Épaules, Bras, Abdos, Cardio
    val equipment: String = "Barre/Haltères",
    val notes: String = ""
)

@Entity(tableName = "workout_routines")
data class WorkoutRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val reminderTime: String = "18:00", // "HH:mm"
    val isReminderEnabled: Boolean = false
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index("exerciseId")]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeightKg: Float = 0f,
    val restTimeSeconds: Int = 90,
    val orderIndex: Int = 0
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long? = null,
    val sessionName: String,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long? = null,
    val notes: String = "",
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class ExerciseSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val completed: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "monthly_analyses")
data class MonthlyAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String, // e.g. "2026-08"
    val analysisText: String,
    val scientificReferencesText: String,
    val generatedAtMillis: Long = System.currentTimeMillis()
)
