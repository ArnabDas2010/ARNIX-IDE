package com.arnix.ide.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ArchiveEntryItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val compressedSizeBytes: Long,
    val formattedSize: String,
    val formattedDate: String,
    val extension: String = if (isDirectory) "" else name.substringAfterLast(".", "")
)

object ArchiveHelper {

    suspend fun listArchive(file: File): List<ArchiveEntryItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ArchiveEntryItem>()
        val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

        try {
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val cleanPath = entry.name.trimEnd('/')
                    val displayName = cleanPath.substringAfterLast('/')
                    val isDir = entry.isDirectory
                    val size = if (entry.size >= 0) entry.size else 0L
                    val compSize = if (entry.compressedSize >= 0) entry.compressedSize else 0L
                    val time = if (entry.time > 0) entry.time else file.lastModified()

                    list.add(
                        ArchiveEntryItem(
                            name = displayName.ifEmpty { cleanPath },
                            path = entry.name,
                            isDirectory = isDir,
                            sizeBytes = size,
                            compressedSizeBytes = compSize,
                            formattedSize = if (isDir) "Folder" else formatSize(size),
                            formattedDate = dateFormat.format(Date(time))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    suspend fun readArchiveEntryText(archiveFile: File, entryPath: String): String = withContext(Dispatchers.IO) {
        try {
            ZipFile(archiveFile).use { zip ->
                val entry = zip.getEntry(entryPath) ?: return@withContext "// File not found inside archive"
                if (entry.size > 2 * 1024 * 1024) {
                    return@withContext "// File is too large to preview directly (${entry.size / 1024} KB)"
                }
                zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        } catch (e: Exception) {
            "// Failed to read file from archive: ${e.localizedMessage}"
        }
    }

    suspend fun extractAll(
        archiveFile: File,
        targetDir: File,
        onProgress: (extractedCount: Int, currentFile: String) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            targetDir.mkdirs()
            val buffer = ByteArray(16384)
            var extracted = 0

            ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val outFile = File(targetDir, entry.name)

                    // Prevent directory traversal attacks
                    if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            BufferedOutputStream(fos).use { bos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    bos.write(buffer, 0, len)
                                }
                            }
                        }
                        extracted++
                        withContext(Dispatchers.Main) {
                            onProgress(extracted, outFile.name)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createZip(
        files: List<File>,
        outputZip: File,
        baseDir: File = files.firstOrNull()?.parentFile ?: File("/")
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            outputZip.parentFile?.mkdirs()
            val buffer = ByteArray(16384)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZip))).use { zos ->
                for (file in files) {
                    addFileToZip(file, baseDir, zos, buffer)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addFileToZip(file: File, baseDir: File, zos: ZipOutputStream, buffer: ByteArray) {
        val relPath = file.relativeToOrSelf(baseDir).path.replace('\\', '/')
        if (file.isDirectory) {
            val dirEntryName = if (relPath.endsWith("/")) relPath else "$relPath/"
            zos.putNextEntry(ZipEntry(dirEntryName))
            zos.closeEntry()
            file.listFiles()?.forEach { child ->
                addFileToZip(child, baseDir, zos, buffer)
            }
        } else {
            zos.putNextEntry(ZipEntry(relPath))
            FileInputStream(file).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    var len: Int
                    while (bis.read(buffer).also { len = it } > 0) {
                        zos.write(buffer, 0, len)
                    }
                }
            }
            zos.closeEntry()
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }
}
