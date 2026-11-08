package com.arnix.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.ide.data.model.IdeSettings
import com.arnix.ide.ui.theme.*

@Composable
fun CodeEditorView(
    code: String,
    extension: String,
    settings: IdeSettings,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val fontSize = settings.fontSizeSp.sp
    val lineHeight = (settings.fontSizeSp * 1.5f).sp

    // Unified Monospace TextStyle for 100% pixel-perfect line height alignment
    val commonTextStyle = TextStyle(
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontFamily = FontFamily.Monospace,
        platformStyle = PlatformTextStyle(
            includeFontPadding = false
        ),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )

    // Calculate line count and line numbers text
    val lines = remember(code) { code.split("\n") }
    val lineCount = lines.size.coerceAtLeast(1)

    val lineNumbersText = remember(code, textLayoutResult, settings.wordWrap) {
        val layout = textLayoutResult
        if (layout != null && settings.wordWrap) {
            // When word wrap is ON, only print line numbers on the first visual line of each logical line
            val sb = StringBuilder()
            var logicalLine = 1
            for (v in 0 until layout.lineCount) {
                val startOffset = layout.getLineStart(v)
                if (v == 0 || (startOffset > 0 && code.getOrNull(startOffset - 1) == '\n')) {
                    sb.append(logicalLine)
                    logicalLine++
                }
                if (v < layout.lineCount - 1) {
                    sb.append("\n")
                }
            }
            sb.toString()
        } else {
            (1..lineCount).joinToString("\n")
        }
    }

    val maxDigits = lineCount.toString().length.coerceAtLeast(2)
    val gutterWidth = (maxDigits * (settings.fontSizeSp * 0.7f) + 20).dp.coerceAtLeast(38.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
            .testTag("code_editor_view")
    ) {
        // SINGLE shared vertical scroll container - Gutter and Code move together 1:1
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // 1. Line numbers gutter (fixed width on left side)
            if (settings.showLineNumbers) {
                Box(
                    modifier = Modifier
                        .width(gutterWidth)
                        .background(IdeSurface)
                        .drawBehind {
                            // Vertical border line on the right edge of the gutter
                            drawLine(
                                color = IdeBorder,
                                start = Offset(size.width, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(top = 12.dp, bottom = 120.dp, start = 4.dp, end = 8.dp)
                        .testTag("line_numbers_column")
                ) {
                    Text(
                        text = lineNumbersText,
                        style = commonTextStyle.copy(
                            color = IdeTextMuted,
                            textAlign = TextAlign.End
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Editable Code Area (takes all remaining space on the right)
            val codeScrollModifier = if (settings.wordWrap) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.horizontalScroll(horizontalScrollState)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(codeScrollModifier)
                    .padding(top = 12.dp, bottom = 120.dp, start = 12.dp, end = 24.dp)
            ) {
                val visualTransformation = VisualTransformation { text ->
                    val annotated = SyntaxHighlighter.highlightCode(text.text, extension)
                    androidx.compose.ui.text.input.TransformedText(
                        annotated,
                        androidx.compose.ui.text.input.OffsetMapping.Identity
                    )
                }

                BasicTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    onTextLayout = { textLayoutResult = it },
                    modifier = Modifier
                        .then(if (settings.wordWrap) Modifier.fillMaxWidth() else Modifier.wrapContentSize())
                        .testTag("code_text_field"),
                    textStyle = commonTextStyle.copy(color = IdeTextPrimary),
                    cursorBrush = SolidColor(IdePrimary),
                    visualTransformation = visualTransformation
                )
            }
        }
    }
}
