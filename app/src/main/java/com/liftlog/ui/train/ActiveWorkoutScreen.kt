package com.liftlog.ui.train

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liftlog.data.entity.Exercise
import com.liftlog.data.repository.UserSettings
import com.liftlog.service.RestTimerService
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
import com.liftlog.ui.train.components.ExerciseCard
import com.liftlog.ui.train.components.RestTimerBanner
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    settings: UserSettings,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val timerState by RestTimerService.timerState.collectAsState()
    val context = LocalContext.current

    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var workoutNote by remember { mutableStateOf("") }

    val availableExercises by viewModel.availableExercises.collectAsState()
    var exerciseToSwap by remember { mutableStateOf<Exercise?>(null) }
    var swapSearchQuery by remember { mutableStateOf("") }

    // Screen stays on during active workout
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Overall Elapsed Workout Timer (computes from startTs)
    val startTs = state.workout?.startTs ?: remember { System.currentTimeMillis() }
    var elapsedSeconds by remember(startTs) {
        mutableLongStateOf(((System.currentTimeMillis() - startTs) / 1000L).coerceAtLeast(0L))
    }
    LaunchedEffect(startTs) {
        while (true) {
            elapsedSeconds = ((System.currentTimeMillis() - startTs) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val formattedDuration = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Active Workout",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedDuration,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showFinishDialog = true }) {
                        Text("FINISH", fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Ongoing Rest Timer Banner
                RestTimerBanner(timerState = timerState)

                val listState = rememberLazyListState()

                // Auto-advance scroll to the next exercise when designated working sets of previous exercise are completed
                val firstIncompleteIndex = state.exercises.indexOfFirst {
                    it.loggedSets.count { s -> s.type != "warmup" } < it.plannedSets
                }
                LaunchedEffect(firstIncompleteIndex) {
                    if (firstIncompleteIndex > 0) {
                        listState.animateScrollToItem(firstIncompleteIndex)
                    }
                }

                // Exercises List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.exercises) { item ->
                        val input = state.prefilledInputs[item.exercise.id]

                        // Determine superset label (e.g. SS1 A / B)
                        val supersetLabel = if (item.slot?.supersetGroup != null) {
                            val groupSlots = state.exercises
                                .mapNotNull { it.slot }
                                .filter { it.supersetGroup == item.slot.supersetGroup }
                                .sortedBy { it.slotOrder }
                            val isA = groupSlots.firstOrNull()?.exerciseId == item.exercise.id
                            "SS${item.slot.supersetGroup} (${if (isA) "A" else "B"})"
                        } else null

                        ExerciseCard(
                            item = item,
                            currentInput = input,
                            supersetLabel = supersetLabel,
                            isResting = timerState.isRunning,
                            restRemainingSec = timerState.remainingSec,
                            onAdjustWeight = { delta -> viewModel.adjustWeight(item.exercise.id, delta) },
                            onAdjustReps = { delta -> viewModel.adjustReps(item.exercise.id, delta) },
                            onDirectWeightChange = { weight -> viewModel.setWeightDirect(item.exercise.id, weight) },
                            onDirectRepsChange = { reps -> viewModel.setRepsDirect(item.exercise.id, reps) },
                            onSetType = { type -> viewModel.setType(item.exercise.id, type) },
                            onSetRpe = { rpe -> viewModel.setRpe(item.exercise.id, rpe) },
                            onToggleSwung = { viewModel.toggleFlagSwung(item.exercise.id) },
                            onTogglePain = { viewModel.toggleFlagPain(item.exercise.id) },
                            onLogSet = { viewModel.logSet(context, item.exercise.id) },
                            onUpdatePastSet = { updated -> viewModel.updatePastSet(updated) },
                            onDeletePastSet = { setId -> viewModel.deletePastSet(setId) },
                            onSwapExercise = {
                                swapSearchQuery = ""
                                exerciseToSwap = item.exercise
                            },
                            onSelectWarmupTarget = { weight, reps ->
                                viewModel.applyWarmupTarget(item.exercise.id, weight, reps)
                            }
                        )
                    }

                    // Discard workout button at bottom
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showDiscardDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            border = BorderStroke(1.dp, DangerRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Discard Workout (Move to Bin)")
                        }
                    }
                }
            }
        }
    }

    // Finish Workout Dialog
    if (showFinishDialog) {
        val workingSetsCount = state.exercises.sumOf { it.loggedSets.count { s -> s.type != "warmup" } }
        val warmupSetsCount = state.exercises.sumOf { it.loggedSets.count { s -> s.type == "warmup" } }
        val setsText = if (warmupSetsCount > 0) "$workingSetsCount working sets (+$warmupSetsCount warmup)" else "$workingSetsCount total sets"

        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Complete Workout") },
            text = {
                Column {
                    Text(
                        text = "Great work! You logged $setsText in $formattedDuration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = workoutNote,
                        onValueChange = { workoutNote = it },
                        label = { Text("Workout Note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishDialog = false
                        viewModel.finishWorkout(context, workoutNote.ifBlank { null }) {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Save & Finish", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Keep Training", color = TextSecondary)
                }
            }
        )
    }

    // Discard Workout Dialog (Moved to Bin)
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Workout?") },
            text = {
                Text(
                    text = "This session will be stopped and moved to the Recycle Bin. You can restore it later from More > Recycle Bin if needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardWorkout(context) {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Discard", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Swap Exercise Dialog
    if (exerciseToSwap != null) {
        val currentEx = exerciseToSwap!!
        val filtered = availableExercises.filter { ex ->
            ex.id != currentEx.id &&
            (ex.name.contains(swapSearchQuery, ignoreCase = true) ||
             ex.primaryMuscles.contains(swapSearchQuery, ignoreCase = true) ||
             ex.equipment.contains(swapSearchQuery, ignoreCase = true))
        }

        AlertDialog(
            onDismissRequest = { exerciseToSwap = null },
            title = {
                Text(
                    text = "Swap ${currentEx.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select a replacement for this session only. Your workout template remains unchanged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = swapSearchQuery,
                        onValueChange = { swapSearchQuery = it },
                        placeholder = { Text("Search exercises...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(filtered) { candidate ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceVariant,
                                border = BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.swapExercise(currentEx.id, candidate.id)
                                        exerciseToSwap = null
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = candidate.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${candidate.equipment.uppercase()} • ${candidate.primaryMuscles}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No exercises found", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { exerciseToSwap = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
