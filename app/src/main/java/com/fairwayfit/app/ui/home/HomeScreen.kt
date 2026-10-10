package com.fairwayfit.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fairwayfit.app.AppContainer
import com.fairwayfit.app.data.AppSettings
import com.fairwayfit.app.data.health.HealthState
import com.fairwayfit.app.data.health.HealthSummary
import com.fairwayfit.app.domain.Content
import com.fairwayfit.app.domain.Plan
import com.fairwayfit.app.domain.estimatedMinutes
import com.fairwayfit.app.ui.appViewModel
import com.fairwayfit.app.ui.components.HeroCard
import com.fairwayfit.app.ui.components.ProgressRing
import com.fairwayfit.app.ui.components.ScreenTitle
import com.fairwayfit.app.ui.components.SectionTitle
import com.fairwayfit.app.ui.components.SurfaceCard
import com.fairwayfit.app.ui.components.Tag
import com.fairwayfit.app.ui.long
import com.fairwayfit.app.ui.toLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.format.TextStyle
import java.util.Locale

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    val sessions = c.repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val logs = c.repository.workoutLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val health = c.health.state
    val healthPermissions: Set<String> get() = c.health.permissions

    fun refreshHealth() {
        viewModelScope.launch { c.health.refresh() }
    }
}

@Composable
fun HomeScreen(
    onOpenWorkout: (String) -> Unit,
    onStartWorkout: (String) -> Unit,
    onTakeTest: () -> Unit,
    onOpenProgress: () -> Unit
) {
    val vm = appViewModel { HomeViewModel(it) }
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val health by vm.health.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { vm.refreshHealth() }

    LaunchedEffect(Unit) { vm.refreshHealth() }

    val today = LocalDate.now()
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    val (nextDate, nextWorkout) = Plan.next(settings.days, today)
    val isToday = nextDate == today
    val doneToday = logs.any { it.dateMillis.toLocalDate() == today && it.workoutId == nextWorkout.id }

    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekCount = logs.count { !it.dateMillis.toLocalDate().isBefore(weekStart) }
    val weekGoal = if (settings.days.isEmpty()) 3 else settings.days.size

    val latest = sessions.lastOrNull()
    val baseline = sessions.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle(title = greeting, subtitle = today.long().uppercase()) }

        item {
            HeroCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val label = if (isToday) "TODAY'S SESSION"
                    else "NEXT UP · " + nextDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(nextWorkout.name, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        nextWorkout.tagline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag("${nextWorkout.estimatedMinutes} min", icon = Icons.Rounded.Timer)
                        Tag("${nextWorkout.items.size} exercises")
                        if (doneToday) {
                            Tag(
                                "Done today",
                                container = MaterialTheme.colorScheme.primaryContainer,
                                content = MaterialTheme.colorScheme.primary,
                                icon = Icons.Rounded.Check
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onStartWorkout(nextWorkout.id) },
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (doneToday) "Train again" else "Start workout")
                        }
                        TextButton(onClick = { onOpenWorkout(nextWorkout.id) }) { Text("Details") }
                    }
                }
            }
        }

        item {
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SurfaceCard(Modifier.weight(1f).fillMaxHeight()) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProgressRing(
                            progress = weekCount.toFloat() / weekGoal,
                            modifier = Modifier.size(92.dp),
                            strokeWidth = 10.dp
                        ) {
                            Text("$weekCount/$weekGoal", style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(
                            if (weekCount >= weekGoal) "Weekly goal hit!" else "sessions this week",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                SurfaceCard(Modifier.weight(1f).fillMaxHeight(), onClick = if (latest != null) onOpenProgress else onTakeTest) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("FAIRWAYFIT SCORE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val overall = latest?.overall
                        Text(
                            overall?.toString() ?: "—",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val baselineOverall = baseline?.overall
                        val message = when {
                            overall == null -> "Take the fitness test"
                            sessions.size == 1 -> "Baseline set"
                            baselineOverall != null -> {
                                val delta = overall - baselineOverall
                                (if (delta >= 0) "+$delta" else "$delta") + " since baseline"
                            }
                            else -> ""
                        }
                        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (latest == null) {
                        Text("Set your baseline", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "8 quick tests · about 15 minutes. Measures your stability, power and mobility so you can chart real progress.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onTakeTest, shape = MaterialTheme.shapes.large) {
                            Icon(Icons.Rounded.QueryStats, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start fitness test")
                        }
                    } else {
                        val daysSince = ChronoUnit.DAYS.between(latest.dateMillis.toLocalDate(), today).toInt()
                        Text("Fitness test", style = MaterialTheme.typography.titleLarge)
                        Text(
                            when {
                                daysSince == 0 -> "Last tested today."
                                daysSince == 1 -> "Last tested yesterday."
                                else -> "Last tested $daysSince days ago."
                            } + if (daysSince >= 28) " Time to retest!" else " Retest every 4 weeks.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onTakeTest, shape = MaterialTheme.shapes.large) {
                            Text("Retest now")
                        }
                    }
                }
            }
        }

        item { HealthCard(health, onConnect = { permissionLauncher.launch(vm.healthPermissions) }, onRetry = vm::refreshHealth) }

        item {
            val warmup = Content.workout("warmup")
            SurfaceCard(onClick = { onStartWorkout(warmup.id) }) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .tile(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(warmup.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${warmup.estimatedMinutes} min · before you tee off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Start", tint = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

/** Rounded tinted tile background used for small icon boxes and stat chips. */
@Composable
private fun Modifier.tile(color: Color): Modifier =
    this.clip(MaterialTheme.shapes.small).background(color)

@Composable
private fun HealthCard(state: HealthState, onConnect: () -> Unit, onRetry: () -> Unit) {
    SurfaceCard {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("From your watch")
            when (state) {
                HealthState.Loading -> Text(
                    "Checking Health Connect…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HealthState.Unavailable -> Text(
                    "Health Connect isn't available on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HealthState.NeedsPermission -> {
                    Text(
                        "Connect Health Connect to see weight, sleep, resting heart rate and steps. Garmin Connect can share its data there.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onConnect, shape = MaterialTheme.shapes.large) { Text("Connect") }
                }
                is HealthState.Error -> {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onRetry, shape = MaterialTheme.shapes.large) { Text("Retry") }
                }
                is HealthState.Ready -> HealthGrid(state.summary)
            }
        }
    }
}

@Composable
private fun HealthGrid(s: HealthSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HealthStat(Icons.Rounded.MonitorWeight, "Weight", s.weightLb?.let { "%.1f lb".format(it) } ?: "—", Modifier.weight(1f))
            HealthStat(Icons.Rounded.MonitorHeart, "Resting HR", s.restingHr?.let { "$it bpm" } ?: "—", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HealthStat(
                Icons.Rounded.Bedtime, "Last sleep",
                s.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—", Modifier.weight(1f)
            )
            HealthStat(
                Icons.Rounded.DirectionsWalk, "Steps / day",
                s.avgDailySteps?.let { "%,d".format(it) } ?: "—", Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HealthStat(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .tile(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, style = MaterialTheme.typography.titleSmall)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
