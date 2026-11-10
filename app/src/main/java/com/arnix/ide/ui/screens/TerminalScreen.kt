package com.arnix.ide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.ide.data.model.TerminalLine
import com.arnix.ide.data.model.TerminalLineType
import com.arnix.ide.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(
    lines: List<TerminalLine>,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom on new lines
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(lines.size - 1)
            }
        }
    }

    val quickCommands = listOf("help", "run", "ls", "git status", "clear")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
            .testTag("terminal_screen")
    ) {
        // Terminal Header
        Surface(
            color = IdeSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(9.dp).background(Color(0xFFEF4444), shape = androidx.compose.foundation.shape.CircleShape))
                    Box(modifier = Modifier.size(9.dp).background(Color(0xFFF59E0B), shape = androidx.compose.foundation.shape.CircleShape))
                    Box(modifier = Modifier.size(9.dp).background(Color(0xFF10B981), shape = androidx.compose.foundation.shape.CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TERMINAL & CONSOLE",
                        color = IdeTextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    color = IdeSurfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "bash",
                        color = TerminalCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Quick Command Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IdeSurface.copy(alpha = 0.5f))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickCommands.forEach { cmd ->
                Surface(
                    color = IdeSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorderSubtle),
                    modifier = Modifier
                        .clickable { onExecuteCommand(cmd) }
                        .testTag("chip_cmd_$cmd")
                ) {
                    Text(
                        text = cmd,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = IdeTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Output Log Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(lines, key = { it.id }) { line ->
                val textColor = when (line.type) {
                    TerminalLineType.PROMPT -> TerminalCyan
                    TerminalLineType.OUTPUT -> IdeTextPrimary
                    TerminalLineType.SUCCESS -> TerminalGreen
                    TerminalLineType.ERROR -> TerminalRed
                    TerminalLineType.INFO -> TerminalYellow
                }

                Text(
                    text = line.text,
                    color = textColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Command Input Bar (Auto-adjusts for keyboard via imePadding)
        Surface(
            color = IdeSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    color = TerminalPrompt,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { currentInput = it },
                    placeholder = {
                        Text("Type command...", color = IdeTextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_input_field"),
                    textStyle = TextStyle(
                        color = IdeTextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (currentInput.isNotBlank()) {
                                onExecuteCommand(currentInput)
                                currentInput = ""
                            }
                        }
                    )
                )

                IconButton(
                    onClick = {
                        if (currentInput.isNotBlank()) {
                            onExecuteCommand(currentInput)
                            currentInput = ""
                        }
                    },
                    modifier = Modifier.testTag("btn_terminal_send")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send command",
                        tint = IdePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
