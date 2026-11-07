package com.arnix.ide.data.model

data class IdeSettings(
    val fontSizeSp: Float = 13f,
    val tabSize: Int = 4,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val autoSave: Boolean = true,
    val themeName: String = "Cyber Dark",
    val editorFont: String = "Monospace"
)

data class TerminalLine(
    val id: String,
    val text: String,
    val type: TerminalLineType = TerminalLineType.OUTPUT
)

enum class TerminalLineType {
    PROMPT,
    OUTPUT,
    SUCCESS,
    ERROR,
    INFO
}
