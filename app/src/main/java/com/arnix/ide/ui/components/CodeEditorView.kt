package com.arnix.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
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

    val lines = code.split("\n")
    val lineCount = lines.size.coerceAtLeast(1)

    val fontSize = settings.fontSizeSp.sp
    val lineHeight = (settings.fontSizeSp * 1.45f).sp

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
            .testTag("code_editor_view")
    ) {
        // Line numbers column
        if (settings.showLineNumbers) {
            Column(
                modifier = Modifier
                    .verticalScroll(verticalScrollState)
                    .background(IdeSurface)
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .widthIn(min = 36.dp)
                    .testTag("line_numbers_column")
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = "$i",
                        color = IdeTextMuted,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        fontFamily = FontFamily.Monospace,
                        style = TextStyle(fontSize = fontSize, lineHeight = lineHeight)
                    )
                }
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(IdeBorder)
            )
        }

        // Editable Code Area
        val scrollModifier = if (settings.wordWrap) {
            Modifier.verticalScroll(verticalScrollState)
        } else {
            Modifier
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(scrollModifier)
                .padding(horizontal = 12.dp, vertical = 12.dp)
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
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("code_text_field"),
                textStyle = TextStyle(
                    color = IdeTextPrimary,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(IdePrimary),
                visualTransformation = visualTransformation
            )
        }
    }
}
