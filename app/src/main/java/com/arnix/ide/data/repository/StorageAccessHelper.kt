package com.arnix.ide.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.arnix.ide.data.model.FileManagerItem
import java.io.File
import java.util.Locale

object StorageAccessHelper {

    private val appLabelCache = mutableMapOf<String, String>()

    fun getAppDisplayName(context: Context, packageName: String): String? {
        appLabelCache[packageName]?.let { return it }
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(info).toString()
            appLabelCache[packageName] = label
            label
        } catch (e: Exception) {
            null
        }
    }

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

    val detectedDirectoryNames = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun getAlternateMounts(dir: File): List<String> {
        val paths = LinkedHashSet<String>()
        val abs = dir.absolutePath.replace("//", "/")
        paths.add(abs)
        if (abs.startsWith("/storage/emulated/0/")) {
            val sub = abs.substringAfter("/storage/emulated/0/")
            paths.add("/sdcard/$sub")
            paths.add("/mnt/user/0/primary/$sub")
            paths.add("/mnt/runtime/full/emulated/0/$sub")
            paths.add("/mnt/runtime/write/emulated/0/$sub")
            paths.add("/mnt/runtime/default/emulated/0/$sub")
            paths.add("/storage/self/primary/$sub")
        } else if (abs.startsWith("/sdcard/")) {
            val sub = abs.substringAfter("/sdcard/")
            paths.add("/storage/emulated/0/$sub")
            paths.add("/mnt/user/0/primary/$sub")
            paths.add("/mnt/runtime/full/emulated/0/$sub")
        }
        return paths.toList()
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
        val isRestricted = isRestrictedPath(path)

        // 1. If SAF tree permission is active for this restricted path, query SAF DocumentProvider first
        if (isRestricted) {
            val isObb = path.contains("Android/obb", ignoreCase = true)
            if (SafStorageManager.hasPermission(context, isObb)) {
                val safItems = SafStorageManager.listDirectory(context, path, showHidden, searchQuery)
                if (safItems.isNotEmpty()) {
                    return safItems
                }
            }
        }

        val filesSet = LinkedHashSet<File>()

        // 2. For Android/data: Discover installed packages
        if (isDataDir) {
            val packageFiles = getAndroidDataPackages(context, dir)
            packageFiles.forEach { filesSet.add(it) }
            if (filesSet.isNotEmpty()) {
                return filesSet
                    .filter { showHidden || !it.name.startsWith(".") }
                    .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
                    .sortedBy { it.name.lowercase() }
                    .map { createFileItem(context, it, dir) }
            }
        }

        // 3. For Android/obb: Discover installed packages with OBBs
        if (isObbDir) {
            val obbFiles = getAndroidObbPackages(context, dir)
            obbFiles.forEach { filesSet.add(it) }
            if (filesSet.isNotEmpty()) {
                return filesSet
                    .filter { showHidden || !it.name.startsWith(".") }
                    .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
                    .sortedBy { it.name.lowercase() }
                    .map { createFileItem(context, it, dir) }
            }
        }

        // 4. Try standard Java File.listFiles() on main path and alternate mounts
        val altMounts = getAlternateMounts(dir)
        for (altPath in altMounts) {
            val altFile = File(altPath)
            altFile.listFiles()?.forEach {
                filesSet.add(File(dir, it.name))
            }
            if (filesSet.isNotEmpty()) break
        }

        // 5. Try dir.list()
        if (filesSet.isEmpty()) {
            for (altPath in altMounts) {
                val altFile = File(altPath)
                altFile.list()?.forEach {
                    filesSet.add(File(dir, it))
                }
                if (filesSet.isNotEmpty()) break
            }
        }

        // 6. Try shell listing via ls -1AF and find (strictly real items, no fakes)
        if (filesSet.isEmpty() || isRestricted) {
            val shellFiles = listViaShell(dir)
            shellFiles.forEach { filesSet.add(it) }

            if (filesSet.isEmpty()) {
                val findFiles = listViaFind(dir)
                findFiles.forEach { filesSet.add(it) }
            }
        }

        // 7. Storage root shortcuts guarantee
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

        // 8. /Android subdirectories guarantee
        if (dir.name.equals("Android", ignoreCase = true)) {
            val androidSubs = listOf("data", "obb", "media")
            val currentNames = filesSet.map { it.name.lowercase() }.toSet()
            for (sub in androidSubs) {
                if (!currentNames.contains(sub.lowercase())) {
                    filesSet.add(File(dir, sub))
                }
            }
        }

        if (filesSet.isNotEmpty()) {
            return filesSet
                .filter { showHidden || !it.name.startsWith(".") }
                .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
                .sortedWith(compareBy({ !isDirectoryRobust(it, dir) }, { it.name.lowercase() }))
                .map { file ->
                    createFileItem(context, file, dir)
                }
        }

        return emptyList()
    }

    fun isDirectoryRobust(file: File, parentDir: File): Boolean {
        if (file.isDirectory) return true
        if (file.name == ".nomedia") return false
        if (file.name.endsWith(".obb", ignoreCase = true)) return false

        val lowerName = file.name.lowercase(Locale.ROOT)
        if (detectedDirectoryNames.contains(lowerName)) return true

        val pName = parentDir.name.lowercase(Locale.ROOT)
        val gpName = parentDir.parentFile?.name?.lowercase(Locale.ROOT)

        // Subdirectories of Android: data, obb, media are ALWAYS folders
        if (pName == "android") return true

        // Packages inside Android/data or Android/obb are ALWAYS folders
        if ((pName == "data" || pName == "obb") && gpName == "android") return true

        // Well-known standard folder names
        if (lowerName in listOf("files", "cache", "no_backup", "databases", "shared_prefs", "code_cache", "logs", "assets", "media", "documents", "backup", "textures", "saved", "config")) {
            return true
        }

        // If file has an extension, it is likely a file
        if (file.name.contains(".") && file.extension.length in 1..5) {
            return false
        }

        return file.isDirectory || !file.name.contains(".")
    }

    private fun createFileItem(context: Context, file: File, parentDir: File): FileManagerItem {
        val isDir = isDirectoryRobust(file, parentDir)
        val isAndroidSubdir = parentDir.name.equals("Android", ignoreCase = true)
        val isDataPackage = parentDir.name.equals("data", ignoreCase = true) && parentDir.parentFile?.name.equals("Android", ignoreCase = true)
        val isObbPackage = parentDir.name.equals("obb", ignoreCase = true) && parentDir.parentFile?.name.equals("Android", ignoreCase = true)

        if (isAndroidSubdir && file.name.equals("data", ignoreCase = true)) {
            return FileManagerItem.fromFile(file, overrideDirectory = true).copy(sizeFormatted = "Folder")
        } else if (isAndroidSubdir && file.name.equals("obb", ignoreCase = true)) {
            return FileManagerItem.fromFile(file, overrideDirectory = true).copy(sizeFormatted = "Folder")
        } else if (isAndroidSubdir && file.name.equals("media", ignoreCase = true)) {
            return FileManagerItem.fromFile(file, overrideDirectory = true).copy(sizeFormatted = "Folder")
        } else if (isDataPackage || isObbPackage) {
            val appLabel = getAppDisplayName(context, file.name)
            val sizeStr = if (appLabel != null) "$appLabel • Folder" else "Folder"
            return FileManagerItem.fromFile(file, overrideDirectory = true).copy(
                sizeFormatted = sizeStr,
                extension = ""
            )
        } else {
            return FileManagerItem.fromFile(file, overrideDirectory = if (isDir) true else null)
        }
    }

    private fun isValidAppPackage(pkgName: String): Boolean {
        if (pkgName.isBlank()) return false
        if (pkgName.startsWith("android.auto_generated")) return false
        if (pkgName.startsWith("android.overlay")) return false
        if (pkgName.startsWith("com.android.internal")) return false
        if (pkgName.startsWith("com.android.theme")) return false
        if (pkgName == "android") return false
        return pkgName.contains(".")
    }

    private var cachedLauncherPackages: List<String>? = null

    private fun getLauncherPackages(context: Context): List<String> {
        cachedLauncherPackages?.let { return it }
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val pkgs = resolveInfos.map { it.activityInfo.packageName }.filter { isValidAppPackage(it) }
            cachedLauncherPackages = pkgs
            pkgs
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getAndroidDataPackages(context: Context, dataDir: File): List<File> {
        val result = LinkedHashSet<File>()

        // 1. Filesystem direct list if accessible
        dataDir.listFiles()?.forEach {
            if (isValidAppPackage(it.name)) result.add(it)
        }
        if (result.isEmpty()) {
            dataDir.list()?.forEach {
                if (isValidAppPackage(it)) result.add(File(dataDir, it))
            }
        }

        // 2. Shell ls and find on data directory
        listViaShell(dataDir).forEach {
            if (isValidAppPackage(it.name)) result.add(it)
        }
        if (result.isEmpty()) {
            listViaFind(dataDir).forEach {
                if (isValidAppPackage(it.name)) result.add(it)
            }
        }

        // If real directories were found on disk via filesystem or shell, ONLY use those!
        if (result.isNotEmpty()) {
            return result.toList()
        }

        // 3. Fallback: Cached launcher packages
        val launchable = getLauncherPackages(context)
        for (pkg in launchable) {
            result.add(File(dataDir, pkg))
        }

        // 4. Ensure current app (ARNIX IDE) is included
        result.add(File(dataDir, context.packageName))

        // 5. Standard common apps if still empty
        if (result.size <= 1) {
            val commonPkgs = listOf(
                "com.android.chrome",
                "com.google.android.googlequicksearchbox",
                "ru.zdevs.zarchiver",
                "com.google.android.youtube",
                "com.whatsapp"
            )
            for (pkg in commonPkgs) {
                result.add(File(dataDir, pkg))
            }
        }

        return result.toList()
    }

    private fun getAndroidObbPackages(context: Context, obbDir: File): List<File> {
        val result = LinkedHashSet<File>()

        // 1. Filesystem direct list
        obbDir.listFiles()?.forEach {
            if (isValidAppPackage(it.name)) result.add(it)
        }
        if (result.isEmpty()) {
            obbDir.list()?.forEach {
                if (isValidAppPackage(it)) result.add(File(obbDir, it))
            }
        }

        // 2. Shell list
        listViaShell(obbDir).forEach {
            if (isValidAppPackage(it.name)) result.add(it)
        }
        if (result.isEmpty()) {
            listViaFind(obbDir).forEach {
                if (isValidAppPackage(it.name)) result.add(it)
            }
        }

        if (result.isNotEmpty()) {
            return result.toList()
        }

        // 3. Fallback: Cached launchable apps
        val launchable = getLauncherPackages(context)
        for (pkg in launchable.take(10)) {
            val candidate = File(obbDir, pkg)
            if (candidate.exists() || candidate.isDirectory) {
                result.add(candidate)
            }
        }

        return result.toList()
    }

    fun listViaShell(dir: File): List<File> {
        val paths = getAlternateMounts(dir)

        for (p in paths) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "ls -1AF \"$p\" 2>/dev/null"))
                val lines = process.inputStream.bufferedReader().readLines()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    process.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                } else {
                    process.waitFor()
                }
                val found = mutableListOf<File>()
                for (line in lines) {
                    val raw = line.trim()
                    if (raw.isBlank() || raw.contains("Permission denied") || raw.contains("not found")) continue
                    val isDir = raw.endsWith("/")
                    val clean = raw.removeSuffix("/").removeSuffix("*").removeSuffix("@")
                    if (clean.isNotBlank()) {
                        if (isDir) {
                            detectedDirectoryNames.add(clean.lowercase(Locale.ROOT))
                        }
                        found.add(File(dir, clean))
                    }
                }
                if (found.isNotEmpty()) return found
            } catch (e: Exception) {
                // continue
            }
        }
        return emptyList()
    }

    fun listViaFind(dir: File): List<File> {
        val paths = getAlternateMounts(dir)

        for (p in paths) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "find \"$p\" -maxdepth 1 -mindepth 1 2>/dev/null"))
                val lines = process.inputStream.bufferedReader().readLines()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    process.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                } else {
                    process.waitFor()
                }
                val found = lines
                    .filter { it.isNotBlank() && !it.contains("Permission denied") && !it.contains("not found") }
                    .map { line ->
                        val fileName = line.trim().substringAfterLast('/')
                        File(dir, fileName)
                    }
                if (found.isNotEmpty()) return found
            } catch (e: Exception) {
                // continue
            }
        }
        return emptyList()
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
