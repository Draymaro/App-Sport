package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY category ASC, name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name ASC")
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExercises(exercises: List<ExerciseEntity>)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)
}

@Dao
interface WorkoutRoutineDao {
    @Query("SELECT * FROM workout_routines ORDER BY id DESC")
    fun getAllRoutines(): Flow<List<WorkoutRoutineEntity>>

    @Transaction
    @Query("SELECT * FROM workout_routines WHERE id = :id")
    fun getRoutineWithExercises(id: Long): Flow<RoutineWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_routines ORDER BY id DESC")
    fun getAllRoutinesWithExercises(): Flow<List<RoutineWithExercises>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: WorkoutRoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: WorkoutRoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: WorkoutRoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(routineExercise: RoutineExerciseEntity): Long

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteRoutineExercisesForRoutine(routineId: Long)

    @Delete
    suspend fun deleteRoutineExercise(routineExercise: RoutineExerciseEntity)
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun getSessionWithSets(id: Long): Flow<SessionWithSets?>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY startTimeMillis DESC")
    fun getAllSessionsWithSets(): Flow<List<SessionWithSets>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 1 ORDER BY startTimeMillis DESC")
    fun getCompletedSessionsWithSets(): Flow<List<SessionWithSets>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}

@Dao
interface ExerciseSetDao {
    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId ORDER BY exerciseId ASC, setNumber ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<ExerciseSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: ExerciseSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<ExerciseSetEntity>)

    @Update
    suspend fun updateSet(set: ExerciseSetEntity)

    @Delete
    suspend fun deleteSet(set: ExerciseSetEntity)

    @Query("DELETE FROM exercise_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteSetsForExerciseInSession(sessionId: Long, exerciseId: Long)

    @Query("""
        SELECT es.* FROM exercise_sets es
        INNER JOIN workout_sessions ws ON es.sessionId = ws.id
        WHERE es.exerciseId = :exerciseId AND ws.isCompleted = 1
        ORDER BY ws.startTimeMillis ASC
    """)
    fun getCompletedSetsForExercise(exerciseId: Long): Flow<List<ExerciseSetEntity>>
}

@Dao
interface MonthlyAnalysisDao {
    @Query("SELECT * FROM monthly_analyses WHERE monthKey = :monthKey LIMIT 1")
    fun getAnalysisForMonth(monthKey: String): Flow<MonthlyAnalysisEntity?>

    @Query("SELECT * FROM monthly_analyses ORDER BY generatedAtMillis DESC")
    fun getAllAnalyses(): Flow<List<MonthlyAnalysisEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: MonthlyAnalysisEntity)
}
