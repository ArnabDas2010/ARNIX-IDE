package com.arnix.ide.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.ide.R
import com.arnix.ide.data.model.FileManagerItem
import com.arnix.ide.data.repository.SafStorageManager
import com.arnix.ide.data.repository.StorageAccessHelper
import com.arnix.ide.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    currentPath: String,
    onNavigateTo: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onSelectWorkspaceFolder: (String) -> Unit,
    onOpenFileInEditor: (File) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showHidden by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDirectPathDialog by remember { mutableStateOf(false) }
    var directPathInput by remember { mutableStateOf(currentPath) }

    // Refresh trigger after granting permission
    var refreshKey by remember { mutableIntStateOf(0) }

    // Double-click tracker for folders
    var lastClickedFolderPath by remember { mutableStateOf<String?>(null) }
    var lastClickTimestamp by remember { mutableLongStateOf(0L) }

    val currentDir = remember(currentPath) { File(currentPath) }
    val isRestricted = remember(currentPath) { StorageAccessHelper.isRestrictedPath(currentPath) }
    val isObb = remember(currentPath) { currentPath.contains("Android/obb", ignoreCase = true) }
    val folderTargetName = if (isObb) "Android/obb" else "Android/data"

    val hasSafAccess = remember(currentPath, refreshKey) {
        if (!isRestricted) true else SafStorageManager.hasPermission(context, isObb)
    }

    // Permission request dialog state for Android 11 (ZArchiver style)
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Auto-prompt dialog if entering restricted folder without permission
    LaunchedEffect(currentPath, hasSafAccess) {
        if (isRestricted && !hasSafAccess) {
            showPermissionDialog = true
        }
    }

    // Storage Access Framework (SAF) Launcher
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                SafStorageManager.saveGrantedTree(context, isObb, uri)
                showPermissionDialog = false
                refreshKey++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val fileList = remember(currentPath, showHidden, searchQuery, refreshKey) {
        StorageAccessHelper.listFilesRobust(context, currentDir, showHidden, searchQuery)
    }

    val canGoUp = currentDir.parentFile != null && currentDir.parentFile?.canRead() == true

    BackHandler {
        if (canGoUp) {
            onNavigateUp()
        } else {
            onClose()
        }
    }

    val developerShortcuts = remember {
        StorageAccessHelper.getDeveloperStorageShortcuts()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
            .testTag("file_manager_screen"),
        topBar = {
            Surface(
                color = IdeSurface,
                tonalElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Bar
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
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(36.dp).testTag("btn_close_file_manager")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close File Manager",
                                    tint = IdeTextPrimary
                                )
                            }

                            Column {
                                Text(
                                    text = "ARNIX STORAGE EXPLORER",
                                    color = IdeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (isRestricted) "Accessing $folderTargetName" else "Native File Manager",
                                    color = if (isRestricted) IdeSecondary else IdeTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Open Current Folder as Workspace Button
                        Button(
                            onClick = { onSelectWorkspaceFolder(currentDir.absolutePath) },
                            colors = ButtonDefaults.buttonColors(containerColor = IdeSecondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_select_current_workspace")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_folder_open),
                                contentDescription = null,
                                tint = IdeBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Use Folder",
                                color = IdeBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Path Bar & Navigation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IdeSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateUp,
                            enabled = canGoUp,
                            modifier = Modifier.size(32.dp).testTag("btn_navigate_up")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Go to Parent Folder",
                                tint = if (canGoUp) IdePrimary else IdeTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Interactive Path Display
                        Surface(
                            color = IdeBackground,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    directPathInput = currentDir.absolutePath
                                    showDirectPathDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = currentDir.absolutePath,
                                    color = IdeTextPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Path",
                                    tint = IdeTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showHidden = !showHidden },
                            modifier = Modifier.size(32.dp).testTag("btn_toggle_hidden_files")
                        ) {
                            Text(
                                text = if (showHidden) ".*" else ".#",
                                color = if (showHidden) IdeSecondary else IdeTextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Shortcuts Row (Android/data, Android/obb, Root, Downloads)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IdeSurface)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        developerShortcuts.forEach { (label, path, _) ->
                            val isSelected = currentPath == path
                            FilterChip(
                                selected = isSelected,
                                onClick = { onNavigateTo(path) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (label.contains("data") || label.contains("obb")) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (label.contains("data")) IdeSecondary.copy(alpha = 0.25f) else IdePrimaryContainer.copy(alpha = 0.3f),
                                    selectedLabelColor = if (label.contains("data")) IdeSecondary else IdePrimary,
                                    containerColor = IdeSurfaceVariant,
                                    labelColor = IdeTextPrimary
                                )
                            )
                        }
                    }

                    // Status Banner when in Android/data or Android/obb
                    if (isRestricted) {
                        Surface(
                            color = if (hasSafAccess) IdeSurfaceVariant else IdeSecondary.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (hasSafAccess) IdeBorder else IdeSecondary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (hasSafAccess) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (hasSafAccess) IdeSecondary else IdeSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (hasSafAccess) "$folderTargetName Access Active" else "$folderTargetName Permission Required",
                                        color = IdeTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (!hasSafAccess) {
                                    Button(
                                        onClick = {
                                            try {
                                                safLauncher.launch(SafStorageManager.createOpenFolderIntent(isObb))
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = IdeSecondary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("Grant Permission", color = IdeBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(IdeBackground)
        ) {
            if (fileList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder_open),
                            contentDescription = null,
                            tint = if (isRestricted && !hasSafAccess) IdeSecondary else IdeTextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (isRestricted && !hasSafAccess) "Permission Required for $folderTargetName" else "Folder is Empty",
                            color = IdeTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (isRestricted && !hasSafAccess) {
                            Text(
                                text = "On Android 11, tap below to grant folder permission, then click 'USE THIS FOLDER' on the next screen (same as ZArchiver).",
                                color = IdeTextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Button(
                                onClick = {
                                    try {
                                        safLauncher.launch(SafStorageManager.createOpenFolderIntent(isObb))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IdeSecondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = IdeBackground,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Grant $folderTargetName Access",
                                    color = IdeBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Text(
                                text = currentDir.absolutePath,
                                color = IdeTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(fileList, key = { it.path }) { item ->
                        FileManagerRowItem(
                            item = item,
                            onFolderClick = {
                                val now = System.currentTimeMillis()
                                if (lastClickedFolderPath == item.path && now - lastClickTimestamp < 500) {
                                    // DOUBLE CLICK: Open directly as IDE Workspace!
                                    onSelectWorkspaceFolder(item.path)
                                } else {
                                    lastClickedFolderPath = item.path
                                    lastClickTimestamp = now
                                    onNavigateTo(item.path)
                                }
                            },
                            onSelectAsWorkspace = { onSelectWorkspaceFolder(item.path) },
                            onFileClick = { onOpenFileInEditor(File(item.path)) }
                        )
                    }
                }
            }

            // ZArchiver-Style Permission Request Dialog for Android 11
            if (showPermissionDialog && isRestricted && !hasSafAccess) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_folder),
                                contentDescription = null,
                                tint = IdeSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Access $folderTargetName",
                                color = IdeTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "To view and manage files in $folderTargetName on Android 11:",
                                color = IdeTextPrimary,
                                fontSize = 13.sp
                            )
                            Surface(
                                color = IdeSurfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("1. Tap 'Grant Access' below.", color = IdeTextSecondary, fontSize = 12.sp)
                                    Text("2. The system file picker will open.", color = IdeTextSecondary, fontSize = 12.sp)
                                    Text("3. Tap 'USE THIS FOLDER' at the bottom.", color = IdeSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("4. Tap 'ALLOW' to confirm.", color = IdeSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                try {
                                    safLauncher.launch(SafStorageManager.createOpenFolderIntent(isObb))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IdeSecondary)
                        ) {
                            Text("Grant Access", color = IdeBackground, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionDialog = false }) {
                            Text("Cancel", color = IdeTextMuted)
                        }
                    }
                )
            }

            // Direct Path Jump Dialog Modal
            if (showDirectPathDialog) {
                AlertDialog(
                    onDismissRequest = { showDirectPathDialog = false },
                    title = { Text("Jump to Directory Path", color = IdeTextPrimary) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Enter directory path (e.g. /storage/emulated/0/Android/data):",
                                color = IdeTextSecondary,
                                fontSize = 12.sp
                            )
                            OutlinedTextField(
                                value = directPathInput,
                                onValueChange = { directPathInput = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (directPathInput.isNotBlank()) {
                                    onNavigateTo(directPathInput.trim())
                                    showDirectPathDialog = false
                                }
                            }
                        ) {
                            Text("Navigate")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDirectPathDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FileManagerRowItem(
    item: FileManagerItem,
    onFolderClick: () -> Unit,
    onSelectAsWorkspace: () -> Unit,
    onFileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = IdeSurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (item.isDirectory) onFolderClick() else onFileClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = if (item.isDirectory) {
                        painterResource(id = R.drawable.ic_folder)
                    } else {
                        painterResource(id = R.drawable.ic_file_code)
                    },
                    contentDescription = null,
                    tint = if (item.isDirectory) IdeSecondary else IdePrimary,
                    modifier = Modifier.size(24.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            color = IdeTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.isHidden) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = IdeSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "hidden",
                                    color = IdeTextMuted,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.sizeFormatted,
                            color = IdeTextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "•",
                            color = IdeTextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = item.lastModifiedFormatted,
                            color = IdeTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (item.isDirectory) {
                IconButton(
                    onClick = onSelectAsWorkspace,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_folder_open),
                        contentDescription = "Set as Workspace",
                        tint = IdeSecondary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
