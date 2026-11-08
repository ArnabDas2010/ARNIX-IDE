package com.arnix.ide.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.arnix.ide.data.model.FileManagerItem
import java.io.File

object StorageAccessHelper {

    fun hasAllFilesPermission(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        } catch (e: Exception) {
            true
        }
    }

    fun requestAllFilesPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(fallbackIntent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    fun isRestrictedPath(path: String): Boolean {
        val normalized = path.replace("//", "/")
        return normalized.contains("Android/data", ignoreCase = true) ||
               normalized.contains("Android/obb", ignoreCase = true)
    }

    fun hasRestrictedPermission(context: Context, path: String): Boolean {
        val isObb = path.contains("Android/obb", ignoreCase = true)
        return SafStorageManager.hasPermission(context, isObb)
    }

    fun listFilesRobust(
        context: Context,
        dir: File,
        showHidden: Boolean = false,
        searchQuery: String = ""
    ): List<FileManagerItem> {
        val path = dir.absolutePath
        val isRestricted = isRestrictedPath(path)

        // For Android/data or Android/obb: query Storage Access Framework (SAF)
        if (isRestricted) {
            val isObb = path.contains("Android/obb", ignoreCase = true)
            if (SafStorageManager.hasPermission(context, isObb)) {
                return SafStorageManager.listDirectory(context, path, showHidden, searchQuery)
            }
        }

        // Standard Java File API for general storage (Downloads, Documents, Projects, etc.)
        val standardFiles = dir.listFiles()
        if (standardFiles != null && standardFiles.isNotEmpty()) {
            return standardFiles
                .filter { showHidden || !it.name.startsWith(".") }
                .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map { FileManagerItem.fromFile(it) }
        }

        // If standard API returned empty but SAF permission is available, try SAF
        if (isRestricted && SafStorageManager.hasPermission(context, path.contains("Android/obb", ignoreCase = true))) {
            return SafStorageManager.listDirectory(context, path, showHidden, searchQuery)
        }

        return emptyList()
    }

    fun readFileRobust(context: Context, filePath: String): String {
        val file = File(filePath)

        // Try standard File reading first
        if (file.exists() && file.canRead()) {
            try {
                if (file.length() < 2 * 1024 * 1024) {
                    return file.readText(Charsets.UTF_8)
                }
                return "// File is too large to display directly (${file.length() / 1024} KB)"
            } catch (e: Exception) {
                // fall through to SAF
            }
        }

        // Try SAF reading
        if (isRestrictedPath(filePath)) {
            val safContent = SafStorageManager.readFile(context, filePath)
            if (safContent != null) return safContent
        }

        return "// Cannot read file: Access not granted"
    }

    fun writeFileRobust(context: Context, filePath: String, content: String): Boolean {
        val file = File(filePath)

        if (file.canWrite() || (!file.exists() && file.parentFile?.canWrite() == true)) {
            try {
                file.writeText(content, Charsets.UTF_8)
                return true
            } catch (e: Exception) {
                // fall through to SAF
            }
        }

        if (isRestrictedPath(filePath)) {
            return SafStorageManager.writeFile(context, filePath, content)
        }

        return false
    }

    fun getDeveloperStorageShortcuts(): List<Triple<String, String, String>> {
        val list = mutableListOf<Triple<String, String, String>>()
        val primary = Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
        val base = primary.absolutePath

        list.add(Triple("⚡ Android/data", "$base/Android/data", "App data & caches"))
        list.add(Triple("🎮 Android/obb", "$base/Android/obb", "Game & expansion packages"))
        list.add(Triple("📷 Android/media", "$base/Android/media", "Shared app media"))
        list.add(Triple("📁 Storage Root", base, "Internal storage root"))
        list.add(Triple("📥 Downloads", "$base/Download", "User downloads"))
        list.add(Triple("📄 Documents", "$base/Documents", "User documents"))

        return list
    }
}
