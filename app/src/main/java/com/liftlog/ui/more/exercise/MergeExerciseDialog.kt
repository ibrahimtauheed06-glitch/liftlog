package com.liftlog.ui.more.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liftlog.data.entity.Exercise
import com.liftlog.ui.theme.TertiaryAmber
import com.liftlog.ui.theme.TextMuted
import com.liftlog.ui.theme.TextPrimary

@Composable
fun MergeExerciseDialog(
    sourceExercise: Exercise,
    allExercises: List<Exercise>,
    onMerge: (targetExerciseId: Long, reassignPastSets: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTargetId by remember { mutableLongStateOf(allExercises.firstOrNull()?.id ?: 0L) }
    var reassignPastSets by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Merge '${sourceExercise.name}'", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Merge or substitute this exercise into a parent exercise. History is never lost: '${sourceExercise.name}' is archived and set as a variantOf the chosen target.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Select target exercise:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(allExercises) { ex ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTargetId = ex.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedTargetId == ex.id,
                                onClick = { selectedTargetId = ex.id }
                            )
                            Text(
                                text = "${ex.name} (${ex.equipment})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = reassignPastSets,
                        onCheckedChange = { reassignPastSets = it }
                    )
                    Text(
                        text = "Reassign all past logged sets directly to target exercise",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTargetId > 0) {
                        onMerge(selectedTargetId, reassignPastSets)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TertiaryAmber, contentColor = Color.Black)
            ) {
                Text("Confirm Merge", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
