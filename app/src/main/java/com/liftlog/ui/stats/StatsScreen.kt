package com.liftlog.ui.stats

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liftlog.ui.stats.components.LineTrendChart
import com.liftlog.ui.stats.components.MuscleVolumeBarChart
import com.liftlog.ui.stats.components.ProteinBarChart
import com.liftlog.ui.theme.DarkBorder
import com.liftlog.ui.theme.DarkSurface
import com.liftlog.ui.theme.DarkSurfaceVariant
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.SuccessGreen
import com.liftlog.ui.theme.TertiaryAmber
import com.liftlog.ui.theme.TextMuted
import com.liftlog.ui.theme.TextPrimary
import com.liftlog.ui.theme.TextSecondary

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateToHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

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
                // Screen Header + Timeframe Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        // Toggle Weekly vs Monthly
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(2.dp)
                        ) {
                            val isWeekly = state.timeframe == StatsTimeframe.WEEKLY
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isWeekly) PrimaryBlue else Color.Transparent)
                                    .clickable { viewModel.setTimeframe(StatsTimeframe.WEEKLY) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Weekly",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWeekly) Color.Black else TextSecondary
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isWeekly) PrimaryBlue else Color.Transparent)
                                    .clickable { viewModel.setTimeframe(StatsTimeframe.MONTHLY) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Monthly",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isWeekly) Color.Black else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // Overview Metrics Card: Sessions Done/Target, Protein Avg, 7-Day BW Avg
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable(enabled = onNavigateToHistory != null) { onNavigateToHistory?.invoke() }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Sessions", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    if (onNavigateToHistory != null) {
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(8.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${state.sessionsDone}/${state.sessionsTarget}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.sessionsDone >= state.sessionsTarget) SuccessGreen else TextPrimary
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Protein Avg", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${state.proteinAverage.toInt()}g",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                )
                                val proteinSubtitle = if (state.proteinDaysTracked > 0) {
                                    "${state.proteinDaysHit}/${state.proteinDaysTracked} days hit"
                                } else {
                                    "0 days hit"
                                }
                                Text(
                                    proteinSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Bodyweight", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                val bwStr = if (state.bodyweight7DayAvg > 0) String.format("%.1f kg", state.bodyweight7DayAvg) else "—"
                                Text(
                                    bwStr,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TertiaryAmber
                                    )
                                )
                                Text("7-day avg", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }

                // e1RM Exercise Trend Chart
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Estimated 1RM Trend (Top Set)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Exercise selector pills
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(state.allActiveExercises) { ex ->
                                    val isSelected = ex.id == state.selectedExerciseIdForChart
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryBlue else DarkSurfaceVariant)
                                            .clickable { viewModel.selectExerciseForChart(ex.id) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = ex.name,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.Black else TextSecondary
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val points = state.exerciseE1rmPoints.map { Pair(it.dateLabel, it.e1rm) }
                            LineTrendChart(points = points, unit = "kg", lineColor = PrimaryBlue)
                        }
                    }
                }

                // Exercise Session Progression Comparison List
                // "Per exercise: last vs previous session (top set, e1RM delta %)"
                item {
                    Text(
                        text = "Session Progress (Top Set & e1RM Delta)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (state.exerciseComparisons.isEmpty()) {
                    item {
                        Text("No completed exercise sessions to compare yet.", color = TextMuted)
                    }
                } else {
                    items(state.exerciseComparisons) { comp ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
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
                                    Text(
                                        comp.exercise.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                                    )
                                    val prevText = comp.previousTopSetText?.let { "Prev: $it" } ?: "First session"
                                    Text(
                                        "Last: ${comp.latestTopSetText}  •  $prevText",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                if (comp.e1rmDeltaPercent != null) {
                                    val isPositive = comp.e1rmDeltaPercent >= 0.0
                                    val sign = if (isPositive) "+" else ""
                                    val deltaColor = if (isPositive) SuccessGreen else MaterialTheme.colorScheme.error
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = deltaColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "$sign${String.format("%.1f", comp.e1rmDeltaPercent)}%",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = deltaColor
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Muscle Set Volume Chart
                // "hard sets per muscle (working sets only) vs weeklySetTarget"
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Hard Sets per Muscle (Working Sets Only)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Secondary muscles count 0.5 set. Pain sets excluded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            MuscleVolumeBarChart(muscleVolumes = state.muscleVolumes)
                        }
                    }
                }

                // Daily Protein Intake Chart
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Daily Protein Intake",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            ProteinBarChart(bars = state.proteinBars, targetGrams = state.proteinTarget)
                        }
                    }
                }

                // Bodyweight Trend Chart
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Bodyweight Trend",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            val bwPoints = state.bodyweightTrend.map { Pair(it.dateLabel, it.kg) }
                            LineTrendChart(points = bwPoints, unit = "kg", lineColor = TertiaryAmber)
                        }
                    }
                }
            }
        }
    }
}
