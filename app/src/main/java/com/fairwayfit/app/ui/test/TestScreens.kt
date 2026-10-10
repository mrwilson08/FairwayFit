package com.fairwayfit.app.ui.test

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fairwayfit.app.data.Repository
import com.fairwayfit.app.domain.Category
import com.fairwayfit.app.domain.Content
import com.fairwayfit.app.domain.FitnessTest
import com.fairwayfit.app.domain.Scoring
import com.fairwayfit.app.domain.TestSession
import com.fairwayfit.app.ui.appViewModel
import com.fairwayfit.app.ui.components.DetailHeader
import com.fairwayfit.app.ui.components.NumberedSteps
import com.fairwayfit.app.ui.components.ProgressRing
import com.fairwayfit.app.ui.components.ScoreBar
import com.fairwayfit.app.ui.components.SectionTitle
import com.fairwayfit.app.ui.components.SurfaceCard
import com.fairwayfit.app.ui.components.Tag
import com.fairwayfit.app.ui.components.categoryColor
import com.fairwayfit.app.ui.format
import com.fairwayfit.app.ui.short
import com.fairwayfit.app.ui.toLocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ------------------------------------------------------------------ view models

class TestViewModel(private val repo: Repository) : ViewModel() {
    val tests: List<FitnessTest> = Content.tests

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _values = MutableStateFlow<Map<String, Double>>(emptyMap())
    val values: StateFlow<Map<String, Double>> = _values.asStateFlow()

    fun record(key: String, value: Double) = _values.update { it + (key to value) }
    fun clear(key: String) = _values.update { it - key }

    fun next() = _index.update { (it + 1).coerceAtMost(tests.lastIndex) }
    fun previous() = _index.update { (it - 1).coerceAtLeast(0) }

    /** Saves the recorded results and returns the new session id. */
    suspend fun save(): String = repo.saveSession(_values.value)
}

class ResultViewModel(repo: Repository) : ViewModel() {
    val sessions: StateFlow<List<TestSession>> = repo.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

// ----------------------------------------------------------------------- intro

@Composable
fun TestIntroScreen(onBack: () -> Unit, onBegin: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { DetailHeader(title = "Fitness test", onBack = onBack) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your golf fitness baseline", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Eight short tests measure your stability, power and mobility. Repeat them every four weeks to chart real progress.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag("About 15 min", icon = Icons.Rounded.Timer)
                Tag("8 tests")
                Tag("Skip any test")
            }
        }
        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("What you'll need")
                    Text("• A hard, flat floor and a wall", style = MaterialTheme.typography.bodyMedium)
                    Text("• A tape measure and a chair", style = MaterialTheme.typography.bodyMedium)
                    Text("• A box or wall to rest your feet on for sit-and-reach", style = MaterialTheme.typography.bodyMedium)
                    Text("• A club for the rotation test, and a partner or phone camera", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { SectionTitle("The tests") }
        items(Content.tests, key = { it.key }) { t ->
            SurfaceCard {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.name, style = MaterialTheme.typography.titleMedium)
                        Text(t.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(t.category.label, style = MaterialTheme.typography.labelMedium, color = categoryColor(t.category))
                }
            }
        }
        item {
            Text(
                "Warm up for a few minutes first and stop if anything hurts. These are general fitness benchmarks, not medical advice — check with your doctor before starting a new exercise program.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Button(
                onClick = onBegin,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Begin test")
            }
        }
    }
}

// ------------------------------------------------------------------------ run

@Composable
fun TestRunScreen(onCancel: () -> Unit, onFinished: (String) -> Unit) {
    val vm = appViewModel { TestViewModel(it.repository) }
    val index by vm.index.collectAsStateWithLifecycle()
    val values by vm.values.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    val test = vm.tests[index]
    val isLast = index == vm.tests.lastIndex
    val recorded = values[test.key]

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.Rounded.Close, contentDescription = "Cancel test") }
            Column(Modifier.weight(1f)) {
                Text("Test ${index + 1} of ${vm.tests.size}", style = MaterialTheme.typography.titleSmall)
                Text(test.category.label, style = MaterialTheme.typography.bodySmall, color = categoryColor(test.category))
            }
            if (index > 0) {
                TextButton(onClick = vm::previous) { Text("Back") }
            }
        }
        Spacer(Modifier.height(6.dp))
        ScoreBar(fraction = (index + 1).toFloat() / vm.tests.size, height = 6.dp)
        Spacer(Modifier.height(14.dp))

        AnimatedContent(
            targetState = index,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
            },
            label = "test-page"
        ) { page ->
            TestPage(
                test = vm.tests[page],
                recorded = values[vm.tests[page].key],
                onRecord = { vm.record(vm.tests[page].key, it) }
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!isLast) {
                OutlinedButton(
                    onClick = { vm.clear(test.key); vm.next() },
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = MaterialTheme.shapes.large
                ) { Text("Skip") }
                Button(
                    onClick = vm::next,
                    enabled = recorded != null,
                    modifier = Modifier.weight(2f).height(58.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Next")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                }
            } else {
                OutlinedButton(
                    onClick = { vm.clear(test.key) },
                    enabled = recorded != null,
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = MaterialTheme.shapes.large
                ) { Text("Clear") }
                Button(
                    onClick = {
                        saving = true
                        scope.launch { onFinished(vm.save()) }
                    },
                    enabled = values.isNotEmpty() && !saving,
                    modifier = Modifier.weight(2f).height(58.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Finish (${values.size} recorded)")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TestPage(test: FitnessTest, recorded: Double?, onRecord: (Double) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(test.name, style = MaterialTheme.typography.headlineMedium)
        Text(test.summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SurfaceCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle("How to test")
                NumberedSteps(test.steps)
            }
        }

        if (test.timed) Stopwatch(test = test, onResult = onRecord)
        if (test.countdownSeconds > 0) Countdown(seconds = test.countdownSeconds)

        ValueEntry(test = test, recorded = recorded, onRecord = onRecord)

        Text(test.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
    }
}

/** Stopwatch that writes the stopped time (whole seconds, capped at the test maximum) as the result. */
@Composable
private fun Stopwatch(test: FitnessTest, onResult: (Double) -> Unit) {
    var running by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(running) {
        while (running) {
            elapsedMs = SystemClock.elapsedRealtime() - startedAt
            delay(50)
        }
    }

    SurfaceCard {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "%d:%02d.%d".format(elapsedMs / 60000, (elapsedMs / 1000) % 60, (elapsedMs / 100) % 10),
                style = MaterialTheme.typography.displayMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!running) {
                    Button(
                        onClick = {
                            startedAt = SystemClock.elapsedRealtime()
                            elapsedMs = 0L
                            running = true
                        },
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (elapsedMs == 0L) "Start" else "Restart")
                    }
                } else {
                    Button(
                        onClick = {
                            running = false
                            elapsedMs = SystemClock.elapsedRealtime() - startedAt
                            onResult((elapsedMs / 1000.0).coerceIn(test.min, test.max).roundToInt().toDouble())
                        },
                        shape = MaterialTheme.shapes.large
                    ) { Text("Stop & record") }
                }
            }
        }
    }
}

/** Plain countdown with a haptic buzz at zero, for timed-count tests. The count is entered separately. */
@Composable
private fun Countdown(seconds: Int) {
    var running by remember { mutableStateOf(false) }
    var remaining by remember { mutableStateOf(seconds) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        running = false
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    SurfaceCard {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (remaining == 0) "Time!" else "$remaining s",
                style = MaterialTheme.typography.displayMedium,
                color = if (remaining == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = {
                    remaining = seconds
                    running = true
                },
                enabled = !running,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (remaining == seconds) "Start $seconds s countdown" else "Restart countdown")
            }
        }
    }
}

/** Big number with −/+ buttons and a slider. The result only counts once the user has set a value. */
@Composable
private fun ValueEntry(test: FitnessTest, recorded: Double?, onRecord: (Double) -> Unit) {
    val value = recorded ?: test.start
    val stepsCount = (((test.max - test.min) / test.step).roundToInt() - 1).coerceAtLeast(0)

    SurfaceCard {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTitle("Your result")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                FilledTonalIconButton(onClick = { onRecord((value - test.step).coerceAtLeast(test.min)) }) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(150.dp)) {
                    Text(
                        test.format(value, withUnit = false),
                        style = MaterialTheme.typography.displayLarge,
                        color = if (recorded != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(test.unit, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = { onRecord((value + test.step).coerceAtMost(test.max)) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase")
                }
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onRecord(((it / test.step.toFloat()).roundToInt() * test.step.toFloat()).toDouble().coerceIn(test.min, test.max)) },
                valueRange = test.min.toFloat()..test.max.toFloat(),
                steps = stepsCount,
                colors = SliderDefaults.colors()
            )
            Text(
                if (recorded == null) "Move the slider or use +/− to record a result" else "Recorded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --------------------------------------------------------------------- result

@Composable
fun TestResultScreen(sessionId: String, onDone: () -> Unit, onViewProgress: () -> Unit) {
    val vm = appViewModel { ResultViewModel(it.repository) }
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val session = sessions.firstOrNull { it.id == sessionId }
    if (session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading…") }
        return
    }
    val baseline = sessions.first()
    val isBaseline = baseline.id == session.id
    val overall = session.overall ?: 0

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (isBaseline) "BASELINE SET" else "TEST COMPLETE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                ProgressRing(progress = overall / 100f, modifier = Modifier.size(190.dp), strokeWidth = 14.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$overall", style = MaterialTheme.typography.displayLarge)
                        Text("FairwayFit score", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val baselineOverall = baseline.overall
                if (!isBaseline && baselineOverall != null) {
                    val delta = overall - baselineOverall
                    Tag(
                        (if (delta >= 0) "+$delta" else "$delta") + " vs baseline (${baseline.dateMillis.toLocalDate().short()})",
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        "This is your starting point. Retest in about 4 weeks to see how far you've come.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle("By category")
                    Category.entries.forEach { cat ->
                        val score = Scoring.categoryScore(session.values, cat)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row {
                                Text(cat.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text(score?.toString() ?: "—", style = MaterialTheme.typography.titleSmall, color = categoryColor(cat))
                            }
                            ScoreBar(fraction = (score ?: 0) / 100f, color = categoryColor(cat))
                        }
                    }
                }
            }
        }
        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Your results")
                    Content.tests.forEach { t ->
                        val v = session.values[t.key]
                        val base = baseline.values[t.key]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(v?.let { t.format(it) } ?: "skipped", style = MaterialTheme.typography.titleSmall)
                                if (!isBaseline && v != null && base != null) {
                                    val d = v - base
                                    Text(
                                        (if (d >= 0) "+" else "") + t.format(d),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (d >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = onViewProgress, modifier = Modifier.fillMaxWidth().height(58.dp), shape = MaterialTheme.shapes.large) {
                Text("View progress")
            }
        }
        item {
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(58.dp), shape = MaterialTheme.shapes.large) {
                Text("Back to home")
            }
        }
    }
}
