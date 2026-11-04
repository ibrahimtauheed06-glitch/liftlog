package com.liftlog.ui.more.template

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liftlog.data.db.ExerciseDao
import com.liftlog.data.db.TemplateDao
import com.liftlog.data.entity.Exercise
import com.liftlog.data.entity.Template
import com.liftlog.data.entity.TemplateSlot
import com.liftlog.ui.theme.DangerRed
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurface
import com.liftlog.ui.theme.DarkSurfaceVariant
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.SecondaryTeal
import com.liftlog.ui.theme.TertiaryAmber
import com.liftlog.ui.theme.TextMuted
import com.liftlog.ui.theme.TextPrimary
import com.liftlog.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    templateDao: TemplateDao,
    exerciseDao: ExerciseDao,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val templates by templateDao.getAllTemplates().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    val exercises by produceState<Map<Long, Exercise>>(initialValue = emptyMap()) {
        value = exerciseDao.getAllExercises().first().associateBy { it.id }
    }

    var editingTemplate by remember { mutableStateOf<Template?>(null) }
    var isCreatingTemplate by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Workout Templates", fontWeight = FontWeight.Bold) },
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
                onClick = { isCreatingTemplate = true },
                containerColor = PrimaryBlue,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Template")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Manage & Customize Routines",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Add, remove, or reorder exercises in any template. Custom templates appear in your Train tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            items(templates) { template ->
                TemplateCardItem(
                    template = template,
                    templateDao = templateDao,
                    exercises = exercises,
                    onEdit = { editingTemplate = template },
                    onDelete = {
                        coroutineScope.launch {
                            templateDao.deleteSlotsForTemplate(template.id)
                            templateDao.deleteTemplate(template.id)
                        }
                    }
                )
            }
        }
    }

    if (isCreatingTemplate) {
        TemplateEditDialog(
            template = null,
            templateDao = templateDao,
            exerciseDao = exerciseDao,
            onDismiss = { isCreatingTemplate = false },
            onSave = { isCreatingTemplate = false }
        )
    }

    if (editingTemplate != null) {
        TemplateEditDialog(
            template = editingTemplate,
            templateDao = templateDao,
            exerciseDao = exerciseDao,
            onDismiss = { editingTemplate = null },
            onSave = { editingTemplate = null }
        )
    }
}

@Composable
fun TemplateCardItem(
    template: Template,
    templateDao: TemplateDao,
    exercises: Map<Long, Exercise>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val slots by produceState<List<TemplateSlot>>(initialValue = emptyList(), key1 = template.id) {
        value = templateDao.getSlotsForTemplateSync(template.id)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = template.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        Text(
                            text = "${slots.size} exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Template", tint = PrimaryBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slots.forEachIndexed { idx, slot ->
                    val ex = exercises[slot.exerciseId]
                    if (ex != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${idx + 1}. ${ex.name}",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                                )
                                if (slot.supersetGroup != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = TertiaryAmber.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "SS${slot.supersetGroup}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TertiaryAmber, fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${ex.defaultSets} × ${ex.repLo}–${ex.repHi}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryTeal
                            )
                        }
                    }
                }
            }
        }
    }
}

data class SlotDraft(
    val exerciseId: Long,
    val exerciseName: String,
    val supersetGroup: Int? = null
)

@Composable
fun TemplateEditDialog(
    template: Template?,
    templateDao: TemplateDao,
    exerciseDao: ExerciseDao,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var templateName by remember { mutableStateOf(template?.name ?: "") }
    val draftSlots = remember { mutableStateListOf<SlotDraft>() }
    var allExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var showAddExercisePicker by remember { mutableStateOf(false) }

    produceState(initialValue = false) {
        val allEx = exerciseDao.getAllExercises().first()
        allExercises = allEx
        if (template != null) {
            val exMap = allEx.associateBy { it.id }
            val existingSlots = templateDao.getSlotsForTemplateSync(template.id)
            draftSlots.clear()
            existingSlots.forEach { s ->
                val name = exMap[s.exerciseId]?.name ?: "Exercise ${s.exerciseId}"
                draftSlots.add(SlotDraft(s.exerciseId, name, s.supersetGroup))
            }
        }
        value = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (template == null) "Create Template" else "Edit Template",
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
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exercises in Template (${draftSlots.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedButton(
                        onClick = { showAddExercisePicker = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Exercise")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (draftSlots.isEmpty()) {
                    Text(
                        text = "No exercises in this template yet. Tap 'Add Exercise' to choose from your dictionary.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        draftSlots.forEachIndexed { index, slot ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${index + 1}. ${slot.exerciseName}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Superset: ",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted
                                            )
                                            listOf(null to "None", 1 to "SS1", 2 to "SS2").forEach { (ssGroup, ssLabel) ->
                                                val isSelected = slot.supersetGroup == ssGroup
                                                Text(
                                                    text = ssLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isSelected) PrimaryBlue else TextMuted,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    modifier = Modifier
                                                        .clickable {
                                                            draftSlots[index] = slot.copy(supersetGroup = ssGroup)
                                                        }
                                                        .padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (index > 0) {
                                            IconButton(
                                                onClick = {
                                                    val temp = draftSlots[index]
                                                    draftSlots[index] = draftSlots[index - 1]
                                                    draftSlots[index - 1] = temp
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        if (index < draftSlots.size - 1) {
                                            IconButton(
                                                onClick = {
                                                    val temp = draftSlots[index]
                                                    draftSlots[index] = draftSlots[index + 1]
                                                    draftSlots[index + 1] = temp
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        IconButton(
                                            onClick = { draftSlots.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = DangerRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (templateName.isNotBlank() && draftSlots.isNotEmpty()) {
                        coroutineScope.launch {
                            val templateId = if (template != null) {
                                templateDao.updateTemplate(template.copy(name = templateName.trim()))
                                template.id
                            } else {
                                val newT = Template(name = templateName.trim(), orderIndex = 99)
                                templateDao.insertTemplate(newT)
                            }

                            // Re-insert slots
                            templateDao.deleteSlotsForTemplate(templateId)
                            val newSlots = draftSlots.mapIndexed { idx, draft ->
                                TemplateSlot(
                                    templateId = templateId,
                                    exerciseId = draft.exerciseId,
                                    slotOrder = idx,
                                    supersetGroup = draft.supersetGroup
                                )
                            }
                            templateDao.insertSlots(newSlots)
                            onSave()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = templateName.isNotBlank() && draftSlots.isNotEmpty()
            ) {
                Text("Save Template", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )

    if (showAddExercisePicker) {
        AlertDialog(
            onDismissRequest = { showAddExercisePicker = false },
            title = { Text("Select Exercise from Dictionary") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val availableEx = allExercises.filter { ex -> draftSlots.none { it.exerciseId == ex.id } }
                    if (availableEx.isEmpty()) {
                        item {
                            Text("All dictionary exercises are already in this template.", color = TextMuted)
                        }
                    } else {
                        items(availableEx) { ex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceVariant)
                                    .clickable {
                                        draftSlots.add(SlotDraft(ex.id, ex.name, null))
                                        showAddExercisePicker = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ex.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(ex.equipment.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddExercisePicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}
