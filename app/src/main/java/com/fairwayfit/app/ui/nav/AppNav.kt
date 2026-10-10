package com.fairwayfit.app.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import com.fairwayfit.app.ui.home.HomeScreen
import com.fairwayfit.app.ui.progress.ProgressScreen
import com.fairwayfit.app.ui.settings.SettingsScreen
import com.fairwayfit.app.ui.test.TestIntroScreen
import com.fairwayfit.app.ui.test.TestResultScreen
import com.fairwayfit.app.ui.test.TestRunScreen
import com.fairwayfit.app.ui.train.ExerciseDetailScreen
import com.fairwayfit.app.ui.train.PlayerScreen
import com.fairwayfit.app.ui.train.TrainScreen
import com.fairwayfit.app.ui.train.WorkoutDetailScreen

object Routes {
    const val HOME = "home"
    const val TRAIN = "train"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val WORKOUT = "workout/{id}"
    const val PLAYER = "player/{id}"
    const val EXERCISE = "exercise/{id}"
    const val TEST_INTRO = "test"
    const val TEST_RUN = "test/run"
    const val TEST_RESULT = "test/result/{id}"

    fun workout(id: String) = "workout/$id"
    fun player(id: String) = "player/$id"
    fun exercise(id: String) = "exercise/$id"
    fun testResult(id: String) = "test/result/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Rounded.Home),
    Tab(Routes.TRAIN, "Train", Icons.Rounded.FitnessCenter),
    Tab(Routes.PROGRESS, "Progress", Icons.Rounded.QueryStats),
    Tab(Routes.SETTINGS, "Settings", Icons.Rounded.Settings)
)

private fun NavBackStackEntry.requireId(): String =
    requireNotNull(arguments?.getString("id")) { "Missing 'id' navigation argument" }

private fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.goToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) }
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenWorkout = { nav.navigate(Routes.workout(it)) },
                    onStartWorkout = { nav.navigate(Routes.player(it)) },
                    onTakeTest = { nav.navigate(Routes.TEST_INTRO) },
                    onOpenProgress = { nav.goToTab(Routes.PROGRESS) }
                )
            }
            composable(Routes.TRAIN) {
                TrainScreen(
                    onOpenWorkout = { nav.navigate(Routes.workout(it)) },
                    onOpenExercise = { nav.navigate(Routes.exercise(it)) }
                )
            }
            composable(Routes.PROGRESS) {
                ProgressScreen(onTakeTest = { nav.navigate(Routes.TEST_INTRO) })
            }
            composable(Routes.SETTINGS) { SettingsScreen() }

            composable(Routes.WORKOUT) { e ->
                val id = e.requireId()
                WorkoutDetailScreen(
                    workoutId = id,
                    onBack = { nav.popBackStack() },
                    onStart = { nav.navigate(Routes.player(id)) },
                    onOpenExercise = { nav.navigate(Routes.exercise(it)) }
                )
            }
            composable(Routes.EXERCISE) { e ->
                ExerciseDetailScreen(exerciseId = e.requireId(), onBack = { nav.popBackStack() })
            }
            composable(Routes.PLAYER) { e ->
                PlayerScreen(workoutId = e.requireId(), onExit = { nav.popBackStack() })
            }

            composable(Routes.TEST_INTRO) {
                TestIntroScreen(
                    onBack = { nav.popBackStack() },
                    onBegin = { nav.navigate(Routes.TEST_RUN) }
                )
            }
            composable(Routes.TEST_RUN) {
                TestRunScreen(
                    onCancel = { nav.popBackStack(Routes.TEST_INTRO, inclusive = false) },
                    onFinished = { sessionId ->
                        nav.navigate(Routes.testResult(sessionId)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                )
            }
            composable(Routes.TEST_RESULT) { e ->
                TestResultScreen(
                    sessionId = e.requireId(),
                    onDone = { nav.popBackStack(Routes.HOME, inclusive = false) },
                    onViewProgress = { nav.goToTab(Routes.PROGRESS) }
                )
            }
        }
    }
}
