package com.arnix.ide.ui.components

import androidx.compose.foundation.background
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
        border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            symbols.forEach { (label, insertValue) ->
                Button(
                    onClick = { onInsertSymbol(insertValue) },
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("symbol_btn_$label"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IdeSurfaceVariant,
                        contentColor = IdeTextPrimary
                    )
                ) {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (label == "TAB") 11.sp else 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
