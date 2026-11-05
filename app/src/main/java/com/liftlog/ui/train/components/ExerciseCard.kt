package com.liftlog.ui.train.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liftlog.data.entity.SetEntry
import com.liftlog.data.repository.ExerciseInWorkout
import com.liftlog.ui.theme.DangerRed
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurface
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.SecondaryTeal
import com.liftlog.ui.theme.SuccessGreen
import com.liftlog.ui.theme.TertiaryAmber
import com.liftlog.ui.theme.TextMuted
import com.liftlog.ui.theme.TextPrimary
import com.liftlog.ui.theme.TextSecondary
import com.liftlog.ui.theme.WarningOrange
import com.liftlog.ui.train.CurrentSetInput
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseCard(
    item: ExerciseInWorkout,
    currentInput: CurrentSetInput?,
    supersetLabel: String?, // e.g. "SS1 (A)" or "SS1 (B)"
    isResting: Boolean = false,
    restRemainingSec: Int = 0,
    onAdjustWeight: (Double) -> Unit,
    onAdjustReps: (Int) -> Unit,
    onDirectWeightChange: (Double) -> Unit,
    onDirectRepsChange: (Int) -> Unit,
    onSetType: (String) -> Unit,
    onSetRpe: (Double) -> Unit,
    onToggleSwung: () -> Unit,
    onTogglePain: () -> Unit,
    onLogSet: () -> Unit,
    onUpdatePastSet: (SetEntry) -> Unit,
    onDeletePastSet: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingSet by remember { mutableStateOf<SetEntry?>(null) }
    var showExtraSetInput by remember(item.loggedSets.size) { mutableStateOf(false) }

    val isBodyweight = item.exercise.bwFactor > 0.0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Exercise Header: Name + Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.exercise.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    val bwInfo = if (isBodyweight && item.bodyweightAvg > 0.0) {
                        " • BW: ${String.format(Locale.US, "%.1f", item.bodyweightAvg)}kg"
                    } else ""
                    Text(
                        text = "${item.exercise.equipment.uppercase()}$bwInfo • ${item.plannedSets} sets • ${item.exercise.repLo}–${item.exercise.repHi} reps",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (supersetLabel != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SecondaryTeal.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, SecondaryTeal),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = supersetLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = SecondaryTeal
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Setup note (if any)
            if (!item.exercise.setupNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.exercise.setupNote,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontStyle = FontStyle.Italic
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progression Summary Row: "Last: 65x7,7,6 (RPE9)" and real-time "Today: ..."
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.lastSessionSummary != null) {
                    Text(
                        text = item.lastSessionSummary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    )
                } else {
                    val startWeight = if (item.exercise.defaultWeightKg > 0.0) {
                        " (Starts @ ${if (item.exercise.defaultWeightKg % 1.0 == 0.0) item.exercise.defaultWeightKg.toInt() else item.exercise.defaultWeightKg}kg)"
                    } else ""
                    Text(
                        text = "First session$startWeight",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                // Real-time Today display: dynamically reflects logged sets as they are completed
                if (item.loggedSets.isNotEmpty()) {
                    val workingSets = item.loggedSets.filter { it.type == "working" }
                    val setsToShow = if (workingSets.isNotEmpty()) workingSets else item.loggedSets
                    val repsStr = setsToShow.joinToString(",") { "${it.reps}" }
                    val lastWeight = setsToShow.last().weightKg
                    val weightStr = if (isBodyweight) {
                        if (lastWeight > 0.0) {
                            "BW+${if (lastWeight % 1.0 == 0.0) lastWeight.toInt() else lastWeight}kg"
                        } else "BW"
                    } else {
                        if (lastWeight % 1.0 == 0.0) "${lastWeight.toInt()}kg" else "${lastWeight}kg"
                    }

                    val isComplete = item.loggedSets.size >= item.plannedSets
                    val progressLabel = if (isComplete) " (Done)" else " (${item.loggedSets.size}/${item.plannedSets})"
                    Text(
                        text = "Today: $weightStr × $repsStr$progressLabel",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isComplete) SuccessGreen else PrimaryBlue
                        )
                    )
                } else {
                    val targetWeightStr = if (item.target.targetWeightKg % 1.0 == 0.0) {
                        item.target.targetWeightKg.toInt().toString()
                    } else {
                        item.target.targetWeightKg.toString()
                    }
                    val targetWeightDisplay = if (isBodyweight) {
                        if (item.target.targetWeightKg > 0.0) "BW+${targetWeightStr}kg" else "BW"
                    } else {
                        "${targetWeightStr}kg"
                    }
                    Text(
                        text = "Today: $targetWeightDisplay × ${item.target.targetReps}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    )
                }
            }

            // Warning chips & notices
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                if (item.hasPainWarning) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DangerRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, DangerRed)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "2+ pain flags recently: consider lowering load or swapping",
                                style = MaterialTheme.typography.labelSmall.copy(color = DangerRed)
                            )
                        }
                    }
                }

                if (item.target.gapDays != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = WarningOrange.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, WarningOrange)
                    ) {
                        Text(
                            text = "Return after ${item.target.gapDays} days",
                            style = MaterialTheme.typography.labelSmall.copy(color = WarningOrange),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.target.isBackOff) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TertiaryAmber.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TertiaryAmber)
                    ) {
                        Text(
                            text = "Back off (-7.5%)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TertiaryAmber),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Logged sets list
            if (item.loggedSets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.loggedSets.forEachIndexed { idx, set ->
                        LoggedSetItem(
                            index = idx + 1,
                            set = set,
                            isBodyweight = isBodyweight,
                            onEditClick = { editingSet = set }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Prescribed sets completion guard & Extra Sets
            val isCompleted = item.loggedSets.size >= item.plannedSets
            if (isCompleted && !showExtraSetInput) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SuccessGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Completed (${item.loggedSets.size}/${item.plannedSets} Sets)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = SuccessGreen
                                )
                            )
                        }
                        OutlinedButton(
                            onClick = { showExtraSetInput = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, DarkBorder),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "+ Extra Set",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            } else if (currentInput != null) {
                val nextSetIndex = item.loggedSets.size + 1
                val isLastPlanned = nextSetIndex >= item.plannedSets

                CurrentSetInputRow(
                    setIndex = nextSetIndex,
                    input = currentInput,
                    isLastPlannedSet = isLastPlanned,
                    incrementKg = item.exercise.incrementKg,
                    isBodyweight = isBodyweight,
                    isResting = isResting,
                    restRemainingSec = restRemainingSec,
                    onAdjustWeight = onAdjustWeight,
                    onAdjustReps = onAdjustReps,
                    onDirectWeightChange = onDirectWeightChange,
                    onDirectRepsChange = onDirectRepsChange,
                    onSetType = onSetType,
                    onSetRpe = onSetRpe,
                    onToggleSwung = onToggleSwung,
                    onTogglePain = onTogglePain,
                    onLogSet = onLogSet
                )

                if (isCompleted && showExtraSetInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showExtraSetInput = false }) {
                            Text("Cancel Extra Set", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    if (editingSet != null) {
        EditPastSetDialog(
            set = editingSet!!,
            onSave = { updated ->
                onUpdatePastSet(updated)
                editingSet = null
            },
            onDelete = { setId ->
                onDeletePastSet(setId)
                editingSet = null
            },
            onDismiss = { editingSet = null }
        )
    }
}
