package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorDialog(
    routineToEdit: RoutineWithExercises?,
    allExercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onSave: (WorkoutRoutineEntity, List<RoutineExerciseEntity>) -> Unit,
    onCreateExercise: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(routineToEdit?.routine?.title ?: "") }
    var description by remember { mutableStateOf(routineToEdit?.routine?.description ?: "") }

    // List of exercises in routine
    var selectedExercises by remember {
        mutableStateOf<List<EditableRoutineExercise>>(
            routineToEdit?.exercisesWithDetails?.map { details ->
                EditableRoutineExercise(
                    exercise = details.exercise,
                    targetSets = details.item.targetSets,
                    targetReps = details.item.targetReps,
                    targetWeightKg = details.item.targetWeightKg,
                    restTimeSeconds = details.item.restTimeSeconds
                )
            } ?: emptyList()
        )
    }

    var showAddExercisePicker by remember { mutableStateOf(false) }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (routineToEdit != null) "Modifier la Séance" else "Nouveau Programme",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Inputs
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre du programme (ex: Push Pectoraux)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exercices (${selectedExercises.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row {
                        IconButton(onClick = { showCreateExerciseDialog = true }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Créer Exercice", tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Button(onClick = { showAddExercisePicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(selectedExercises) { index, item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${item.exercise.name}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedExercises = selectedExercises.toMutableList().apply { removeAt(index) }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Sets
                                    OutlinedTextField(
                                        value = item.targetSets.toString(),
                                        onValueChange = { v ->
                                            val valInt = v.toIntOrNull() ?: item.targetSets
                                            selectedExercises = selectedExercises.toMutableList().apply {
                                                this[index] = item.copy(targetSets = valInt)
                                            }
                                        },
                                        label = { Text("Séries") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    // Reps
                                    OutlinedTextField(
                                        value = item.targetReps.toString(),
                                        onValueChange = { v ->
                                            val valInt = v.toIntOrNull() ?: item.targetReps
                                            selectedExercises = selectedExercises.toMutableList().apply {
                                                this[index] = item.copy(targetReps = valInt)
                                            }
                                        },
                                        label = { Text("Répétitions") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    // Weight
                                    OutlinedTextField(
                                        value = item.targetWeightKg.toString(),
                                        onValueChange = { v ->
                                            val valFloat = v.toFloatOrNull() ?: item.targetWeightKg
                                            selectedExercises = selectedExercises.toMutableList().apply {
                                                this[index] = item.copy(targetWeightKg = valFloat)
                                            }
                                        },
                                        label = { Text("Charge (kg)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    // Rest Time
                                    OutlinedTextField(
                                        value = item.restTimeSeconds.toString(),
                                        onValueChange = { v ->
                                            val valInt = v.toIntOrNull() ?: item.restTimeSeconds
                                            selectedExercises = selectedExercises.toMutableList().apply {
                                                this[index] = item.copy(restTimeSeconds = valInt)
                                            }
                                        },
                                        label = { Text("Repos (s)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val routineEntity = WorkoutRoutineEntity(
                            id = routineToEdit?.routine?.id ?: 0L,
                            title = title.ifBlank { "Séance d'Entraînement" },
                            description = description
                        )
                        val routineExercises = selectedExercises.mapIndexed { idx, item ->
                            RoutineExerciseEntity(
                                routineId = routineEntity.id,
                                exerciseId = item.exercise.id,
                                targetSets = item.targetSets,
                                targetReps = item.targetReps,
                                targetWeightKg = item.targetWeightKg,
                                restTimeSeconds = item.restTimeSeconds,
                                orderIndex = idx
                            )
                        }
                        onSave(routineEntity, routineExercises)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Enregistrer le Programme", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Exercise Selection Dialog
    if (showAddExercisePicker) {
        com.example.ui.components.ExerciseLibraryDialog(
            allExercises = allExercises,
            onDismiss = { showAddExercisePicker = false },
            onCreateExercise = onCreateExercise,
            onSelectExercise = { ex ->
                selectedExercises = selectedExercises + EditableRoutineExercise(
                    exercise = ex,
                    targetSets = 3,
                    targetReps = 10,
                    targetWeightKg = 20f,
                    restTimeSeconds = 90
                )
            }
        )
    }

    // Create New Exercise Dialog
    if (showCreateExerciseDialog) {
        com.example.ui.components.CreateExerciseDialog(
            onDismiss = { showCreateExerciseDialog = false },
            onCreate = { name, category, equipment, notes ->
                onCreateExercise(name, category, equipment, notes)
                showCreateExerciseDialog = false
            }
        )
    }
}

data class EditableRoutineExercise(
    val exercise: ExerciseEntity,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Float,
    val restTimeSeconds: Int
)
