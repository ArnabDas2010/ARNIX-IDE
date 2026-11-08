package com.arnix.ide.data.model

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileManagerItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeFormatted: String,
    val lastModifiedFormatted: String,
    val extension: String,
    val isHidden: Boolean,
    val itemCount: Int = 0
) {
    companion object {
        fun fromFile(file: File): FileManagerItem {
            val isDir = file.isDirectory
            val ext = if (isDir) "" else file.extension
            val count = if (isDir) (file.listFiles()?.size ?: 0) else 0

            val sizeStr = if (isDir) {
                "$count items"
            } else {
                formatFileSize(file.length())
            }

            val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            val modifiedStr = dateFormat.format(Date(file.lastModified()))

            return FileManagerItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = isDir,
                sizeFormatted = sizeStr,
                lastModifiedFormatted = modifiedStr,
                extension = ext,
                isHidden = file.name.startsWith("."),
                itemCount = count
            )
        }

        private fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
        }
    }
}
