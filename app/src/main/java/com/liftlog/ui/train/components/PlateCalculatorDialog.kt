package com.liftlog.ui.train.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liftlog.engine.PlateCalculator
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurface
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.TertiaryAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateCalculatorDialog(
    exerciseName: String,
    weightKg: Double,
    plateKg: Double?,
    barWeightKg: Double = 20.0,
    availablePlates: List<Double> = listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25),
    onDismiss: () -> Unit
) {
    val isStack = plateKg != null && plateKg > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Plate Calculator: $exerciseName",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isStack) {
                    val stackDisplay = PlateCalculator.formatStackDisplay(weightKg, plateKg)
                    Text(
                        text = "Stack Weight: $stackDisplay",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stack increment: ${plateKg}kg per plate pin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val result = PlateCalculator.calculateBarbellLoad(weightKg, barWeightKg, availablePlates)

                    Text(
                        text = "Target: ${weightKg}kg (Bar: ${barWeightKg}kg)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Per Side:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (result.platesPerSide.isEmpty()) {
                        Text(
                            text = "Empty Bar (no plates needed)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TertiaryAmber
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (plate in result.platesPerSide) {
                                PlateBadge(plateWeight = plate.plateWeightKg, count = plate.countPerSide)
                            }
                        }
                    }

                    if (result.remainderKg > 0.0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Remaining unmatchable load: ${result.remainderKg}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun PlateBadge(plateWeight: Double, count: Int) {
    val plateColor = when (plateWeight) {
        25.0 -> Color(0xFFD32F2F) // Red
        20.0 -> Color(0xFF1976D2) // Blue
        15.0 -> Color(0xFFFBC02D) // Yellow
        10.0 -> Color(0xFF388E3C) // Green
        5.0 -> Color(0xFFE0E0E0)  // White
        2.5 -> Color(0xFF424242)  // Black
        else -> Color(0xFF7B1FA2)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, plateColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(plateColor)
        )
        Text(
            text = " ${count}x ${plateWeight}kg",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
