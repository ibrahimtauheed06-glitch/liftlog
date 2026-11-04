package com.liftlog.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liftlog.data.entity.Template
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurface
import com.liftlog.ui.theme.DarkSurfaceVariant
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.SuccessGreen
import com.liftlog.ui.theme.TertiaryAmber
import com.liftlog.ui.theme.TextMuted
import com.liftlog.ui.theme.TextPrimary
import com.liftlog.ui.theme.TextSecondary
import com.liftlog.ui.theme.WarningOrange

@Composable
fun TrainScreen(
    viewModel: TrainViewModel,
    onNavigateToActiveWorkout: (Long) -> Unit,
    onNavigateToBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Deload Banner (Rule 5: >=3 exercises stalled in 14 days)
                if (state.showDeloadBanner) {
                    item {
                        DeloadBanner(
                            onAccept = { viewModel.acceptDeload() },
                            onDismiss = { viewModel.dismissDeload() }
                        )
                    }
                }

                // Backup Reminder (No backup in 14 days)
                if (state.showBackupReminder) {
                    item {
                        BackupReminderBanner(onNavigateToBackup = onNavigateToBackup)
                    }
                }

                // Active Workout Resume Banner (if an active workout exists)
                if (state.activeWorkout != null) {
                    item {
                        ActiveWorkoutResumeCard(
                            workoutId = state.activeWorkout!!.id,
                            onResume = { onNavigateToActiveWorkout(state.activeWorkout!!.id) }
                        )
                    }
                }

                // Header
                item {
                    Column {
                        Text(
                            text = "Train",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Select today's template or start an empty workout",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Templates List
                items(state.templates) { template ->
                    val isSuggested = template.id == state.suggestedTemplateId
                    TemplateCard(
                        template = template,
                        isSuggested = isSuggested,
                        onStart = {
                            viewModel.startWorkout(template.id) { id ->
                                onNavigateToActiveWorkout(id)
                            }
                        }
                    )
                }

                // Quick Free Workout Button
                item {
                    OutlinedButton(
                        onClick = {
                            viewModel.startWorkout(null) { id ->
                                onNavigateToActiveWorkout(id)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Empty Workout", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveWorkoutResumeCard(
    workoutId: Long,
    onResume: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Workout in Progress",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = SuccessGreen)
                )
                Text(
                    text = "Resuming where you left off",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RESUME", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TemplateCard(
    template: Template,
    isSuggested: Boolean,
    onStart: () -> Unit
) {
    val borderColor = if (isSuggested) PrimaryBlue else DarkBorder
    val containerColor = if (isSuggested) DarkSurfaceVariant else DarkSurface

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(if (isSuggested) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStart() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSuggested) PrimaryBlue.copy(alpha = 0.2f) else DarkBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = if (isSuggested) PrimaryBlue else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        if (isSuggested) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = PrimaryBlue.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "TODAY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Tap to start session",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Button(
                onClick = onStart,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSuggested) PrimaryBlue else DarkSurfaceVariant,
                    contentColor = if (isSuggested) Color.Black else TextPrimary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start")
            }
        }
    }
}

@Composable
fun DeloadBanner(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Deload Recommended",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = WarningOrange)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "3 or more exercises stalled over the past 14 days. Deloading drops weights to 90% and removes 1 set for 7 days.",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss (14d)", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange, contentColor = Color.Black)
                ) {
                    Text("Accept Deload", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BackupReminderBanner(onNavigateToBackup: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TertiaryAmber.copy(alpha = 0.15f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, TertiaryAmber),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Backup Overdue",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TertiaryAmber)
                )
                Text(
                    text = "No backup in 14+ days. Export your workouts to keep them safe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
            TextButton(onClick = onNavigateToBackup) {
                Text("Export", color = TertiaryAmber, fontWeight = FontWeight.Bold)
            }
        }
    }
}
