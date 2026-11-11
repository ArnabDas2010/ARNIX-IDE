package com.arnix.ide.data.repository

import java.io.File

data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val files: List<Pair<String, String>>
)

object TemplateProjects {

    val templates = listOf(
        ProjectTemplate(
            id = "web_starter",
            name = "HTML5 / CSS / JS Web App",
            description = "Modern responsive web app with live preview, clean styling and interactive scripts",
            category = "Web",
            files = listOf(
                "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ARNIX Web App</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="container">
    <header>
      <div class="badge">ARNIX IDE</div>
      <h1>Hello, Developer</h1>
      <p>Your responsive native web workspace is live.</p>
    </header>
    
    <main>
      <div class="card">
        <h3>Interactive Counter</h3>
        <p>Tap the button below to test DOM events:</p>
        <button id="counterBtn" class="btn">Clicked 0 times</button>
      </div>

      <div class="card console-card">
        <h3>Live Console Output</h3>
        <p id="logOutput">Console initialized. Ready for events.</p>
      </div>
    </main>
  </div>
  <script src="app.js"></script>
</body>
</html>
""",
                "style.css" to """* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  background: #090D16;
  color: #F8FAFC;
  padding: 24px 16px;
  min-height: 100vh;
}

.container {
  max-width: 600px;
  margin: 0 auto;
}

header {
  margin-bottom: 24px;
}

.badge {
  display: inline-block;
  background: rgba(56, 189, 248, 0.15);
  color: #38BDF8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid rgba(56, 189, 248, 0.3);
  margin-bottom: 12px;
}

h1 {
  font-size: 26px;
  font-weight: 800;
  color: #F8FAFC;
  margin-bottom: 6px;
}

p {
  color: #94A3B8;
  font-size: 14px;
  line-height: 1.5;
}

.card {
  background: #111827;
  border: 1px solid #1E293B;
  border-radius: 12px;
  padding: 18px;
  margin-bottom: 16px;
}

.card h3 {
  font-size: 16px;
  color: #F8FAFC;
  margin-bottom: 8px;
}

.btn {
  display: inline-block;
  background: #38BDF8;
  color: #090D16;
  border: none;
  font-weight: 700;
  font-size: 14px;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  margin-top: 12px;
  transition: transform 0.1s ease;
}

.btn:active {
  transform: scale(0.97);
}

.console-card p {
  font-family: monospace;
  font-size: 12px;
  color: #34D399;
}
""",
                "app.js" to """// ARNIX IDE Interactive Script
console.log("App script loaded successfully.");

let count = 0;
const button = document.getElementById("counterBtn");
const logOutput = document.getElementById("logOutput");

button.addEventListener("click", () => {
  count++;
  button.textContent = `Clicked ${'$'}{count} times`;
  const time = new Date().toLocaleTimeString();
  const msg = `[${'$'}{time}] Event fired: count = ${'$'}{count}`;
  console.log(msg);
  logOutput.textContent = msg;
});
"""
            )
        ),
        ProjectTemplate(
            id = "kotlin_starter",
            name = "Kotlin Clean App",
            description = "Kotlin data models, repository patterns, and business logic script",
            category = "Kotlin",
            files = listOf(
                "Main.kt" to """package com.arnix.sample

import java.time.Instant

data class TaskItem(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class TaskManager {
    private val tasks = mutableListOf<TaskItem>()

    fun addTask(title: String): TaskItem {
        val task = TaskItem(id = "task_${'$'}{tasks.size + 1}", title = title)
        tasks.add(task)
        return task
    }

    fun listPending(): List<TaskItem> = tasks.filter { !it.isCompleted }

    fun printSummary() {
        println("=== ARNIX Task Manager ===")
        println("Total Tasks: ${'$'}{tasks.size}")
        tasks.forEach { println("- [${'$'}{if (it.isCompleted) "x" else " "}] ${'$'}{it.title}") }
    }
}

fun main() {
    val manager = TaskManager()
    manager.addTask("Build native Jetpack Compose UI")
    manager.addTask("Implement SAF Storage Access")
    manager.addTask("Run Live HTML/CSS Preview")
    manager.printSummary()
}
"""
            )
        ),
        ProjectTemplate(
            id = "python_starter",
            name = "Python Analytics Script",
            description = "Python modular script with data processing and console reporting",
            category = "Python",
            files = listOf(
                "main.py" to """# ARNIX IDE Python Script
import sys
import math

def calculate_stats(numbers):
    if not numbers:
        return {}
    total = sum(numbers)
    count = len(numbers)
    mean = total / count
    variance = sum((x - mean) ** 2 for x in numbers) / count
    std_dev = math.sqrt(variance)
    return {
        "count": count,
        "total": total,
        "mean": round(mean, 2),
        "std_dev": round(std_dev, 2)
    }

def main():
    data = [14, 28, 42, 56, 70, 84, 98, 112]
    print(f"ARNIX Python Engine: Processing {len(data)} items...")
    stats = calculate_stats(data)
    for key, value in stats.items():
        print(f"  {key}: {value}")

if __name__ == "__main__":
    main()
"""
            )
        ),
        ProjectTemplate(
            id = "markdown_starter",
            name = "Markdown Documentation",
            description = "Project documentation with headers, badges, tables, and checklists",
            category = "Docs",
            files = listOf(
                "README.md" to """# Project Documentation

Welcome to your native project crafted in **ARNIX IDE**.

## Overview
This documentation supports instant rendered preview inside the editor.

### Features
- Native Jetpack Compose architecture
- Live WebView HTML5/JS runner
- Integrated Archive (ZIP/APK) viewer
- Direct SAF kernel storage integration

### Quick Table
| Component | Status | Performance |
|-----------|--------|-------------|
| Code Editor | Complete | 120 FPS |
| File Manager | Active | Low Latency |
| Live Preview | Enabled | Hardware Accel |

### Checklist
- [x] Create project template
- [x] Configure live preview
- [ ] Deploy to production

*Authored in ARNIX IDE.*
"""
            )
        )
    )

    fun createTemplate(targetDir: File, template: ProjectTemplate): Boolean {
        return try {
            targetDir.mkdirs()
            for ((relPath, content) in template.files) {
                val file = File(targetDir, relPath)
                file.parentFile?.mkdirs()
                file.writeText(content, Charsets.UTF_8)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
