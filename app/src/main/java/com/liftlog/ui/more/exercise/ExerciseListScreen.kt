package com.liftlog.ui.more.exercise

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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Unarchive
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
import androidx.compose.ui.unit.sp
import com.liftlog.data.entity.Exercise
import com.liftlog.data.repository.ExerciseRepository
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurface
import com.liftlog.ui.theme.DarkSurfaceVariant
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.SecondaryTeal
import com.liftlog.ui.theme.TertiaryAmber
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

    var showArchived by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var mergingExercise by remember { mutableStateOf<Exercise?>(null) }

    val filteredList = allExercises
        .filter { it.archived == showArchived }
        .filter { it.name.contains(searchQuery, ignoreCase = true) }
        .sortedBy { it.name }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Exercise Library", fontWeight = FontWeight.Bold) },
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
            // Search and Active/Archived Toggle
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search exercises...") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!showArchived) PrimaryBlue else Color.Transparent)
                            .clickable { showArchived = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Active (${allExercises.count { !it.archived }})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (!showArchived) Color.Black else TextSecondary
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (showArchived) PrimaryBlue else Color.Transparent)
                            .clickable { showArchived = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Archived (${allExercises.count { it.archived }})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (showArchived) Color.Black else TextSecondary
                            )
                        )
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Text(
                        text = if (showArchived) "No archived exercises." else "No matching exercises found.",
                        color = TextMuted,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(filteredList) { ex ->
                    ExerciseRowCard(
                        exercise = ex,
                        onEdit = { editingExercise = ex },
                        onToggleArchive = {
                            coroutineScope.launch {
                                if (ex.archived) {
                                    exerciseRepository.unarchiveExercise(ex.id)
                                } else {
                                    exerciseRepository.archiveExercise(ex.id)
                                }
                            }
                        },
                        onMerge = { mergingExercise = ex }
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

    if (mergingExercise != null) {
        MergeExerciseDialog(
            sourceExercise = mergingExercise!!,
            allExercises = allExercises.filter { it.id != mergingExercise!!.id },
            onMerge = { targetId, reassignSets ->
                coroutineScope.launch {
                    exerciseRepository.mergeExercise(mergingExercise!!.id, targetId, reassignSets)
                    mergingExercise = null
                }
            },
            onDismiss = { mergingExercise = null }
        )
    }
}

@Composable
fun ExerciseRowCard(
    exercise: Exercise,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onMerge: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    if (exercise.isCompound) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PrimaryBlue.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Compound",
                                style = MaterialTheme.typography.labelSmall.copy(color = PrimaryBlue),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                val stackNote = if (exercise.plateKg != null) " • ${exercise.plateKg}kg plates" else ""
                val homeNote = if (exercise.homeCapKg != null) " • Home cap ${exercise.homeCapKg}kg" else ""
                Text(
                    text = "${exercise.equipment.capitalize(java.util.Locale.ROOT)} • ${exercise.defaultSets}×${exercise.repLo}-${exercise.repHi} (+${exercise.incrementKg}kg)$stackNote$homeNote",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                if (exercise.primaryMuscles.isNotBlank()) {
                    Text(
                        text = "Muscles: ${exercise.primaryMuscles}${if (exercise.secondaryMuscles.isNotBlank()) " (${exercise.secondaryMuscles})" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMerge, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.MergeType, contentDescription = "Merge", tint = TertiaryAmber, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggleArchive, modifier = Modifier.size(34.dp)) {
                    Icon(
                        if (exercise.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = "Archive",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
