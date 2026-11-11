package com.arnix.ide.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

data class StorageShortcutDestination(
    val title: String,
    val subtitle: String,
    val path: String,
    val iconRes: Int
)

data class ActiveClipboardItem(
    val item: FileManagerItem,
    val isCut: Boolean
)

enum class FileCategoryFilter {
    ALL, FOLDERS, FILES
}

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
    var showHidden by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf(FileCategoryFilter.ALL) }

    var showLocationPickerDialog by remember { mutableStateOf(false) }
    var customPathInput by remember { mutableStateOf(currentPath) }

    // Refresh trigger after creating, deleting, renaming, pasting, or granting permission
    var refreshKey by remember { mutableIntStateOf(0) }

    // In-App Clipboard for Copy / Cut / Paste
    var clipboardItem by remember { mutableStateOf<ActiveClipboardItem?>(null) }

    // Dialog states
    var itemToDelete by remember { mutableStateOf<FileManagerItem?>(null) }
    var itemToRename by remember { mutableStateOf<FileManagerItem?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var itemForDetails by remember { mutableStateOf<FileManagerItem?>(null) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var createIsFolder by remember { mutableStateOf(false) }
    var createNameInput by remember { mutableStateOf("") }

    var showNewMenu by remember { mutableStateOf(false) }

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

    val hasAllFilesAccess = remember(refreshKey) {
        StorageAccessHelper.hasAllFilesPermission(context)
    }
    var showAllFilesPermissionDialog by remember { mutableStateOf(!hasAllFilesAccess) }

    // Auto-refresh when user returns to app from system settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                refreshKey++
                Toast.makeText(context, "Access granted to $folderTargetName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var isLoading by remember(currentPath, refreshKey) { mutableStateOf(true) }
    var fileList by remember { mutableStateOf<List<FileManagerItem>>(emptyList()) }

    LaunchedEffect(currentPath, showHidden, searchQuery, refreshKey) {
        isLoading = true
        val list = withContext(Dispatchers.IO) {
            StorageAccessHelper.listFilesRobust(context, currentDir, showHidden, searchQuery)
        }
        fileList = list
        isLoading = false
    }

    val folderCount = remember(fileList) { fileList.count { it.isDirectory } }
    val fileCount = remember(fileList) { fileList.count { !it.isDirectory } }

    val filteredList = remember(fileList, activeCategory) {
        when (activeCategory) {
            FileCategoryFilter.ALL -> fileList
            FileCategoryFilter.FOLDERS -> fileList.filter { it.isDirectory }
            FileCategoryFilter.FILES -> fileList.filter { !it.isDirectory }
        }
    }

    val canGoUp = currentDir.parentFile != null && currentDir.absolutePath != "/"

    BackHandler {
        if (canGoUp) {
            onNavigateUp()
        } else {
            onClose()
        }
    }

    val baseStorage = remember {
        android.os.Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
    }

    val verticalShortcuts = remember(baseStorage) {
        listOf(
            StorageShortcutDestination(
                title = "Android Data",
                subtitle = "$baseStorage/Android/data",
                path = "$baseStorage/Android/data",
                iconRes = R.drawable.ic_android
            ),
            StorageShortcutDestination(
                title = "Android OBB",
                subtitle = "$baseStorage/Android/obb",
                path = "$baseStorage/Android/obb",
                iconRes = R.drawable.ic_android
            ),
            StorageShortcutDestination(
                title = "Android Media",
                subtitle = "$baseStorage/Android/media",
                path = "$baseStorage/Android/media",
                iconRes = R.drawable.ic_android
            ),
            StorageShortcutDestination(
                title = "Internal Storage",
                subtitle = baseStorage,
                path = baseStorage,
                iconRes = R.drawable.ic_storage
            ),
            StorageShortcutDestination(
                title = "Downloads",
                subtitle = "$baseStorage/Download",
                path = "$baseStorage/Download",
                iconRes = R.drawable.ic_download
            ),
            StorageShortcutDestination(
                title = "Documents",
                subtitle = "$baseStorage/Documents",
                path = "$baseStorage/Documents",
                iconRes = R.drawable.ic_document
            )
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
            .testTag("file_manager_screen"),
        topBar = {
            Surface(
                color = IdeSurface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, IdeBorderSubtle)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    // Header Bar 1: Back + Title/Status + New + Refresh + Use Folder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(34.dp).testTag("btn_close_file_manager")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close File Manager",
                                    tint = IdeTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "FILE MANAGER",
                                        color = IdeTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )


                                }

                                Text(
                                    text = "$folderCount folders, $fileCount files",
                                    color = IdeTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Action Buttons: + New, Refresh, Use Folder
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // + New Button
                            Box {
                                IconButton(
                                    onClick = { showNewMenu = true },
                                    modifier = Modifier.size(32.dp).testTag("btn_file_manager_new")
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_new_file),
                                        contentDescription = "Create New",
                                        tint = IdePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showNewMenu,
                                    onDismissRequest = { showNewMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("New File") },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_new_file),
                                                contentDescription = null,
                                                tint = IdePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showNewMenu = false
                                            createIsFolder = false
                                            createNameInput = ""
                                            showCreateDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("New Folder") },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_new_folder),
                                                contentDescription = null,
                                                tint = IdeSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showNewMenu = false
                                            createIsFolder = true
                                            createNameInput = ""
                                            showCreateDialog = true
                                        }
                                    )
                                }
                            }

                            // Refresh Button
                            IconButton(
                                onClick = { refreshKey++ },
                                modifier = Modifier.size(32.dp).testTag("btn_file_manager_refresh")
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_refresh),
                                    contentDescription = "Refresh",
                                    tint = IdeTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Use Folder Button (Set as IDE Workspace)
                            Button(
                                onClick = { onSelectWorkspaceFolder(currentDir.absolutePath) },
                                colors = ButtonDefaults.buttonColors(containerColor = IdeSecondary, contentColor = IdeBackground),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp).testTag("btn_select_current_workspace")
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_folder_open),
                                    contentDescription = null,
                                    tint = IdeBackground,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Workspace",
                                    color = IdeBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Header Bar 2: Path Navigation + Search Toggle + Hidden Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
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
                                contentDescription = "Go Up",
                                tint = if (canGoUp) IdePrimary else IdeTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Clickable Path Field: tapping opens vertical shortcut selector!
                        Surface(
                            color = IdeBackground,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, IdeBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    customPathInput = currentDir.absolutePath
                                    showLocationPickerDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_folder),
                                    contentDescription = null,
                                    tint = IdeSecondary,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

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
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Location",
                                    tint = IdePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Search Toggle
                        IconButton(
                            onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) searchQuery = ""
                            },
                            modifier = Modifier.size(32.dp).testTag("btn_toggle_search")
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search Files",
                                tint = if (isSearchActive || searchQuery.isNotEmpty()) IdePrimary else IdeTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Hidden files toggle button
                        IconButton(
                            onClick = { showHidden = !showHidden },
                            modifier = Modifier.size(32.dp).testTag("btn_toggle_hidden_files")
                        ) {
                            Text(
                                text = if (showHidden) ".*" else ".#",
                                color = if (showHidden) IdeSecondary else IdeTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Optional Search Input Row
                    if (isSearchActive) {
                        Surface(
                            color = IdeBackground,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, IdeBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = IdeTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Filter files & folders in current path...", fontSize = 11.sp, color = IdeTextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = IdeTextPrimary)
                                )
                            }
                        }
                    }

                    // Category Filter Chips: All | Folders | Files (Horizontally scrollable & auto-fit)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chipColors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IdePrimary.copy(alpha = 0.15f),
                            selectedLabelColor = IdePrimary,
                            containerColor = IdeSurfaceVariant.copy(alpha = 0.5f),
                            labelColor = IdeTextSecondary
                        )
                        FilterChip(
                            selected = activeCategory == FileCategoryFilter.ALL,
                            onClick = { activeCategory = FileCategoryFilter.ALL },
                            label = { Text("All (${fileList.size})", fontSize = 11.sp, fontWeight = if (activeCategory == FileCategoryFilter.ALL) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(16.dp),
                            colors = chipColors,
                            border = BorderStroke(1.dp, if (activeCategory == FileCategoryFilter.ALL) IdePrimary.copy(alpha = 0.4f) else IdeBorderSubtle),
                            modifier = Modifier.heightIn(min = 30.dp)
                        )
                        FilterChip(
                            selected = activeCategory == FileCategoryFilter.FOLDERS,
                            onClick = { activeCategory = FileCategoryFilter.FOLDERS },
                            label = { Text("Folders ($folderCount)", fontSize = 11.sp, fontWeight = if (activeCategory == FileCategoryFilter.FOLDERS) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(16.dp),
                            colors = chipColors,
                            border = BorderStroke(1.dp, if (activeCategory == FileCategoryFilter.FOLDERS) IdePrimary.copy(alpha = 0.4f) else IdeBorderSubtle),
                            modifier = Modifier.heightIn(min = 30.dp)
                        )
                        FilterChip(
                            selected = activeCategory == FileCategoryFilter.FILES,
                            onClick = { activeCategory = FileCategoryFilter.FILES },
                            label = { Text("Files ($fileCount)", fontSize = 11.sp, fontWeight = if (activeCategory == FileCategoryFilter.FILES) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(16.dp),
                            colors = chipColors,
                            border = BorderStroke(1.dp, if (activeCategory == FileCategoryFilter.FILES) IdePrimary.copy(alpha = 0.4f) else IdeBorderSubtle),
                            modifier = Modifier.heightIn(min = 30.dp)
                        )
                    }

                    if (!hasAllFilesAccess) {
                        Surface(
                            color = IdeTertiary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, IdeTertiary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_info),
                                        contentDescription = null,
                                        tint = IdeTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "All Files Access required on Android 11",
                                        color = IdeTextPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = { StorageAccessHelper.requestAllFilesPermission(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = IdeTertiary),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Grant", color = IdeBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Sticky Clipboard Bar for Paste Operation
            if (clipboardItem != null) {
                Surface(
                    color = IdeSurface,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, IdePrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = if (clipboardItem!!.isCut) painterResource(id = R.drawable.ic_cut) else painterResource(id = R.drawable.ic_copy),
                                contentDescription = null,
                                tint = IdePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = if (clipboardItem!!.isCut) "Moving: ${clipboardItem!!.item.name}" else "Copied: ${clipboardItem!!.item.name}",
                                    color = IdeTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Target: ${currentDir.name}",
                                    color = IdeTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Paste Button
                            Button(
                                onClick = {
                                    val item = clipboardItem!!.item
                                    val isCut = clipboardItem!!.isCut
                                    val success = if (isCut) {
                                        StorageAccessHelper.moveItem(context, item.path, currentDir.absolutePath)
                                    } else {
                                        StorageAccessHelper.copyItem(context, item.path, currentDir.absolutePath)
                                    }
                                    if (success) {
                                        Toast.makeText(context, if (isCut) "Moved successfully" else "Copied successfully", Toast.LENGTH_SHORT).show()
                                        refreshKey++
                                        if (isCut) clipboardItem = null
                                    } else {
                                        Toast.makeText(context, "Failed to paste item", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IdePrimary),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_paste),
                                    contentDescription = null,
                                    tint = IdeBackground,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste Here", color = IdeBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Cancel Button
                            TextButton(
                                onClick = { clipboardItem = null },
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Cancel", color = IdeTextMuted, fontSize = 11.sp)
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = IdePrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Loading files...",
                            color = IdeTextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder_open),
                            contentDescription = null,
                            tint = if (isRestricted && !hasSafAccess) IdeSecondary else IdeTextMuted,
                            modifier = Modifier.size(50.dp)
                        )

                        if (!hasAllFilesAccess) {
                            Text(
                                text = "All Files Access Required",
                                color = IdeTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "On Android 11, All Files Access permission is required to view and manage files in root storage and subfolders.\nTap below to enable it in Settings.",
                                color = IdeTextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { StorageAccessHelper.requestAllFilesPermission(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = IdePrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_storage),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enable All Files Access", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            val hiddenCount = remember(currentPath, refreshKey) {
                                StorageAccessHelper.listFilesRobust(context, currentDir, showHidden = true).count { it.isHidden }
                            }

                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching files found" else "This folder is empty",
                                color = IdeTextSecondary,
                                fontSize = 14.sp
                            )

                            if (hiddenCount > 0 && !showHidden) {
                                OutlinedButton(
                                    onClick = { showHidden = true },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Show $hiddenCount hidden items", color = IdeSecondary, fontSize = 12.sp)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (canGoUp) {
                                    OutlinedButton(
                                        onClick = onNavigateUp,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Parent Folder", color = IdeTextPrimary, fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        createIsFolder = false
                                        createNameInput = ""
                                        showCreateDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IdePrimaryContainer)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_new_file),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("New File", color = IdePrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (canGoUp) {
                        item(key = "__parent_folder_up__") {
                            ParentFolderUpRow(
                                onNavigateUp = onNavigateUp,
                                parentName = currentDir.parentFile?.name ?: ".."
                            )
                        }
                    }
                    items(filteredList, key = { it.path }) { item ->
                        FileManagerRowItem(
                            item = item,
                            onFolderClick = {
                                val now = System.currentTimeMillis()
                                if (lastClickedFolderPath == item.path && now - lastClickTimestamp < 500) {
                                    // Double-click: Set folder directly as IDE Workspace!
                                    onSelectWorkspaceFolder(item.path)
                                } else {
                                    lastClickedFolderPath = item.path
                                    lastClickTimestamp = now
                                    onNavigateTo(item.path)
                                }
                            },
                            onSelectAsWorkspace = { onSelectWorkspaceFolder(item.path) },
                            onFileClick = {
                                // Open file directly into Code Editor tab WITHOUT changing workspace!
                                onOpenFileInEditor(File(item.path))
                            },
                            onCopy = {
                                clipboardItem = ActiveClipboardItem(item = item, isCut = false)
                                Toast.makeText(context, "Copied: ${item.name}", Toast.LENGTH_SHORT).show()
                            },
                            onCut = {
                                clipboardItem = ActiveClipboardItem(item = item, isCut = true)
                                Toast.makeText(context, "Cut: ${item.name}", Toast.LENGTH_SHORT).show()
                            },
                            onRename = {
                                itemToRename = item
                                renameInput = item.name
                            },
                            onDelete = {
                                itemToDelete = item
                            },
                            onDetails = {
                                itemForDetails = item
                            }
                        )
                    }
                }
            }

            // Create New File / Folder Dialog
            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = if (createIsFolder) painterResource(id = R.drawable.ic_new_folder) else painterResource(id = R.drawable.ic_new_file),
                                contentDescription = null,
                                tint = if (createIsFolder) IdeSecondary else IdePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (createIsFolder) "Create New Folder" else "Create New File",
                                color = IdeTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 480.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Location: ${currentDir.absolutePath}",
                                color = IdeTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            OutlinedTextField(
                                value = createNameInput,
                                onValueChange = { createNameInput = it },
                                placeholder = {
                                    Text(
                                        if (createIsFolder) "Folder name" else "File name (e.g. index.kt, note.txt)",
                                        color = IdeTextMuted,
                                        fontSize = 12.sp
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (createNameInput.isNotBlank()) {
                                    val name = createNameInput.trim()
                                    val success = if (createIsFolder) {
                                        StorageAccessHelper.createFolder(context, currentDir.absolutePath, name)
                                    } else {
                                        StorageAccessHelper.createFile(context, currentDir.absolutePath, name, "")
                                    }
                                    if (success) {
                                        Toast.makeText(context, if (createIsFolder) "Folder created" else "File created", Toast.LENGTH_SHORT).show()
                                        refreshKey++
                                        showCreateDialog = false
                                        if (!createIsFolder) {
                                            // Optionally open created file immediately in Editor
                                            onOpenFileInEditor(File(currentDir, name))
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to create", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IdePrimary)
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = IdeTextMuted)
                        }
                    }
                )
            }

            // Rename Dialog
            if (itemToRename != null) {
                AlertDialog(
                    onDismissRequest = { itemToRename = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_rename),
                                contentDescription = null,
                                tint = IdePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Rename Item",
                                color = IdeTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 480.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Current: ${itemToRename!!.name}",
                                color = IdeTextSecondary,
                                fontSize = 12.sp
                            )
                            OutlinedTextField(
                                value = renameInput,
                                onValueChange = { renameInput = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (renameInput.isNotBlank()) {
                                    val success = StorageAccessHelper.renameItem(context, itemToRename!!.path, renameInput.trim())
                                    if (success) {
                                        Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show()
                                        refreshKey++
                                        itemToRename = null
                                    } else {
                                        Toast.makeText(context, "Failed to rename", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IdePrimary)
                        ) {
                            Text("Rename")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { itemToRename = null }) {
                            Text("Cancel", color = IdeTextMuted)
                        }
                    }
                )
            }

            // Delete Confirmation Dialog
            if (itemToDelete != null) {
                AlertDialog(
                    onDismissRequest = { itemToDelete = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = if (itemToDelete!!.isDirectory) "Delete Folder?" else "Delete File?",
                                color = IdeTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to permanently delete \"${itemToDelete!!.name}\"? This cannot be undone.",
                            color = IdeTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val success = StorageAccessHelper.deleteItem(context, itemToDelete!!.path)
                                if (success) {
                                    Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                                    refreshKey++
                                    itemToDelete = null
                                } else {
                                    Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { itemToDelete = null }) {
                            Text("Cancel", color = IdeTextMuted)
                        }
                    }
                )
            }

            // Item Details Dialog
            if (itemForDetails != null) {
                val item = itemForDetails!!
                AlertDialog(
                    onDismissRequest = { itemForDetails = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_info),
                                contentDescription = null,
                                tint = IdePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Item Details",
                                color = IdeTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 480.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            DetailRow("Name", item.name)
                            DetailRow("Type", if (item.isDirectory) "Directory / Folder" else "${item.extension.uppercase().ifEmpty { "Generic" }} File")
                            DetailRow("Size", item.sizeFormatted)
                            DetailRow("Modified", item.lastModifiedFormatted)
                            DetailRow("Full Path", item.path)

                            Button(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("path", item.path))
                                    Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IdePrimaryContainer),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_copy),
                                    contentDescription = null,
                                    tint = IdePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Full Path", color = IdePrimary, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { itemForDetails = null }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Vertical Location Picker Dialog (Ak ke niche ak vertically stacked list on click)
            if (showLocationPickerDialog) {
                AlertDialog(
                    onDismissRequest = { showLocationPickerDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_storage),
                                contentDescription = null,
                                tint = IdePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Select Storage Location",
                                color = IdeTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            verticalShortcuts.forEach { destination ->
                                val isSelected = currentPath == destination.path
                                Surface(
                                    color = if (isSelected) IdePrimaryContainer.copy(alpha = 0.35f) else IdeSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (isSelected) BorderStroke(1.dp, IdePrimary) else BorderStroke(1.dp, IdeBorder.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onNavigateTo(destination.path)
                                            showLocationPickerDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = destination.iconRes),
                                            contentDescription = null,
                                            tint = if (isSelected) IdePrimary else IdeSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = destination.title,
                                                color = IdeTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = destination.subtitle,
                                                color = IdeTextSecondary,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isSelected) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_check),
                                                contentDescription = null,
                                                tint = IdeSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = IdeBorder)
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Or enter custom path:",
                                color = IdeTextSecondary,
                                fontSize = 11.sp
                            )
                            OutlinedTextField(
                                value = customPathInput,
                                onValueChange = { customPathInput = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (customPathInput.isNotBlank()) {
                                    onNavigateTo(customPathInput.trim())
                                    showLocationPickerDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IdePrimary)
                        ) {
                            Text("Go")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLocationPickerDialog = false }) {
                            Text("Close", color = IdeTextMuted)
                        }
                    }
                )
            }



            // All Files Access Permission Dialog for Android 11
            if (showAllFilesPermissionDialog && !hasAllFilesAccess) {
                AlertDialog(
                    onDismissRequest = { showAllFilesPermissionDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_storage),
                                contentDescription = null,
                                tint = IdePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "All Files Access Required",
                                color = IdeTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Android 11 restricts reading device folders without All Files Access. To see all your files and folders inside root storage, please grant permission:",
                                color = IdeTextSecondary,
                                fontSize = 13.sp
                            )
                            Surface(
                                color = IdeSurfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("1. Tap 'Grant Access' below.", color = IdeTextSecondary, fontSize = 12.sp)
                                    Text("2. Turn ON 'Allow access to manage all files'.", color = IdeSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("3. Return to ARNIX IDE.", color = IdeTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                StorageAccessHelper.requestAllFilesPermission(context)
                                showAllFilesPermissionDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IdePrimary)
                        ) {
                            Text("Grant Access", color = IdeBackground, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAllFilesPermissionDialog = false }) {
                            Text("Later", color = IdeTextMuted)
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
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = IdeSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, IdeBorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable {
                if (item.isDirectory) onFolderClick() else onFileClick()
            }
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Item Icon based on type
                val iconRes = remember(item.isDirectory, item.extension) {
                    when {
                        item.isDirectory -> R.drawable.ic_folder
                        isCodeExtension(item.extension) -> R.drawable.ic_file_code
                        isArchiveExtension(item.extension) -> R.drawable.ic_archive
                        isImageExtension(item.extension) -> R.drawable.ic_image
                        else -> R.drawable.ic_document
                    }
                }

                val iconTint = remember(item.isDirectory, item.extension) {
                    when {
                        item.isDirectory -> IdeSecondary
                        isCodeExtension(item.extension) -> IdePrimary
                        isArchiveExtension(item.extension) -> IdeTertiary
                        isImageExtension(item.extension) -> IdeSecondary
                        else -> IdeTextSecondary
                    }
                }

                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = if (item.isHidden) IdeTextSecondary else IdeTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = item.sizeFormatted,
                            color = if (item.sizeFormatted.contains("•")) IdeSecondary else IdeTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!item.isDirectory && item.extension.isNotBlank() && item.extension.length in 1..6 && !item.extension.contains("_")) {
                            Text(text = "•", color = IdeTextMuted.copy(alpha = 0.5f), fontSize = 10.sp)
                            Text(
                                text = item.extension.uppercase(),
                                color = IdePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (item.lastModifiedFormatted.isNotEmpty()) {
                            Text(text = "•", color = IdeTextMuted.copy(alpha = 0.5f), fontSize = 10.sp)
                            Text(
                                text = item.lastModifiedFormatted,
                                color = IdeTextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Action buttons on row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.isDirectory) {
                    IconButton(
                        onClick = onSelectAsWorkspace,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_folder_open),
                            contentDescription = "Set as Workspace",
                            tint = IdeSecondary.copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onFileClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_file_code),
                            contentDescription = "Open in Editor",
                            tint = IdePrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // 3-Dot Overflow Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more_vert),
                            contentDescription = "Options",
                            tint = IdeTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!item.isDirectory) {
                            DropdownMenuItem(
                                text = { Text("Open in Editor") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_file_code),
                                        contentDescription = null,
                                        tint = IdePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onFileClick()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Set as Workspace") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_folder_open),
                                        contentDescription = null,
                                        tint = IdeSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onSelectAsWorkspace()
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Copy") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_copy),
                                    contentDescription = null,
                                    tint = IdeTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onCopy()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Cut (Move)") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_cut),
                                    contentDescription = null,
                                    tint = IdeTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onCut()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_rename),
                                    contentDescription = null,
                                    tint = IdeTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Details") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_info),
                                    contentDescription = null,
                                    tint = IdeTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDetails()
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = label, color = IdeTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            color = IdeTextPrimary,
            fontSize = 12.sp,
            fontFamily = if (label == "Full Path") FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
fun ParentFolderUpRow(
    onNavigateUp: () -> Unit,
    parentName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = IdeSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, IdeBorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateUp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = IdeSecondary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Parent Folder",
                        tint = IdeSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "..",
                    color = IdeTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Parent: $parentName",
                    color = IdeTextSecondary,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = IdeTextMuted.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun isCodeExtension(ext: String): Boolean {
    val lower = ext.lowercase()
    return lower in setOf(
        "kt", "java", "xml", "json", "js", "ts", "py", "html", "css",
        "c", "cpp", "h", "hpp", "sh", "md", "txt", "gradle", "properties",
        "yaml", "yml", "sql", "rb", "go", "rs", "php"
    )
}

private fun isArchiveExtension(ext: String): Boolean {
    val lower = ext.lowercase()
    return lower in setOf("zip", "apk", "rar", "7z", "tar", "gz", "obb", "jar", "aar")
}

private fun isImageExtension(ext: String): Boolean {
    val lower = ext.lowercase()
    return lower in setOf("png", "jpg", "jpeg", "webp", "gif", "svg", "bmp", "ico")
}
