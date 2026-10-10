package com.fairwayfit.app.ui.train

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairwayfit.app.AppContainer
import com.fairwayfit.app.data.AppSettings
import com.fairwayfit.app.domain.Category
import com.fairwayfit.app.domain.Content
import com.fairwayfit.app.domain.Plan
import com.fairwayfit.app.domain.estimatedMinutes
import com.fairwayfit.app.domain.prescription
import com.fairwayfit.app.ui.appViewModel
import com.fairwayfit.app.ui.components.CategoryDot
import com.fairwayfit.app.ui.components.DetailHeader
import com.fairwayfit.app.ui.components.ListRow
import com.fairwayfit.app.ui.components.NumberedSteps
import com.fairwayfit.app.ui.components.ScreenTitle
import com.fairwayfit.app.ui.components.SectionTitle
import com.fairwayfit.app.ui.components.SurfaceCard
import com.fairwayfit.app.ui.components.Tag
import com.fairwayfit.app.ui.components.categoryColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class TrainViewModel(c: AppContainer) : ViewModel() {
    val settings = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
}

@Composable
fun TrainScreen(onOpenWorkout: (String) -> Unit, onOpenExercise: (String) -> Unit) {
    val vm = appViewModel { TrainViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf<Category?>(null) }

    val exercises = Content.exercises.filter { filter == null || it.category == filter }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenTitle(title = "Train", subtitle = "STABILITY · SPEED · MOBILITY") }
        item { WeekStrip(settings.days) }
        item { SectionTitle("Workouts") }
        items(Content.workouts, key = { it.id }) { w ->
            ListRow(
                title = w.name,
                subtitle = "${w.tagline} · ${w.estimatedMinutes} min",
                category = w.category,
                badge = w.badge,
                onClick = { onOpenWorkout(w.id) }
            )
        }
        item { Spacer(Modifier.height(6.dp)) }
        item { SectionTitle("Exercise library") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                }
                items(Category.entries) { cat ->
                    FilterChip(selected = filter == cat, onClick = { filter = cat }, label = { Text(cat.label) })
                }
            }
        }
        items(exercises, key = { it.id }) { ex ->
            ListRow(
                title = ex.name,
                subtitle = "${ex.focus} · ${ex.equipment}",
                category = ex.category,
                onClick = { onOpenExercise(ex.id) }
            )
        }
    }
}

@Composable
private fun WeekStrip(days: Set<Int>) {
    val today = LocalDate.now().dayOfWeek
    SurfaceCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("YOUR WEEK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DayOfWeek.entries.forEach { day ->
                    val workout = Plan.sessionFor(days, LocalDate.now().with(day))
                    val isToday = day == today
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            Modifier
                                .size(width = 40.dp, height = 52.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (workout != null) categoryColor(workout.category).copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                                )
                                .then(
                                    if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (workout != null) {
                                Text(
                                    workout.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryColor(workout.category),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text("rest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutDetailScreen(workoutId: String, onBack: () -> Unit, onStart: () -> Unit, onOpenExercise: (String) -> Unit) {
    val workout = Content.workout(workoutId)
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { DetailHeader(title = "Workout", onBack = onBack, modifier = Modifier.padding(bottom = 4.dp)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(workout.name, style = MaterialTheme.typography.headlineLarge)
                    Text(workout.tagline, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag("${workout.estimatedMinutes} min", icon = Icons.Rounded.Timer)
                        Tag("${workout.items.size} exercises")
                        Tag("${workout.restSeconds}s rest")
                    }
                    Text(workout.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item { SectionTitle("Exercises", Modifier.padding(top = 8.dp)) }
            items(workout.items.size) { i ->
                val item = workout.items[i]
                val ex = Content.exercise(item.exerciseId)
                SurfaceCard(onClick = { onOpenExercise(ex.id) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.titleMedium)
                            Text(ex.equipment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(item.prescription(), style = MaterialTheme.typography.titleSmall, color = categoryColor(ex.category))
                    }
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start workout")
            }
        }
    }
}

@Composable
fun ExerciseDetailScreen(exerciseId: String, onBack: () -> Unit) {
    val ex = Content.exercise(exerciseId)
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { DetailHeader(title = "Exercise", onBack = onBack) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryDot(ex.category)
                    Spacer(Modifier.width(8.dp))
                    Text(ex.category.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = categoryColor(ex.category))
                }
                Text(ex.name, style = MaterialTheme.typography.headlineLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag(ex.focus)
                    Tag(ex.equipment)
                }
            }
        }
        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionTitle("How to")
                    NumberedSteps(ex.steps)
                }
            }
        }
        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("Coaching cue")
                    Text("“${ex.cue}”", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("Why it helps your golf")
                    Text(ex.why, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
