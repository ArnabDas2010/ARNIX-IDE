package com.arnix.ide.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arnix.ide.R
import com.arnix.ide.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ConsoleLogItem(
    val id: String = UUID.randomUUID().toString(),
    val level: ConsoleMessage.MessageLevel,
    val message: String,
    val sourceId: String,
    val lineNumber: Int,
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePreviewDialog(
    fileName: String,
    fileContent: String,
    parentDirectory: File?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val consoleLogs = remember { mutableStateListOf<ConsoleLogItem>() }
    var showConsoleDrawer by remember { mutableStateOf(false) }
    var selectedLevelFilter by remember { mutableStateOf<ConsoleMessage.MessageLevel?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val isMarkdown = fileName.endsWith(".md", ignoreCase = true) || fileName.endsWith(".markdown", ignoreCase = true)

    val processedHtml = remember(fileContent, isMarkdown, refreshKey) {
        if (isMarkdown) {
            renderMarkdownToHtml(fileName, fileContent)
        } else {
            fileContent
        }
    }

    val baseUrl = remember(parentDirectory) {
        if (parentDirectory != null && parentDirectory.exists()) {
            "file://${parentDirectory.absolutePath}/"
        } else {
            "file:///android_asset/"
        }
    }

    val errorCount = consoleLogs.count { it.level == ConsoleMessage.MessageLevel.ERROR }
    val warnCount = consoleLogs.count { it.level == ConsoleMessage.MessageLevel.WARNING }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("live_preview_dialog"),
            color = IdeBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Surface(
                    color = IdeSurface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, IdeBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title & Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isMarkdown) IdeAccentViolet.copy(alpha = 0.2f) else IdeSecondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = if (isMarkdown) R.drawable.ic_document else R.drawable.ic_file_code),
                                    contentDescription = null,
                                    tint = if (isMarkdown) IdeAccentViolet else IdeSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = fileName,
                                    color = IdeTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isMarkdown) "Markdown Reader" else "Live Web Preview",
                                    color = IdeTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Reload button
                            IconButton(
                                onClick = {
                                    refreshKey++
                                    consoleLogs.clear()
                                    webViewInstance?.reload()
                                },
                                modifier = Modifier.size(34.dp).testTag("preview_reload_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = IdeTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Open in external browser / viewer
                            IconButton(
                                onClick = {
                                    try {
                                        if (parentDirectory != null) {
                                            val file = File(parentDirectory, fileName)
                                            if (file.exists()) {
                                                val uri = Uri.fromFile(file)
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, if (isMarkdown) "text/plain" else "text/html")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } else {
                                                Toast.makeText(context, "Save file before opening externally", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(34.dp).testTag("preview_external_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Open externally",
                                    tint = IdeTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Console Drawer Toggle Button with Error/Warn Badge
                            if (!isMarkdown) {
                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(if (showConsoleDrawer) IdePrimary.copy(alpha = 0.2f) else IdeSurfaceVariant)
                                        .clickable { showConsoleDrawer = !showConsoleDrawer }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_terminal),
                                            contentDescription = "Console",
                                            tint = if (showConsoleDrawer) IdePrimary else IdeTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Console",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (showConsoleDrawer) IdePrimary else IdeTextSecondary
                                        )
                                        if (errorCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(IdeError),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = errorCount.toString(),
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else if (consoleLogs.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(IdePrimary.copy(alpha = 0.3f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = consoleLogs.size.toString(),
                                                    color = IdePrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Close button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(34.dp).testTag("preview_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_close),
                                    tint = IdeTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Main WebView Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    databaseEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    cacheMode = WebSettings.LOAD_NO_CACHE
                                }

                                webViewClient = object : WebViewClient() {}

                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                                        consoleLogs.add(
                                            ConsoleLogItem(
                                                level = consoleMessage.messageLevel(),
                                                message = consoleMessage.message(),
                                                sourceId = consoleMessage.sourceId()?.substringAfterLast("/") ?: "",
                                                lineNumber = consoleMessage.lineNumber()
                                            )
                                        )
                                        return true
                                    }
                                }

                                webViewInstance = this
                                loadDataWithBaseURL(baseUrl, processedHtml, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            webViewInstance = webView
                            webView.loadDataWithBaseURL(baseUrl, processedHtml, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Collapsible Console Drawer
                AnimatedVisibility(visible = showConsoleDrawer && !isMarkdown) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .testTag("preview_console_drawer"),
                        color = IdeSurface,
                        border = BorderStroke(1.dp, IdeBorderSubtle)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Console Toolbar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(IdeBackground)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "DevTools Console",
                                        color = IdeTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )

                                    // Filter pills
                                    FilterBadge(
                                        label = "All (${consoleLogs.size})",
                                        isSelected = selectedLevelFilter == null,
                                        color = IdeTextSecondary,
                                        onClick = { selectedLevelFilter = null }
                                    )

                                    if (errorCount > 0) {
                                        FilterBadge(
                                            label = "Errors ($errorCount)",
                                            isSelected = selectedLevelFilter == ConsoleMessage.MessageLevel.ERROR,
                                            color = IdeError,
                                            onClick = { selectedLevelFilter = ConsoleMessage.MessageLevel.ERROR }
                                        )
                                    }

                                    if (warnCount > 0) {
                                        FilterBadge(
                                            label = "Warn ($warnCount)",
                                            isSelected = selectedLevelFilter == ConsoleMessage.MessageLevel.WARNING,
                                            color = IdeTertiary,
                                            onClick = { selectedLevelFilter = ConsoleMessage.MessageLevel.WARNING }
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { consoleLogs.clear() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_delete),
                                            contentDescription = "Clear logs",
                                            tint = IdeTextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showConsoleDrawer = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Hide console",
                                            tint = IdeTextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Logs List
                            val filteredLogs = consoleLogs.filter { log ->
                                selectedLevelFilter == null || log.level == selectedLevelFilter
                            }

                            if (filteredLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No console messages",
                                        color = IdeTextMuted,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(filteredLogs, key = { it.id }) { log ->
                                        ConsoleLogRow(log)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBadge(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) color else IdeBorderSubtle),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) color else IdeTextMuted,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ConsoleLogRow(log: ConsoleLogItem) {
    val (icon, color, bg) = when (log.level) {
        ConsoleMessage.MessageLevel.ERROR -> Triple(Icons.Default.Warning, IdeError, IdeError.copy(alpha = 0.08f))
        ConsoleMessage.MessageLevel.WARNING -> Triple(Icons.Default.Warning, IdeTertiary, IdeTertiary.copy(alpha = 0.08f))
        ConsoleMessage.MessageLevel.DEBUG -> Triple(Icons.Default.Info, IdeAccentViolet, Color.Transparent)
        else -> Triple(Icons.Default.Info, IdePrimary, Color.Transparent)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp).padding(top = 1.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.message,
                color = IdeTextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            if (log.sourceId.isNotEmpty() || log.lineNumber > 0) {
                Text(
                    text = "${log.sourceId}:${log.lineNumber} • ${log.timestamp}",
                    color = IdeTextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }
    }
}

/**
 * Converts standard Markdown text into an obsidian-themed HTML page with GitHub Dark styling
 */
private fun renderMarkdownToHtml(title: String, markdown: String): String {
    // Convert basic markdown tags to styled HTML
    var htmlBody = markdown
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    // Code blocks ```code```
    val codeBlockRegex = Regex("```([a-zA-Z0-9]*)\\n([\\s\\S]*?)```")
    htmlBody = codeBlockRegex.replace(htmlBody) { match ->
        val lang = match.groupValues[1]
        val code = match.groupValues[2]
        """<div class="code-box"><div class="code-lang">$lang</div><pre><code>$code</code></pre></div>"""
    }

    // Inline `code`
    htmlBody = htmlBody.replace(Regex("`([^`]+)`"), "<code>$1</code>")

    // Headings
    htmlBody = htmlBody.replace(Regex("^### (.*)$", RegexOption.MULTILINE), "<h3>$1</h3>")
    htmlBody = htmlBody.replace(Regex("^## (.*)$", RegexOption.MULTILINE), "<h2>$1</h2>")
    htmlBody = htmlBody.replace(Regex("^# (.*)$", RegexOption.MULTILINE), "<h1>$1</h1>")

    // Bold & Italic
    htmlBody = htmlBody.replace(Regex("\\*\\*([^*]+)\\*\\*"), "<strong>$1</strong>")
    htmlBody = htmlBody.replace(Regex("\\*([^*]+)\\*"), "<em>$1</em>")

    // Blockquotes
    htmlBody = htmlBody.replace(Regex("^> (.*)$", RegexOption.MULTILINE), "<blockquote>$1</blockquote>")

    // Unordered lists
    htmlBody = htmlBody.replace(Regex("^[*-] (.*)$", RegexOption.MULTILINE), "<li>$1</li>")

    // Paragraph line breaks
    htmlBody = htmlBody.replace("\n\n", "</p><p>").replace("\n", "<br/>")

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            <title>$title</title>
            <style>
                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    padding: 20px;
                    background-color: #090D16;
                    color: #E2E8F0;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    font-size: 15px;
                    line-height: 1.6;
                }
                h1, h2, h3 { color: #38BDF8; font-weight: 700; margin-top: 24px; margin-bottom: 12px; }
                h1 { font-size: 24px; border-bottom: 1px solid #1E293B; padding-bottom: 8px; }
                h2 { font-size: 20px; border-bottom: 1px solid #1E293B; padding-bottom: 6px; }
                h3 { font-size: 17px; }
                p { margin: 8px 0; }
                code {
                    background: #1E293B;
                    color: #34D399;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: monospace;
                    font-size: 13px;
                }
                .code-box {
                    background: #111827;
                    border: 1px solid #1E293B;
                    border-radius: 8px;
                    margin: 14px 0;
                    overflow: hidden;
                }
                .code-lang {
                    background: #1F2937;
                    color: #94A3B8;
                    font-size: 11px;
                    font-family: monospace;
                    padding: 4px 10px;
                    text-transform: uppercase;
                }
                pre {
                    margin: 0;
                    padding: 12px;
                    overflow-x: auto;
                    font-family: monospace;
                    font-size: 13px;
                    color: #F8FAFC;
                }
                pre code { background: transparent; padding: 0; }
                blockquote {
                    margin: 12px 0;
                    padding-left: 14px;
                    border-left: 4px solid #38BDF8;
                    color: #94A3B8;
                    font-style: italic;
                }
                li { margin: 4px 0; margin-left: 20px; }
            </style>
        </head>
        <body>
            <p>$htmlBody</p>
        </body>
        </html>
    """.trimIndent()
}
