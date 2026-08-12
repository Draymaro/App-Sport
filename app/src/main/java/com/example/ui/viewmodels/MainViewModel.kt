package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.util.UserPreferences

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val userPreferences = UserPreferences(application)
    val userBodyweightKg: StateFlow<Float> = userPreferences.userBodyweightKg

    val db = AppDatabase.getInstance(application)
    val repository = WorkoutRepository(
        exerciseDao = db.exerciseDao(),
        routineDao = db.workoutRoutineDao(),
        sessionDao = db.workoutSessionDao(),
        setDao = db.exerciseSetDao(),
        monthlyAnalysisDao = db.monthlyAnalysisDao()
    )

    private val _isDarkMode = MutableStateFlow(true) // Default to gym dark mode for battery saving
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    val allExercises: StateFlow<List<ExerciseEntity>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routines: StateFlow<List<RoutineWithExercises>> = repository.allRoutinesWithExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSessions: StateFlow<List<SessionWithSets>> = repository.completedSessionsWithSets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<SessionWithSets?> = _activeSessionId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getSessionWithSets(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Auto check for any uncompleted session on app launch
        viewModelScope.launch {
            repository.allSessionsWithSets.firstOrNull()?.firstOrNull { !it.session.isCompleted }?.let {
                _activeSessionId.value = it.session.id
            }
        }
    }

    fun setUserBodyweight(weightKg: Float) {
        userPreferences.setUserBodyweightKg(weightKg)
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            if (_activeSessionId.value == sessionId) {
                _activeSessionId.value = null
            }
            repository.deleteSession(sessionId)
        }
    }

    fun updateSessionDetails(
        session: WorkoutSessionEntity,
        updatedSets: List<ExerciseSetEntity>
    ) {
        viewModelScope.launch {
            repository.updateSession(session)
            updatedSets.forEach { set ->
                repository.insertOrUpdateSet(set)
            }
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun startSessionFromRoutine(routine: RoutineWithExercises, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val newSessionId = repository.createNewSessionFromRoutine(routine)
            _activeSessionId.value = newSessionId
            onStarted(newSessionId)
        }
    }

    fun startBlankSession(sessionName: String, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val newSessionId = repository.createBlankSession(sessionName)
            _activeSessionId.value = newSessionId
            onStarted(newSessionId)
        }
    }

    fun resumeSession(sessionId: Long) {
        _activeSessionId.value = sessionId
    }

    fun clearActiveSession() {
        _activeSessionId.value = null
    }

    fun createNewExercise(name: String, category: String, equipment: String, notes: String) {
        viewModelScope.launch {
            repository.insertExercise(
                ExerciseEntity(
                    name = name.trim(),
                    category = category.trim(),
                    equipment = equipment.trim(),
                    notes = notes.trim()
                )
            )
        }
    }
}
