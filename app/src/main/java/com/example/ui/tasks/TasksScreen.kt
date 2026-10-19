package com.example.ui.tasks

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.data.model.TaskItem
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Rose400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onNavigateToAddTask: () -> Unit,
    onNavigateToEditTask: (Long) -> Unit,
    onStartTimer: (TaskItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Tasks (${uiState.totalCount})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort tasks")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Time (Earliest first)") },
                                onClick = {
                                    viewModel.setSortOption(TaskSortOption.TIME)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Priority (High to Low)") },
                                onClick = {
                                    viewModel.setSortOption(TaskSortOption.PRIORITY)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Title (A - Z)") },
                                onClick = {
                                    viewModel.setSortOption(TaskSortOption.TITLE)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("tasks_fab_add")
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
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search tasks, categories...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Primary Filter Chips (All, Today, Upcoming, Completed)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedFilter == TaskFilterOption.ALL,
                        onClick = { viewModel.setFilter(TaskFilterOption.ALL) },
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedFilter == TaskFilterOption.TODAY,
                        onClick = { viewModel.setFilter(TaskFilterOption.TODAY) },
                        label = { Text("Today") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedFilter == TaskFilterOption.UPCOMING,
                        onClick = { viewModel.setFilter(TaskFilterOption.UPCOMING) },
                        label = { Text("Upcoming") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedFilter == TaskFilterOption.COMPLETED,
                        onClick = { viewModel.setFilter(TaskFilterOption.COMPLETED) },
                        label = { Text("Completed") }
                    )
                }
            }

            // Category Filter Chips
            if (uiState.categories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.categories) { cat ->
                        val isSelected = uiState.selectedCategory?.equals(cat.name, ignoreCase = true) == true
                        val color = try {
                            Color(android.graphics.Color.parseColor(cat.colorHex))
                        } catch (e: Exception) {
                            Indigo400
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(cat.name) },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Task List
            if (uiState.filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "No matching tasks found" else "No tasks in this list",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to create a new task.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredTasks, key = { it.id }) { task ->
                        TaskListItem(
                            task = task,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                            onClick = { onNavigateToEditTask(task.id) },
                            onStartTimer = { onStartTimer(task) },
                            onDuplicate = { viewModel.duplicateTask(task) },
                            onDelete = { viewModel.deleteTask(task) }
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

@Composable
private fun TaskListItem(
    task: TaskItem,
    onToggleCompletion: () -> Unit,
    onClick: () -> Unit,
    onStartTimer: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val categoryColor = try {
        Color(android.graphics.Color.parseColor(task.categoryColorHex))
    } catch (e: Exception) {
        Indigo400
    }

    val priorityColor = when (task.priority) {
        "High" -> Rose400
        "Medium" -> Amber500
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleCompletion() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Emerald400,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                // Action Menu
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
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
                            text = { Text("Start Timer") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onStartTimer()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Task") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Task") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
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

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row: Date, Time, Category, Priority, Logged time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = categoryColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Priority Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(priorityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.priority,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = priorityColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Schedule Time
                Text(
                    text = "${task.date} • ${task.startTime} - ${task.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                // Tracked vs Planned Duration
                if (task.actualMinutesSpent > 0) {
                    Text(
                        text = "${task.actualMinutesSpent}m / ${task.calculateDurationMinutes()}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Cyan400,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}
