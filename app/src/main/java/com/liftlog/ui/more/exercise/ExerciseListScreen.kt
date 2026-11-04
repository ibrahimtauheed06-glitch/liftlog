package com.liftlog.ui.more.exercise

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liftlog.data.entity.Exercise
import com.liftlog.data.repository.ExerciseRepository
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurface
import com.liftlog.ui.theme.DarkSurfaceVariant
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.SecondaryTeal
import com.liftlog.ui.theme.TextMuted
import com.liftlog.ui.theme.TextPrimary
import com.liftlog.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    exerciseRepository: ExerciseRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allExercises by exerciseRepository.getAllExercises().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    val filteredList = allExercises
        .filter { !it.archived }
        .filter { it.name.contains(searchQuery, ignoreCase = true) || it.equipment.contains(searchQuery, ignoreCase = true) }
        .sortedBy { it.name }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Exercise Dictionary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isCreatingNew = true },
                containerColor = PrimaryBlue,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search exercises by name or equipment...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "${filteredList.size} Exercises in Dictionary",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TextMuted)
                )
            }

            if (filteredList.isEmpty()) {
                item {
                    Text(
                        text = "No matching exercises found.",
                        color = TextMuted,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(filteredList) { ex ->
                    ExerciseDictionaryCard(
                        exercise = ex,
                        onEdit = { editingExercise = ex }
                    )
                }
            }
        }
    }

    if (isCreatingNew) {
        ExerciseEditDialog(
            exercise = null,
            onSave = { newEx ->
                coroutineScope.launch {
                    exerciseRepository.saveExercise(newEx)
                    isCreatingNew = false
                }
            },
            onDismiss = { isCreatingNew = false }
        )
    }

    if (editingExercise != null) {
        ExerciseEditDialog(
            exercise = editingExercise,
            onSave = { updated ->
                coroutineScope.launch {
                    exerciseRepository.updateExercise(updated)
                    editingExercise = null
                }
            },
            onDismiss = { editingExercise = null }
        )
    }
}

@Composable
fun ExerciseDictionaryCard(
    exercise: Exercise,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
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
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = DarkSurfaceVariant
                    ) {
                        Text(
                            text = exercise.equipment.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = TextSecondary),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${exercise.defaultSets} sets × ${exercise.repLo}–${exercise.repHi} reps",
                        style = MaterialTheme.typography.bodySmall.copy(color = SecondaryTeal)
                    )

                    if (exercise.defaultWeightKg > 0.0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val weightStr = if (exercise.defaultWeightKg % 1.0 == 0.0) exercise.defaultWeightKg.toInt() else exercise.defaultWeightKg
                        Text(
                            text = "• Starts @ ${weightStr}kg",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    }
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted)
            }
        }
    }
}
