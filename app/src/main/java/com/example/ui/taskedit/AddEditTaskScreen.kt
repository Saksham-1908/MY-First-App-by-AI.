package com.example.ui.taskedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CategoryColors
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Rose400
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    viewModel: AddEditTaskViewModel,
    taskId: Long?,
    initialDate: LocalDate? = null,
    initialHour: Int? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        viewModel.initializeWithTask(taskId, initialDate, initialHour)
    }

    val durationMins = Duration.between(uiState.startTime, uiState.endTime).toMinutes().toInt().let {
        if (it <= 0) it + (24 * 60) else it
    }
    val formattedDuration = "${durationMins / 60}h ${durationMins % 60}m"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.taskId != null && uiState.taskId!! > 0) "Edit Task" else "New Task",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Name
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Task Name *") },
                placeholder = { Text("e.g. Physics Problem Set, Sprint Review") },
                singleLine = true,
                isError = uiState.errorMessage != null,
                supportingText = {
                    if (uiState.errorMessage != null) {
                        Text(text = uiState.errorMessage!!, color = Rose400)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                shape = RoundedCornerShape(14.dp)
            )

            // Description (Optional)
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.onDescriptionChange(it) },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Add key objectives, resources, or links") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Date & Duration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Date",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Change")
                        }
                    }

                    // Times: Start Time & End Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Time
                        Card(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Start Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // End Time
                        Card(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "End Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.endTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Duration Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Planned Duration: $formattedDuration",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Category Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Custom Category")
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = uiState.category.equals(category.name, ignoreCase = true)
                        val color = try {
                            Color(android.graphics.Color.parseColor(category.colorHex))
                        } catch (e: Exception) {
                            Indigo400
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onCategorySelect(category) },
                            label = { Text(category.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        )
                    }
                }
            }

            // Priority Selection (Low, Medium, High)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Low", "Medium", "High").forEach { priority ->
                        val isSelected = uiState.priority == priority
                        val pColor = when (priority) {
                            "High" -> Rose400
                            "Medium" -> Cyan400
                            else -> Indigo400
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onPriorityChange(priority) },
                            label = { Text(priority) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            // Reminder & Repeat Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Reminder Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reminder Notification",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Alert before start time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = uiState.hasReminder,
                            onCheckedChange = { viewModel.onReminderToggle(it) }
                        )
                    }

                    // Repeat Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("None", "Daily", "Weekdays", "Weekly").forEach { repeatOption ->
                                val isSelected = uiState.repeatType == repeatOption
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onRepeatChange(repeatOption) },
                                    label = { Text(repeatOption, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text("Notes & Checklists (Optional)") },
                placeholder = { Text("Jot down quick reminders or checklist items...") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveTask(onComplete = onNavigateBack) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (uiState.taskId != null && uiState.taskId!! > 0) "Update Task" else "Create Task",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Date Picker Dialog
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = uiState.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            viewModel.onDateChange(picked)
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Start Time Picker Dialog
        if (showStartTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = uiState.startTime.hour,
                initialMinute = uiState.startTime.minute,
                is24Hour = true
            )
            TimePickerDialog(
                title = "Select Start Time",
                onDismiss = { showStartTimePicker = false },
                onConfirm = {
                    viewModel.onStartTimeChange(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showStartTimePicker = false
                }
            ) {
                TimePicker(state = timePickerState)
            }
        }

        // End Time Picker Dialog
        if (showEndTimePicker) {
            val timePickerState = rememberTimePickerState(
                initialHour = uiState.endTime.hour,
                initialMinute = uiState.endTime.minute,
                is24Hour = true
            )
            TimePickerDialog(
                title = "Select End Time",
                onDismiss = { showEndTimePicker = false },
                onConfirm = {
                    viewModel.onEndTimeChange(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showEndTimePicker = false
                }
            ) {
                TimePicker(state = timePickerState)
            }
        }

        // Custom Category Dialog
        if (showAddCategoryDialog) {
            var newCategoryName by remember { mutableStateOf("") }
            var selectedColorHex by remember { mutableStateOf("#6366F1") }

            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Add Custom Category") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Category Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Color", style = MaterialTheme.typography.labelMedium)

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(CategoryColors) { color ->
                                val hex = String.format("#%06X", 0xFFFFFF and color.value.toInt())
                                val isChosen = selectedColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorHex = hex }
                                        .then(
                                            if (isChosen) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChosen) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addCustomCategory(newCategoryName, selectedColorHex)
                            showAddCategoryDialog = false
                        },
                        enabled = newCategoryName.isNotBlank()
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
