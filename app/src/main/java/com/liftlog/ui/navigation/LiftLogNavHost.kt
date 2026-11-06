package com.liftlog.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreHoriz
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
import com.liftlog.ui.history.WorkoutDetailScreen
import com.liftlog.ui.history.WorkoutDetailViewModel
import com.liftlog.ui.history.WorkoutHistoryScreen
import com.liftlog.ui.history.WorkoutHistoryViewModel
import com.liftlog.ui.more.MoreScreen
import com.liftlog.ui.more.backup.BackupScreen
import com.liftlog.ui.more.bin.RecycleBinScreen
import com.liftlog.ui.more.bodyweight.BodyweightScreen
import com.liftlog.ui.more.exercise.ExerciseListScreen
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
import com.liftlog.ui.train.WorkoutPreviewScreen

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

    val moreSubroutes = setOf(
        Screen.ExerciseList.route,
        Screen.TemplateList.route,
        Screen.Bodyweight.route,
        Screen.RecycleBin.route,
        Screen.Settings.route,
        Screen.Backup.route
    )

    // Hide bottom navigation bar on active workout, preview, and workout detail screens
    val showBottomBar = currentRoute?.startsWith("active_workout") != true &&
            currentRoute?.startsWith("workout_preview") != true &&
            currentRoute?.startsWith("workout_detail") != true

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextSecondary
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = if (item.screen == Screen.More) {
                            currentRoute == Screen.More.route || currentRoute in moreSubroutes
                        } else {
                            currentRoute == item.screen.route
                        }
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
                                if (item.screen == Screen.More) {
                                    // Always reset to root More screen instead of restoring sub-screens
                                    navController.popBackStack(Screen.More.route, inclusive = false)
                                    navController.navigate(Screen.More.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                } else {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
            modifier = Modifier.padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
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
                    onNavigateToPreview = { templateId ->
                        navController.navigate(Screen.WorkoutPreview.createRoute(templateId))
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.WorkoutHistory.route)
                    },
                    onNavigateToBackup = {
                        navController.navigate(Screen.Backup.route)
                    }
                )
            }

            composable(
                route = Screen.WorkoutPreview.route,
                arguments = listOf(navArgument("templateId") { type = NavType.LongType })
            ) { backStackEntry ->
                val templateId = backStackEntry.arguments?.getLong("templateId") ?: 0L
                WorkoutPreviewScreen(
                    templateId = templateId,
                    templateDao = container.database.templateDao(),
                    exerciseDao = container.database.exerciseDao(),
                    workoutRepository = container.workoutRepository,
                    onStartWorkout = { workoutId ->
                        navController.navigate(Screen.ActiveWorkout.createRoute(workoutId)) {
                            popUpTo(Screen.Train.route)
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
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
                        settingsRepository = container.settingsRepository,
                        exerciseRepository = container.exerciseRepository
                    )
                )
                ActiveWorkoutScreen(
                    viewModel = activeViewModel,
                    settings = settings,
                    onNavigateBack = {
                        navController.navigate(Screen.Train.route) {
                            popUpTo(Screen.Train.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
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
                StatsScreen(
                    viewModel = statsViewModel,
                    onNavigateToHistory = {
                        navController.navigate(Screen.WorkoutHistory.route)
                    }
                )
            }

            composable(Screen.WorkoutHistory.route) {
                val historyViewModel: WorkoutHistoryViewModel = viewModel(
                    factory = WorkoutHistoryViewModel.Factory(
                        workoutRepository = container.workoutRepository,
                        templateDao = container.database.templateDao(),
                        exerciseDao = container.database.exerciseDao(),
                        setEntryDao = container.database.setEntryDao()
                    )
                )
                WorkoutHistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToDetail = { workoutId ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.WorkoutDetail.route,
                arguments = listOf(navArgument("workoutId") { type = NavType.LongType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: 0L
                val detailViewModel: WorkoutDetailViewModel = viewModel(
                    key = "detail_$workoutId",
                    factory = WorkoutDetailViewModel.Factory(
                        workoutId = workoutId,
                        workoutRepository = container.workoutRepository,
                        templateDao = container.database.templateDao(),
                        exerciseDao = container.database.exerciseDao(),
                        setEntryDao = container.database.setEntryDao()
                    )
                )
                WorkoutDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
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

            composable(Screen.RecycleBin.route) {
                RecycleBinScreen(
                    workoutRepository = container.workoutRepository,
                    templateDao = container.database.templateDao(),
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
