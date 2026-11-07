package com.arnix.ide.data.repository

import com.arnix.ide.data.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class WorkspaceRepository {

    private val _rootFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val rootFiles: StateFlow<List<FileItem>> = _rootFiles.asStateFlow()

    init {
        seedSampleWorkspace()
    }

    private fun seedSampleWorkspace() {
        val mainActivityKt = FileItem(
            id = "file_main_activity",
            name = "MainActivity.kt",
            path = "app/src/main/java/com/arnix/app/MainActivity.kt",
            isDirectory = false,
            extension = "kt",
            content = """package com.arnix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ArnixGreeting(name = "Android Developer")
                }
            }
        }
    }
}

@Composable
fun ArnixGreeting(name: String, modifier: Modifier = Modifier) {
    var count by remember { mutableStateOf(0) }
    
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to ARNIX IDE!",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Native Android coding right on your device.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { count++ }) {
            Text("Clicked: " + "$" + "count times")
        }
    }
}
""".trimIndent()
        )

        val buildGradle = FileItem(
            id = "file_build_gradle",
            name = "build.gradle.kts",
            path = "app/build.gradle.kts",
            isDirectory = false,
            extension = "kts",
            content = """plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.arnix.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.arnix.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
}
""".trimIndent()
        )

        val readmeMd = FileItem(
            id = "file_readme",
            name = "README.md",
            path = "README.md",
            isDirectory = false,
            extension = "md",
            content = """# ARNIX IDE
Native, lightweight, and clean Android IDE.

## Features
- Complete Jetpack Compose code editor with syntax highlighting
- In-memory & local workspace file management
- Git version control & GitHub integration
- Embedded interactive terminal shell
- Mobile coding symbol bar for rapid syntax typing

Developed for Android with Kotlin & Jetpack Compose.
""".trimIndent()
        )

        val pythonScript = FileItem(
            id = "file_script_py",
            name = "analytics.py",
            path = "scripts/analytics.py",
            isDirectory = false,
            extension = "py",
            content = """# ARNIX IDE Data Analytics Script
import sys

def compute_metrics(data_list):
    total = sum(data_list)
    avg = total / len(data_list) if data_list else 0
    return {"total": total, "average": avg}

if __name__ == "__main__":
    sample = [12, 45, 68, 23, 90, 114]
    results = compute_metrics(sample)
    print(f"ARNIX Metrics: {results}")
""".trimIndent()
        )

        val javaFolder = FileItem(
            id = "dir_java",
            name = "arnix",
            path = "app/src/main/java/com/arnix",
            isDirectory = true,
            children = listOf(mainActivityKt),
            isExpanded = true
        )

        val scriptsFolder = FileItem(
            id = "dir_scripts",
            name = "scripts",
            path = "scripts",
            isDirectory = true,
            children = listOf(pythonScript),
            isExpanded = false
        )

        val appFolder = FileItem(
            id = "dir_app",
            name = "app",
            path = "app",
            isDirectory = true,
            children = listOf(javaFolder, buildGradle),
            isExpanded = true
        )

        _rootFiles.value = listOf(appFolder, scriptsFolder, readmeMd)
    }

    fun findFileByPath(path: String, items: List<FileItem> = _rootFiles.value): FileItem? {
        for (item in items) {
            if (item.path == path) return item
            if (item.isDirectory) {
                val found = findFileByPath(path, item.children)
                if (found != null) return found
            }
        }
        return null
    }

    fun updateFileContent(path: String, newContent: String) {
        fun updateInList(items: List<FileItem>): List<FileItem> {
            return items.map { item ->
                if (item.path == path) {
                    item.copy(content = newContent)
                } else if (item.isDirectory) {
                    item.copy(children = updateInList(item.children))
                } else {
                    item
                }
            }
        }
        _rootFiles.value = updateInList(_rootFiles.value)
    }

    fun createFile(parentPath: String, fileName: String, isDirectory: Boolean): FileItem {
        val extension = if (fileName.contains(".")) fileName.substringAfterLast(".") else ""
        val fullPath = if (parentPath.isEmpty()) fileName else "$parentPath/$fileName"
        val newItem = FileItem(
            id = UUID.randomUUID().toString(),
            name = fileName,
            path = fullPath,
            isDirectory = isDirectory,
            extension = extension,
            content = if (isDirectory) "" else "// New file: $fileName\n",
            children = emptyList(),
            isExpanded = true
        )

        if (parentPath.isEmpty()) {
            _rootFiles.value = _rootFiles.value + newItem
        } else {
            fun addToParent(items: List<FileItem>): List<FileItem> {
                return items.map { item ->
                    if (item.path == parentPath && item.isDirectory) {
                        item.copy(children = item.children + newItem, isExpanded = true)
                    } else if (item.isDirectory) {
                        item.copy(children = addToParent(item.children))
                    } else {
                        item
                    }
                }
            }
            _rootFiles.value = addToParent(_rootFiles.value)
        }
        return newItem
    }

    fun deleteFile(path: String) {
        fun removeFromList(items: List<FileItem>): List<FileItem> {
            return items.filter { it.path != path }.map { item ->
                if (item.isDirectory) {
                    item.copy(children = removeFromList(item.children))
                } else {
                    item
                }
            }
        }
        _rootFiles.value = removeFromList(_rootFiles.value)
    }

    fun toggleFolderExpanded(folderPath: String) {
        fun toggleInList(items: List<FileItem>): List<FileItem> {
            return items.map { item ->
                if (item.path == folderPath && item.isDirectory) {
                    item.copy(isExpanded = !item.isExpanded)
                } else if (item.isDirectory) {
                    item.copy(children = toggleInList(item.children))
                } else {
                    item
                }
            }
        }
        _rootFiles.value = toggleInList(_rootFiles.value)
    }
}
