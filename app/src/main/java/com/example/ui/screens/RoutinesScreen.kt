package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.notification.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    routines: List<RoutineWithExercises>,
    allExercises: List<ExerciseEntity>,
    onStartRoutine: (RoutineWithExercises) -> Unit,
    onSaveRoutine: (WorkoutRoutineEntity, List<RoutineExerciseEntity>) -> Unit,
    onDeleteRoutine: (WorkoutRoutineEntity) -> Unit,
    onCreateExercise: (String, String, String, String) -> Unit
) {
    var editingRoutine by remember { mutableStateOf<RoutineWithExercises?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    var showExerciseLibrary by remember { mutableStateOf(false) }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }

    var schedulingReminderForRoutine by remember { mutableStateOf<WorkoutRoutineEntity?>(null) }
    var reminderTimeText by remember { mutableStateOf("18:30") }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Programmes & Séances",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = { showExerciseLibrary = true }) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Bibliothèque d'exercices",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isCreatingNew = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nouveau Programme", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        if (routines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucune séance programmée",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { isCreatingNew = true }) {
                        Text("Créer mon premier programme")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(routines) { item ->
                    RoutineDetailCard(
                        routineWithExercises = item,
                        onStart = { onStartRoutine(item) },
                        onEdit = { editingRoutine = item },
                        onScheduleReminder = { schedulingReminderForRoutine = item.routine },
                        onDelete = { onDeleteRoutine(item.routine) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Exercise Library Dialog
    if (showExerciseLibrary) {
        com.example.ui.components.ExerciseLibraryDialog(
            allExercises = allExercises,
            onDismiss = { showExerciseLibrary = false },
            onCreateExercise = onCreateExercise
        )
    }

    // Edit or Create Dialog
    if (editingRoutine != null || isCreatingNew) {
        RoutineEditorDialog(
            routineToEdit = editingRoutine,
            allExercises = allExercises,
            onDismiss = {
                editingRoutine = null
                isCreatingNew = false
            },
            onSave = { routine, exercises ->
                onSaveRoutine(routine, exercises)
                editingRoutine = null
                isCreatingNew = false
            },
            onCreateExercise = onCreateExercise
        )
    }

    // Schedule Reminder Dialog
    if (schedulingReminderForRoutine != null) {
        val targetRoutine = schedulingReminderForRoutine!!
        AlertDialog(
            onDismissRequest = { schedulingReminderForRoutine = null },
            icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
            title = { Text("Rappel de Séance") },
            text = {
                Column {
                    Text("Programmer une notification de rappel pour '${targetRoutine.title}' :")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reminderTimeText,
                        onValueChange = { reminderTimeText = it },
                        label = { Text("Heure du rappel (ex: 18:30)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        NotificationHelper.scheduleWorkoutReminder(
                            context = context,
                            routineId = targetRoutine.id,
                            routineTitle = targetRoutine.title,
                            timeString = reminderTimeText
                        )
                        schedulingReminderForRoutine = null
                    }
                ) {
                    Text("Activer le Rappel")
                }
            },
            dismissButton = {
                TextButton(onClick = { schedulingReminderForRoutine = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun RoutineDetailCard(
    routineWithExercises: RoutineWithExercises,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onScheduleReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val routine = routineWithExercises.routine
    val exercises = routineWithExercises.exercisesWithDetails

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    if (routine.description.isNotBlank()) {
                        Text(
                            text = routine.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onScheduleReminder) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Rappel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Exercise List inside routine
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercises.forEach { ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ex.exercise.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "${ex.exercise.category} • Repos: ${ex.item.restTimeSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${ex.item.targetSets} x ${ex.item.targetReps} @ ${ex.item.targetWeightKg}kg",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Démarrer cette séance", fontWeight = FontWeight.Bold)
            }
        }
    }
}
