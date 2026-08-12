package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExerciseProgressPoint
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProgressChart(
    points: List<ExerciseProgressPoint>,
    metricType: String, // "WEIGHT", "VOLUME", "1RM"
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gradientColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aucune donnée enregistrée pour cet exercice.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    val values = points.map {
        when (metricType) {
            "VOLUME" -> it.totalVolumeKg
            "1RM" -> it.estimated1RM
            else -> it.maxWeightKg
        }
    }

    val minY = (values.minOrNull() ?: 0f) * 0.9f
    val maxY = (values.maxOrNull() ?: 100f) * 1.1f
    val rangeY = if (maxY - minY == 0f) 1f else maxY - minY

    val dateFormat = SimpleDateFormat("dd/MM", Locale.FRENCH)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        // Summary Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (metricType) {
                        "VOLUME" -> "Volume Total Soulevé"
                        "1RM" -> "1RM Estimé (Max théorique)"
                        else -> "Charge Maximale (kg)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${values.lastOrNull() ?: 0f} kg",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val diff = if (values.size >= 2) values.last() - values.first() else 0f
            val percent = if (values.first() > 0) (diff / values.first()) * 100f else 0f
            if (values.size >= 2) {
                Box(
                    modifier = Modifier
                        .background(
                            if (diff >= 0) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (diff >= 0) "+${percent.toInt()}%" else "${percent.toInt()}%",
                        color = if (diff >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Line Chart Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val width = size.width
            val height = size.height - 30.dp.toPx()

            val stepX = if (points.size > 1) width / (points.size - 1) else width

            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, point ->
                val currentValue = when (metricType) {
                    "VOLUME" -> point.totalVolumeKg
                    "1RM" -> point.estimated1RM
                    else -> point.maxWeightKg
                }

                val x = index * stepX
                val y = height - ((currentValue - minY) / rangeY) * height

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (index - 1) * stepX
                    val prevVal = when (metricType) {
                        "VOLUME" -> points[index - 1].totalVolumeKg
                        "1RM" -> points[index - 1].estimated1RM
                        else -> points[index - 1].maxWeightKg
                    }
                    val prevY = height - ((prevVal - minY) / rangeY) * height

                    val controlX1 = prevX + (x - prevX) / 2f
                    val controlY1 = prevY
                    val controlX2 = prevX + (x - prevX) / 2f
                    val controlY2 = y

                    path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                }

                if (index == points.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            // Draw Area Gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(gradientColor, Color.Transparent)
                )
            )

            // Draw Stroke Line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Points
            points.forEachIndexed { index, point ->
                val currentValue = when (metricType) {
                    "VOLUME" -> point.totalVolumeKg
                    "1RM" -> point.estimated1RM
                    else -> point.maxWeightKg
                }
                val x = index * stepX
                val y = height - ((currentValue - minY) / rangeY) * height

                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val firstDate = dateFormat.format(Date(points.first().timestampMillis))
            val lastDate = dateFormat.format(Date(points.last().timestampMillis))
            Text(text = firstDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (points.size > 2) {
                val midDate = dateFormat.format(Date(points[points.size / 2].timestampMillis))
                Text(text = midDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = lastDate, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
