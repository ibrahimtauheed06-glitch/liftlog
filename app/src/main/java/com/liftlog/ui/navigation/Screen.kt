package com.liftlog.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Train : Screen("train", "Train")
    object ActiveWorkout : Screen("active_workout/{workoutId}", "Active Workout") {
        fun createRoute(workoutId: Long) = "active_workout/$workoutId"
    }
    object Protein : Screen("protein", "Protein")
    object Stats : Screen("stats", "Stats")
    object More : Screen("more", "More")

    // More sub-screens
    object ExerciseList : Screen("exercises", "Exercises")
    object TemplateList : Screen("templates", "Templates")
    object Bodyweight : Screen("bodyweight", "Bodyweight")
    object RunLog : Screen("runs", "Cardio / Runs")
    object Settings : Screen("settings", "Settings")
    object Backup : Screen("backup", "Backup & Export")
}
