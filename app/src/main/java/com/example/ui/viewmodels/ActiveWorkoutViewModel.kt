package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.BuzzerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ActiveWorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = WorkoutRepository(
        exerciseDao = db.exerciseDao(),
        routineDao = db.workoutRoutineDao(),
        sessionDao = db.workoutSessionDao(),
        setDao = db.exerciseSetDao(),
        monthlyAnalysisDao = db.monthlyAnalysisDao()
    )

    private val buzzerManager = BuzzerManager(application)

    private var timerJob: Job? = null

    private val _timerSecondsLeft = MutableStateFlow(0)
    val timerSecondsLeft: StateFlow<Int> = _timerSecondsLeft.asStateFlow()

    private val _totalTimerDuration = MutableStateFlow(90)
    val totalTimerDuration: StateFlow<Int> = _totalTimerDuration.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timerLabel = MutableStateFlow("Repos")
    val timerLabel: StateFlow<String> = _timerLabel.asStateFlow()

    fun startRestTimer(seconds: Int, label: String = "Temps de Repos") {
        timerJob?.cancel()
        _totalTimerDuration.value = seconds
        _timerSecondsLeft.value = seconds
        _timerLabel.value = label
        _isTimerRunning.value = true

        timerJob = viewModelScope.launch {
            while (_timerSecondsLeft.value > 0) {
                delay(1000L)
                _timerSecondsLeft.value -= 1

                if (_timerSecondsLeft.value in 1..5) {
                    buzzerManager.speakCountdownNumber(_timerSecondsLeft.value)
                }
            }
            _isTimerRunning.value = false
            buzzerManager.playBuzzerSound() // Loud buzzer sound over background music
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = false
    }

    fun resumeTimer() {
        if (_timerSecondsLeft.value > 0) {
            _isTimerRunning.value = true
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (_timerSecondsLeft.value > 0) {
                    delay(1000L)
                    _timerSecondsLeft.value -= 1
                    if (_timerSecondsLeft.value in 1..5) {
                        buzzerManager.speakCountdownNumber(_timerSecondsLeft.value)
                    }
                }
                _isTimerRunning.value = false
                buzzerManager.playBuzzerSound()
            }
        }
    }

    fun addTimerSeconds(addSeconds: Int) {
        _timerSecondsLeft.value += addSeconds
        _totalTimerDuration.value += addSeconds
    }

    fun skipTimer() {
        timerJob?.cancel()
        _timerSecondsLeft.value = 0
        _isTimerRunning.value = false
    }

    fun toggleSetCompletion(
        set: ExerciseSetEntity,
        restTimeSeconds: Int = 90,
        autoTimerEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            val updated = set.copy(completed = !set.completed)
            repository.insertOrUpdateSet(updated)

            if (updated.completed && autoTimerEnabled) {
                startRestTimer(restTimeSeconds, "Repos - Série suivante")
            }
        }
    }

    fun updateSetData(set: ExerciseSetEntity, weightKg: Float, reps: Int) {
        viewModelScope.launch {
            repository.insertOrUpdateSet(set.copy(weightKg = weightKg, reps = reps))
        }
    }

    fun addSetToExercise(sessionId: Long, exerciseId: Long, currentSetCount: Int, defaultWeight: Float, defaultReps: Int) {
        viewModelScope.launch {
            repository.insertOrUpdateSet(
                ExerciseSetEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    setNumber = currentSetCount + 1,
                    reps = defaultReps,
                    weightKg = defaultWeight,
                    completed = false
                )
            )
        }
    }

    fun addExerciseToSession(sessionId: Long, exerciseId: Long, defaultSets: Int = 3) {
        viewModelScope.launch {
            for (num in 1..defaultSets) {
                repository.insertOrUpdateSet(
                    ExerciseSetEntity(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        setNumber = num,
                        reps = 10,
                        weightKg = 20f,
                        completed = false
                    )
                )
            }
        }
    }

    fun createAndAddCustomExerciseToSession(
        sessionId: Long,
        name: String,
        category: String,
        equipment: String,
        notes: String,
        defaultSets: Int = 3
    ) {
        viewModelScope.launch {
            val exercise = ExerciseEntity(
                name = name.trim(),
                category = category.ifBlank { "Autre" }.trim(),
                equipment = equipment.ifBlank { "Libre" }.trim(),
                notes = notes.trim()
            )
            val newExerciseId = repository.insertExercise(exercise)
            addExerciseToSession(sessionId, newExerciseId, defaultSets)
        }
    }

    fun removeExerciseFromSession(sessionId: Long, exerciseId: Long) {
        viewModelScope.launch {
            repository.deleteSetsForExerciseInSession(sessionId, exerciseId)
        }
    }

    fun finishSession(
        session: WorkoutSessionEntity,
        notes: String,
        onFinished: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateSession(
                session.copy(
                    endTimeMillis = System.currentTimeMillis(),
                    notes = notes,
                    isCompleted = true
                )
            )
            timerJob?.cancel()
            onFinished()
        }
    }

    override fun onCleared() {
        super.onCleared()
        buzzerManager.release()
    }
}
