package com.arnix.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.launch

@Composable
fun CodeEditorView(
    code: String,
    extension: String,
    settings: IdeSettings,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    // Natural Pinch-Zoom scale factor: 0.5x to 3.0x
    var zoomFactor by remember { mutableFloatStateOf(1.0f) }

    // Dynamic scalable font and line dimensions
    val effectiveFontSize = (settings.fontSizeSp * zoomFactor).coerceIn(8f, 44f).sp
    val effectiveLineHeight = (settings.fontSizeSp * zoomFactor * 1.5f).coerceIn(12f, 66f).sp

    // Unified Monospace TextStyle for pixel-perfect line height alignment
    val commonTextStyle = TextStyle(
        fontSize = effectiveFontSize,
        lineHeight = effectiveLineHeight,
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
    val gutterWidth = (maxDigits * (settings.fontSizeSp * zoomFactor * 0.7f) + 20).dp.coerceAtLeast(36.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
            // Natural Gesture Handler:
            // 1. Tapping places cursor / types code normally
            // 2. 1-Finger drag moves corner-to-corner (2D freeform pan)
            // 3. 2-Finger pinch zooms in/out smoothly and pans simultaneously
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var pastTouchSlop = false
                    var totalPan = Offset.Zero
                    val touchSlop = viewConfiguration.touchSlop

                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.size

                        if (pointerCount >= 2) {
                            // Multi-touch: Natural pinch zoom & corner-to-corner 2D pan
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            if (zoom != 1f) {
                                zoomFactor = (zoomFactor * zoom).coerceIn(0.5f, 3.0f)
                            }
                            if (pan.x != 0f || pan.y != 0f) {
                                coroutineScope.launch {
                                    if (pan.x != 0f) horizontalScrollState.scrollBy(-pan.x)
                                    if (pan.y != 0f) verticalScrollState.scrollBy(-pan.y)
                                }
                            }
                            event.changes.forEach { it.consume() }
                        } else if (pointerCount == 1) {
                            val change = event.changes.first()
                            val dragDelta = change.position - change.previousPosition
                            totalPan += dragDelta

                            if (!pastTouchSlop && totalPan.getDistance() > touchSlop) {
                                pastTouchSlop = true
                            }

                            // Once touch slop is passed, move both axes simultaneously (corner-to-corner)
                            if (pastTouchSlop && (dragDelta.x != 0f || dragDelta.y != 0f)) {
                                coroutineScope.launch {
                                    if (dragDelta.x != 0f) horizontalScrollState.scrollBy(-dragDelta.x)
                                    if (dragDelta.y != 0f) verticalScrollState.scrollBy(-dragDelta.y)
                                }
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .testTag("code_editor_view")
    ) {
        // Shared 2D scroll canvas
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // 1. Line numbers gutter (Fixed to left, drag on gutter also scrolls in 2D freely)
            if (settings.showLineNumbers) {
                Box(
                    modifier = Modifier
                        .width(gutterWidth)
                        .background(IdeSurface)
                        .drawBehind {
                            drawLine(
                                color = IdeBorderSubtle,
                                start = Offset(size.width, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(top = 12.dp, bottom = 400.dp, start = 4.dp, end = 8.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    if (dragAmount.x != 0f) horizontalScrollState.scrollBy(-dragAmount.x)
                                    if (dragAmount.y != 0f) verticalScrollState.scrollBy(-dragAmount.y)
                                }
                            }
                        }
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

            // 2. Editable Code Area with generous 2D overscroll for corner-to-corner freeform positioning
            val codeScrollModifier = if (settings.wordWrap) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.horizontalScroll(horizontalScrollState)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(codeScrollModifier)
                    // Generous end & bottom overscroll allows moving any code corner-to-corner freely
                    .padding(top = 12.dp, bottom = 450.dp, start = 12.dp, end = 650.dp)
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
