package com.liftlog.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.liftlog.AppContainer
import com.liftlog.data.repository.UserSettings
import com.liftlog.ui.more.MoreScreen
import com.liftlog.ui.more.backup.BackupScreen
import com.liftlog.ui.more.bodyweight.BodyweightScreen
import com.liftlog.ui.more.exercise.ExerciseListScreen
import com.liftlog.ui.more.run.RunLogScreen
import com.liftlog.ui.more.settings.SettingsScreen
import com.liftlog.ui.more.template.TemplateListScreen
import com.liftlog.ui.protein.ProteinScreen
import com.liftlog.ui.protein.ProteinViewModel
import com.liftlog.ui.stats.StatsScreen
import com.liftlog.ui.stats.StatsViewModel
import com.liftlog.ui.theme.DarkSurface
import com.liftlog.ui.theme.PrimaryBlue
import com.liftlog.ui.theme.TextMuted
import com.liftlog.ui.theme.TextSecondary
import com.liftlog.ui.train.ActiveWorkoutScreen
import com.liftlog.ui.train.ActiveWorkoutViewModel
import com.liftlog.ui.train.TrainScreen
import com.liftlog.ui.train.TrainViewModel

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun LiftLogNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val settings by container.settingsRepository.settingsFlow.collectAsState(initial = UserSettings())

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Train, Icons.Default.FitnessCenter, "Train"),
        BottomNavItem(Screen.Protein, Icons.Default.Egg, "Protein"),
        BottomNavItem(Screen.Stats, Icons.Default.BarChart, "Stats"),
        BottomNavItem(Screen.More, Icons.Default.MoreHoriz, "More")
    )

    // Hide bottom navigation bar on active workout screen
    val showBottomBar = currentRoute?.startsWith("active_workout") != true

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextSecondary
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = PrimaryBlue,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Train.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Train.route) {
                val trainViewModel: TrainViewModel = viewModel(
                    factory = TrainViewModel.Factory(
                        workoutRepository = container.workoutRepository,
                        templateDao = container.database.templateDao(),
                        settingsRepository = container.settingsRepository,
                        backupRepository = container.backupRepository
                    )
                )
                TrainScreen(
                    viewModel = trainViewModel,
                    onNavigateToActiveWorkout = { workoutId ->
                        navController.navigate(Screen.ActiveWorkout.createRoute(workoutId))
                    },
                    onNavigateToBackup = {
                        navController.navigate(Screen.Backup.route)
                    }
                )
            }

            composable(
                route = Screen.ActiveWorkout.route,
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: 0L
                val activeViewModel: ActiveWorkoutViewModel = viewModel(
                    key = "active_$workoutId",
                    factory = ActiveWorkoutViewModel.Factory(
                        workoutId = workoutId,
                        workoutRepository = container.workoutRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                ActiveWorkoutScreen(
                    viewModel = activeViewModel,
                    settings = settings,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Protein.route) {
                val proteinViewModel: ProteinViewModel = viewModel(
                    factory = ProteinViewModel.Factory(
                        proteinRepository = container.proteinRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                ProteinScreen(viewModel = proteinViewModel)
            }

            composable(Screen.Stats.route) {
                val statsViewModel: StatsViewModel = viewModel(
                    factory = StatsViewModel.Factory(
                        workoutDao = container.database.workoutDao(),
                        setEntryDao = container.database.setEntryDao(),
                        exerciseDao = container.database.exerciseDao(),
                        proteinDao = container.database.proteinDao(),
                        bodyweightRepository = container.bodyweightRepository,
                        settingsRepository = container.settingsRepository
                    )
                )
                StatsScreen(viewModel = statsViewModel)
            }

            composable(Screen.More.route) {
                MoreScreen(onNavigate = { route -> navController.navigate(route) })
            }

            composable(Screen.ExerciseList.route) {
                ExerciseListScreen(
                    exerciseRepository = container.exerciseRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.TemplateList.route) {
                TemplateListScreen(
                    templateDao = container.database.templateDao(),
                    exerciseDao = container.database.exerciseDao(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Bodyweight.route) {
                BodyweightScreen(
                    bodyweightRepository = container.bodyweightRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.RunLog.route) {
                RunLogScreen(
                    runRepository = container.runRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsRepository = container.settingsRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Backup.route) {
                BackupScreen(
                    backupRepository = container.backupRepository,
                    settingsRepository = container.settingsRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
