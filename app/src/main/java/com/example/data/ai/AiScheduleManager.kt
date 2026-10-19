package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.TaskItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class GeneratedScheduleResult(
    val summary: String,
    val tasks: List<TaskItem>,
    val tips: List<String>,
    val isFromRemoteAi: Boolean
)

data class ScheduleOptimizationResult(
    val analysis: String,
    val recommendations: List<String>,
    val productivityScore: Int,
    val suggestedAdjustments: List<String>
)

class AiScheduleManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateSchedule(
        userPrompt: String,
        targetDate: LocalDate = LocalDate.now(),
        startHour: Int = 8
    ): GeneratedScheduleResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey) {
            try {
                val remoteResult = callGeminiApi(userPrompt, targetDate, apiKey)
                if (remoteResult != null && remoteResult.tasks.isNotEmpty()) {
                    return@withContext remoteResult
                }
            } catch (e: Exception) {
                Log.w("AiScheduleManager", "Remote Gemini call failed, falling back to smart planner: ${e.message}")
            }
        }

        // Offline / Smart Heuristic Schedule Generator
        return@withContext generateSmartLocalSchedule(userPrompt, targetDate, startHour)
    }

    suspend fun analyzeSchedule(
        tasks: List<TaskItem>,
        targetDate: LocalDate = LocalDate.now()
    ): ScheduleOptimizationResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey && tasks.isNotEmpty()) {
            try {
                val remoteOpt = callGeminiOptimizer(tasks, targetDate, apiKey)
                if (remoteOpt != null) {
                    return@withContext remoteOpt
                }
            } catch (e: Exception) {
                Log.w("AiScheduleManager", "Remote optimizer failed, falling back to local analysis", e)
            }
        }

        return@withContext analyzeScheduleLocally(tasks)
    }

    private fun callGeminiApi(
        prompt: String,
        targetDate: LocalDate,
        apiKey: String
    ): GeneratedScheduleResult? {
        val dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val systemInstruction = """
            You are TimeTrack AI, a world-class schedule and productivity planner.
            Generate a realistic, balanced timetable for the date $dateStr based on the user's goals.
            Respond strictly with valid JSON with no markdown formatting.
            Structure:
            {
              "summary": "Brief summary of the day's plan",
              "tasks": [
                {
                  "title": "Task name",
                  "description": "Short explanation",
                  "startTime": "HH:mm",
                  "endTime": "HH:mm",
                  "category": "Work | Study | Health | Personal | Chores",
                  "categoryColorHex": "#6366F1",
                  "priority": "Low | Medium | High",
                  "notes": "Tip for this task"
                }
              ],
              "tips": [
                "Productivity tip 1",
                "Productivity tip 2"
              ]
            }
            Ensure tasks do not overlap, times are in strictly 24-hour HH:mm format, and durations are realistic.
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nUser request: $prompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }

        val jsonString = executeGeminiRequest(jsonPayload, apiKey) ?: return null
        val parsedJson = JSONObject(jsonString)

        val summary = when (val s = parsedJson.opt("summary")) {
            is String -> s
            is JSONObject -> s.optString("focus", s.optString("totalTasks", "AI Generated Schedule for $dateStr"))
            else -> "AI Generated Schedule for $dateStr"
        }

        val tasksArray = parsedJson.optJSONArray("tasks") ?: JSONArray()
        val tipsArray = parsedJson.optJSONArray("tips") ?: JSONArray()

        val parsedTasks = mutableListOf<TaskItem>()
        for (i in 0 until tasksArray.length()) {
            val obj = tasksArray.getJSONObject(i)
            val title = obj.optString("title", "Task ${i + 1}")
            val description = obj.optString("description", "")
            val startTime = obj.optString("startTime", String.format("%02d:00", 9 + i))
            val endTime = obj.optString("endTime", String.format("%02d:45", 9 + i))
            val category = obj.optString("category", "Work")
            val colorHex = when (category.lowercase()) {
                "study" -> "#06B6D4"
                "health", "fitness" -> "#10B981"
                "personal" -> "#F59E0B"
                "chores" -> "#F97316"
                else -> "#6366F1"
            }
            val priority = obj.optString("priority", "Medium")
            val notes = obj.optString("notes", "")

            parsedTasks.add(
                TaskItem(
                    title = title,
                    description = description,
                    date = dateStr,
                    startTime = startTime,
                    endTime = endTime,
                    category = category,
                    categoryColorHex = colorHex,
                    priority = priority,
                    hasReminder = true,
                    notes = notes,
                    orderIndex = i
                )
            )
        }

        val parsedTips = mutableListOf<String>()
        for (i in 0 until tipsArray.length()) {
            parsedTips.add(tipsArray.getString(i))
        }

        return GeneratedScheduleResult(
            summary = summary,
            tasks = parsedTasks,
            tips = parsedTips,
            isFromRemoteAi = true
        )
    }

    private fun callGeminiOptimizer(
        tasks: List<TaskItem>,
        targetDate: LocalDate,
        apiKey: String
    ): ScheduleOptimizationResult? {
        val summaryTasks = tasks.joinToString("\n") {
            "- ${it.title} (${it.startTime} to ${it.endTime}, ${it.category}, ${it.priority}, Done: ${it.isCompleted}, Logged: ${it.actualMinutesSpent}m)"
        }
        val systemPrompt = """
            You are TimeTrack AI Schedule Optimizer.
            Analyze the following schedule for ${targetDate}:
            $summaryTasks

            Provide JSON:
            {
              "analysis": "2-3 sentences analyzing workload balance, breaks, and pacing.",
              "productivityScore": 85,
              "recommendations": ["Recommendation 1", "Recommendation 2", "Recommendation 3"],
              "suggestedAdjustments": ["Adjustment 1", "Adjustment 2"]
            }
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val jsonString = executeGeminiRequest(jsonPayload, apiKey) ?: return null
        val parsed = JSONObject(jsonString)

        val recs = mutableListOf<String>()
        val recArray = parsed.optJSONArray("recommendations") ?: JSONArray()
        for (i in 0 until recArray.length()) recs.add(recArray.getString(i))

        val adjs = mutableListOf<String>()
        val adjArray = parsed.optJSONArray("suggestedAdjustments") ?: JSONArray()
        for (i in 0 until adjArray.length()) adjs.add(adjArray.getString(i))

        return ScheduleOptimizationResult(
            analysis = parsed.optString("analysis", "Your schedule is well balanced with good intervals."),
            recommendations = recs,
            productivityScore = parsed.optInt("productivityScore", 80),
            suggestedAdjustments = adjs
        )
    }

    private fun executeGeminiRequest(jsonPayload: JSONObject, apiKey: String): String? {
        val modelsToTry = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite")
        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())

        for (modelName in modelsToTry) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.w("AiScheduleManager", "Gemini HTTP error ${response.code} for $modelName: $errBody")
                    continue
                }

                val responseBody = response.body?.string() ?: continue
                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates") ?: continue
                if (candidates.length() == 0) continue

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content") ?: continue
                val parts = content.optJSONArray("parts") ?: continue

                val fullText = StringBuilder()
                for (p in 0 until parts.length()) {
                    val part = parts.getJSONObject(p)
                    if (part.optBoolean("thought", false)) continue
                    val partText = part.optString("text", "")
                    fullText.append(partText)
                }

                val text = fullText.toString().trim()
                if (text.isNotBlank()) {
                    return text
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                }
            } catch (e: Exception) {
                Log.e("AiScheduleManager", "Request error for $modelName: ${e.message}")
            }
        }
        return null
    }

    private fun generateSmartLocalSchedule(
        userPrompt: String,
        targetDate: LocalDate,
        startHour: Int
    ): GeneratedScheduleResult {
        val dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val lower = userPrompt.lowercase()
        val generatedTasks = mutableListOf<TaskItem>()

        var currentHour = startHour.coerceIn(6, 12)
        var currentMinute = 0

        fun addSlot(title: String, durationMins: Int, category: String, color: String, priority: String, notes: String) {
            val start = String.format("%02d:%02d", currentHour, currentMinute)
            val endTotal = currentHour * 60 + currentMinute + durationMins
            val endH = (endTotal / 60) % 24
            val endM = endTotal % 60
            val end = String.format("%02d:%02d", endH, endM)

            generatedTasks.add(
                TaskItem(
                    title = title,
                    description = "Planned via TimeTrack Intelligent Assistant",
                    date = dateStr,
                    startTime = start,
                    endTime = end,
                    durationMinutes = durationMins,
                    category = category,
                    categoryColorHex = color,
                    priority = priority,
                    hasReminder = true,
                    notes = notes,
                    orderIndex = generatedTasks.size
                )
            )

            // Buffer 15 mins after task
            val nextTotal = endTotal + 15
            currentHour = (nextTotal / 60) % 24
            currentMinute = nextTotal % 60
        }

        // Custom recognition heuristics
        if (lower.contains("workout") || lower.contains("gym") || lower.contains("exercise") || lower.contains("run")) {
            addSlot("Morning Workout & Stretch", 45, "Health", "#10B981", "Medium", "Hydrate well and warm up")
        }

        if (lower.contains("study") || lower.contains("exam") || lower.contains("class") || lower.contains("learn")) {
            addSlot("Deep Focus: Core Study Session", 90, "Study", "#06B6D4", "High", "Pomodoro intervals with no distractions")
            addSlot("Active Recall & Flashcards", 45, "Study", "#06B6D4", "Medium", "Review key summary formulas and questions")
        }

        if (lower.contains("work") || lower.contains("coding") || lower.contains("project") || lower.contains("meeting") || lower.contains("office")) {
            addSlot("Deep Work: Priority Deliverable", 120, "Work", "#6366F1", "High", "Tackle the most challenging project milestone")
            addSlot("Lunch Break & Recharging", 45, "Personal", "#F59E0B", "Low", "Step away from screens")
            addSlot("Team Sync & Communication", 45, "Work", "#6366F1", "Medium", "Respond to messages and finalize tasks")
        }

        if (lower.contains("chore") || lower.contains("clean") || lower.contains("grocery") || lower.contains("errand")) {
            addSlot("Home Chores & Organization", 45, "Chores", "#F97316", "Low", "Tidy desk and reset living space")
        }

        // If generic or not matched, provide a complete balanced timetable
        if (generatedTasks.isEmpty()) {
            addSlot("Morning Routine & Planning", 30, "Personal", "#F59E0B", "Medium", "Set top 3 daily priorities")
            addSlot("Deep Work Block 1", 90, "Work", "#6366F1", "High", "Focus on high-leverage objective")
            addSlot("Mindful Break & Walk", 30, "Health", "#10B981", "Low", "Step outside for fresh air")
            addSlot("Deep Work Block 2", 90, "Work", "#6366F1", "High", "Continue key project deliverables")
            addSlot("Lunch & Rest", 60, "Personal", "#F59E0B", "Low", "Healthy nutritious meal")
            addSlot("Skill Learning & Reading", 60, "Study", "#06B6D4", "Medium", "Read articles or tutorials")
            addSlot("Daily Wrap-up & Tomorrow Prep", 30, "Personal", "#F59E0B", "Low", "Log actual time and celebrate progress")
        }

        return GeneratedScheduleResult(
            summary = "Optimized schedule tailored for your goals with built-in rest buffers.",
            tasks = generatedTasks,
            tips = listOf(
                "Keep 15-minute buffers between complex tasks to avoid cognitive fatigue.",
                "Start with your highest priority task when your willpower is peaked.",
                "Log your actual time with the live timer to spot planning discrepancies."
            ),
            isFromRemoteAi = false
        )
    }

    private fun analyzeScheduleLocally(tasks: List<TaskItem>): ScheduleOptimizationResult {
        if (tasks.isEmpty()) {
            return ScheduleOptimizationResult(
                analysis = "Your day is currently empty. Add tasks or let the AI planner build a structured day.",
                recommendations = listOf(
                    "Aim for 3-5 key tasks per day for maximum focus.",
                    "Include at least one wellness or fitness block."
                ),
                productivityScore = 50,
                suggestedAdjustments = listOf("Tap '+ Add Task' or 'Ask AI' to schedule your day.")
            )
        }

        val totalMinutes = tasks.sumOf { it.calculateDurationMinutes() }
        val completedCount = tasks.count { it.isCompleted }
        val completionPct = (completedCount * 100) / tasks.size

        val recommendations = mutableListOf<String>()
        val adjustments = mutableListOf<String>()

        if (totalMinutes > 9 * 60) {
            recommendations.add("You have over 9 hours scheduled. Consider moving secondary tasks to tomorrow to prevent burnout.")
            adjustments.add("Reduce task duration by 15-20% for non-urgent tasks.")
        } else if (totalMinutes < 3 * 60) {
            recommendations.add("Light schedule today. Great opportunity to add a learning or workout session.")
        }

        val highPriority = tasks.filter { it.priority == "High" }
        if (highPriority.size > 3) {
            recommendations.add("Too many 'High' priority tasks (${highPriority.size}). Pick 1 or 2 as primary.")
        }

        val workMinutes = tasks.filter { it.category == "Work" }.sumOf { it.calculateDurationMinutes() }
        val healthMinutes = tasks.filter { it.category == "Health" }.sumOf { it.calculateDurationMinutes() }

        if (workMinutes > 240 && healthMinutes == 0) {
            adjustments.add("Add a 20-30 minute walk or stretching break between work sessions.")
        }

        val score = (60 + (completionPct * 0.3) + (if (tasks.size in 3..7) 10 else 0)).toInt().coerceIn(40, 100)

        return ScheduleOptimizationResult(
            analysis = "You have ${tasks.size} tasks planned ($completedCount completed) totaling ${totalMinutes / 60}h ${totalMinutes % 60}m of planned focus time.",
            recommendations = if (recommendations.isEmpty()) listOf("Schedule is well structured with manageable workloads.") else recommendations,
            productivityScore = score,
            suggestedAdjustments = if (adjustments.isEmpty()) listOf("Maintain current pacing and track actual time accurately.") else adjustments
        )
    }
}
