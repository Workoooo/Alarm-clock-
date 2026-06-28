package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Alarm
import com.example.ui.theme.*
import com.example.ui.viewmodel.AlarmViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmClockScreen(
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val nextAlarmText by viewModel.nextAlarmTimeText.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }

    // Real-time Clock State
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    // Request Notification permission for Android 13+
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Real-time time updater
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        while (true) {
            val calendar = Calendar.getInstance()
            currentTimeString = timeFormat.format(calendar.time)
            currentDateString = dateFormat.format(calendar.time)
            kotlinx.coroutines.delay(1000)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepSpaceBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NeonCyan,
                contentColor = DeepSpaceBackground,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("add_alarm_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Alarm",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header / Clock Section
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "COSMIC ALARM",
                style = MaterialTheme.typography.labelLarge,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Realtime Glowing Clock Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SpaceSurface, SpaceCardBg)
                        )
                    )
                    .border(1.dp, NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentTimeString.ifEmpty { "00:00:00 AM" },
                        style = MaterialTheme.typography.displayLarge,
                        color = NeonCyan,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.shadow(
                            elevation = 0.dp,
                            shape = RoundedCornerShape(0.dp),
                            ambientColor = NeonCyan,
                            spotColor = NeonCyan
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentDateString.ifEmpty { "Loading date..." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = SlateGrey,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Next Alarm Info Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SpaceCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Next Alarm Status",
                        tint = GlowingAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = nextAlarmText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OffWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Alarms list title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scheduled Alarms",
                    style = MaterialTheme.typography.titleLarge,
                    color = OffWhite
                )
                Text(
                    text = "${alarms.size} total",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SlateGrey
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Empty state or LazyColumn
            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "No Alarms",
                            tint = SlateGrey.copy(alpha = 0.4f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Alarms Scheduled",
                            style = MaterialTheme.typography.titleLarge,
                            color = SlateGrey,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the '+' button to schedule a customized alarm alert.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SlateGrey.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("alarms_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { viewModel.toggleAlarm(alarm) },
                            onEdit = { alarmToEdit = alarm },
                            onDelete = { viewModel.deleteAlarm(alarm) }
                        )
                    }
                }
            }
        }

        // Add Alarm Dialog
        if (showAddDialog) {
            AlarmEditDialog(
                alarm = null,
                onDismiss = { showAddDialog = false },
                onSave = { hour, minute, label, isVibrate, isSound, daysOfWeek, snooze ->
                    viewModel.addAlarm(hour, minute, label, isVibrate, isSound, daysOfWeek, snooze)
                    showAddDialog = false
                }
            )
        }

        // Edit Alarm Dialog
        if (alarmToEdit != null) {
            AlarmEditDialog(
                alarm = alarmToEdit,
                onDismiss = { alarmToEdit = null },
                onSave = { hour, minute, label, isVibrate, isSound, daysOfWeek, snooze ->
                    alarmToEdit?.let { existing ->
                        val updated = existing.copy(
                            hour = hour,
                            minute = minute,
                            label = label,
                            isVibrate = isVibrate,
                            isSound = isSound,
                            daysOfWeek = daysOfWeek,
                            snoozeDurationMinutes = snooze
                        )
                        viewModel.updateAlarm(updated)
                    }
                    alarmToEdit = null
                }
            )
        }
    }
}

@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("alarm_card_${alarm.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) SpaceSurface else SpaceSurface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (alarm.isEnabled) NeonCyan.copy(alpha = 0.2f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Time Text
                Row(verticalAlignment = Alignment.Bottom) {
                    val formattedTime = alarm.getFormattedTime()
                    val timePart = formattedTime.substring(0, 5)
                    val amPmPart = formattedTime.substring(6)

                    Text(
                        text = timePart,
                        style = MaterialTheme.typography.displayMedium,
                        color = if (alarm.isEnabled) OffWhite else SlateGrey,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amPmPart,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (alarm.isEnabled) NeonCyan else SlateGrey,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Label if present
                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (alarm.isEnabled) OffWhite.copy(alpha = 0.9f) else SlateGrey.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Days summary + Snooze/Vibrate mini indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = alarm.getDaysFormatted(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (alarm.isEnabled) GlowPrimary else SlateGrey.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Normal
                    )

                    if (alarm.isEnabled) {
                        Text(text = "•", color = SlateGrey.copy(alpha = 0.4f))
                        if (alarm.isVibrate) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Vibration Enabled",
                                tint = SlateGrey,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (alarm.isSound) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Sound Enabled",
                                tint = SlateGrey,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Controls on the right: Switch and Quick-Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active Switch
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DeepSpaceBackground,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = SlateGrey,
                        uncheckedTrackColor = SpaceCardBg
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_alarm_${alarm.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Alarm",
                        tint = CoralRed.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmEditDialog(
    alarm: Alarm?,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, label: String, isVibrate: Boolean, isSound: Boolean, daysOfWeek: String, snooze: Int) -> Unit
) {
    val isEdit = alarm != null
    
    var hourText by remember { mutableStateOf(alarm?.hour?.toString() ?: "08") }
    var minuteText by remember { mutableStateOf(alarm?.minute?.toString() ?: "30") }
    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var isVibrate by remember { mutableStateOf(alarm?.isVibrate ?: true) }
    var isSound by remember { mutableStateOf(alarm?.isSound ?: true) }
    var snoozeDuration by remember { mutableStateOf(alarm?.snoozeDurationMinutes ?: 5) }

    // Repetition days: map representing Mon(1) to Sun(7).
    var selectedDays by remember {
        mutableStateOf(alarm?.getDaysList()?.toSet() ?: emptySet())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(4.dp)
                .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = SpaceSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEdit) "Configure Alarm" else "Schedule Alarm",
                    style = MaterialTheme.typography.titleLarge,
                    color = OffWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Time picker fields
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hour TextField
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            if (filtered.length <= 2) {
                                hourText = filtered
                            }
                        },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .width(80.dp)
                            .testTag("input_hour"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = SpaceCardBg,
                            focusedContainerColor = SpaceCardBg,
                            unfocusedContainerColor = SpaceCardBg
                        )
                    )

                    Text(
                        text = ":",
                        color = OffWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Minute TextField
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            if (filtered.length <= 2) {
                                minuteText = filtered
                            }
                        },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .width(80.dp)
                            .testTag("input_minute"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = SpaceCardBg,
                            focusedContainerColor = SpaceCardBg,
                            unfocusedContainerColor = SpaceCardBg
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter 24-hour time format (00:00 to 23:59)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGrey
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Label OutlinedTextField
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label", color = SlateGrey) },
                    placeholder = { Text("e.g. Rise & Shine", color = SlateGrey.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SpaceCardBg,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = SlateGrey,
                        focusedTextColor = OffWhite,
                        unfocusedTextColor = OffWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_label")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Days of week selector
                Text(
                    text = "Repeat Days",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OffWhite,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 1..7) {
                        val isSelected = selectedDays.contains(i)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) NeonCyan else SpaceCardBg)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else SlateGrey.copy(alpha = 0.3f),
                                    CircleShape
                                )
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        selectedDays - i
                                    } else {
                                        selectedDays + i
                                    }
                                }
                                .testTag("day_button_$i"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayLabels[i - 1],
                                color = if (isSelected) DeepSpaceBackground else OffWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sound & Vibration Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Sound", tint = SlateGrey)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ring Alarm Sound", color = OffWhite, style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(
                        checked = isSound,
                        onCheckedChange = { isSound = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpaceBackground,
                            checkedTrackColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("dialog_sound_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = "Vibration", tint = SlateGrey)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vibrate", color = OffWhite, style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(
                        checked = isVibrate,
                        onCheckedChange = { isVibrate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepSpaceBackground,
                            checkedTrackColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("dialog_vibrate_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Snooze Interval Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Snooze Duration", color = OffWhite, style = MaterialTheme.typography.bodyLarge)
                        Text("$snoozeDuration Min", color = NeonCyan, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = snoozeDuration.toFloat(),
                        onValueChange = { snoozeDuration = it.toInt() },
                        valueRange = 1f..30f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = SpaceCardBg
                        ),
                        modifier = Modifier.testTag("dialog_snooze_slider")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons (Cancel / Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, SlateGrey.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateGrey),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dialog_cancel_button")
                    ) {
                        Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            var h = hourText.toIntOrNull() ?: 8
                            var m = minuteText.toIntOrNull() ?: 30
                            
                            // Bounds safety checks
                            h = h.coerceIn(0, 23)
                            m = m.coerceIn(0, 59)

                            val daysStr = selectedDays.sorted().joinToString(",")

                            onSave(h, m, label, isVibrate, isSound, daysStr, snoozeDuration)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = DeepSpaceBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dialog_save_button")
                    ) {
                        Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
