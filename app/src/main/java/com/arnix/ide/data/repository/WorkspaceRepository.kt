package com.arnix.ide.data.repository

import android.content.Context
import com.arnix.ide.data.model.FileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class WorkspaceRepository(private val context: Context? = null) {

    private val _rootFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val rootFiles: StateFlow<List<FileItem>> = _rootFiles.asStateFlow()

    private val _workspaceRootPath = MutableStateFlow<String?>("Sample Workspace")
    val workspaceRootPath: StateFlow<String?> = _workspaceRootPath.asStateFlow()

    private val _workspaceName = MutableStateFlow("ARNIX-IDE Project")
    val workspaceName: StateFlow<String> = _workspaceName.asStateFlow()

    init {
        seedSampleWorkspace()
    }

    private val repoScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    fun setWorkspaceFolder(folderPath: String) {
        val dir = File(folderPath)
        _workspaceRootPath.value = dir.absolutePath
        _workspaceName.value = dir.name.ifEmpty { dir.absolutePath }
        repoScope.launch {
            val directItems = loadDirectChildrenFromDisk(dir)
            _rootFiles.value = directItems
        }
    }

    fun refreshWorkspace() {
        val currentPath = _workspaceRootPath.value
        if (currentPath != null && currentPath != "Sample Workspace") {
            val dir = File(currentPath)
            repoScope.launch {
                val directItems = loadDirectChildrenFromDisk(dir)
                _rootFiles.value = directItems
            }
            return
        }
        seedSampleWorkspace()
    }

    private fun loadDirectChildrenFromDisk(directory: File): List<FileItem> {
        val robustItems = if (context != null) {
            StorageAccessHelper.listFilesRobust(context, directory, showHidden = false)
        } else {
            val files = directory.listFiles() ?: emptyArray()
            files.map { com.arnix.ide.data.model.FileManagerItem.fromFile(it) }
        }

        val sortedItems = robustItems
            .filter { !it.name.startsWith(".git") && it.name != "build" && it.name != ".gradle" }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        return sortedItems.map { item ->
            FileItem(
                id = item.path,
                name = item.name,
                path = item.path,
                isDirectory = item.isDirectory,
                extension = if (item.isDirectory) "" else item.extension,
                content = "",
                children = emptyList(),
                isExpanded = false
            )
        }
    }

    private fun readViaShell(filePath: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "cat \"$filePath\" | head -n 500"))
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "// Cannot read $filePath: ${e.localizedMessage}"
        }
    }

    private fun seedSampleWorkspace() {
        _workspaceRootPath.value = "Sample Workspace"
        _workspaceName.value = "ARNIX-IDE Project"

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
        // If not in in-memory tree but file exists on physical disk, read it directly
        val diskFile = File(path)
        val content = if (context != null) {
            StorageAccessHelper.readFileRobust(context, path)
        } else if (diskFile.exists() && !diskFile.isDirectory) {
            try {
                diskFile.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                ""
            }
        } else {
            ""
        }

        if (content.isNotBlank() && !content.startsWith("// Cannot read")) {
            return FileItem(
                id = path,
                name = diskFile.name,
                path = path,
                isDirectory = false,
                extension = diskFile.extension,
                content = content
            )
        }
        return null
    }

    fun updateFileContent(path: String, newContent: String) {
        if (context != null) {
            StorageAccessHelper.writeFileRobust(context, path, newContent)
        } else {
            val diskFile = File(path)
            if (diskFile.exists() && !diskFile.isDirectory) {
                try {
                    diskFile.writeText(newContent, Charsets.UTF_8)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

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
        val fullPath = if (parentPath.isEmpty()) {
            val root = _workspaceRootPath.value
            if (root != null && root != "Sample Workspace") "$root/$fileName" else fileName
        } else {
            "$parentPath/$fileName"
        }

        val diskFile = File(fullPath)
        try {
            if (diskFile.parentFile?.exists() == true) {
                if (isDirectory) {
                    diskFile.mkdirs()
                } else {
                    diskFile.createNewFile()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val newItem = FileItem(
            id = if (diskFile.isAbsolute) diskFile.absolutePath else UUID.randomUUID().toString(),
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
        val diskFile = File(path)
        if (diskFile.exists()) {
            try {
                diskFile.deleteRecursively()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
        repoScope.launch {
            fun toggleInList(items: List<FileItem>): List<FileItem> {
                return items.map { item ->
                    if (item.path == folderPath && item.isDirectory) {
                        val willExpand = !item.isExpanded
                        val childrenToUse = if (willExpand && item.children.isEmpty()) {
                            loadDirectChildrenFromDisk(File(item.path))
                        } else {
                            item.children
                        }
                        item.copy(isExpanded = willExpand, children = childrenToUse)
                    } else if (item.isDirectory && item.isExpanded) {
                        item.copy(children = toggleInList(item.children))
                    } else {
                        item
                    }
                }
            }
            _rootFiles.value = toggleInList(_rootFiles.value)
        }
    }
}
