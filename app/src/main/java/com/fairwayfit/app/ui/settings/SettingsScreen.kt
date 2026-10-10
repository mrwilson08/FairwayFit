package com.fairwayfit.app.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fairwayfit.app.AppContainer
import com.fairwayfit.app.data.AppSettings
import com.fairwayfit.app.data.health.HealthState
import com.fairwayfit.app.reminders.ReminderScheduler
import com.fairwayfit.app.ui.appViewModel
import com.fairwayfit.app.ui.components.ScreenTitle
import com.fairwayfit.app.ui.components.SectionTitle
import com.fairwayfit.app.ui.components.SurfaceCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val settings = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    val sync = c.sync.status
    val health = c.health.state
    val healthPermissions: Set<String> get() = c.health.permissions

    val pebbleStatus = c.pebble.status

    private fun change(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val updated = c.settings.update(transform)
            ReminderScheduler.apply(c.appContext, updated)
            // The watch's timeline pin follows the reminder days and time.
            c.pebble.syncSessionPin(updated)
        }
    }

    fun setPebbleOn(on: Boolean) {
        viewModelScope.launch {
            // Remove the pin while the setting is still on; syncSessionPin is a no-op once it is off.
            if (!on) c.pebble.removeSessionPin()
            val updated = c.settings.update { it.copy(pebbleOn = on) }
            if (on) c.pebble.syncSessionPin(updated)
        }
    }

    fun sendPebbleTest() = c.pebble.sendTest()

    fun setRemindersOn(on: Boolean) = change { it.copy(remindersOn = on) }
    fun setTime(hour: Int, minute: Int) = change { it.copy(hour = hour, minute = minute) }
    fun toggleDay(day: Int) = change { s ->
        s.copy(days = if (day in s.days) s.days - day else s.days + day)
    }

    fun refreshHealth() {
        viewModelScope.launch { c.health.refresh() }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch { c.sync.signIn(email.trim(), password) }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch { c.sync.register(email.trim(), password) }
    }

    fun signOut() = c.sync.signOut()

    fun syncNow() {
        viewModelScope.launch { c.sync.syncNow() }
    }
}

@Composable
fun SettingsScreen() {
    val vm = appViewModel { SettingsViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val health by vm.health.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.refreshHealth() }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setRemindersOn(true)
    }
    val healthLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { vm.refreshHealth() }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle(title = "Settings", subtitle = "REMINDERS · WATCH DATA · SYNC") }

        // ------------------------------------------------------------ reminders
        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Workout reminders", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "A morning nudge on your training days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.remindersOn,
                            onCheckedChange = { on ->
                                val needsPermission = on && Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                    PackageManager.PERMISSION_GRANTED
                                if (needsPermission) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    vm.setRemindersOn(on)
                                }
                            }
                        )
                    }

                    SectionTitle("Training days")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = day.value in settings.days,
                                onClick = { vm.toggleDay(day.value) },
                                label = { Text(day.getDisplayName(TextStyle.NARROW, Locale.getDefault())) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    SectionTitle("Reminder time")
                    val timeText = LocalTime.of(settings.hour, settings.minute).format(DateTimeFormatter.ofPattern("h:mm a"))
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m -> vm.setTime(h, m) },
                                settings.hour, settings.minute, false
                            ).show()
                        },
                        shape = MaterialTheme.shapes.large
                    ) { Text(timeText) }
                    Text(
                        "Training days also decide your weekly plan and goal. With three days you rotate Stability, Speed & Power and Total Golf Circuit; a fourth adds Mobility Reset. Android may deliver reminders a few minutes late to save battery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---------------------------------------------------------------- pebble
        item {
            val pebbleStatus by vm.pebbleStatus.collectAsStateWithLifecycle()
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pebble watch", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Workout on your wrist, plus session reminders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = settings.pebbleOn, onCheckedChange = vm::setPebbleOn)
                    }
                    if (settings.pebbleOn) {
                        Text(pebbleStatus, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = vm::sendPebbleTest, shape = MaterialTheme.shapes.large) {
                            Text("Send test to watch")
                        }
                    }
                    Text(
                        "Needs the Core Devices Pebble app on this phone and the FairwayFit watchapp installed on the watch (see the README). On the watch: SELECT starts a hold or moves on, UP adds 15 seconds of rest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --------------------------------------------------------- health connect
        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Health Connect", style = MaterialTheme.typography.titleMedium)
                    val (message, canConnect) = when (health) {
                        HealthState.Loading -> "Checking…" to false
                        HealthState.Unavailable -> "Not available on this device." to false
                        HealthState.NeedsPermission -> "Not connected. Connect to read weight, sleep, resting heart rate and steps." to true
                        is HealthState.Ready -> "Connected. Data shows on the Home screen." to false
                        is HealthState.Error -> (health as HealthState.Error).message to false
                    }
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (canConnect) {
                        Button(onClick = { healthLauncher.launch(vm.healthPermissions) }, shape = MaterialTheme.shapes.large) {
                            Text("Connect")
                        }
                    }
                    Text(
                        "To bring in Garmin data: open Garmin Connect, go to Settings → Connected Apps → Health Connect, and enable what you want to share.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ------------------------------------------------------------ cloud sync
        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cloud sync", style = MaterialTheme.typography.titleMedium)
                    if (!sync.available) {
                        Text(
                            "Not configured. Add your Firebase google-services.json to the app folder and rebuild (see the README). Your data is safe on this device in the meantime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (sync.email != null) {
                        Text("Signed in as ${sync.email}", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = vm::syncNow, enabled = !sync.busy, shape = MaterialTheme.shapes.large) { Text("Sync now") }
                            OutlinedButton(onClick = vm::signOut, enabled = !sync.busy, shape = MaterialTheme.shapes.large) { Text("Sign out") }
                        }
                    } else {
                        var email by remember { mutableStateOf("") }
                        var password by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        val ready = email.isNotBlank() && password.length >= 6 && !sync.busy
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { vm.signIn(email, password) }, enabled = ready, shape = MaterialTheme.shapes.large) { Text("Sign in") }
                            OutlinedButton(onClick = { vm.register(email, password) }, enabled = ready, shape = MaterialTheme.shapes.large) { Text("Create account") }
                        }
                        Text("Passwords need at least 6 characters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (sync.message.isNotEmpty() && sync.available) {
                        Text(sync.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ------------------------------------------------------------------ about
        item {
            SurfaceCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "FairwayFit uses US units (inches, pounds). Fitness-test scores compare you with generic benchmarks for active adults, so the most useful number is your own change over time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "This app is general fitness guidance, not medical advice. Check with your doctor before starting a new exercise program and stop if anything hurts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
