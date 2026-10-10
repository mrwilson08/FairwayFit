package com.fairwayfit.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fairwayfit.app.AppContainer
import com.fairwayfit.app.FairwayFitApp
import com.fairwayfit.app.domain.FitnessTest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Creates (or reuses) a ViewModel scoped to the current navigation destination. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    noinline create: (AppContainer) -> VM
): VM {
    val container = (LocalContext.current.applicationContext as FairwayFitApp).container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val longDate = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())

fun LocalDate.short(): String = format(shortDate)
fun LocalDate.long(): String = format(longDate)

/** Formats a test value for display, e.g. "45 sec", "-1.5 in". */
fun FitnessTest.format(value: Double, withUnit: Boolean = true): String {
    val number = if (step % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)
    return if (withUnit) "$number ${unit}" else number
}

fun formatDuration(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
