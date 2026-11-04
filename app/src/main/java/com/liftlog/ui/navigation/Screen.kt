package com.liftlog.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Train : Screen("train", "Train")
    object WorkoutPreview : Screen("workout_preview/{templateId}", "Workout Preview") {
        fun createRoute(templateId: Long) = "workout_preview/$templateId"
    }
    object ActiveWorkout : Screen("active_workout/{workoutId}", "Active Workout") {
        fun createRoute(workoutId: Long) = "active_workout/$workoutId"
    }
    object WorkoutHistory : Screen("workout_history", "Workout History")
    object WorkoutDetail : Screen("workout_detail/{workoutId}", "Workout Detail") {
        fun createRoute(workoutId: Long) = "workout_detail/$workoutId"
    }
    object Protein : Screen("protein", "Protein")
    object Stats : Screen("stats", "Stats")
    object More : Screen("more", "More")

    // More sub-screens
    object ExerciseList : Screen("exercises", "Exercises")
    object TemplateList : Screen("templates", "Templates")
    object Bodyweight : Screen("bodyweight", "Bodyweight")
    object RecycleBin : Screen("recycle_bin", "Recycle Bin")
    object Settings : Screen("settings", "Settings")
    object Backup : Screen("backup", "Backup & Export")
}
