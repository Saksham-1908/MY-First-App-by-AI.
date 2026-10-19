package com.example.ui.schedule

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskItem
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Rose400
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onAddTaskForHour: (LocalDate, Int) -> Unit,
    onEditTask: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Timetable",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.goToToday() }) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Today",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTaskForHour(uiState.selectedDate, 9) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("schedule_add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // View Mode Selector (Day vs Week)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = uiState.viewMode == ScheduleViewMode.DAY,
                    onClick = { viewModel.setViewMode(ScheduleViewMode.DAY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Day View")
                }
                SegmentedButton(
                    selected = uiState.viewMode == ScheduleViewMode.WEEK,
                    onClick = { viewModel.setViewMode(ScheduleViewMode.WEEK) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Week View")
                }
            }

            // Date Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.goToPrevious() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                }

                Text(
                    text = uiState.formattedDateHeader,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = { viewModel.goToNext() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                }
            }

            // Main Content: Day vs Week
            if (uiState.viewMode == ScheduleViewMode.DAY) {
                DayTimelineView(
                    selectedDate = uiState.selectedDate,
                    timelineSlots = uiState.timelineSlots,
                    onAddTaskForHour = { hour -> onAddTaskForHour(uiState.selectedDate, hour) },
                    onEditTask = onEditTask,
                    onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onDuplicateTask = { viewModel.duplicateTaskToTomorrow(it) },
                    onShiftTask = { task, delta -> viewModel.shiftTaskTime(task, delta) }
                )
            } else {
                WeekTimetableView(
                    weekDays = uiState.weekDays,
                    onSelectDay = { viewModel.selectDate(it) },
                    onAddTaskForDay = { date -> onAddTaskForHour(date, 9) },
                    onEditTask = onEditTask,
                    onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                    onDeleteTask = { viewModel.deleteTask(it) }
                )
            }
        }
    }
}

@Composable
private fun DayTimelineView(
    selectedDate: LocalDate,
    timelineSlots: List<DayTimelineSlot>,
    onAddTaskForHour: (Int) -> Unit,
    onEditTask: (Long) -> Unit,
    onToggleComplete: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onDuplicateTask: (TaskItem) -> Unit,
    onShiftTask: (TaskItem, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(timelineSlots, key = { it.hour }) { slot ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Time Marker Column
                Column(
                    modifier = Modifier.width(56.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = slot.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(end = 8.dp, top = 6.dp)
                    )
                }

                // Vertical Divider Line & Task Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (slot.tasks.isEmpty()) {
                        // Empty Slot Affordance: tap to schedule task for this hour
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddTaskForHour(slot.hour) }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "+ Tap to schedule at ${slot.label}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        }
                    } else {
                        // Scheduled Task Cards for this slot
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            slot.tasks.forEach { task ->
                                TimelineTaskCard(
                                    task = task,
                                    onClick = { onEditTask(task.id) },
                                    onToggleComplete = { onToggleComplete(task) },
                                    onDelete = { onDeleteTask(task) },
                                    onDuplicate = { onDuplicateTask(task) },
                                    onShiftHour = { delta -> onShiftTask(task, delta) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun TimelineTaskCard(
    task: TaskItem,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onShiftHour: (Int) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val categoryColor = try {
        Color(android.graphics.Color.parseColor(task.categoryColorHex))
    } catch (e: Exception) {
        Indigo400
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                categoryColor.copy(alpha = 0.12f)
            }
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant
                else categoryColor.copy(alpha = 0.4f)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category accent pill/bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (task.isCompleted) MaterialTheme.colorScheme.outline else categoryColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${task.startTime} - ${task.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = categoryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    if (task.actualMinutesSpent > 0) {
                        Text(
                            text = "(${task.formattedActualDuration()} logged)",
                            style = MaterialTheme.typography.labelSmall.copy(color = Cyan400)
                        )
                    }
                }
            }

            // Quick actions & overflow menu
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Toggle Complete",
                    tint = if (task.isCompleted) Emerald400 else MaterialTheme.colorScheme.outline
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Task") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate to Tomorrow") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delay 1 Hour (+1h)") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShiftHour(1)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Advance 1 Hour (-1h)") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShiftHour(-1)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete Task", color = Rose400) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Rose400) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekTimetableView(
    weekDays: List<WeekDayData>,
    onSelectDay: (LocalDate) -> Unit,
    onAddTaskForDay: (LocalDate) -> Unit,
    onEditTask: (Long) -> Unit,
    onToggleComplete: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Week Header Selector Row (Monday to Sunday)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(weekDays) { day ->
                val isSelected = day.isSelected
                val isToday = day.isToday

                Surface(
                    onClick = { onSelectDay(day.date) },
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.width(46.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.dayName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = day.dayNumber,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Task count dot
                        if (day.tasks.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else Cyan400)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Selected Day's Task List in Week view
        val currentDay = weekDays.find { it.isSelected } ?: weekDays.firstOrNull()

        if (currentDay != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentDay.dayName}, ${currentDay.date} (${currentDay.tasks.size} tasks)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedButton(
                    onClick = { onAddTaskForDay(currentDay.date) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (currentDay.tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No tasks for ${currentDay.dayName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onAddTaskForDay(currentDay.date) }
                        ) {
                            Text("+ Add Task for ${currentDay.dayName}")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentDay.tasks, key = { it.id }) { task ->
                        TimelineTaskCard(
                            task = task,
                            onClick = { onEditTask(task.id) },
                            onToggleComplete = { onToggleComplete(task) },
                            onDelete = { onDeleteTask(task) },
                            onDuplicate = { },
                            onShiftHour = { }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}
