package com.liftlog.ui.more.backup

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liftlog.data.repository.BackupRepository
import com.liftlog.data.repository.ImportMode
import com.liftlog.data.repository.SettingsRepository
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    backupRepository: BackupRepository,
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by settingsRepository.settingsFlow.collectAsState(initial = com.liftlog.data.repository.UserSettings())

    val isOverdue by produceState(initialValue = false, key1 = settings.lastExportTs) {
        value = backupRepository.isBackupOverdue()
    }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImportMode by remember { mutableStateOf(ImportMode.MERGE) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Folder export launcher (SAF Document Tree)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        if (folderUri != null) {
            coroutineScope.launch {
                val result = backupRepository.exportToFolder(folderUri)
                result.onSuccess { msg ->
                    settingsRepository.updateLastExportTs(System.currentTimeMillis())
                    statusMessage = msg
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    statusMessage = "Export failed: ${err.message}"
                    Toast.makeText(context, "Export error: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // JSON file picker launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { fileUri ->
        if (fileUri != null) {
            pendingImportUri = fileUri
        }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Backup & Export", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Overdue reminder
            if (isOverdue) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "No backup in 14+ days. Export your workouts now to prevent data loss.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Monthly Auto-Backup Status
            item {
                val autoBackupInfo = remember { backupRepository.getAutoBackupInfo() }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Monthly Auto-Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SuccessGreen.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SuccessGreen),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Automatically creates a local backup once a month in liftlog_auto_backup.json, overwriting the older backup each time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val lastSavedText = if (autoBackupInfo != null) {
                            "Last saved: ${dateFormat.format(Date(autoBackupInfo.second))}"
                        } else if (settings.lastAutoBackupTs > 0L) {
                            "Last saved: ${dateFormat.format(Date(settings.lastAutoBackupTs))}"
                        } else {
                            "Status: Will save on next launch"
                        }
                        Text(
                            lastSavedText,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }
            }

            // Export Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Database & CSVs", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Exports full JSON backup plus CSV files for sets, protein, and bodyweight to any directory you select via the Storage Access Framework.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val lastText = if (settings.lastExportTs > 0L) dateFormat.format(Date(settings.lastExportTs)) else "Never"
                        Text(
                            text = "Last Export: $lastText",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { exportLauncher.launch(null) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("CHOOSE FOLDER & EXPORT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Import Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = SecondaryTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import JSON Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Restore workout history from a previously exported liftlog_backup.json file. You can choose to merge or replace existing records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = { importLauncher.launch("application/json") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("SELECT BACKUP JSON FILE")
                        }
                    }
                }
            }

            if (statusMessage != null) {
                item {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SuccessGreen,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }

    // Confirmation Import Dialog
    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Import Data", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Choose how to import the selected backup file into your local database:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedImportMode == ImportMode.MERGE,
                            onClick = { selectedImportMode = ImportMode.MERGE }
                        )
                        Column(modifier = Modifier.padding(start = 6.dp)) {
                            Text("Merge", fontWeight = FontWeight.Bold)
                            Text("Keeps existing data and adds backup records", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedImportMode == ImportMode.REPLACE,
                            onClick = { selectedImportMode = ImportMode.REPLACE }
                        )
                        Column(modifier = Modifier.padding(start = 6.dp)) {
                            Text("Replace (Overwrite)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Clears current database and restores backup exactly", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri
                        pendingImportUri = null
                        if (uri != null) {
                            coroutineScope.launch {
                                val result = backupRepository.importFromJsonUri(uri, selectedImportMode)
                                result.onSuccess { msg ->
                                    statusMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    statusMessage = "Import failed: ${err.message}"
                                    Toast.makeText(context, "Import error: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedImportMode == ImportMode.REPLACE) MaterialTheme.colorScheme.error else PrimaryBlue,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Confirm Import", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
