package com.arnix.ide.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.arnix.ide.data.model.FileManagerItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun listFilesRobust(
        context: Context,
        dir: File,
        showHidden: Boolean = true,
        searchQuery: String = ""
    ): List<FileManagerItem> {
        val path = dir.absolutePath.replace("//", "/")
        val isDataDir = dir.name.equals("data", ignoreCase = true) && dir.parentFile?.name.equals("Android", ignoreCase = true)
        val isObbDir = dir.name.equals("obb", ignoreCase = true) && dir.parentFile?.name.equals("Android", ignoreCase = true)

        val filesSet = LinkedHashSet<File>()

        // 1. Try standard Java File.listFiles()
        dir.listFiles()?.forEach { filesSet.add(it) }

        // 2. Try dir.list()
        if (filesSet.isEmpty()) {
            dir.list()?.forEach { filesSet.add(File(dir, it)) }
        }

        // 3. Try shell ls -1A
        if (filesSet.isEmpty() || isDataDir || isObbDir) {
            val shellFiles = listViaShell(dir)
            shellFiles.forEach { filesSet.add(it) }
        }

        // 4. For Android/data: Discover installed package folders just like ZArchiver
        if (isDataDir) {
            val packageFiles = getAndroidDataPackages(context, dir)
            packageFiles.forEach { filesSet.add(it) }
        }

        // 5. For Android/obb: Discover installed package folders with OBBs
        if (isObbDir) {
            val obbFiles = getAndroidObbPackages(context, dir)
            obbFiles.forEach { filesSet.add(it) }
        }

        // 6. Guarantee standard root directories are always visible if in storage root
        val normPath = path.trimEnd('/')
        val extPath = Environment.getExternalStorageDirectory()?.absolutePath?.trimEnd('/') ?: "/storage/emulated/0"
        if (normPath == "/storage/emulated/0" || normPath == extPath || normPath == "/sdcard") {
            val wellKnown = listOf(
                "Android", "Download", "Documents", "DCIM", "Pictures",
                "Music", "Movies", "Notifications", "Alarms", "Ringtones", "Podcasts"
            )
            val currentNames = filesSet.map { it.name.lowercase() }.toSet()
            for (name in wellKnown) {
                if (!currentNames.contains(name.lowercase())) {
                    val candidate = File(dir, name)
                    filesSet.add(candidate)
                }
            }
        }

        // 7. Guarantee Android subdirectories are visible if inside /Android
        if (dir.name.equals("Android", ignoreCase = true)) {
            val androidSubs = listOf("data", "obb", "media")
            val currentNames = filesSet.map { it.name.lowercase() }.toSet()
            for (sub in androidSubs) {
                if (!currentNames.contains(sub.lowercase())) {
                    filesSet.add(File(dir, sub))
                }
            }
        }

        // 8. If SAF tree permission was previously granted, merge any additional items found
        val isRestricted = isRestrictedPath(path)
        if (isRestricted) {
            val isObb = path.contains("Android/obb", ignoreCase = true)
            if (SafStorageManager.hasPermission(context, isObb)) {
                val safItems = SafStorageManager.listDirectory(context, path, showHidden, searchQuery)
                if (safItems.isNotEmpty()) {
                    val safNames = safItems.map { it.name.lowercase() }.toSet()
                    val existingFromSaf = filesSet.filter { !safNames.contains(it.name.lowercase()) }
                    return safItems + existingFromSaf.map { FileManagerItem.fromFile(it) }
                }
            }
        }

        if (filesSet.isNotEmpty()) {
            return filesSet
                .filter { showHidden || !it.name.startsWith(".") }
                .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map { file ->
                    val isAndroidSubdir = file.isDirectory && file.parentFile?.name.equals("Android", ignoreCase = true)
                    if (isAndroidSubdir && file.name.equals("data", ignoreCase = true)) {
                        val count = countFolderItemsQuick(context, file, isData = true, isObb = false)
                        val sizeStr = if (count > 0) "$count items" else "Empty"
                        FileManagerItem.fromFile(file).copy(sizeFormatted = sizeStr)
                    } else if (isAndroidSubdir && file.name.equals("obb", ignoreCase = true)) {
                        val count = countFolderItemsQuick(context, file, isData = false, isObb = true)
                        val sizeStr = if (count > 0) "$count items" else "Empty"
                        FileManagerItem.fromFile(file).copy(sizeFormatted = sizeStr)
                    } else if (isAndroidSubdir && file.name.equals("media", ignoreCase = true)) {
                        val count = file.listFiles()?.size ?: 0
                        val sizeStr = if (count > 0) "$count items" else "Empty"
                        FileManagerItem.fromFile(file).copy(sizeFormatted = sizeStr)
                    } else {
                        FileManagerItem.fromFile(file)
                    }
                }
        }

        return emptyList()
    }

    private fun getAndroidDataPackages(context: Context, dataDir: File): List<File> {
        val result = LinkedHashSet<File>()

        // Check installed packages from PackageManager
        try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(0)
            for (app in installedApps) {
                val pkgDir = File(dataDir, app.packageName)
                if (pkgDir.exists() || pkgDir.isDirectory) {
                    result.add(pkgDir)
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        // Also check common packages explicitly
        val commonPkgs = listOf(
            "com.android.chrome",
            "com.google.android.googlequicksearchbox",
            "ru.zdevs.zarchiver",
            "com.arnix.ide",
            "com.google.android.youtube",
            "com.android.vending",
            "com.google.android.gms",
            "com.whatsapp"
        )
        for (pkg in commonPkgs) {
            val pkgDir = File(dataDir, pkg)
            if (pkgDir.exists() || pkgDir.isDirectory) {
                result.add(pkgDir)
            }
        }

        // If shell can find anything
        listViaShell(dataDir).forEach { result.add(it) }

        return result.toList()
    }

    private fun getAndroidObbPackages(context: Context, obbDir: File): List<File> {
        val result = LinkedHashSet<File>()

        try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(0)
            for (app in installedApps) {
                val pkgDir = File(obbDir, app.packageName)
                if (pkgDir.exists() || pkgDir.isDirectory) {
                    result.add(pkgDir)
                }
            }
        } catch (e: Exception) {
            // ignore
        }

        listViaShell(obbDir).forEach { result.add(it) }

        return result.toList()
    }

    private fun countFolderItemsQuick(context: Context, dir: File, isData: Boolean, isObb: Boolean): Int {
        val direct = dir.listFiles()?.size ?: 0
        if (direct > 0) return direct

        val shell = listViaShell(dir).size
        if (shell > 0) return shell

        if (isData) {
            val pkgs = getAndroidDataPackages(context, dir).size
            if (pkgs > 0) return pkgs
        } else if (isObb) {
            val pkgs = getAndroidObbPackages(context, dir).size
            if (pkgs > 0) return pkgs
        }

        if (SafStorageManager.hasPermission(context, isObb)) {
            val safCount = SafStorageManager.getFolderItemCount(context, isObb)
            if (safCount > 0) return safCount
        }

        return 0
    }

    fun listViaShell(dir: File): List<File> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "ls -1A \"${dir.absolutePath}\" 2>/dev/null"))
            val lines = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            lines.filter { it.isNotBlank() }.map { File(dir, it.trim()) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun runShellCommand(cmd: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun runShellOutput(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val text = process.inputStream.bufferedReader().readText()
            process.waitFor()
            text
        } catch (e: Exception) {
            ""
        }
    }

    fun readFileRobust(context: Context, filePath: String): String {
        val file = File(filePath)

        // 1. Try standard File reading
        if (file.exists() && file.canRead()) {
            try {
                if (file.length() < 2 * 1024 * 1024) {
                    return file.readText(Charsets.UTF_8)
                }
                return "// File is too large to display directly (${file.length() / 1024} KB)"
            } catch (e: Exception) {
                // fall through
            }
        }

        // 2. Try Shell reading
        val shellOutput = runShellOutput("cat \"$filePath\" 2>/dev/null")
        if (shellOutput.isNotEmpty()) {
            return shellOutput
        }

        // 3. Try SAF reading if permission exists
        if (isRestrictedPath(filePath)) {
            val safContent = SafStorageManager.readFile(context, filePath)
            if (safContent != null) return safContent
        }

        return "// Cannot read file: Access not granted"
    }

    fun writeFileRobust(context: Context, filePath: String, content: String): Boolean {
        val file = File(filePath)

        // 1. Try standard File writing
        try {
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            return true
        } catch (e: Exception) {
            // fall through
        }

        // 2. Try shell writing
        try {
            val escaped = content.replace("'", "'\\''")
            val success = runShellCommand("printf '%s' '$escaped' > \"$filePath\"")
            if (success && file.exists()) return true
        } catch (e: Exception) {
            // fall through
        }

        // 3. Try SAF writing
        if (isRestrictedPath(filePath)) {
            return SafStorageManager.writeFile(context, filePath, content)
        }

        return false
    }

    fun deleteItem(context: Context, path: String): Boolean {
        val file = File(path)

        // 1. Try standard deletion
        try {
            if (file.exists() && file.deleteRecursively()) {
                return true
            }
        } catch (e: Exception) {
            // fall through
        }

        // 2. Try shell deletion
        if (runShellCommand("rm -rf \"$path\"")) {
            return true
        }

        // 3. Try SAF deletion
        if (isRestrictedPath(path)) {
            val safResult = SafStorageManager.deleteDocument(context, path)
            if (safResult) return true
        }

        return false
    }

    fun renameItem(context: Context, path: String, newName: String): Boolean {
        if (newName.isBlank()) return false
        val file = File(path)
        val target = File(file.parentFile, newName)

        // 1. Try standard rename
        try {
            if (file.exists() && file.renameTo(target)) {
                return true
            }
        } catch (e: Exception) {
            // fall through
        }

        // 2. Try shell rename
        if (runShellCommand("mv \"$path\" \"${target.absolutePath}\"")) {
            return true
        }

        // 3. Try SAF rename
        if (isRestrictedPath(path)) {
            val safResult = SafStorageManager.renameDocument(context, path, newName)
            if (safResult) return true
        }

        return false
    }

    fun createFolder(context: Context, parentPath: String, name: String): Boolean {
        if (name.isBlank()) return false
        val target = File(parentPath, name)

        // 1. Try standard mkdirs
        try {
            if (target.mkdirs()) return true
        } catch (e: Exception) {
            // fall through
        }

        // 2. Try shell mkdir
        if (runShellCommand("mkdir -p \"${target.absolutePath}\"")) {
            return true
        }

        // 3. Try SAF createDirectory
        if (isRestrictedPath(parentPath)) {
            val safResult = SafStorageManager.createDirectory(context, parentPath, name)
            if (safResult) return true
        }

        return false
    }

    fun createFile(context: Context, parentPath: String, name: String, content: String = ""): Boolean {
        if (name.isBlank()) return false
        val target = File(parentPath, name)

        // 1. Try standard file creation
        try {
            target.parentFile?.mkdirs()
            if (content.isNotEmpty()) {
                target.writeText(content, Charsets.UTF_8)
            } else {
                target.createNewFile()
            }
            return true
        } catch (e: Exception) {
            // fall through
        }

        // 2. Try shell touch
        if (runShellCommand("touch \"${target.absolutePath}\"")) {
            if (content.isNotEmpty()) {
                writeFileRobust(context, target.absolutePath, content)
            }
            return true
        }

        // 3. Try SAF createFile
        if (isRestrictedPath(parentPath)) {
            val safResult = SafStorageManager.createFile(context, parentPath, name, content)
            if (safResult) return true
        }

        return false
    }

    fun copyItem(context: Context, sourcePath: String, destinationDirPath: String): Boolean {
        val srcFile = File(sourcePath)
        val destDir = File(destinationDirPath)
        destDir.mkdirs()

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

        // 1. Try standard copy
        try {
            if (srcFile.isDirectory) {
                srcFile.copyRecursively(targetFile, overwrite = true)
            } else {
                srcFile.copyTo(targetFile, overwrite = true)
            }
            return true
        } catch (e: Exception) {
            // fall through
        }

        // 2. Try shell copy
        val shellCmd = if (srcFile.isDirectory) "cp -r \"$sourcePath\" \"${targetFile.absolutePath}\"" else "cp \"$sourcePath\" \"${targetFile.absolutePath}\""
        if (runShellCommand(shellCmd)) {
            return true
        }

        // 3. Try SAF copy
        if (isRestrictedPath(destinationDirPath)) {
            return if (srcFile.isDirectory) {
                SafStorageManager.createDirectory(context, destinationDirPath, targetName)
            } else {
                val content = readFileRobust(context, sourcePath)
                SafStorageManager.createFile(context, destinationDirPath, targetName, content)
            }
        }

        return false
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
