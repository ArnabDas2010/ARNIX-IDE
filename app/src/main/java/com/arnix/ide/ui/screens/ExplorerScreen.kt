package com.arnix.ide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.ide.R
import com.arnix.ide.data.model.FileItem
import com.arnix.ide.ui.theme.*

private data class FlatFileRow(
    val item: FileItem,
    val depth: Int
)

private fun flattenTree(items: List<FileItem>, depth: Int = 0): List<FlatFileRow> {
    val result = mutableListOf<FlatFileRow>()
    for (item in items) {
        result.add(FlatFileRow(item, depth))
        if (item.isDirectory && item.isExpanded) {
            result.addAll(flattenTree(item.children, depth + 1))
        }
    }
    return result
}

@Composable
fun ExplorerScreen(
    files: List<FileItem>,
    workspaceName: String,
    onBrowseClick: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onToggleFolder: (String) -> Unit,
    onNewFileClick: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    val flatFiles = remember(files) { flattenTree(files) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
            .testTag("explorer_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = IdeSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "PROJECT EXPLORER",
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = workspaceName,
                            color = IdeSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Browse storage button
                        OutlinedButton(
                            onClick = onBrowseClick,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_browse_storage"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IdePrimary.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_folder_open),
                                contentDescription = "Browse Device Files",
                                tint = IdePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Browse",
                                fontSize = 12.sp,
                                color = IdePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // New file button
                        FilledTonalButton(
                            onClick = { onNewFileClick("") },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_new_file_root"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = IdePrimaryContainer,
                                contentColor = IdeTextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.action_new_file),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Files Tree List
            if (flatFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder_open),
                            contentDescription = null,
                            tint = IdeTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Workspace is empty",
                            color = IdeTextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap 'Browse' to select a folder from your storage, or 'New' to create files.",
                            color = IdeTextMuted,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(flatFiles, key = { it.item.id }) { row ->
                        FileRowItem(
                            item = row.item,
                            depth = row.depth,
                            onOpenFile = onOpenFile,
                            onToggleFolder = onToggleFolder,
                            onNewFile = onNewFileClick,
                            onDelete = { fileToDelete = it }
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (fileToDelete != null) {
            AlertDialog(
                onDismissRequest = { fileToDelete = null },
                title = { Text("Delete ${fileToDelete?.name}?", color = IdeTextPrimary) },
                text = {
                    Text(
                        "Are you sure you want to delete this ${if (fileToDelete?.isDirectory == true) "folder and all its contents" else "file"}?",
                        color = IdeTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            fileToDelete?.let { onDeleteFile(it.path) }
                            fileToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IdeError)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { fileToDelete = null }) {
                        Text("Cancel", color = IdeTextSecondary)
                    }
                },
                containerColor = IdeSurface
            )
        }
    }
}

@Composable
private fun FileRowItem(
    item: FileItem,
    depth: Int,
    onOpenFile: (FileItem) -> Unit,
    onToggleFolder: (String) -> Unit,
    onNewFile: (String) -> Unit,
    onDelete: (FileItem) -> Unit
) {
    val drawableRes = when {
        item.isDirectory -> if (item.isExpanded) R.drawable.ic_folder_open else R.drawable.ic_folder
        else -> R.drawable.ic_file_code
    }

    val iconColor = when {
        item.isDirectory -> IdeTertiary
        item.name.endsWith(".kt") -> IdePrimary
        item.name.endsWith(".py") -> IdeSecondary
        item.name.endsWith(".md") -> TerminalCyan
        else -> IdeTextSecondary
    }

    Surface(
        color = IdeSurface,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 14).dp)
            .clickable {
                if (item.isDirectory) onToggleFolder(item.path) else onOpenFile(item)
            }
            .testTag("file_item_${item.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = item.name,
                    color = IdeTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = if (item.isDirectory) FontFamily.Default else FontFamily.Monospace,
                    fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (item.isDirectory) {
                    IconButton(
                        onClick = { onNewFile(item.path) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New file in folder",
                            tint = IdeTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onDelete(item) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete file",
                        tint = IdeTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
