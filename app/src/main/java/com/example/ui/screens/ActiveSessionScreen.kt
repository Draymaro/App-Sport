package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExerciseEntity
import com.example.data.ExerciseSetEntity
import com.example.data.SessionWithSets
import com.example.ui.viewmodels.ActiveWorkoutViewModel

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    sessionWithSets: SessionWithSets?,
    allExercises: List<ExerciseEntity>,
    viewModel: ActiveWorkoutViewModel,
    onFinishSession: () -> Unit
) {
    if (sessionWithSets == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Aucune séance active.")
        }
        return
    }

    val session = sessionWithSets.session
    val setsWithDetails = sessionWithSets.setsWithDetails

    val timerSecondsLeft by viewModel.timerSecondsLeft.collectAsState()
    val totalTimerDuration by viewModel.totalTimerDuration.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val timerLabel by viewModel.timerLabel.collectAsState()

    var showFinishDialog by remember { mutableStateOf(false) }
    var showQuitWithoutSaveDialog by remember { mutableStateOf(false) }
    var sessionNotes by remember { mutableStateOf(session.notes) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    // Group sets by exercise
    val setsByExercise = remember(setsWithDetails) {
        setsWithDetails.groupBy { it.exercise }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = session.sessionName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Séance en cours • Buzzer overlay actif",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { showQuitWithoutSaveDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Quitter", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { showFinishDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Terminer", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Live Rest Timer Widget Top Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTimerRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (isTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timerLabel.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val mins = timerSecondsLeft / 60
                        val secs = timerSecondsLeft % 60
                        val formattedTime = String.format("%02d:%02d", mins, secs)

                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Timer Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.addTimerSeconds(30) },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Text("+30s", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = {
                                if (isTimerRunning) viewModel.pauseTimer() else viewModel.resumeTimer()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }

                        IconButton(
                            onClick = { viewModel.skipTimer() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Passer")
                        }
                    }
                }
            }

            // Exercise List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                setsByExercise.forEach { (exercise, setsDetails) ->
                    item(key = exercise.id) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Exercise Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${exercise.category} • ${exercise.equipment}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.removeExerciseFromSession(session.id, exercise.id)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Retirer",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Sets Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Série", modifier = Modifier.weight(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Charge (kg)", modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Reps", modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Valider", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Sets Row List
                                setsDetails.sortedBy { it.set.setNumber }.forEach { setDetail ->
                                    val set = setDetail.set
                                    var weightText by remember(set.weightKg) { mutableStateOf(set.weightKg.toString()) }
                                    var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Set Number Pill
                                        Box(
                                            modifier = Modifier
                                                .weight(0.8f)
                                                .background(
                                                    if (set.completed) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                                    CircleShape
                                                )
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${set.setNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (set.completed) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Weight Input
                                        OutlinedTextField(
                                            value = weightText,
                                            onValueChange = {
                                                weightText = it
                                                val w = it.toFloatOrNull() ?: set.weightKg
                                                viewModel.updateSetData(set, w, set.reps)
                                            },
                                            modifier = Modifier.weight(1.5f),
                                            singleLine = true
                                        )

                                        // Reps Input
                                        OutlinedTextField(
                                            value = repsText,
                                            onValueChange = {
                                                repsText = it
                                                val r = it.toIntOrNull() ?: set.reps
                                                viewModel.updateSetData(set, set.weightKg, r)
                                            },
                                            modifier = Modifier.weight(1.5f),
                                            singleLine = true
                                        )

                                        // Checkmark Completion Button
                                        IconButton(
                                            onClick = {
                                                viewModel.toggleSetCompletion(set)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (set.completed) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(12.dp)
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Valider Série",
                                                tint = if (set.completed) Color.Black else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Add extra set button
                                TextButton(
                                    onClick = {
                                        val lastSet = setsDetails.lastOrNull()?.set
                                        viewModel.addSetToExercise(
                                            sessionId = session.id,
                                            exerciseId = exercise.id,
                                            currentSetCount = setsDetails.size,
                                            defaultWeight = lastSet?.weightKg ?: 20f,
                                            defaultReps = lastSet?.reps ?: 10
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ajouter une série")
                                }
                            }
                        }
                    }
                }

                // Add Exercise Mid-Session Button
                item {
                    OutlinedButton(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajouter un exercice à la séance", fontWeight = FontWeight.Bold)
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    // Add Exercise Mid-Session Dialog
    if (showAddExerciseDialog) {
        com.example.ui.components.ExerciseLibraryDialog(
            allExercises = allExercises,
            onDismiss = { showAddExerciseDialog = false },
            onCreateExercise = { name, category, equipment, notes ->
                viewModel.createAndAddCustomExerciseToSession(
                    sessionId = session.id,
                    name = name,
                    category = category,
                    equipment = equipment,
                    notes = notes,
                    defaultSets = 3
                )
            },
            onSelectExercise = { ex ->
                viewModel.addExerciseToSession(session.id, ex.id, 3)
            }
        )
    }

    // Quit Without Save Confirmation Dialog
    if (showQuitWithoutSaveDialog) {
        AlertDialog(
            onDismissRequest = { showQuitWithoutSaveDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Quitter la séance ?") },
            text = {
                Text("Voulez-vous vraiment quitter sans enregistrer ? Toutes les séries validées durant cette session seront effacées.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitWithoutSaveDialog = false
                        viewModel.discardSession(session.id, onFinishSession)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Quitter sans enregistrer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitWithoutSaveDialog = false }) {
                    Text("Poursuivre l'entraînement")
                }
            }
        )
    }

    // Finish Session Dialog
    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Séance Terminée ! 👏") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Félicitations pour votre séance. Laissez une note sur vos ressentis (RPE, sensations) :")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { sessionNotes = it },
                        label = { Text("Notes (ex: Bonne congestion, RPE 8)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showFinishDialog = false
                            viewModel.finishSession(session, sessionNotes, onFinishSession)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enregistrer & Quitter")
                    }

                    OutlinedButton(
                        onClick = {
                            showFinishDialog = false
                            showQuitWithoutSaveDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Quitter sans enregistrer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Continuer la séance")
                }
            }
        )
    }
}
