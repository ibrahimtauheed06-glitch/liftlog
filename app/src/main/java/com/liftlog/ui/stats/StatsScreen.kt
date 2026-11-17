package com.liftlog.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.liftlog.ui.theme.WarningOrange
import kotlinx.coroutines.launch

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
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            var selectedQuickJump by remember { mutableStateOf("Overview") }
            var isProgressionExpanded by remember { mutableStateOf(false) }

            val jumpItems = listOf(
                "Overview" to 0,
                "Readiness" to 1,
                "1RM Trend" to 2,
                "Progress" to 3,
                "Volume" to 4,
                "Protein" to 5,
                "Bodyweight" to 6
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Pinned Top Header: Title + Weekly/Monthly + Quick Jump Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Jump Pills Bar
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(jumpItems) { (title, targetIndex) ->
                            val isSelected = selectedQuickJump == title
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryBlue else DarkSurfaceVariant)
                                    .clickable {
                                        selectedQuickJump = title
                                        if (title == "Readiness") {
                                            isProgressionExpanded = true
                                        }
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(targetIndex)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // Scrollable Content Sections
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 0. Overview Metrics Card: Sessions Done/Target, Avg Time, Protein Avg, 7-Day BW Avg
                    item(key = "overview") {
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
                                    Text("target", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Avg Time", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        state.averageWorkoutDurationText,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                    )
                                    Text("per workout", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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
                                        "${state.proteinDaysHit}/${state.proteinDaysTracked} hit"
                                    } else {
                                        "0 hit"
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

                    // 1. Progression Readiness (Dropdown / Accordion Card with Actionable Increases at Top)
                    item(key = "readiness") {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Accordion Header: Clickable to toggle dropdown
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isProgressionExpanded = !isProgressionExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Progression Readiness",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            )
                                            if (state.readyToAdvanceCount > 0) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = SuccessGreen.copy(alpha = 0.2f),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen)
                                                ) {
                                                    Text(
                                                        text = "${state.readyToAdvanceCount} READY",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = SuccessGreen
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val summarySubtitle = if (state.progressionStatuses.isEmpty()) {
                                            "No completed exercise sessions logged yet"
                                        } else {
                                            "${state.readyToAdvanceCount} Ready • ${state.buildingRepsCount} Building reps • ${state.backoffCount + state.stalledCount} Attention"
                                        }
                                        Text(
                                            text = summarySubtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = if (isProgressionExpanded) "Hide" else "View All",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = PrimaryBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isProgressionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isProgressionExpanded) "Collapse" else "Expand",
                                            tint = PrimaryBlue
                                        )
                                    }
                                }

                                // Expanded Dropdown Contents
                                AnimatedVisibility(
                                    visible = isProgressionExpanded,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(modifier = Modifier.padding(top = 16.dp)) {
                                        if (state.progressionStatuses.isEmpty()) {
                                            Text(
                                                text = "Complete a workout session to see exercise progression targets.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMuted,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        } else {
                                            var readinessFilter by remember { mutableStateOf("All") }
                                            val filterOptions = listOf(
                                                "All (${state.progressionStatuses.size})" to "All",
                                                "Ready (${state.readyToAdvanceCount})" to "Ready",
                                                "Building (${state.buildingRepsCount})" to "Building",
                                                "Attention (${state.backoffCount + state.stalledCount})" to "Attention"
                                            )

                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                items(filterOptions) { (label, key) ->
                                                    val isSelected = readinessFilter == key
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isSelected) PrimaryBlue else DarkSurfaceVariant)
                                                            .clickable { readinessFilter = key }
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) Color.Black else TextSecondary
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            val filteredStatuses = state.progressionStatuses.filter { p ->
                                                when (readinessFilter) {
                                                    "Ready" -> p.status == ProgressionStatusType.READY_TO_ADVANCE
                                                    "Building" -> p.status == ProgressionStatusType.BUILDING_REPS
                                                    "Attention" -> p.status == ProgressionStatusType.BACKOFF_ADVISED || p.status == ProgressionStatusType.STALLED
                                                    else -> true
                                                }
                                            }

                                            filteredStatuses.forEach { p ->
                                                val (badgeColor, badgeBg) = when (p.status) {
                                                    ProgressionStatusType.READY_TO_ADVANCE -> Pair(SuccessGreen, SuccessGreen.copy(alpha = 0.15f))
                                                    ProgressionStatusType.BUILDING_REPS -> Pair(PrimaryBlue, PrimaryBlue.copy(alpha = 0.15f))
                                                    ProgressionStatusType.BACKOFF_ADVISED -> Pair(WarningOrange, WarningOrange.copy(alpha = 0.15f))
                                                    ProgressionStatusType.STALLED -> Pair(TertiaryAmber, TertiaryAmber.copy(alpha = 0.15f))
                                                }

                                                Card(
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = p.exercise.name,
                                                                style = MaterialTheme.typography.titleSmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = TextPrimary
                                                                )
                                                            )
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = badgeBg,
                                                                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                                                            ) {
                                                                Text(
                                                                    text = p.statusLabel,
                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = badgeColor
                                                                    ),
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = p.detailText,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. e1RM Exercise Trend Chart + Rep-Max Estimates
                    item(key = "1rm") {
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

                                if (state.repMaxEstimates.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Estimated Rep-Maxes (from Personal Best)",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        state.repMaxEstimates.forEach { est ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "${est.reps}RM",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted
                                                )
                                                val wStr = if (est.weightKg % 1.0 == 0.0) "${est.weightKg.toInt()}" else "${est.weightKg}"
                                                Text(
                                                    text = "${wStr}kg",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = PrimaryBlue
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Exercise Session Progression Comparison List
                    item(key = "progress") {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Session Progress (Top Set & e1RM Delta)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Comparison of top working sets between your last two sessions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (state.exerciseComparisons.isEmpty()) {
                                    Text(
                                        text = "No completed exercise sessions to compare yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                } else {
                                    state.exerciseComparisons.forEach { comp ->
                                        Card(
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.5f)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
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
                                                        text = comp.exercise.name,
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                    )
                                                    val prevText = comp.previousTopSetText?.let { "Prev: $it" } ?: "First session"
                                                    Text(
                                                        text = "Last: ${comp.latestTopSetText}  •  $prevText",
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
                            }
                        }
                    }

                    // 4. Muscle Set Volume Chart
                    item(key = "volume") {
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

                    // 5. Daily Protein Intake Chart
                    item(key = "protein") {
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

                    // 6. Bodyweight Trend Chart
                    item(key = "bodyweight") {
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
                                val bwSmoothed = state.bodyweightTrend.map { it.smoothedKg }
                                LineTrendChart(
                                    points = bwPoints,
                                    unit = "kg",
                                    lineColor = PrimaryBlue,
                                    smoothedValues = bwSmoothed,
                                    smoothedColor = TertiaryAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
