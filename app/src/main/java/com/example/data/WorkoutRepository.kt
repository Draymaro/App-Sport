package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt

class WorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val routineDao: WorkoutRoutineDao,
    private val sessionDao: WorkoutSessionDao,
    private val setDao: ExerciseSetDao,
    private val monthlyAnalysisDao: MonthlyAnalysisDao
) {
    // --- Exercises ---
    val allExercises: Flow<List<ExerciseEntity>> = exerciseDao.getAllExercises()

    suspend fun insertExercise(exercise: ExerciseEntity): Long {
        return exerciseDao.insertExercise(exercise)
    }

    suspend fun updateExercise(exercise: ExerciseEntity) {
        exerciseDao.updateExercise(exercise)
    }

    suspend fun deleteExercise(exercise: ExerciseEntity) {
        exerciseDao.deleteExercise(exercise)
    }

    // --- Workout Routines ---
    val allRoutinesWithExercises: Flow<List<RoutineWithExercises>> = routineDao.getAllRoutinesWithExercises()

    fun getRoutineWithExercises(routineId: Long): Flow<RoutineWithExercises?> {
        return routineDao.getRoutineWithExercises(routineId)
    }

    suspend fun saveRoutine(
        routine: WorkoutRoutineEntity,
        exercises: List<RoutineExerciseEntity>
    ): Long {
        val routineId = if (routine.id == 0L) {
            routineDao.insertRoutine(routine)
        } else {
            routineDao.updateRoutine(routine)
            routine.id
        }

        // Replace routine exercises
        routineDao.deleteRoutineExercisesForRoutine(routineId)
        exercises.forEachIndexed { index, item ->
            routineDao.insertRoutineExercise(
                item.copy(id = 0L, routineId = routineId, orderIndex = index)
            )
        }

        return routineId
    }

    suspend fun deleteRoutine(routine: WorkoutRoutineEntity) {
        routineDao.deleteRoutine(routine)
    }

    // --- Workout Sessions ---
    val allSessionsWithSets: Flow<List<SessionWithSets>> = sessionDao.getAllSessionsWithSets()
    val completedSessionsWithSets: Flow<List<SessionWithSets>> = sessionDao.getCompletedSessionsWithSets()

    fun getSessionWithSets(sessionId: Long): Flow<SessionWithSets?> {
        return sessionDao.getSessionWithSets(sessionId)
    }

    suspend fun createNewSessionFromRoutine(routine: RoutineWithExercises): Long {
        val sessionEntity = WorkoutSessionEntity(
            routineId = routine.routine.id,
            sessionName = routine.routine.title,
            startTimeMillis = System.currentTimeMillis(),
            isCompleted = false
        )
        val sessionId = sessionDao.insertSession(sessionEntity)

        // Pre-populate sets from routine targets
        val initialSets = mutableListOf<ExerciseSetEntity>()
        routine.exercisesWithDetails.forEach { item ->
            val exId = item.exercise.id
            val targetSets = item.item.targetSets
            val targetReps = item.item.targetReps
            val targetWeight = item.item.targetWeightKg

            for (setNum in 1..targetSets) {
                initialSets.add(
                    ExerciseSetEntity(
                        sessionId = sessionId,
                        exerciseId = exId,
                        setNumber = setNum,
                        reps = targetReps,
                        weightKg = targetWeight,
                        completed = false
                    )
                )
            }
        }
        if (initialSets.isNotEmpty()) {
            setDao.insertSets(initialSets)
        }

        return sessionId
    }

    suspend fun createBlankSession(sessionName: String): Long {
        val sessionEntity = WorkoutSessionEntity(
            routineId = null,
            sessionName = sessionName.ifBlank { "Séance Libre" },
            startTimeMillis = System.currentTimeMillis(),
            isCompleted = false
        )
        return sessionDao.insertSession(sessionEntity)
    }

    suspend fun updateSession(session: WorkoutSessionEntity) {
        sessionDao.updateSession(session)
    }

    suspend fun finishSession(sessionId: Long, notes: String = "") {
        val sessionWithSets = sessionDao.getSessionWithSets(sessionId)
        // We can update end time and notes
        val session = sessionDao.getAllSessions()
        // Simple direct update
    }

    suspend fun deleteSession(sessionId: Long) {
        sessionDao.deleteSessionById(sessionId)
    }

    // --- Sets ---
    suspend fun insertOrUpdateSet(set: ExerciseSetEntity): Long {
        return setDao.insertSet(set)
    }

    suspend fun deleteSet(set: ExerciseSetEntity) {
        setDao.deleteSet(set)
    }

    suspend fun deleteSetsForExerciseInSession(sessionId: Long, exerciseId: Long) {
        setDao.deleteSetsForExerciseInSession(sessionId, exerciseId)
    }

    // --- Progress & Analytics ---
    fun getExerciseProgressHistory(exerciseId: Long, userBodyweightKg: Float = 75.0f): Flow<List<ExerciseProgressPoint>> {
        return completedSessionsWithSets.map { sessions ->
            sessions
                .filter { session -> session.session.isCompleted }
                .mapNotNull { session ->
                    val matchingSets = session.setsWithDetails.filter {
                        it.set.exerciseId == exerciseId && it.set.completed
                    }
                    if (matchingSets.isEmpty()) return@mapNotNull null

                    val sampleExercise = matchingSets.firstOrNull()?.exercise
                    val isBodyweight = sampleExercise != null && (
                        sampleExercise.equipment.contains("Poids du corps", ignoreCase = true) ||
                        sampleExercise.equipment.contains("Poids corps", ignoreCase = true) ||
                        sampleExercise.category.contains("Poids du corps", ignoreCase = true) ||
                        sampleExercise.name.contains("Pompe", ignoreCase = true) ||
                        sampleExercise.name.contains("Traction", ignoreCase = true) ||
                        sampleExercise.name.contains("Dip", ignoreCase = true)
                    )

                    fun getEffectiveWeight(setWeight: Float): Float {
                        return if (isBodyweight) userBodyweightKg + setWeight else setWeight
                    }

                    val maxWeight = matchingSets.maxOfOrNull { getEffectiveWeight(it.set.weightKg) } ?: 0f
                    val totalVolume = matchingSets.sumOf { (getEffectiveWeight(it.set.weightKg) * it.set.reps).toDouble() }.toFloat()
                    val totalReps = matchingSets.sumOf { it.set.reps }

                    // Compute max 1RM using Epley formula on best set
                    val bestSet = matchingSets.maxByOrNull { getEffectiveWeight(it.set.weightKg) * (1f + it.set.reps / 30f) }
                    val est1RM = if (bestSet != null) {
                        val effWeight = getEffectiveWeight(bestSet.set.weightKg)
                        if (bestSet.set.reps == 1) effWeight
                        else effWeight * (1f + bestSet.set.reps / 30f)
                    } else 0f

                    ExerciseProgressPoint(
                        sessionId = session.session.id,
                        sessionName = session.session.sessionName,
                        timestampMillis = session.session.startTimeMillis,
                        maxWeightKg = maxWeight,
                        totalVolumeKg = totalVolume,
                        totalReps = totalReps,
                        estimated1RM = (est1RM * 10f).roundToInt() / 10f
                    )
                }
                .sortedBy { it.timestampMillis }
        }
    }

    // --- Monthly Analyses ---
    fun getMonthlyAnalysis(monthKey: String): Flow<MonthlyAnalysisEntity?> {
        return monthlyAnalysisDao.getAnalysisForMonth(monthKey)
    }

    suspend fun saveMonthlyAnalysis(analysis: MonthlyAnalysisEntity) {
        monthlyAnalysisDao.insertAnalysis(analysis)
    }
}
