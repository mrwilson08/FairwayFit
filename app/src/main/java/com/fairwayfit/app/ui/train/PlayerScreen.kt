package com.fairwayfit.app.ui.train

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fairwayfit.app.data.Repository
import com.fairwayfit.app.data.pebble.PebbleManager
import com.fairwayfit.app.data.pebble.PebbleProtocol
import com.fairwayfit.app.data.pebble.WatchCommand
import com.fairwayfit.app.data.pebble.WatchWorkoutState
import com.fairwayfit.app.domain.Content
import com.fairwayfit.app.domain.Exercise
import com.fairwayfit.app.domain.Workout
import com.fairwayfit.app.ui.appViewModel
import com.fairwayfit.app.ui.components.ProgressRing
import com.fairwayfit.app.ui.components.ScoreBar
import com.fairwayfit.app.ui.components.SurfaceCard
import com.fairwayfit.app.ui.components.Tag
import com.fairwayfit.app.ui.components.categoryColor
import com.fairwayfit.app.ui.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface Step {
    data class Work(
        val exercise: Exercise,
        val setNo: Int,
        val totalSets: Int,
        val side: String?,
        val reps: Int,
        val seconds: Int,
        val exerciseIndex: Int,
        val exerciseCount: Int
    ) : Step {
        val timed: Boolean get() = seconds > 0
    }

    data class Rest(val seconds: Int, val next: Work) : Step
}

data class PlayerUi(
    val index: Int,
    val remaining: Int,
    val running: Boolean,
    val finished: Boolean = false,
    val durationSec: Int = 0
)

class PlayerViewModel(
    private val workout: Workout,
    private val repo: Repository,
    private val pebble: PebbleManager
) : ViewModel() {

    val steps: List<Step> = buildSteps(workout)
    val workSteps: Int = steps.count { it is Step.Work }

    private val startedAt = SystemClock.elapsedRealtime()
    private val _ui = MutableStateFlow(enter(0))
    val ui: StateFlow<PlayerUi> = _ui.asStateFlow()

    private var lastSentToWatch: PlayerUi? = null

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                tick()
            }
        }

        // Mirror the workout on the Pebble. Only state changes are sent, not every countdown second:
        // the watch counts down by itself between messages.
        pebble.startWorkoutSession()
        viewModelScope.launch { ui.collect { mirrorToWatch(it) } }
        viewModelScope.launch {
            pebble.commands.collect { command ->
                when (command) {
                    WatchCommand.PRIMARY -> primary()
                    WatchCommand.ADD_REST -> addRest(REST_EXTENSION_SEC)
                }
            }
        }
    }

    val title: String get() = workout.name

    override fun onCleared() {
        pebble.endWorkoutSession()
    }

    /** Completed work steps before the current one, used for the progress bar. */
    fun workStepsDone(index: Int): Int = steps.take(index).count { it is Step.Work }

    /** Starts the countdown on a timed work step. */
    fun startTimer() = _ui.update { it.copy(running = true) }

    /** Finishes a rep-based set, skips a rest, or skips the rest of a timed hold. */
    fun next() = advance()

    fun addRest(seconds: Int) = _ui.update {
        if (steps[it.index] is Step.Rest) it.copy(remaining = it.remaining + seconds) else it
    }

    /** The one main action, shared by the on-screen button and the watch's SELECT button. */
    fun primary() {
        val u = _ui.value
        if (u.finished) return
        when (val s = steps[u.index]) {
            is Step.Work -> if (s.timed && !u.running) startTimer() else advance()
            is Step.Rest -> advance()
        }
    }

    private fun mirrorToWatch(u: PlayerUi) {
        val previous = lastSentToWatch
        lastSentToWatch = u
        val changed = previous == null ||
            u.index != previous.index ||
            u.running != previous.running ||
            u.finished != previous.finished ||
            u.remaining > previous.remaining // "+15 s" on a rest
        if (changed) pebble.showWorkout(toWatchState(u))
    }

    private fun toWatchState(u: PlayerUi): WatchWorkoutState {
        if (u.finished) {
            return WatchWorkoutState(PebbleProtocol.PHASE_DONE, "Workout complete", "${formatDuration(u.durationSec)} total", 0, 100)
        }
        val progress = workStepsDone(u.index) * 100 / workSteps
        return when (val s = steps[u.index]) {
            is Step.Work -> {
                val detail = "Set ${s.setNo}/${s.totalSets}" + (s.side?.let { " · $it" } ?: "")
                val phase = when {
                    !s.timed -> PebbleProtocol.PHASE_REPS
                    u.running -> PebbleProtocol.PHASE_HOLD_RUNNING
                    else -> PebbleProtocol.PHASE_HOLD_READY
                }
                WatchWorkoutState(phase, s.exercise.name, detail, if (s.timed) u.remaining else s.reps, progress)
            }
            is Step.Rest -> WatchWorkoutState(
                PebbleProtocol.PHASE_REST, "Rest", "Next: ${s.next.exercise.name}", u.remaining, progress
            )
        }
    }

    private fun tick() {
        val u = _ui.value
        if (u.finished || !u.running) return
        val r = u.remaining - 1
        if (r <= 0) advance() else _ui.value = u.copy(remaining = r)
    }

    private fun advance() {
        val u = _ui.value
        if (u.finished) return
        val nextIndex = u.index + 1
        if (nextIndex >= steps.size) finish() else _ui.value = enter(nextIndex)
    }

    private fun finish() {
        val duration = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
        _ui.value = PlayerUi(index = steps.lastIndex, remaining = 0, running = false, finished = true, durationSec = duration)
        viewModelScope.launch { repo.logWorkout(workout.id, duration) }
    }

    private fun enter(index: Int): PlayerUi = when (val s = steps[index]) {
        is Step.Work -> PlayerUi(index, remaining = s.seconds, running = false)
        is Step.Rest -> PlayerUi(index, remaining = s.seconds, running = true)
    }

    private companion object {
        const val REST_EXTENSION_SEC = 15

        fun buildSteps(workout: Workout): List<Step> {
            val works = mutableListOf<Step.Work>()
            workout.items.forEachIndexed { exerciseIndex, item ->
                val exercise = Content.exercise(item.exerciseId)
                for (set in 1..item.sets) {
                    val sides: List<String?> = if (item.perSide) listOf("Left", "Right") else listOf(null)
                    for (side in sides) {
                        works += Step.Work(
                            exercise = exercise, setNo = set, totalSets = item.sets, side = side,
                            reps = item.reps, seconds = item.seconds,
                            exerciseIndex = exerciseIndex, exerciseCount = workout.items.size
                        )
                    }
                }
            }
            val out = mutableListOf<Step>()
            works.forEachIndexed { i, work ->
                out += work
                val next = works.getOrNull(i + 1)
                // No rest between the left and right side of the same set, and none after the last step.
                if (next != null && work.side != "Left") {
                    out += Step.Rest(workout.restSeconds, next)
                }
            }
            return out
        }
    }
}

@Composable
fun PlayerScreen(workoutId: String, onExit: () -> Unit) {
    val workout = Content.workout(workoutId)
    val vm = appViewModel(key = "player-$workoutId") { PlayerViewModel(workout, it.repository, it.pebble) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmQuit by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    BackHandler(enabled = !ui.finished) { confirmQuit = true }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text("Quit workout?") },
            text = { Text("Your progress for this session won't be saved.") },
            confirmButton = { TextButton(onClick = onExit) { Text("Quit") } },
            dismissButton = { TextButton(onClick = { confirmQuit = false }) { Text("Keep going") } }
        )
    }

    if (ui.finished) {
        FinishedContent(vm, ui, onExit)
        return
    }

    val step = vm.steps[ui.index]
    val done = vm.workStepsDone(ui.index)

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { confirmQuit = true }) { Icon(Icons.Rounded.Close, contentDescription = "Quit") }
            Column(Modifier.weight(1f)) {
                Text(vm.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Set ${done.coerceAtMost(vm.workSteps)} of ${vm.workSteps} done", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        ScoreBar(fraction = done.toFloat() / vm.workSteps, height = 6.dp)
        Spacer(Modifier.height(18.dp))

        when (step) {
            is Step.Work -> WorkContent(step, ui, onStart = vm::startTimer, onDone = vm::next)
            is Step.Rest -> RestContent(step, ui, onSkip = vm::next, onAdd = { vm.addRest(15) })
        }
    }
}

@Composable
private fun WorkContent(step: Step.Work, ui: PlayerUi, onStart: () -> Unit, onDone: () -> Unit) {
    val ex = step.exercise
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag(
                    ex.category.label,
                    container = categoryColor(ex.category).copy(alpha = 0.16f),
                    content = categoryColor(ex.category)
                )
                Tag("Exercise ${step.exerciseIndex + 1}/${step.exerciseCount}")
            }
            Text(ex.name, style = MaterialTheme.typography.displaySmall)
            Text(
                "Set ${step.setNo} of ${step.totalSets}" + (step.side?.let { " · $it side" } ?: ""),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                if (step.timed) {
                    val total = step.seconds
                    ProgressRing(
                        progress = if (ui.running) 1f - ui.remaining.toFloat() / total else 0f,
                        modifier = Modifier.size(210.dp),
                        strokeWidth = 14.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${ui.remaining}", style = MaterialTheme.typography.displayLarge)
                            Text("seconds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${step.reps}", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                        Text("reps", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            SurfaceCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("“${ex.cue}”", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    ex.steps.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (step.timed && !ui.running) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start ${step.seconds} s hold")
            }
        } else if (step.timed) {
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = MaterialTheme.shapes.large
            ) { Text("Finish early") }
        } else {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Done")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RestContent(step: Step.Rest, ui: PlayerUi, onSkip: () -> Unit, onAdd: () -> Unit) {
    val next = step.next
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("REST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(16.dp))
            ProgressRing(
                progress = (ui.remaining.toFloat() / (step.seconds + 0.0001f)).coerceIn(0f, 1f),
                modifier = Modifier.size(210.dp),
                strokeWidth = 14.dp,
                color = MaterialTheme.colorScheme.secondary
            ) {
                Text("${ui.remaining}", style = MaterialTheme.typography.displayLarge)
            }
            Spacer(Modifier.height(28.dp))
            SurfaceCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("UP NEXT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(next.exercise.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Set ${next.setNo} of ${next.totalSets}" + (next.side?.let { " · $it side" } ?: "") +
                            " · " + if (next.timed) "${next.seconds} s" else "${next.reps} reps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier.weight(1f).height(60.dp),
                shape = MaterialTheme.shapes.large
            ) { Text("+15 s") }
            Button(
                onClick = onSkip,
                modifier = Modifier.weight(2f).height(60.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Skip rest")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FinishedContent(vm: PlayerViewModel, ui: PlayerUi, onExit: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(84.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text("Workout complete", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            vm.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tag(formatDuration(ui.durationSec) + " total")
            Tag("${vm.workSteps} sets")
        }
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = MaterialTheme.shapes.large
        ) { Text("Done") }
    }
}
