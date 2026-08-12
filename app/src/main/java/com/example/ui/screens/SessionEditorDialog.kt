package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditorDialog(
    sessionWithSets: SessionWithSets,
    allExercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onSave: (WorkoutSessionEntity, List<ExerciseSetEntity>) -> Unit,
    onDelete: (Long) -> Unit,
    onCreateExercise: (String, String, String, String) -> Unit
) {
    var sessionName by remember { mutableStateOf(sessionWithSets.session.sessionName) }
    var sessionNotes by remember { mutableStateOf(sessionWithSets.session.notes) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddExercisePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.FRENCH) }
    val formattedDate = remember(sessionWithSets.session.startTimeMillis) {
        dateFormat.format(Date(sessionWithSets.session.startTimeMillis))
    }

    // Editable sets state
    var editableSets by remember {
        mutableStateOf(sessionWithSets.setsWithDetails.map { it.set })
    }

    // Group editable sets by exercise
    val setsByExercise = remember(editableSets, allExercises) {
        editableSets.groupBy { set ->
            allExercises.find { it.id == set.exerciseId } ?: ExerciseEntity(
                id = set.exerciseId,
                name = "Exercice #${set.exerciseId}",
                category = "Musculation"
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Modifier la Séance",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name & Notes Fields
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Nom de la séance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sessionNotes,
                    onValueChange = { sessionNotes = it },
                    label = { Text("Notes & Ressentis (ex: RPE 8, bonne séance)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Exercises List Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exercices (${setsByExercise.keys.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = { showAddExercisePicker = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter Exercice")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Exercises and Sets List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    setsByExercise.forEach { (exercise, sets) ->
                        item(key = exercise.id) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
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
                                                // Remove all sets for this exercise
                                                editableSets = editableSets.filter { it.exerciseId != exercise.id }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Supprimer l'exercice",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Sets table header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Série", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Charge (kg)", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Reps", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Statut", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Sets rows
                                    sets.sortedBy { it.setNumber }.forEachIndexed { idx, set ->
                                        var weightInput by remember(set.weightKg) { mutableStateOf(set.weightKg.toString()) }
                                        var repsInput by remember(set.reps) { mutableStateOf(set.reps.toString()) }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Set number
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
                                                    fontSize = 12.sp,
                                                    color = if (set.completed) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            // Weight input
                                            OutlinedTextField(
                                                value = weightInput,
                                                onValueChange = { valStr ->
                                                    weightInput = valStr
                                                    val newW = valStr.toFloatOrNull() ?: set.weightKg
                                                    editableSets = editableSets.map {
                                                        if (it.id == set.id || (it.sessionId == set.sessionId && it.exerciseId == set.exerciseId && it.setNumber == set.setNumber)) {
                                                            it.copy(weightKg = newW)
                                                        } else it
                                                    }
                                                },
                                                modifier = Modifier.weight(1.5f),
                                                singleLine = true
                                            )

                                            // Reps input
                                            OutlinedTextField(
                                                value = repsInput,
                                                onValueChange = { valStr ->
                                                    repsInput = valStr
                                                    val newR = valStr.toIntOrNull() ?: set.reps
                                                    editableSets = editableSets.map {
                                                        if (it.id == set.id || (it.sessionId == set.sessionId && it.exerciseId == set.exerciseId && it.setNumber == set.setNumber)) {
                                                            it.copy(reps = newR)
                                                        } else it
                                                    }
                                                },
                                                modifier = Modifier.weight(1.5f),
                                                singleLine = true
                                            )

                                            // Toggle status button
                                            IconButton(
                                                onClick = {
                                                    editableSets = editableSets.map {
                                                        if (it.id == set.id || (it.sessionId == set.sessionId && it.exerciseId == set.exerciseId && it.setNumber == set.setNumber)) {
                                                            it.copy(completed = !it.completed)
                                                        } else it
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (set.completed) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                                                        RoundedCornerShape(10.dp)
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = if (set.completed) Icons.Default.Check else Icons.Default.Close,
                                                    contentDescription = "Complété",
                                                    tint = if (set.completed) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Add Set Button for this exercise
                                    TextButton(
                                        onClick = {
                                            val lastSet = sets.lastOrNull()
                                            val nextNum = (sets.maxOfOrNull { it.setNumber } ?: 0) + 1
                                            val newSet = ExerciseSetEntity(
                                                sessionId = sessionWithSets.session.id,
                                                exerciseId = exercise.id,
                                                setNumber = nextNum,
                                                reps = lastSet?.reps ?: 10,
                                                weightKg = lastSet?.weightKg ?: 20f,
                                                completed = true
                                            )
                                            editableSets = editableSets + newSet
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
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Actions: Save or Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Supprimer")
                    }

                    Button(
                        onClick = {
                            val updatedSession = sessionWithSets.session.copy(
                                sessionName = sessionName.ifBlank { "Séance d'Entraînement" },
                                notes = sessionNotes
                            )
                            onSave(updatedSession, editableSets)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enregistrer les Modifs", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Exercise Library Picker
    if (showAddExercisePicker) {
        com.example.ui.components.ExerciseLibraryDialog(
            allExercises = allExercises,
            onDismiss = { showAddExercisePicker = false },
            onCreateExercise = onCreateExercise,
            onSelectExercise = { ex ->
                val newSets = (1..3).map { num ->
                    ExerciseSetEntity(
                        sessionId = sessionWithSets.session.id,
                        exerciseId = ex.id,
                        setNumber = num,
                        reps = 10,
                        weightKg = 20f,
                        completed = true
                    )
                }
                editableSets = editableSets + newSets
            }
        )
    }

    // Delete Session Confirmation
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Supprimer cette séance ?") },
            text = { Text("Cette action est définitive et retirera cette séance de votre historique et de vos statistiques.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(sessionWithSets.session.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Oui, Supprimer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
