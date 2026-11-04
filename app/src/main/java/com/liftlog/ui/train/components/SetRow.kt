package com.liftlog.ui.train.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liftlog.data.entity.SetEntry
import com.liftlog.ui.theme.DangerRed
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurface
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "@${set.rpe}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TertiaryAmber, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CurrentSetInputRow(
    setIndex: Int,
    input: CurrentSetInput,
    isLastPlannedSet: Boolean,
    incrementKg: Double,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
    onDirectWeightChange: (Double) -> Unit,
    onDirectRepsChange: (Int) -> Unit,
    onSetType: (String) -> Unit,
    onSetRpe: (Double) -> Unit,
    onToggleSwung: () -> Unit,
    onTogglePain: () -> Unit,
    onLogSet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(12.dp)
    ) {
        // Top: Direct Keyboard + Steppers for Weight and Reps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weight Column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("WEIGHT (KG)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdjustWeight(-incrementKg) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease weight")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    val weightText = if (input.weightKg % 1.0 == 0.0) input.weightKg.toInt().toString() else input.weightKg.toString()
                    var editWeightStr by remember(input.weightKg) { mutableStateOf(weightText) }

                    OutlinedTextField(
                        value = editWeightStr,
                        onValueChange = { newVal ->
                            editWeightStr = newVal
                            newVal.toDoubleOrNull()?.let { onDirectWeightChange(it) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier
                            .width(76.dp)
                            .height(50.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { onAdjustWeight(incrementKg) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase weight")
                    }
                }
            }

            // Reps Column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REPS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdjustReps(-1) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease reps")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    var editRepsStr by remember(input.reps) { mutableStateOf(input.reps.toString()) }

                    OutlinedTextField(
                        value = editRepsStr,
                        onValueChange = { newVal ->
                            editRepsStr = newVal
                            newVal.toIntOrNull()?.let { onDirectRepsChange(it) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        modifier = Modifier
                            .width(64.dp)
                            .height(50.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { onAdjustReps(1) },
                        modifier = Modifier
                            .size(42.dp)
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

        // Set Type Selector (Working / Warmup / Drop)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("working", "warmup", "drop").forEach { type ->
                val selected = input.type == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) PrimaryBlue else DarkBorder)
                        .clickable { onSetType(type) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.Black else TextSecondary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Flags & RPE Row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // RPE chip (appears on the last planned set)
            if (isLastPlannedSet && input.type == "working") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RPE: ", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    listOf(7.0, 8.0, 9.0, 10.0).forEach { rpeVal ->
                        val isSelected = input.rpe == rpeVal
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) TertiaryAmber else DarkBorder,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clickable { onSetRpe(rpeVal) }
                        ) {
                            Text(
                                text = if (rpeVal % 1.0 == 0.0) rpeVal.toInt().toString() else rpeVal.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Flag Swung
            FilterChip(
                selected = input.flagSwung,
                onClick = onToggleSwung,
                label = { Text("Swung") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TertiaryAmber.copy(alpha = 0.3f),
                    selectedLabelColor = TertiaryAmber
                )
            )

            // Flag Pain
            FilterChip(
                selected = input.flagPain,
                onClick = onTogglePain,
                label = { Text("Pain") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DangerRed.copy(alpha = 0.3f),
                    selectedLabelColor = DangerRed
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Log Set Button (Big, prominent, 1-tap logging)
        Button(
            onClick = onLogSet,
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LOG SET $setIndex",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
        }
    }
}
