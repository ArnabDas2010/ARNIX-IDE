package com.arnix.ide.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arnix.ide.R
import com.arnix.ide.data.repository.ArchiveEntryItem
import com.arnix.ide.data.repository.ArchiveHelper
import com.arnix.ide.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveViewerDialog(
    archiveFile: File,
    onDismiss: () -> Unit,
    onExtractionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var entries by remember { mutableStateOf<List<ArchiveEntryItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    // Extraction state
    var isExtracting by remember { mutableStateOf(false) }
    var extractionProgress by remember { mutableFloatStateOf(0f) }
    var extractionStatusText by remember { mutableStateOf("") }

    // In-archive entry viewer
    var previewEntry by remember { mutableStateOf<ArchiveEntryItem?>(null) }
    var previewContent by remember { mutableStateOf<String?>(null) }
    var isPreviewLoading by remember { mutableStateOf(false) }

    LaunchedEffect(archiveFile) {
        isLoading = true
        val list = withContext(Dispatchers.IO) {
            ArchiveHelper.listArchive(archiveFile)
        }
        entries = list
        isLoading = false
    }

    val filteredEntries = remember(entries, searchQuery) {
        if (searchQuery.isBlank()) {
            entries
        } else {
            entries.filter { it.name.contains(searchQuery, ignoreCase = true) || it.path.contains(searchQuery, ignoreCase = true) }
        }
    }

    val totalUncompressed = remember(entries) { entries.sumOf { it.sizeBytes } }
    val totalFilesCount = remember(entries) { entries.count { !it.isDirectory } }

    Dialog(
        onDismissRequest = { if (!isExtracting) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isExtracting,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("archive_viewer_dialog"),
            color = IdeBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = IdeSurface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, IdeBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(IdeTertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_archive),
                                    contentDescription = null,
                                    tint = IdeTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = archiveFile.name,
                                    color = IdeTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$totalFilesCount files • ${formatFileSize(totalUncompressed)} uncompressed",
                                    color = IdeTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            enabled = !isExtracting,
                            modifier = Modifier.size(32.dp).testTag("archive_close_btn")
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

                // Search Bar within archive
                Surface(
                    color = IdeBackground,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Filter files inside archive...",
                                color = IdeTextMuted,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = IdeTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = IdeTextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = IdeSurface,
                            unfocusedContainerColor = IdeSurface,
                            focusedTextColor = IdeTextPrimary,
                            unfocusedTextColor = IdeTextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    )
                }

                // Extraction Progress overlay if busy
                if (isExtracting) {
                    Surface(
                        color = IdeSurface,
                        border = BorderStroke(1.dp, IdePrimary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = extractionStatusText,
                                    color = IdePrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${(extractionProgress * 100).toInt()}%",
                                    color = IdeTextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { extractionProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = IdePrimary,
                                trackColor = IdeSurfaceVariant
                            )
                        }
                    }
                }

                // Main Entries List
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = IdePrimary, modifier = Modifier.size(36.dp))
                        }
                    } else if (filteredEntries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching entries found" else "Archive is empty",
                                color = IdeTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(filteredEntries, key = { it.path }) { entry ->
                                ArchiveEntryRow(
                                    entry = entry,
                                    onClick = {
                                        if (!entry.isDirectory) {
                                            previewEntry = entry
                                            isPreviewLoading = true
                                            coroutineScope.launch {
                                                val content = withContext(Dispatchers.IO) {
                                                    ArchiveHelper.readArchiveEntryText(archiveFile, entry.path)
                                                }
                                                previewContent = content
                                                isPreviewLoading = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom Action Footer Bar
                Surface(
                    color = IdeSurface,
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, IdeBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Extract to Subfolder button
                        OutlinedButton(
                            onClick = {
                                val destFolder = File(archiveFile.parentFile, "${archiveFile.nameWithoutExtension}_extracted")
                                isExtracting = true
                                extractionStatusText = "Extracting to subfolder..."
                                coroutineScope.launch {
                                    val success = withContext(Dispatchers.IO) {
                                        ArchiveHelper.extractAll(archiveFile, destFolder) { done, currentFile ->
                                            if (totalFilesCount > 0) {
                                                extractionProgress = (done.toFloat() / totalFilesCount.toFloat()).coerceIn(0f, 1f)
                                                extractionStatusText = "Extracting: $currentFile"
                                            }
                                        }
                                    }
                                    isExtracting = false
                                    if (success) {
                                        Toast.makeText(context, "Extracted to ${destFolder.name}", Toast.LENGTH_LONG).show()
                                        onExtractionComplete()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Extraction encountered an error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isExtracting && entries.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, IdeBorderSubtle)
                        ) {
                            Text(
                                "Extract Subfolder",
                                color = IdeTextPrimary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        // Extract All Here button
                        Button(
                            onClick = {
                                val destFolder = archiveFile.parentFile ?: File("/storage/emulated/0")
                                isExtracting = true
                                extractionStatusText = "Extracting files..."
                                coroutineScope.launch {
                                    val success = withContext(Dispatchers.IO) {
                                        ArchiveHelper.extractAll(archiveFile, destFolder) { done, currentFile ->
                                            if (totalFilesCount > 0) {
                                                extractionProgress = (done.toFloat() / totalFilesCount.toFloat()).coerceIn(0f, 1f)
                                                extractionStatusText = "Extracting: $currentFile"
                                            }
                                        }
                                    }
                                    isExtracting = false
                                    if (success) {
                                        Toast.makeText(context, "All files extracted successfully", Toast.LENGTH_LONG).show()
                                        onExtractionComplete()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Extraction encountered an error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isExtracting && entries.isNotEmpty(),
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IdePrimary,
                                contentColor = IdeBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_archive),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Extract All Here",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // In-Memory Entry Preview Dialog (without extracting to disk)
            if (previewEntry != null) {
                AlertDialog(
                    onDismissRequest = {
                        previewEntry = null
                        previewContent = null
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_file_code),
                                contentDescription = null,
                                tint = IdePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = previewEntry?.name ?: "File Preview",
                                color = IdeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 320.dp)
                                .background(IdeSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            if (isPreviewLoading) {
                                CircularProgressIndicator(
                                    color = IdePrimary,
                                    modifier = Modifier.size(28.dp).align(Alignment.Center)
                                )
                            } else {
                                val text = previewContent ?: "Unable to read binary preview"
                                Text(
                                    text = text,
                                    color = IdeTextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                previewContent?.let {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Archive Content", it))
                                    Toast.makeText(context, "Copied content", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Copy", color = IdePrimary)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                previewEntry = null
                                previewContent = null
                            }
                        ) {
                            Text("Close", color = IdeTextSecondary)
                        }
                    },
                    containerColor = IdeSurface
                )
            }
        }
    }
}

@Composable
private fun ArchiveEntryRow(
    entry: ArchiveEntryItem,
    onClick: () -> Unit
) {
    val ext = entry.extension.lowercase()
    val iconRes = when {
        entry.isDirectory -> R.drawable.ic_folder
        ext in listOf("kt", "java", "py", "js", "html", "css", "json", "xml", "gradle") -> R.drawable.ic_file_code
        ext in listOf("png", "jpg", "jpeg", "webp", "gif") -> R.drawable.ic_image
        ext in listOf("zip", "jar", "apk", "aar") -> R.drawable.ic_archive
        else -> R.drawable.ic_document
    }

    val iconTint = when {
        entry.isDirectory -> IdeTertiary
        ext in listOf("kt", "java") -> IdePrimary
        ext in listOf("py") -> IdeSecondary
        ext in listOf("png", "jpg", "webp") -> IdeAccentViolet
        else -> IdeTextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.path,
                color = IdeTextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!entry.isDirectory) {
                Text(
                    text = "${entry.formattedSize} (compressed: ${formatFileSize(entry.compressedSizeBytes)}) • ${entry.formattedDate}",
                    color = IdeTextMuted,
                    fontSize = 10.sp
                )
            }
        }

        if (!entry.isDirectory) {
            Icon(
                painter = painterResource(id = R.drawable.ic_file_code),
                contentDescription = "Preview",
                tint = IdeTextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
}
