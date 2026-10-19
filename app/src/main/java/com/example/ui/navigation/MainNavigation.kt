package com.example.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.TimeTrackRepository
import com.example.ui.ai.AiPlannerViewModel
import com.example.ui.ai.AiScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.schedule.ScheduleScreen
import com.example.ui.schedule.ScheduleViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.statistics.StatisticsScreen
import com.example.ui.statistics.StatisticsViewModel
import com.example.ui.taskedit.AddEditTaskScreen
import com.example.ui.taskedit.AddEditTaskViewModel
import com.example.ui.tasks.TasksScreen
import com.example.ui.tasks.TasksViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Schedule : Screen("schedule", "Schedule", Icons.Default.CalendarMonth)
    data object Tasks : Screen("tasks", "Tasks", Icons.Default.CheckCircle)
    data object Statistics : Screen("statistics", "Stats", Icons.Default.BarChart)
    data object Ai : Screen("ai", "AI", Icons.Default.AutoAwesome)
    data object Settings : Screen("settings")
    data object AddEditTask : Screen("add_edit_task?taskId={taskId}&date={date}&hour={hour}") {
        fun createRoute(taskId: Long? = null, date: String? = null, hour: Int? = null): String {
            val tId = taskId ?: -1L
            val d = date ?: ""
            val h = hour ?: -1
            return "add_edit_task?taskId=$tId&date=$d&hour=$h"
        }
    }
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Schedule,
    Screen.Tasks,
    Screen.Statistics,
    Screen.Ai
)

@Composable
fun TimeTrackNavApp(
    repository: TimeTrackRepository,
    navController: NavHostController = rememberNavController()
) {
    val coroutineScope = rememberCoroutineScope()
    val isOnboardingCompleted by repository.preferencesRepository.isOnboardingCompleted.collectAsState(initial = null)

    // Ask for POST_NOTIFICATIONS on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (isOnboardingCompleted == null) {
        // Waiting for DataStore initial read
        return
    }

    val startDestination = if (isOnboardingCompleted == true) Screen.Home.route else Screen.Onboarding.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in BottomNavItems.map { it.route }

    // Shared ViewModels
    val homeViewModel: HomeViewModel = viewModel(initializer = { HomeViewModel(repository) })
    val scheduleViewModel: ScheduleViewModel = viewModel(initializer = { ScheduleViewModel(repository) })
    val tasksViewModel: TasksViewModel = viewModel(initializer = { TasksViewModel(repository) })
    val statisticsViewModel: StatisticsViewModel = viewModel(initializer = { StatisticsViewModel(repository) })
    val aiViewModel: AiPlannerViewModel = viewModel(initializer = { AiPlannerViewModel(repository) })
    val settingsViewModel: SettingsViewModel = viewModel(initializer = { SettingsViewModel(repository) })

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    BottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                item.icon?.let {
                                    Icon(imageVector = it, contentDescription = item.title)
                                }
                            },
                            label = { Text(item.title) },
                            modifier = Modifier.testTag("nav_tab_${item.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { androidx.compose.animation.fadeIn(tween(200)) },
            exitTransition = { androidx.compose.animation.fadeOut(tween(200)) }
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        coroutineScope.launch {
                            repository.preferencesRepository.setOnboardingCompleted(true)
                        }
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAddTask = {
                        navController.navigate(Screen.AddEditTask.createRoute())
                    },
                    onNavigateToAi = {
                        navController.navigate(Screen.Ai.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToSchedule = {
                        navController.navigate(Screen.Schedule.route)
                    },
                    onNavigateToEditTask = { taskId ->
                        navController.navigate(Screen.AddEditTask.createRoute(taskId = taskId))
                    }
                )
            }

            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    viewModel = scheduleViewModel,
                    onAddTaskForHour = { date, hour ->
                        navController.navigate(
                            Screen.AddEditTask.createRoute(
                                date = date.toString(),
                                hour = hour
                            )
                        )
                    },
                    onEditTask = { taskId ->
                        navController.navigate(Screen.AddEditTask.createRoute(taskId = taskId))
                    }
                )
            }

            composable(Screen.Tasks.route) {
                TasksScreen(
                    viewModel = tasksViewModel,
                    onNavigateToAddTask = {
                        navController.navigate(Screen.AddEditTask.createRoute())
                    },
                    onNavigateToEditTask = { taskId ->
                        navController.navigate(Screen.AddEditTask.createRoute(taskId = taskId))
                    },
                    onStartTimer = { task ->
                        homeViewModel.startTimerForTask(task)
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            composable(Screen.Statistics.route) {
                StatisticsScreen(viewModel = statisticsViewModel)
            }

            composable(Screen.Ai.route) {
                AiScreen(
                    viewModel = aiViewModel,
                    onNavigateToSchedule = {
                        navController.navigate(Screen.Schedule.route)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.AddEditTask.route,
                arguments = listOf(
                    navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" },
                    navArgument("hour") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val taskIdParam = backStackEntry.arguments?.getLong("taskId")?.takeIf { it > 0 }
                val dateParam = backStackEntry.arguments?.getString("date")?.takeIf { it.isNotBlank() }?.let {
                    try { LocalDate.parse(it) } catch (e: Exception) { null }
                }
                val hourParam = backStackEntry.arguments?.getInt("hour")?.takeIf { it >= 0 }

                val addEditViewModel: AddEditTaskViewModel = viewModel(
                    initializer = { AddEditTaskViewModel(repository) }
                )

                AddEditTaskScreen(
                    viewModel = addEditViewModel,
                    taskId = taskIdParam,
                    initialDate = dateParam,
                    initialHour = hourParam,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
