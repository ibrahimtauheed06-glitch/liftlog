package com.liftlog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.liftlog.receiver.ProteinReminderScheduler
import com.liftlog.ui.navigation.LiftLogNavHost
import com.liftlog.ui.theme.LiftLogTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Permission result handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS permission on API 33+ (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val appContainer = (application as LiftLogApplication).container

        lifecycleScope.launch(Dispatchers.IO) {
            appContainer.backupRepository.performAutoMonthlyBackupIfNeeded()
            val settings = appContainer.settingsRepository.settingsFlow.first()
            if (settings.proteinReminderEnabled) {
                ProteinReminderScheduler.schedule(this@MainActivity, settings.proteinReminderHour)
            }
        }

        setContent {
            LiftLogTheme {
                LiftLogNavHost(container = appContainer)
            }
        }
    }
}
