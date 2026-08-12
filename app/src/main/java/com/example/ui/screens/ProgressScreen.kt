package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExerciseEntity
import com.example.data.ExerciseProgressPoint
import com.example.data.SessionWithSets
import com.example.ui.components.MuscleGroupRadarChart
import com.example.ui.components.ProgressChart
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    allExercises: List<ExerciseEntity>,
    completedSessions: List<SessionWithSets>,
    onLoadExerciseProgress: (Long) -> StateFlow<List<ExerciseProgressPoint>>
) {
    var selectedExercise by remember(allExercises) { mutableStateOf(allExercises.firstOrNull()) }
    var selectedMetric by remember { mutableStateOf("WEIGHT") } // "WEIGHT", "VOLUME", "1RM"
    var showExerciseMenu by remember { mutableStateOf(false) }

    val progressPointsFlow = remember(selectedExercise) {
        selectedExercise?.let { onLoadExerciseProgress(it.id) }
    }
    val progressPointsState = progressPointsFlow?.collectAsState(initial = emptyList())
    val progressPoints = progressPointsState?.value ?: emptyList()

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Évolution & Graphiques",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Radar Chart for Muscle Volume Distribution
            item {
                MuscleGroupRadarChart(completedSessions = completedSessions)
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            item {
                Text(
                    text = "Évolution de la Charge par Exercice",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Exercise Selector Dropdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showExerciseMenu = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Exercice Sélectionné",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedExercise?.name ?: "Sélectionner un exercice",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = showExerciseMenu,
                            onDismissRequest = { showExerciseMenu = false }
                        ) {
                            allExercises.forEach { ex ->
                                DropdownMenuItem(
                                    text = { Text("${ex.name} (${ex.category})") },
                                    onClick = {
                                        selectedExercise = ex
                                        showExerciseMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Metric Tabs Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val metrics = listOf(
                        "WEIGHT" to "Charge Max",
                        "VOLUME" to "Volume Total",
                        "1RM" to "1RM Estimé"
                    )

                    metrics.forEach { (key, label) ->
                        val isSelected = selectedMetric == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMetric = key },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Main Canvas Chart
            item {
                ProgressChart(
                    points = progressPoints,
                    metricType = selectedMetric
                )
            }

            // Quick Stats Row for Selected Exercise
            item {
                Text(
                    text = "Records & Statistiques Clefs",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val maxRecordWeight = progressPoints.maxOfOrNull { it.maxWeightKg } ?: 0f
                val maxRecordVolume = progressPoints.maxOfOrNull { it.totalVolumeKg } ?: 0f
                val best1RM = progressPoints.maxOfOrNull { it.estimated1RM } ?: 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Record Charge", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${maxRecordWeight} kg", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Peak Volume", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${maxRecordVolume.toInt()} kg", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("1RM Estimé", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${best1RM} kg", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            // Historical Log Items
            item {
                Text(
                    text = "Historique des Séances",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (progressPoints.isEmpty()) {
                item {
                    Text(
                        text = "Aucune séance enregistrée pour cet exercice pour le moment.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(progressPoints.reversed()) { pt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(pt.sessionName, fontWeight = FontWeight.Bold)
                                Text(dateFormat.format(Date(pt.timestampMillis)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Max: ${pt.maxWeightKg}kg", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Volume: ${pt.totalVolumeKg.toInt()}kg • 1RM: ${pt.estimated1RM}kg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
