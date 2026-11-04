package com.liftlog.ui.train.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.liftlog.data.entity.SetEntry
import com.liftlog.ui.theme.DangerRed
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.TertiaryAmber
import com.liftlog.ui.theme.TextSecondary

@Composable
fun EditPastSetDialog(
    set: SetEntry,
    onSave: (SetEntry) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf(set.weightKg.toString()) }
    var repsText by remember { mutableStateOf(set.reps.toString()) }
    var rpeText by remember { mutableStateOf(set.rpe?.toString() ?: "") }
    var type by remember { mutableStateOf(set.type) }
    var flagSwung by remember { mutableStateOf(set.flagSwung) }
    var flagPain by remember { mutableStateOf(set.flagPain) }
    var note by remember { mutableStateOf(set.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Past Set") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rpeText,
                    onValueChange = { rpeText = it },
                    label = { Text("RPE (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Type selector
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("working", "warmup", "drop").forEach { t ->
                        val isSelected = type.equals(t, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PrimaryBlue else DarkBorder)
                                .clickable { type = t }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t.capitalize(java.util.Locale.ROOT),
                                color = if (isSelected) Color.Black else TextSecondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Flags
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (flagSwung) TertiaryAmber else DarkBorder)
                            .clickable { flagSwung = !flagSwung }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Swung",
                            color = if (flagSwung) Color.Black else TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (flagPain) DangerRed else DarkBorder)
                            .clickable { flagPain = !flagPain }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Pain",
                            color = if (flagPain) Color.White else TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Set note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightText.toDoubleOrNull() ?: set.weightKg
                    val reps = repsText.toIntOrNull() ?: set.reps
                    val rpe = rpeText.toDoubleOrNull()
                    onSave(
                        set.copy(
                            weightKg = weight,
                            reps = reps,
                            rpe = rpe,
                            type = type,
                            flagSwung = flagSwung,
                            flagPain = flagPain,
                            note = if (note.isBlank()) null else note
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { onDelete(set.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
