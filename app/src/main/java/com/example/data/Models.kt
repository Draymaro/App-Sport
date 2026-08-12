package com.example.data

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineExerciseWithDetails(
    @Embedded val item: RoutineExerciseEntity,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity
)

data class RoutineWithExercises(
    @Embedded val routine: WorkoutRoutineEntity,
    @Relation(
        entity = RoutineExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val exercisesWithDetails: List<RoutineExerciseWithDetails>
)

data class ExerciseSetWithDetails(
    @Embedded val set: ExerciseSetEntity,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity
)

data class SessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = ExerciseSetEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val setsWithDetails: List<ExerciseSetWithDetails>
)

data class ExerciseProgressPoint(
    val sessionId: Long,
    val sessionName: String,
    val timestampMillis: Long,
    val maxWeightKg: Float,
    val totalVolumeKg: Float,
    val totalReps: Int,
    val estimated1RM: Float
)
