package com.fairwayfit.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fairwayfit.app.AppContainer
import com.fairwayfit.app.domain.Category
import com.fairwayfit.app.domain.Content
import com.fairwayfit.app.domain.Scoring
import com.fairwayfit.app.ui.appViewModel
import com.fairwayfit.app.ui.components.BarChart
import com.fairwayfit.app.ui.components.LineChart
import com.fairwayfit.app.ui.components.ScoreBar
import com.fairwayfit.app.ui.components.ScreenTitle
import com.fairwayfit.app.ui.components.SectionTitle
import com.fairwayfit.app.ui.components.Sparkline
import com.fairwayfit.app.ui.components.SurfaceCard
import com.fairwayfit.app.ui.components.Tag
import com.fairwayfit.app.ui.components.categoryColor
import com.fairwayfit.app.ui.format
import com.fairwayfit.app.ui.short
import com.fairwayfit.app.ui.toLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToInt

class ProgressViewModel(c: AppContainer) : ViewModel() {
    val sessions = c.repository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val logs = c.repository.workoutLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun ProgressScreen(onTakeTest: () -> Unit) {
    val vm = appViewModel { ProgressViewModel(it) }
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    // Sessions per week for the last 8 weeks, oldest first.
    val weekCounts = (7 downTo 0).map { back ->
        val start = weekStart.minusWeeks(back.toLong())
        val end = start.plusDays(7)
        logs.count {
            val d = it.dateMillis.toLocalDate()
            !d.isBefore(start) && d.isBefore(end)
        }
    }
    val weekLabels = (7 downTo 0).map { back -> weekStart.minusWeeks(back.toLong()).dayOfMonth.toString() }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle(title = "Progress", subtitle = "YOUR GOLF FITNESS TREND") }

        if (sessions.isEmpty()) {
            item {
                SurfaceCard {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.QueryStats, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Text("No baseline yet", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Take the fitness test once to set your baseline, then retest every four weeks to chart your growth.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onTakeTest, shape = MaterialTheme.shapes.large) { Text("Start fitness test") }
                    }
                }
            }
        } else {
            val baseline = sessions.first()
            val latest = sessions.last()
            val overallPoints = sessions.mapNotNull { s -> s.overall?.let { s to it } }
            val latestOverall = latest.overall
            val baselineOverall = baseline.overall

            item {
                SurfaceCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                SectionTitle("FairwayFit score")
                                Text(
                                    latestOverall?.toString() ?: "—",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (sessions.size > 1 && latestOverall != null && baselineOverall != null) {
                                val delta = latestOverall - baselineOverall
                                Tag(
                                    (if (delta >= 0) "+$delta" else "$delta") + " since baseline",
                                    container = if (delta >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                    content = if (delta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            } else {
                                Tag("Baseline")
                            }
                        }
                        LineChart(
                            values = overallPoints.map { it.second.toFloat() },
                            xLabels = overallPoints.map { it.first.dateMillis.toLocalDate().short() },
                            modifier = Modifier.fillMaxWidth().height(190.dp)
                        )
                    }
                }
            }

            item {
                SurfaceCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionTitle("Categories: baseline vs latest")
                        Category.entries.forEach { cat ->
                            val now = Scoring.categoryScore(latest.values, cat)
                            val then = Scoring.categoryScore(baseline.values, cat)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    Text(now?.toString() ?: "—", style = MaterialTheme.typography.titleSmall, color = categoryColor(cat))
                                }
                                ScoreBar(
                                    fraction = (then ?: 0) / 100f,
                                    color = MaterialTheme.colorScheme.outline,
                                    height = 5.dp
                                )
                                ScoreBar(fraction = (now ?: 0) / 100f, color = categoryColor(cat))
                            }
                        }
                    }
                }
            }

            item { SectionTitle("Each test") }
            items(Content.tests, key = { it.key }) { t ->
                val series = sessions.mapNotNull { s -> s.values[t.key] }
                if (series.isEmpty()) return@items
                val first = series.first()
                val last = series.last()
                val delta = last - first
                SurfaceCard {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(t.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(t.format(last), style = MaterialTheme.typography.headlineSmall)
                            if (series.size > 1) {
                                val improved = delta >= 0
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (improved) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                        contentDescription = null,
                                        tint = if (improved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    val pct = if (first != 0.0) " (${(abs(delta) / abs(first) * 100).roundToInt()}%)" else ""
                                    Text(
                                        (if (improved) "+" else "-") + t.format(abs(delta)) + pct,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (improved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                Text("Baseline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Sparkline(
                            values = series.map { it.toFloat() },
                            modifier = Modifier.width(110.dp).height(48.dp),
                            color = categoryColor(t.category)
                        )
                    }
                }
            }

            item {
                val daysSince = ChronoUnit.DAYS.between(latest.dateMillis.toLocalDate(), today).toInt()
                SurfaceCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("Next retest")
                        Text(
                            if (daysSince >= 28) "You're due for a retest."
                            else "Retest in ${28 - daysSince} days (about every 4 weeks).",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(onClick = onTakeTest, shape = MaterialTheme.shapes.large) { Text("Retest now") }
                    }
                }
            }
        }

        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Workouts per week")
                    BarChart(
                        values = weekCounts,
                        labels = weekLabels,
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                    Text(
                        "${logs.size} workouts completed in total · labels show the Monday of each week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
