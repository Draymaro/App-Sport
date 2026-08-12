package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SessionWithSets
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class MuscleGroupStat(
    val category: String,
    val totalVolumeKg: Float,
    val totalSets: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun MuscleGroupRadarChart(
    completedSessions: List<SessionWithSets>,
    modifier: Modifier = Modifier
) {
    // Standard default categories + any custom ones found
    val defaultCategories = listOf("Pectoraux", "Dos", "Jambes", "Épaules", "Bras", "Abdos")

    // Filter time range state (e.g. "Toutes les séances", "30 derniers jours")
    var timeFilterDays by remember { mutableStateOf(30) } // 30 days or 0 for all time

    val filteredSessions = remember(completedSessions, timeFilterDays) {
        if (timeFilterDays <= 0) {
            completedSessions.filter { it.session.isCompleted }
        } else {
            val cutoff = System.currentTimeMillis() - (timeFilterDays * 24L * 3600L * 1000L)
            completedSessions.filter { it.session.isCompleted && it.session.startTimeMillis >= cutoff }
        }
    }

    // Compute volume & sets per category
    val statsList = remember(filteredSessions) {
        val categoryVolumeMap = mutableMapOf<String, Float>()
        val categorySetsMap = mutableMapOf<String, Int>()

        // Initialize defaults
        defaultCategories.forEach {
            categoryVolumeMap[it] = 0f
            categorySetsMap[it] = 0
        }

        filteredSessions.forEach { session ->
            session.setsWithDetails.forEach { setWithEx ->
                if (setWithEx.set.completed) {
                    val cat = setWithEx.exercise.category.ifBlank { "Autre" }
                    val vol = if (setWithEx.set.weightKg > 0f) {
                        setWithEx.set.weightKg * setWithEx.set.reps
                    } else {
                        setWithEx.set.reps * 1f // bodyweight
                    }
                    categoryVolumeMap[cat] = (categoryVolumeMap[cat] ?: 0f) + vol
                    categorySetsMap[cat] = (categorySetsMap[cat] ?: 0) + 1
                }
            }
        }

        // Gather all categories that have data or are in default
        val allCats = (defaultCategories + categoryVolumeMap.keys).distinct()

        allCats.map { cat ->
            MuscleGroupStat(
                category = cat,
                totalVolumeKg = categoryVolumeMap[cat] ?: 0f,
                totalSets = categorySetsMap[cat] ?: 0
            )
        }
    }

    val maxVolume = remember(statsList) {
        (statsList.maxOfOrNull { it.totalVolumeKg } ?: 100f).coerceAtLeast(100f)
    }

    val underworkedZones = remember(statsList, maxVolume) {
        statsList.filter { stat ->
            stat.totalSets == 0 || (stat.totalVolumeKg < maxVolume * 0.25f)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Time Filter Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Répartition par Groupe Musculaire",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Identification des zones sous-travaillées",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Time filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = timeFilterDays == 30,
                    onClick = { timeFilterDays = 30 },
                    label = { Text("30 jours", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = timeFilterDays == 0,
                    onClick = { timeFilterDays = 0 },
                    label = { Text("Tout", fontSize = 11.sp) }
                )
            }
        }

        // Canvas Radar Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            val numSides = statsList.size
            if (numSides >= 3) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val maxRadius = minOf(centerX, centerY) - 50.dp.toPx()

                    val angleStep = (2 * PI / numSides).toFloat()
                    val startAngle = (-PI / 2).toFloat() // Top

                    // 1. Draw web grid levels (20%, 40%, 60%, 80%, 100%)
                    val levels = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
                    levels.forEach { level ->
                        val levelRadius = maxRadius * level
                        val gridPath = Path()

                        for (i in 0 until numSides) {
                            val angle = startAngle + i * angleStep
                            val x = centerX + levelRadius * cos(angle)
                            val y = centerY + levelRadius * sin(angle)

                            if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                        }
                        gridPath.close()

                        drawPath(
                            path = gridPath,
                            color = surfaceVariant.copy(alpha = 0.6f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // 2. Draw radial axis lines from center
                    for (i in 0 until numSides) {
                        val angle = startAngle + i * angleStep
                        val endX = centerX + maxRadius * cos(angle)
                        val endY = centerY + maxRadius * sin(angle)

                        drawLine(
                            color = surfaceVariant.copy(alpha = 0.6f),
                            start = Offset(centerX, centerY),
                            end = Offset(endX, endY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 3. Construct user volume polygon
                    val userPath = Path()
                    val pointOffsets = mutableListOf<Offset>()

                    statsList.forEachIndexed { index, stat ->
                        val angle = startAngle + index * angleStep
                        val norm = if (maxVolume > 0) (stat.totalVolumeKg / maxVolume).coerceIn(0.04f, 1.0f) else 0.04f
                        val r = maxRadius * norm
                        val x = centerX + r * cos(angle)
                        val y = centerY + r * sin(angle)

                        val offset = Offset(x, y)
                        pointOffsets.add(offset)

                        if (index == 0) userPath.moveTo(x, y) else userPath.lineTo(x, y)
                    }
                    userPath.close()

                    // Draw filled semi-transparent polygon
                    drawPath(
                        path = userPath,
                        color = primaryColor.copy(alpha = 0.30f)
                    )

                    // Draw polygon boundary stroke
                    drawPath(
                        path = userPath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // 4. Draw vertex points & category labels
                    statsList.forEachIndexed { index, stat ->
                        val angle = startAngle + index * angleStep
                        val isUnderworked = stat.totalSets == 0 || stat.totalVolumeKg < maxVolume * 0.25f

                        // Point on path
                        val pt = pointOffsets[index]
                        drawCircle(
                            color = if (isUnderworked) Color(0xFFE53935) else primaryColor,
                            radius = 4.dp.toPx(),
                            center = pt
                        )

                        // Label offset outside the max circle
                        val labelR = maxRadius + 28.dp.toPx()
                        val labelX = centerX + labelR * cos(angle)
                        val labelY = centerY + labelR * sin(angle)

                        val labelText = "${stat.category}\n${stat.totalVolumeKg.toInt()} kg (${stat.totalSets} s)"

                        val textResult = textMeasurer.measure(
                            text = labelText,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = if (isUnderworked) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isUnderworked) Color(0xFFE53935) else Color.Unspecified
                            )
                        )

                        // Center the text bounding box on label position
                        val drawX = labelX - (textResult.size.width / 2f)
                        val drawY = labelY - (textResult.size.height / 2f)

                        drawText(
                            textLayoutResult = textResult,
                            topLeft = Offset(drawX, drawY)
                        )
                    }
                }
            }
        }

        // Underworked zones alert card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (underworkedZones.isNotEmpty()) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (underworkedZones.isNotEmpty()) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (underworkedZones.isNotEmpty()) Color(0xFFE65100) else Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (underworkedZones.isNotEmpty()) "Diagnostic Zones Sous-travaillées" else "Équilibre Musculaire Optimal !",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (underworkedZones.isNotEmpty()) Color(0xFFE65100) else Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (underworkedZones.isNotEmpty()) {
                    Text(
                        text = "Les groupes musculaires suivants présentent un volume d'entraînement faible ou nul sur la période :",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF424242)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    underworkedZones.forEach { stat ->
                        val msg = if (stat.totalSets == 0) {
                            "• ${stat.category} : 0 série enregistrée"
                        } else {
                            "• ${stat.category} : seulement ${stat.totalVolumeKg.toInt()} kg (${stat.totalSets} séries)"
                        }
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFBF360C)
                        )
                    }
                } else {
                    Text(
                        text = "Excellente répartition ! Tous vos groupes musculaires majeurs reçoivent une stimulation suffisante.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }
    }
}
