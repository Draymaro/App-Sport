package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MonthlyAnalysisEntity
import com.example.notification.NotificationHelper
import com.example.ui.screens.*
import com.example.ui.theme.FitProgressTheme
import com.example.ui.viewmodels.ActiveWorkoutViewModel
import com.example.ui.viewmodels.MainViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val activeWorkoutViewModel: ActiveWorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        NotificationHelper.createNotificationChannel(this)

        // Check launch intent for notification routine click
        val launchRoutineId = intent.getLongExtra("LAUNCH_ROUTINE_ID", -1L)
        if (launchRoutineId != -1L) {
            // Auto start routine if launched from notification
            lifecycleScopeLaunch {
                mainViewModel.routines.firstOrNull()?.find { it.routine.id == launchRoutineId }?.let { targetRoutine ->
                    mainViewModel.startSessionFromRoutine(targetRoutine) {}
                }
            }
        }

        setContent {
            val isDarkMode by mainViewModel.isDarkMode.collectAsStateWithLifecycle()

            // Request Notification Permission on Android 13+
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            FitProgressTheme(darkTheme = isDarkMode) {
                FitProgressApp(
                    mainViewModel = mainViewModel,
                    activeWorkoutViewModel = activeWorkoutViewModel
                )
            }
        }
    }

    private fun lifecycleScopeLaunch(block: suspend () -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            block()
        }
    }
}

enum class ScreenTab(val title: String, val icon: ImageVector) {
    HOME("Accueil", Icons.Default.Home),
    ROUTINES("Séances", Icons.Default.FitnessCenter),
    ACTIVE("Séance Active", Icons.Default.Timer),
    PROGRESS("Évolution", Icons.Default.ShowChart),
    SCIENCE("Analyse IA", Icons.Default.AutoAwesome)
}

@Composable
fun FitProgressApp(
    mainViewModel: MainViewModel,
    activeWorkoutViewModel: ActiveWorkoutViewModel
) {
    var selectedTab by remember { mutableStateOf(ScreenTab.HOME) }

    val isDarkMode by mainViewModel.isDarkMode.collectAsStateWithLifecycle()
    val userBodyweightKg by mainViewModel.userBodyweightKg.collectAsStateWithLifecycle()
    val routines by mainViewModel.routines.collectAsStateWithLifecycle()
    val allExercises by mainViewModel.allExercises.collectAsStateWithLifecycle()
    val activeSession by mainViewModel.activeSession.collectAsStateWithLifecycle()
    val completedSessions by mainViewModel.completedSessions.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val monthYearKey = remember {
        java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.FRENCH).format(java.util.Date())
    }
    val currentAnalysis by mainViewModel.repository.getMonthlyAnalysis(monthYearKey).collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.HOME,
                    onClick = { selectedTab = ScreenTab.HOME },
                    icon = { Icon(ScreenTab.HOME.icon, contentDescription = null) },
                    label = { Text(ScreenTab.HOME.title, fontWeight = FontWeight.Bold) }
                )

                NavigationBarItem(
                    selected = selectedTab == ScreenTab.ROUTINES,
                    onClick = { selectedTab = ScreenTab.ROUTINES },
                    icon = { Icon(ScreenTab.ROUTINES.icon, contentDescription = null) },
                    label = { Text(ScreenTab.ROUTINES.title, fontWeight = FontWeight.Bold) }
                )

                if (activeSession != null) {
                    NavigationBarItem(
                        selected = selectedTab == ScreenTab.ACTIVE,
                        onClick = { selectedTab = ScreenTab.ACTIVE },
                        icon = {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = Color(0xFF10B981))
                                }
                            ) {
                                Icon(ScreenTab.ACTIVE.icon, contentDescription = null)
                            }
                        },
                        label = { Text("Active", fontWeight = FontWeight.Bold) }
                    )
                }

                NavigationBarItem(
                    selected = selectedTab == ScreenTab.PROGRESS,
                    onClick = { selectedTab = ScreenTab.PROGRESS },
                    icon = { Icon(ScreenTab.PROGRESS.icon, contentDescription = null) },
                    label = { Text(ScreenTab.PROGRESS.title, fontWeight = FontWeight.Bold) }
                )

                NavigationBarItem(
                    selected = selectedTab == ScreenTab.SCIENCE,
                    onClick = { selectedTab = ScreenTab.SCIENCE },
                    icon = { Icon(ScreenTab.SCIENCE.icon, contentDescription = null) },
                    label = { Text("IA Science", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                ScreenTab.HOME -> {
                    HomeScreen(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { mainViewModel.toggleDarkMode() },
                        userBodyweightKg = userBodyweightKg,
                        onUpdateBodyweight = { mainViewModel.setUserBodyweight(it) },
                        routines = routines,
                        activeSession = activeSession,
                        completedSessions = completedSessions,
                        allExercises = allExercises,
                        onStartRoutine = { routine ->
                            mainViewModel.startSessionFromRoutine(routine) {
                                selectedTab = ScreenTab.ACTIVE
                            }
                        },
                        onStartBlankSession = { name ->
                            mainViewModel.startBlankSession(name) {
                                selectedTab = ScreenTab.ACTIVE
                            }
                        },
                        onResumeActiveSession = { _ ->
                            selectedTab = ScreenTab.ACTIVE
                        },
                        onUpdateSession = { updatedSession, updatedSets ->
                            mainViewModel.updateSessionDetails(updatedSession, updatedSets)
                        },
                        onDeleteSession = { sessionId ->
                            mainViewModel.deleteSession(sessionId)
                        },
                        onCreateExercise = { name, cat, equip, notes ->
                            mainViewModel.createNewExercise(name, cat, equip, notes)
                        },
                        onNavigateToRoutines = { selectedTab = ScreenTab.ROUTINES },
                        onNavigateToScience = { selectedTab = ScreenTab.SCIENCE },
                        onNavigateToProgress = { selectedTab = ScreenTab.PROGRESS }
                    )
                }

                ScreenTab.ROUTINES -> {
                    RoutinesScreen(
                        routines = routines,
                        allExercises = allExercises,
                        onStartRoutine = { routine ->
                            mainViewModel.startSessionFromRoutine(routine) {
                                selectedTab = ScreenTab.ACTIVE
                            }
                        },
                        onSaveRoutine = { routine, exercises ->
                            scope.launch {
                                mainViewModel.repository.saveRoutine(routine, exercises)
                            }
                        },
                        onDeleteRoutine = { routine ->
                            scope.launch {
                                mainViewModel.repository.deleteRoutine(routine)
                            }
                        },
                        onCreateExercise = { name, cat, equip, notes ->
                            mainViewModel.createNewExercise(name, cat, equip, notes)
                        }
                    )
                }

                ScreenTab.ACTIVE -> {
                    ActiveSessionScreen(
                        sessionWithSets = activeSession,
                        allExercises = allExercises,
                        viewModel = activeWorkoutViewModel,
                        onFinishSession = {
                            mainViewModel.clearActiveSession()
                            selectedTab = ScreenTab.HOME
                        }
                    )
                }

                ScreenTab.PROGRESS -> {
                    ProgressScreen(
                        allExercises = allExercises,
                        completedSessions = completedSessions,
                        onLoadExerciseProgress = { exId ->
                            mainViewModel.repository.getExerciseProgressHistory(exId)
                                .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
                        }
                    )
                }

                ScreenTab.SCIENCE -> {
                    ScienceAnalysisScreen(
                        completedSessions = completedSessions,
                        currentAnalysis = currentAnalysis,
                        onSaveAnalysis = { analysis ->
                            scope.launch {
                                mainViewModel.repository.saveMonthlyAnalysis(analysis)
                            }
                        }
                    )
                }
            }
        }
    }
}
