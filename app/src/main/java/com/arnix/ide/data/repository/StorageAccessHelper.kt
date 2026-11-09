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
            val intents = listOf(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            for (intent in intents) {
                try {
                    context.startActivity(intent)
                    return
                } catch (e: Exception) {
                    // Try next fallback
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
        showHidden: Boolean = true,
        searchQuery: String = ""
    ): List<FileManagerItem> {
        val path = dir.absolutePath
        val isRestricted = isRestrictedPath(path)

        // For Android/data or Android/obb: query Storage Access Framework (SAF)
        if (isRestricted) {
            val isObb = path.contains("Android/obb", ignoreCase = true)
            if (SafStorageManager.hasPermission(context, isObb)) {
                val safItems = SafStorageManager.listDirectory(context, path, showHidden, searchQuery)
                if (safItems.isNotEmpty()) {
                    return safItems
                }
            }
        }

        // 1. Try standard Java File.listFiles()
        var filesList: List<File>? = dir.listFiles()?.toList()

        // 2. Fallback: If null or empty, try dir.list()
        if (filesList.isNullOrEmpty()) {
            val names = dir.list()
            if (names != null && names.isNotEmpty()) {
                filesList = names.map { File(dir, it) }
            }
        }

        // 3. Fallback: If still null or empty, try shell ls -1A
        if (filesList.isNullOrEmpty()) {
            val shellFiles = listViaShell(dir)
            if (shellFiles.isNotEmpty()) {
                filesList = shellFiles
            }
        }

        // 4. Ensure standard root directories are always visible if in storage root
        val normPath = dir.absolutePath.trimEnd('/')
        val extPath = Environment.getExternalStorageDirectory()?.absolutePath?.trimEnd('/') ?: "/storage/emulated/0"
        if (normPath == "/storage/emulated/0" || normPath == extPath || normPath == "/sdcard") {
            val wellKnown = listOf(
                "Android", "Download", "Documents", "DCIM", "Pictures",
                "Music", "Movies", "Notifications", "Alarms", "Ringtones", "Podcasts"
            )
            val currentNames = filesList?.map { it.name.lowercase() }?.toSet() ?: emptySet()
            val extra = wellKnown.filter { !currentNames.contains(it.lowercase()) }
                .map { File(dir, it) }
                .filter { it.exists() || it.isDirectory || File("/storage/emulated/0/$it").exists() }
            if (extra.isNotEmpty()) {
                filesList = (filesList ?: emptyList()) + extra
            }
        }

        // 5. Ensure Android subdirectories are visible if inside /Android
        if (dir.name.equals("Android", ignoreCase = true)) {
            val androidSubs = listOf("data", "obb", "media")
            val currentNames = filesList?.map { it.name.lowercase() }?.toSet() ?: emptySet()
            val extra = androidSubs.filter { !currentNames.contains(it.lowercase()) }
                .map { File(dir, it) }
            if (extra.isNotEmpty()) {
                filesList = (filesList ?: emptyList()) + extra
            }
        }

        if (filesList != null) {
            return filesList
                .filter { showHidden || !it.name.startsWith(".") }
                .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map { file ->
                    val isAndroidSubdir = file.isDirectory && file.parentFile?.name.equals("Android", ignoreCase = true)
                    if (isAndroidSubdir && file.name.equals("data", ignoreCase = true)) {
                        val hasPerm = SafStorageManager.hasPermission(context, false)
                        val count = if (hasPerm) SafStorageManager.getFolderItemCount(context, false) else -1
                        val sizeStr = if (hasPerm) {
                            if (count > 0) "$count items" else "Folder"
                        } else {
                            "Access required"
                        }
                        FileManagerItem.fromFile(file).copy(sizeFormatted = sizeStr)
                    } else if (isAndroidSubdir && file.name.equals("obb", ignoreCase = true)) {
                        val hasPerm = SafStorageManager.hasPermission(context, true)
                        val count = if (hasPerm) SafStorageManager.getFolderItemCount(context, true) else -1
                        val sizeStr = if (hasPerm) {
                            if (count > 0) "$count items" else "Folder"
                        } else {
                            "Access required"
                        }
                        FileManagerItem.fromFile(file).copy(sizeFormatted = sizeStr)
                    } else {
                        FileManagerItem.fromFile(file)
                    }
                }
        }

        // 6. If standard API returned empty but SAF permission is available, try SAF
        if (isRestricted && SafStorageManager.hasPermission(context, path.contains("Android/obb", ignoreCase = true))) {
            return SafStorageManager.listDirectory(context, path, showHidden, searchQuery)
        }

        return emptyList()
    }

    fun listViaShell(dir: File): List<File> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "ls -1A \"${dir.absolutePath}\""))
            val lines = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            lines.filter { it.isNotBlank() }.map { File(dir, it.trim()) }
        } catch (e: Exception) {
            emptyList()
        }
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

    fun deleteItem(context: Context, path: String): Boolean {
        if (isRestrictedPath(path)) {
            val safResult = SafStorageManager.deleteDocument(context, path)
            if (safResult) return true
        }
        val file = File(path)
        return try {
            if (file.exists()) {
                file.deleteRecursively()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun renameItem(context: Context, path: String, newName: String): Boolean {
        if (newName.isBlank()) return false
        if (isRestrictedPath(path)) {
            val safResult = SafStorageManager.renameDocument(context, path, newName)
            if (safResult) return true
        }
        val file = File(path)
        return try {
            if (file.exists()) {
                val target = File(file.parentFile, newName)
                file.renameTo(target)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun createFolder(context: Context, parentPath: String, name: String): Boolean {
        if (name.isBlank()) return false
        if (isRestrictedPath(parentPath)) {
            val safResult = SafStorageManager.createDirectory(context, parentPath, name)
            if (safResult) return true
        }
        val target = File(parentPath, name)
        return try {
            target.mkdirs()
        } catch (e: Exception) {
            false
        }
    }

    fun createFile(context: Context, parentPath: String, name: String, content: String = ""): Boolean {
        if (name.isBlank()) return false
        if (isRestrictedPath(parentPath)) {
            val safResult = SafStorageManager.createFile(context, parentPath, name, content)
            if (safResult) return true
        }
        val target = File(parentPath, name)
        return try {
            target.parentFile?.mkdirs()
            if (content.isNotEmpty()) {
                target.writeText(content, Charsets.UTF_8)
            } else {
                target.createNewFile()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun copyItem(context: Context, sourcePath: String, destinationDirPath: String): Boolean {
        val srcFile = File(sourcePath)
        val destDir = File(destinationDirPath)
        if (!destDir.exists() && !isRestrictedPath(destinationDirPath)) {
            destDir.mkdirs()
        }

        var targetName = srcFile.name
        var targetFile = File(destDir, targetName)
        if (targetFile.exists()) {
            val baseName = if (srcFile.isDirectory) targetName else targetName.substringBeforeLast(".")
            val ext = if (srcFile.isDirectory || !targetName.contains(".")) "" else "." + targetName.substringAfterLast(".")
            var counter = 1
            while (targetFile.exists()) {
                targetName = "${baseName}_copy$counter$ext"
                targetFile = File(destDir, targetName)
                counter++
            }
        }

        if (isRestrictedPath(destinationDirPath)) {
            return if (srcFile.isDirectory) {
                SafStorageManager.createDirectory(context, destinationDirPath, targetName)
            } else {
                val content = readFileRobust(context, sourcePath)
                SafStorageManager.createFile(context, destinationDirPath, targetName, content)
            }
        }

        if (isRestrictedPath(sourcePath)) {
            return try {
                if (srcFile.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    val content = SafStorageManager.readFile(context, sourcePath) ?: ""
                    targetFile.writeText(content, Charsets.UTF_8)
                }
                true
            } catch (e: Exception) {
                false
            }
        }

        return try {
            if (srcFile.isDirectory) {
                srcFile.copyRecursively(targetFile, overwrite = true)
            } else {
                srcFile.copyTo(targetFile, overwrite = true)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun moveItem(context: Context, sourcePath: String, destinationDirPath: String): Boolean {
        val copied = copyItem(context, sourcePath, destinationDirPath)
        if (copied) {
            deleteItem(context, sourcePath)
            return true
        }
        return false
    }

    fun getDeveloperStorageShortcuts(): List<Triple<String, String, String>> {
        val list = mutableListOf<Triple<String, String, String>>()
        val primary = Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
        val base = primary.absolutePath

        list.add(Triple("Android/data", "$base/Android/data", "App data and caches"))
        list.add(Triple("Android/obb", "$base/Android/obb", "Game expansion packages"))
        list.add(Triple("Android/media", "$base/Android/media", "Shared app media"))
        list.add(Triple("Internal Storage", base, "Device storage root"))
        list.add(Triple("Downloads", "$base/Download", "User downloads"))
        list.add(Triple("Documents", "$base/Documents", "User documents"))

        return list
    }
}
