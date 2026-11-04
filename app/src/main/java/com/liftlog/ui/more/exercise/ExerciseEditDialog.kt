package com.liftlog.ui.more.exercise

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.liftlog.data.entity.Exercise
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.TextSecondary

@Composable
fun ExerciseEditDialog(
    exercise: Exercise?,
    onSave: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var equipment by remember { mutableStateOf(exercise?.equipment ?: "barbell") }
    var defaultWeightKgText by remember { mutableStateOf(if (exercise != null && exercise.defaultWeightKg > 0.0) exercise.defaultWeightKg.toString() else "") }
    var incrementKgText by remember { mutableStateOf(exercise?.incrementKg?.toString() ?: "2.5") }
    var repLoText by remember { mutableStateOf(exercise?.repLo?.toString() ?: "5") }
    var repHiText by remember { mutableStateOf(exercise?.repHi?.toString() ?: "8") }
    var defaultSetsText by remember { mutableStateOf(exercise?.defaultSets?.toString() ?: "3") }
    var restSecText by remember { mutableStateOf(exercise?.restSec?.toString() ?: "150") }
    var isCompound by remember { mutableStateOf(exercise?.isCompound ?: true) }
    var bwFactor by remember { mutableStateOf(exercise?.bwFactor ?: 0.0) }
    var primaryMuscles by remember { mutableStateOf(exercise?.primaryMuscles ?: "") }
    var setupNote by remember { mutableStateOf(exercise?.setupNote ?: "") }

    val equipments = listOf("barbell", "dumbbell", "cable", "machine", "bodyweight")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (exercise == null) "New Exercise" else "Edit Exercise",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Equipment", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    equipments.forEach { eq ->
                        val isSelected = equipment.equals(eq, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PrimaryBlue else DarkBorder)
                                .clickable {
                                    equipment = eq
                                    if (eq == "bodyweight") bwFactor = 1.0 else if (bwFactor == 1.0 && eq != "bodyweight") bwFactor = 0.0
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = eq.uppercase(),
                                color = if (isSelected) Color.Black else TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultWeightKgText,
                        onValueChange = { defaultWeightKgText = it },
                        label = { Text("Initial Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = incrementKgText,
                        onValueChange = { incrementKgText = it },
                        label = { Text("Increment (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = repLoText,
                        onValueChange = { repLoText = it },
                        label = { Text("Rep Low") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = repHiText,
                        onValueChange = { repHiText = it },
                        label = { Text("Rep High") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaultSetsText,
                        onValueChange = { defaultSetsText = it },
                        label = { Text("Sets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = restSecText,
                        onValueChange = { restSecText = it },
                        label = { Text("Rest (sec)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCompound, onCheckedChange = { isCompound = it })
                    Text("Compound lift (warm-up prefill & 150s rest)")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = bwFactor == 1.0, onCheckedChange = { bwFactor = if (it) 1.0 else 0.0 })
                    Text("Bodyweight lift (e.g. pullups/dips)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = primaryMuscles,
                    onValueChange = { primaryMuscles = it },
                    label = { Text("Primary Muscles (e.g. Chest, Quads)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = setupNote,
                    onValueChange = { setupNote = it },
                    label = { Text("Setup Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val initWeight = defaultWeightKgText.toDoubleOrNull() ?: 0.0
                        val inc = incrementKgText.toDoubleOrNull() ?: 2.5
                        val rLo = repLoText.toIntOrNull() ?: 5
                        val rHi = repHiText.toIntOrNull() ?: 8
                        val dSets = defaultSetsText.toIntOrNull() ?: 3
                        val rest = restSecText.toIntOrNull() ?: 150

                        val updated = (exercise ?: Exercise(name = name, equipment = equipment)).copy(
                            name = name.trim(),
                            equipment = equipment,
                            defaultWeightKg = initWeight,
                            incrementKg = inc,
                            repLo = rLo,
                            repHi = rHi,
                            defaultSets = dSets,
                            restSec = rest,
                            isCompound = isCompound,
                            bwFactor = bwFactor,
                            primaryMuscles = primaryMuscles.trim(),
                            setupNote = if (setupNote.isBlank()) null else setupNote.trim()
                        )
                        onSave(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
