package com.liftlog.ui.more

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liftlog.ui.navigation.Screen
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

@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "More",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure exercises, templates, logs, and settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                MoreMenuItem(
                    icon = Icons.Default.History,
                    iconTint = PrimaryBlue,
                    title = "Workout History",
                    subtitle = "View past sessions, edit weights/reps, or remove workouts",
                    onClick = { onNavigate(Screen.WorkoutHistory.route) }
                )
            }

            item {
                MoreMenuItem(
                    icon = Icons.Default.FitnessCenter,
                    iconTint = PrimaryBlue,
                    title = "Exercise Dictionary",
                    subtitle = "Manage dictionary, equipment types, and starting weights",
                    onClick = { onNavigate(Screen.ExerciseList.route) }
                )
            }

            item {
                MoreMenuItem(
                    icon = Icons.Default.ViewList,
                    iconTint = SecondaryTeal,
                    title = "Workout Templates",
                    subtitle = "Customize existing routines or create new custom templates",
                    onClick = { onNavigate(Screen.TemplateList.route) }
                )
            }

            item {
                MoreMenuItem(
                    icon = Icons.Default.MonitorWeight,
                    iconTint = TertiaryAmber,
                    title = "Bodyweight",
                    subtitle = "Track scale weight and 7-day rolling average",
                    onClick = { onNavigate(Screen.Bodyweight.route) }
                )
            }

            item {
                MoreMenuItem(
                    icon = Icons.Default.Delete,
                    iconTint = DangerRed,
                    title = "Recycle Bin",
                    subtitle = "View, restore, or permanently remove discarded workouts",
                    onClick = { onNavigate(Screen.RecycleBin.route) }
                )
            }

            item {
                MoreMenuItem(
                    icon = Icons.Default.Backup,
                    iconTint = SecondaryTeal,
                    title = "Backup & Export",
                    subtitle = "Export JSON + CSVs to folder, import data safely",
                    onClick = { onNavigate(Screen.Backup.route) }
                )
            }

            item {
                MoreMenuItem(
                    icon = Icons.Default.Settings,
                    iconTint = TextSecondary,
                    title = "Settings",
                    subtitle = "Protein targets, day rollover hour, and weekly goals",
                    onClick = { onNavigate(Screen.Settings.route) }
                )
            }
        }
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
