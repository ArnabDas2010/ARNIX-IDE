package com.arnix.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.ide.ui.theme.IdeBorder
import com.arnix.ide.ui.theme.IdeSurface
import com.arnix.ide.ui.theme.IdeSurfaceVariant
import com.arnix.ide.ui.theme.IdeTextPrimary

@Composable
fun QuickSymbolBar(
    onInsertSymbol: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val symbols = listOf(
        "TAB" to "    ",
        "{" to "{",
        "}" to "}",
        "(" to "(",
        ")" to ")",
        "[" to "[",
        "]" to "]",
        ";" to ";",
        "=" to "=",
        "\"" to "\"",
        "'" to "'",
        ":" to ":",
        "." to ".",
        "," to ",",
        "<" to "<",
        ">" to ">",
        "/" to "/",
        "\\" to "\\",
        "+" to "+",
        "-" to "-",
        "*" to "*",
        "!" to "!",
        "?" to "?",
        "&" to "&",
        "|" to "|",
        "$" to "$",
        "_" to "_",
        "#" to "#",
        "@" to "@",
        "->" to " -> "
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_symbol_bar"),
        color = IdeSurface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, com.arnix.ide.ui.theme.IdeBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            symbols.forEach { (label, insertValue) ->
                val keyColor = when (label) {
                    "TAB", "->" -> com.arnix.ide.ui.theme.IdeSecondary
                    "{", "}", "(", ")", "[", "]" -> com.arnix.ide.ui.theme.IdePrimary
                    ";", ":", ",", "." -> com.arnix.ide.ui.theme.IdeTextSecondary
                    "=", "+", "-", "*", "/", "\\" -> com.arnix.ide.ui.theme.IdeTertiary
                    "\"", "'" -> com.arnix.ide.ui.theme.IdeAccentViolet
                    else -> com.arnix.ide.ui.theme.IdeTextPrimary
                }

                Surface(
                    color = IdeSurfaceVariant.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.arnix.ide.ui.theme.IdeBorderSubtle),
                    modifier = Modifier
                        .heightIn(min = 34.dp, max = 40.dp)
                        .testTag("symbol_btn_$label")
                        .clickable { onInsertSymbol(insertValue) }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (label == "TAB" || label == "->") 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = keyColor
                        )
                    }
                }
            }
        }
    }
}
