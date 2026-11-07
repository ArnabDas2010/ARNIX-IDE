package com.arnix.ide.data.model

data class CodeTab(
    val id: String,
    val filePath: String,
    val fileName: String,
    val content: String,
    val isDirty: Boolean = false,
    val cursorPosition: Int = 0
)
