package com.arnix.ide.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val extension: String = "",
    val content: String = "",
    val children: List<FileItem> = emptyList(),
    val isExpanded: Boolean = false,
    val isReadOnly: Boolean = false
) {
    val iconName: String
        get() = when {
            isDirectory -> if (isExpanded) "folder_open" else "folder"
            name.endsWith(".kt") -> "kotlin"
            name.endsWith(".java") -> "java"
            name.endsWith(".xml") -> "xml"
            name.endsWith(".gradle.kts") || name.endsWith(".gradle") -> "gradle"
            name.endsWith(".js") || name.endsWith(".ts") -> "javascript"
            name.endsWith(".html") -> "html"
            name.endsWith(".css") -> "css"
            name.endsWith(".py") -> "python"
            name.endsWith(".json") -> "json"
            name.endsWith(".md") -> "markdown"
            else -> "file"
        }
}
