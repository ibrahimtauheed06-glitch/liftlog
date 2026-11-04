package com.liftlog.ui.more.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.liftlog.data.repository.SettingsRepository
import com.liftlog.data.repository.UserSettings
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurface
import com.liftlog.ui.theme.DarkSurfaceVariant
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.SuccessGreen
import com.liftlog.ui.theme.TertiaryAmber
import com.liftlog.ui.theme.TextMuted
import com.liftlog.ui.theme.TextPrimary
import com.liftlog.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by settingsRepository.settingsFlow.collectAsState(initial = UserSettings())
    val coroutineScope = rememberCoroutineScope()

    var proteinTargetText by remember(settings.proteinTarget) { mutableStateOf(settings.proteinTarget.toInt().toString()) }
    var rolloverHourText by remember(settings.dayRolloverHour) { mutableStateOf(settings.dayRolloverHour.toString()) }
    var weeklySetTargetText by remember(settings.weeklySetTarget) { mutableStateOf(settings.weeklySetTarget.toString()) }
    var sessionsTargetText by remember(settings.sessionsPerWeekTarget) { mutableStateOf(settings.sessionsPerWeekTarget.toString()) }
    var platesText by remember(settings.availablePlates) { mutableStateOf(settings.availablePlates) }
    var barWeightText by remember(settings.barWeight) { mutableStateOf(settings.barWeight.toString()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("App Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Training Phase Mode: [GYM | HOME]
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Training Phase Mode", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "In HOME mode, weights are capped at home limits and reps push to 30. Switching back to GYM safely scales weights to 90%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(3.dp)
                        ) {
                            val isGym = settings.phase.equals("GYM", ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isGym) PrimaryBlue else Color.Transparent)
                                    .clickable { coroutineScope.launch { settingsRepository.updatePhase("GYM") } }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = if (isGym) Color.Black else TextSecondary)
                                    Text(" GYM Phase", fontWeight = FontWeight.Bold, color = if (isGym) Color.Black else TextSecondary)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isGym) TertiaryAmber else Color.Transparent)
                                    .clickable { coroutineScope.launch { settingsRepository.updatePhase("HOME") } }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = if (!isGym) Color.Black else TextSecondary)
                                    Text(" HOME Phase", fontWeight = FontWeight.Bold, color = if (!isGym) Color.Black else TextSecondary)
                                }
                            }
                        }

                        // "Return to gym" button if in HOME phase
                        if (!settings.phase.equals("GYM", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch { settingsRepository.updatePhase("GYM") }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Return to Gym (90% Last Gym Weight)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Targets
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Targets & Schedules", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = proteinTargetText,
                            onValueChange = {
                                proteinTargetText = it
                                it.toDoubleOrNull()?.let { target ->
                                    coroutineScope.launch { settingsRepository.updateProteinTarget(target) }
                                }
                            },
                            label = { Text("Daily Protein Target (grams)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = rolloverHourText,
                            onValueChange = {
                                rolloverHourText = it
                                it.toIntOrNull()?.let { hour ->
                                    coroutineScope.launch { settingsRepository.updateDayRolloverHour(hour) }
                                }
                            },
                            label = { Text("Day Rollover Hour (0-23, default 4 AM)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = weeklySetTargetText,
                            onValueChange = {
                                weeklySetTargetText = it
                                it.toIntOrNull()?.let { sets ->
                                    coroutineScope.launch { settingsRepository.updateWeeklySetTarget(sets) }
                                }
                            },
                            label = { Text("Weekly Hard Set Target per Muscle") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = sessionsTargetText,
                            onValueChange = {
                                sessionsTargetText = it
                                it.toIntOrNull()?.let { count ->
                                    coroutineScope.launch { settingsRepository.updateSessionsPerWeekTarget(count) }
                                }
                            },
                            label = { Text("Weekly Workout Sessions Target") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Equipment & Warm-up Options
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Equipment & Plate Calculator", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = barWeightText,
                            onValueChange = {
                                barWeightText = it
                                it.toDoubleOrNull()?.let { weight ->
                                    coroutineScope.launch { settingsRepository.updateBarWeight(weight) }
                                }
                            },
                            label = { Text("Standard Olympic Bar Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = platesText,
                            onValueChange = {
                                platesText = it
                                coroutineScope.launch { settingsRepository.updateAvailablePlates(it) }
                            },
                            label = { Text("Available Plates (kg, comma-separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("2-Set Warm-Up Ramp", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                Text("Prefills 50% and 75% ramp sets instead of single 60% set", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                            Switch(
                                checked = settings.twoSetWarmup,
                                onCheckedChange = { coroutineScope.launch { settingsRepository.updateTwoSetWarmup(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}
