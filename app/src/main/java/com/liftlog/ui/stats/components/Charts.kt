package com.liftlog.ui.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liftlog.ui.stats.DailyProteinBar
import com.liftlog.ui.stats.MuscleSetVolume
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurfaceVariant
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.SuccessGreen
import com.liftlog.ui.theme.TertiaryAmber
import com.liftlog.ui.theme.TextMuted
import com.liftlog.ui.theme.TextPrimary
import com.liftlog.ui.theme.TextSecondary

@Composable
fun ProteinBarChart(
    bars: List<DailyProteinBar>,
    targetGrams: Double,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) {
        Text("No protein data for this period", color = TextMuted)
        return
    }

    val maxGrams = maxOf(targetGrams * 1.25, (bars.maxOfOrNull { it.grams } ?: 0.0) * 1.1, 10.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Target line
                val targetY = canvasHeight - ((targetGrams / maxGrams).toFloat() * canvasHeight)
                drawLine(
                    color = TertiaryAmber,
                    start = Offset(0f, targetY),
                    end = Offset(canvasWidth, targetY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Bars
                val barCount = bars.size
                val barSpacing = canvasWidth / barCount
                val barWidth = (barSpacing * 0.6f).coerceAtMost(28.dp.toPx())

                bars.forEachIndexed { index, bar ->
                    val barHeight = ((bar.grams / maxGrams).toFloat() * canvasHeight).coerceAtLeast(4f)
                    val left = (index * barSpacing) + (barSpacing - barWidth) / 2f
                    val top = canvasHeight - barHeight

                    val isTargetHit = bar.grams >= targetGrams
                    val barColor = if (isTargetHit) SuccessGreen else PrimaryBlue

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }
        }

        // Labels below bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bars.forEach { bar ->
                Text(
                    text = bar.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.width(28.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TertiaryAmber)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Target (${targetGrams.toInt()}g)",
                style = MaterialTheme.typography.labelSmall,
                color = TertiaryAmber
            )
        }
    }
}

@Composable
fun MuscleVolumeBarChart(
    muscleVolumes: List<MuscleSetVolume>,
    modifier: Modifier = Modifier
) {
    if (muscleVolumes.isEmpty()) {
        Text("No workout sets logged in this period", color = TextMuted)
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        muscleVolumes.forEach { item ->
            val isTargetHit = item.setsCount >= item.targetSets
            val fraction = (item.setsCount / item.targetSets.toDouble()).coerceIn(0.0, 1.5).toFloat()
            val cleanSets = if (item.setsCount % 1.0 == 0.0) item.setsCount.toInt().toString() else item.setsCount.toString()

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.muscleName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary
                    )
                    Text(
                        text = "$cleanSets / ${item.targetSets} sets",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isTargetHit) SuccessGreen else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtMost(1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isTargetHit) SuccessGreen else PrimaryBlue)
                    )
                }
            }
        }
    }
}

@Composable
fun LineTrendChart(
    points: List<Pair<String, Double>>, // label to value
    unit: String = "",
    lineColor: Color = PrimaryBlue,
    smoothedValues: List<Double?> = emptyList(),
    smoothedColor: Color = TertiaryAmber,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Text("No data points available", color = TextMuted)
        return
    }

    if (points.size == 1) {
        val (label, value) = points[0]
        Text("Single entry: $value$unit ($label)", color = TextPrimary)
        return
    }

    val allValues = points.map { it.second } + smoothedValues.filterNotNull()
    val minVal = (allValues.minOrNull() ?: 0.0) * 0.95
    val maxVal = ((allValues.maxOrNull() ?: 100.0) * 1.05).coerceAtLeast(minVal + 1.0)
    val range = maxVal - minVal

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val pointCount = points.size
                val stepX = canvasWidth / (pointCount - 1)

                val path = Path()
                val offsets = points.mapIndexed { idx, item ->
                    val x = idx * stepX
                    val y = canvasHeight - (((item.second - minVal) / range).toFloat() * canvasHeight)
                    Offset(x, y)
                }

                offsets.forEachIndexed { i, offset ->
                    if (i == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
                }

                // If smoothed values are provided, draw smoothed line
                if (smoothedValues.size == pointCount) {
                    val smoothPath = Path()
                    var hasStarted = false
                    smoothedValues.forEachIndexed { idx, sVal ->
                        if (sVal != null) {
                            val x = idx * stepX
                            val y = canvasHeight - (((sVal - minVal) / range).toFloat() * canvasHeight)
                            if (!hasStarted) {
                                smoothPath.moveTo(x, y)
                                hasStarted = true
                            } else {
                                smoothPath.lineTo(x, y)
                            }
                        }
                    }
                    if (hasStarted) {
                        drawPath(
                            path = smoothPath,
                            color = smoothedColor,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Draw main line
                drawPath(
                    path = path,
                    color = lineColor.copy(alpha = if (smoothedValues.isNotEmpty()) 0.4f else 1.0f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw dots
                offsets.forEach { offset ->
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = offset
                    )
                }
            }
        }

        if (smoothedValues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(lineColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Daily", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(3.dp)
                        .background(smoothedColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("7-Day Avg", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(points.first().first, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(points.last().first, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
