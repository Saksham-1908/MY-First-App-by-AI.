# TimeTrack ⏱️

**TimeTrack** is a native Android application built with Jetpack Compose and Room Database. It serves as a personal daily planner, timetable builder, task tracker, live focus timer, and AI-powered schedule assistant.

---

## 🌟 Key Highlights

- **Clean First Launch**: Starts with an empty schedule — never forces pre-filled dummy tasks. The user builds and owns their timetable.
- **Full Offline Functionality**: Local persistence powered by **Room Database** and **DataStore**. All timetable planning, tasks, timers, statistics, and intelligent heuristic scheduling work completely offline without an active network connection.
- **Live Focus Timer**: Real-time ticker with Pause, Resume, Stop & Save, and Complete actions. Automatically logs actual minutes spent and records discrepancies between planned vs actual durations.
- **Dual Timetable Views**:
  - **Day View**: Vertical timeline (06:00 to 23:00) with interactive hour slots, task chips, and quick-add actions.
  - **Week View**: Complete Monday-to-Sunday overview with day-by-day task breakdown and quick scheduling.
- **AI-Powered Schedule Assistant**:
  - **Timetable Builder**: Generates structured schedules from natural language prompts (e.g., *"Study 3 hours for physics, gym in morning, work 9 to 5 with lunch"*).
  - **Schedule Optimizer**: Evaluates workload balance, flags burnout risks, and suggests breaks and pacing adjustments.
  - **Gemini API & Offline Smart Heuristic**: Works with Gemini 2.5 Flash when `GEMINI_API_KEY` is provided, and automatically falls back to an intelligent local planner when offline.
- **Android Notifications & Exact Alarms**: Alerts users before scheduled tasks begin using `AlarmManager` and `NotificationManager`.

---

## 🏗️ Architecture

The app follows **Clean Architecture & MVVM** design principles:

```
com.example
├── MainActivity.kt                      # Host Activity with Edge-to-Edge support
├── TimeTrackApplication.kt              # App singleton initializing DB and Channels
├── data
│   ├── ai
│   │   └── AiScheduleManager.kt         # Gemini API client & smart heuristic fallback
│   ├── local
│   │   ├── CategoryDao.kt               # Category queries & inserts
│   │   ├── TaskDao.kt                   # Timetable task queries & mutations
│   │   └── TimeTrackDatabase.kt         # Room Database with category pre-seeding
│   ├── model
│   │   ├── CategoryItem.kt              # Category Room entity
│   │   └── TaskItem.kt                  # Task Room entity
│   ├── preferences
│   │   └── UserPreferencesRepository.kt # DataStore for onboarding & preferences
│   └── repository
│       └── TimeTrackRepository.kt       # Repository consolidating DB, preferences & AI
├── receiver
│   └── TaskReminderReceiver.kt          # BroadcastReceiver for AlarmManager notifications
├── ui
│   ├── ai
│   │   ├── AiPlannerViewModel.kt
│   │   └── AiScreen.kt                  # AI Timetable generator & Schedule optimizer
│   ├── home
│   │   ├── HomeScreen.kt                # Today's Progress, Current Task Timer, Upcoming
│   │   └── HomeViewModel.kt
│   ├── navigation
│   │   └── MainNavigation.kt            # Bottom bar navigation & type-safe routes
│   ├── onboarding
│   │   └── OnboardingScreen.kt          # First-launch onboarding experience
│   ├── schedule
│   │   ├── ScheduleScreen.kt            # Day timeline & Week timetable
│   │   └── ScheduleViewModel.kt
│   ├── settings
│   │   ├── SettingsScreen.kt            # Default durations, notifications, clear data
│   │   └── SettingsViewModel.kt
│   ├── taskedit
│   │   ├── AddEditTaskScreen.kt         # Task creation/editing form with time pickers
│   │   └── AddEditTaskViewModel.kt
│   ├── tasks
│   │   ├── TasksScreen.kt               # All tasks list, filters, search & sorting
│   │   └── TasksViewModel.kt
│   ├── statistics
│   │   ├── StatisticsScreen.kt          # Planned vs actual charts & productivity scores
│   │   └── StatisticsViewModel.kt
│   └── theme
│       ├── Color.kt                     # Modern high-contrast color scheme
│       ├── Theme.kt                     # Material Design 3 theme
│       └── Type.kt
└── util
    └── NotificationHelper.kt            # Channels and AlarmManager scheduling
```

---

## 🚀 Setup & Build Instructions

1. **Prerequisites**: Android Studio Ladybug (or newer), JDK 11+, Android SDK 34+.
2. **Build and Run**:
   ```bash
   gradle assembleDebug
   ```
3. **Run Local Tests**:
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. **AI Features Configuration**:
   - Set your Gemini API key in the AI Studio **Secrets** panel (`GEMINI_API_KEY`).
   - If no key is set or when working offline, TimeTrack uses its built-in rule-based heuristic schedule engine.
