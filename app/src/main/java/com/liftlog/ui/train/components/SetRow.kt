package com.liftlog.ui.train.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liftlog.data.entity.SetEntry
import com.liftlog.ui.theme.DangerRed
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurfaceVariant
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.SuccessGreen
import com.liftlog.ui.theme.TertiaryAmber
import com.liftlog.ui.theme.TextMuted
import com.liftlog.ui.theme.TextPrimary
import com.liftlog.ui.theme.TextSecondary
import com.liftlog.ui.train.CurrentSetInput

@Composable
fun LoggedSetItem(
    index: Int,
    set: SetEntry,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .clickable { onEditClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = SuccessGreen)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            val weightStr = if (set.weightKg % 1.0 == 0.0) set.weightKg.toInt().toString() else set.weightKg.toString()
            Text(
                text = "${weightStr}kg × ${set.reps}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
            )

            if (set.type != "working") {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = set.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            if (set.rpe != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "@${set.rpe}",
                    style = MaterialTheme.typography.bodySmall.copy(color = PrimaryBlue, fontWeight = FontWeight.Medium)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (set.flagSwung) {
                Text(
                    text = "Swung",
                    style = MaterialTheme.typography.labelSmall.copy(color = TertiaryAmber),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            if (set.flagPain) {
                Text(
                    text = "Pain",
                    style = MaterialTheme.typography.labelSmall.copy(color = DangerRed),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Text(
                text = "Edit",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )
        }
    }
}

@Composable
fun CurrentSetInputRow(
    setIndex: Int,
    input: CurrentSetInput,
    isLastPlannedSet: Boolean,
    incrementKg: Double,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
    onSetType: (String) -> Unit,
    onSetRpe: (Double) -> Unit,
    onToggleSwung: () -> Unit,
    onTogglePain: () -> Unit,
    onLogSet: () -> Unit,
    onOpenPlateCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(12.dp)
    ) {
        // Top: Steppers for Weight and Reps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weight Stepper
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("WEIGHT (KG)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdjustWeight(-incrementKg) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease weight")
                    }

                    val weightText = if (input.weightKg % 1.0 == 0.0) input.weightKg.toInt().toString() else input.weightKg.toString()
                    Text(
                        text = "${weightText}kg",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable { onOpenPlateCalculator() }
                    )

                    IconButton(
                        onClick = { onAdjustWeight(incrementKg) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase weight")
                    }
                }
            }

            // Reps Stepper
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REPS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdjustReps(-1) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease reps")
                    }

                    Text(
                        text = "${input.reps}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    IconButton(
                        onClick = { onAdjustReps(1) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase reps")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Middle Row: Type Toggle, RPE Chip (if last set), Swung, Pain
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type toggle (Warmup / Working / Drop)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("working", "warmup", "drop").forEach { typeName ->
                    val isSelected = input.type.equals(typeName, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) PrimaryBlue else DarkBorder)
                            .clickable { onSetType(typeName) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = typeName.capitalize(java.util.Locale.ROOT),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        )
                    }
                }
            }

            // Flag Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // RPE Chip (appears only on the last planned set, default 9, one tap to change)
                if (isLastPlannedSet) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, PrimaryBlue, RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .clickable {
                                // Cycle: 7.0 -> 7.5 -> 8.0 -> 8.5 -> 9.0 -> 9.5 -> 10.0 -> 7.0
                                val nextRpe = if (input.rpe >= 10.0) 7.0 else input.rpe + 0.5
                                onSetRpe(nextRpe)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "@${input.rpe}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        )
                    }
                }

                // Swung Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (input.flagSwung) TertiaryAmber else DarkBorder)
                        .clickable { onToggleSwung() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Swung",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (input.flagSwung) FontWeight.Bold else FontWeight.Normal,
                            color = if (input.flagSwung) Color.Black else TextSecondary
                        )
                    )
                }

                // Pain Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (input.flagPain) DangerRed else DarkBorder)
                        .clickable { onTogglePain() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Pain",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (input.flagPain) FontWeight.Bold else FontWeight.Normal,
                            color = if (input.flagPain) Color.White else TextSecondary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large LOG SET Button (Speed: max 2 taps)
        Button(
            onClick = onLogSet,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (input.type == "warmup") TertiaryAmber else SuccessGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LOG SET $setIndex (${input.weightKg}kg × ${input.reps})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
