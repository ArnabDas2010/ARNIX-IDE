package com.arnix.ide.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.ide.R
import com.arnix.ide.ui.components.CodeEditorView
import com.arnix.ide.ui.components.EditorTopBar
import com.arnix.ide.ui.components.QuickSymbolBar
import com.arnix.ide.ui.theme.*
import com.arnix.ide.ui.viewmodel.IdeScreen
import com.arnix.ide.ui.viewmodel.IdeViewModel

@Composable
fun ArnixMainScreen(
    viewModel: IdeViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val openTabs by viewModel.openTabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val rootFiles by viewModel.rootFiles.collectAsState()
    val workspaceName by viewModel.workspaceName.collectAsState()
    val showFileManager by viewModel.showFileManager.collectAsState()
    val fileManagerPath by viewModel.fileManagerPath.collectAsState()
    val gitState by viewModel.gitState.collectAsState()
    val terminalLines by viewModel.terminalLines.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val showNewFileDialog by viewModel.showNewFileDialog.collectAsState()
    val targetFolderPath by viewModel.targetFolderPath.collectAsState()

    // Handle Back Press gracefully: returns to Editor screen if on another screen
    BackHandler(enabled = currentScreen != IdeScreen.EDITOR && !showFileManager) {
        viewModel.setScreen(IdeScreen.EDITOR)
    }

    val activeTab = openTabs.find { it.id == activeTabId }

    // Auto-fit & Responsive Layout via BoxWithConstraints
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
            .testTag("arnix_main_container")
    ) {
        val isWideScreen = maxWidth >= 700.dp
        val isExpanded = maxWidth >= 960.dp

        if (isWideScreen) {
            // Tablet / Foldable / Landscape layout with NavigationRail
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Adaptive NavigationRail
                NavigationRail(
                    containerColor = IdeSurface,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        ) {
                            Surface(
                                color = IdePrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "{ }",
                                        color = IdePrimary,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("adaptive_nav_rail"),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationRailItem(
                        selected = currentScreen == IdeScreen.EDITOR && !showFileManager,
                        onClick = {
                            viewModel.closeFileManager()
                            viewModel.setScreen(IdeScreen.EDITOR)
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_file_code),
                                contentDescription = stringResource(R.string.nav_editor),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_editor), fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = IdePrimary,
                            selectedTextColor = IdePrimary,
                            indicatorColor = IdePrimary.copy(alpha = 0.15f),
                            unselectedIconColor = IdeTextMuted,
                            unselectedTextColor = IdeTextMuted
                        ),
                        modifier = Modifier.testTag("rail_item_editor")
                    )

                    NavigationRailItem(
                        selected = currentScreen == IdeScreen.EXPLORER,
                        onClick = { viewModel.setScreen(IdeScreen.EXPLORER) },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_folder),
                                contentDescription = stringResource(R.string.nav_explorer),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_explorer), fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = IdeSecondary,
                            selectedTextColor = IdeSecondary,
                            indicatorColor = IdeSecondary.copy(alpha = 0.15f),
                            unselectedIconColor = IdeTextMuted,
                            unselectedTextColor = IdeTextMuted
                        ),
                        modifier = Modifier.testTag("rail_item_explorer")
                    )

                    NavigationRailItem(
                        selected = currentScreen == IdeScreen.GIT && !showFileManager,
                        onClick = {
                            viewModel.closeFileManager()
                            viewModel.setScreen(IdeScreen.GIT)
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_git_branch),
                                contentDescription = stringResource(R.string.nav_git),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_git), fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = IdeAccentViolet,
                            selectedTextColor = IdeAccentViolet,
                            indicatorColor = IdeAccentViolet.copy(alpha = 0.15f),
                            unselectedIconColor = IdeTextMuted,
                            unselectedTextColor = IdeTextMuted
                        ),
                        modifier = Modifier.testTag("rail_item_git")
                    )

                    NavigationRailItem(
                        selected = currentScreen == IdeScreen.TERMINAL && !showFileManager,
                        onClick = {
                            viewModel.closeFileManager()
                            viewModel.setScreen(IdeScreen.TERMINAL)
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_terminal),
                                contentDescription = stringResource(R.string.nav_terminal),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_terminal), fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = TerminalCyan,
                            selectedTextColor = TerminalCyan,
                            indicatorColor = TerminalCyan.copy(alpha = 0.15f),
                            unselectedIconColor = IdeTextMuted,
                            unselectedTextColor = IdeTextMuted
                        ),
                        modifier = Modifier.testTag("rail_item_terminal")
                    )

                    NavigationRailItem(
                        selected = currentScreen == IdeScreen.SETTINGS && !showFileManager,
                        onClick = {
                            viewModel.closeFileManager()
                            viewModel.setScreen(IdeScreen.SETTINGS)
                        },
                        icon = {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings), modifier = Modifier.size(20.dp))
                        },
                        label = { Text(stringResource(R.string.nav_settings), fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = IdeTertiary,
                            selectedTextColor = IdeTertiary,
                            indicatorColor = IdeTertiary.copy(alpha = 0.15f),
                            unselectedIconColor = IdeTextMuted,
                            unselectedTextColor = IdeTextMuted
                        ),
                        modifier = Modifier.testTag("rail_item_settings")
                    )
                }

                VerticalDivider(color = IdeBorderSubtle, thickness = 1.dp)

                // Wide Content Area (Dual pane on Expanded screens for Editor + Explorer)
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (currentScreen == IdeScreen.EDITOR && !showFileManager && isExpanded) {
                        // Desktop / Expanded Tablet Dual-Pane: Explorer on left, Editor on right
                        Row(modifier = Modifier.fillMaxSize()) {
                            Surface(
                                color = IdeSurface,
                                modifier = Modifier
                                    .width(280.dp)
                                    .fillMaxHeight()
                            ) {
                                ExplorerScreen(
                                    files = rootFiles,
                                    workspaceName = workspaceName,
                                    onBrowseClick = { viewModel.openFileManager() },
                                    onOpenFile = { viewModel.openFileInEditor(it) },
                                    onToggleFolder = { viewModel.toggleFolder(it) },
                                    onNewFileClick = { viewModel.openNewFileDialog(it) },
                                    onDeleteFile = { viewModel.deleteFile(it) }
                                )
                            }

                            VerticalDivider(color = IdeBorderSubtle, thickness = 1.dp)

                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                EditorTopBar(
                                    tabs = openTabs,
                                    activeTabId = activeTabId,
                                    onSelectTab = { viewModel.selectTab(it) },
                                    onCloseTab = { viewModel.closeTab(it) },
                                    onSave = { viewModel.saveCurrentFile() },
                                    onRun = { viewModel.runProject() },
                                    onUndo = { viewModel.undo() },
                                    onRedo = { viewModel.redo() },
                                    onToggleFind = { viewModel.toggleFindBar() }
                                )

                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    if (activeTab != null) {
                                        val ext = activeTab.fileName.substringAfterLast(".", "")
                                        CodeEditorView(
                                            code = activeTab.content,
                                            extension = ext,
                                            settings = settings,
                                            onCodeChange = { viewModel.updateCurrentCode(it) }
                                        )
                                    } else {
                                        EmptyEditorState(onOpenExplorer = { viewModel.setScreen(IdeScreen.EXPLORER) })
                                    }
                                }

                                if (activeTab != null) {
                                    QuickSymbolBar(
                                        onInsertSymbol = { symbol -> viewModel.insertSymbolAtCursor(symbol) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Single Pane Content Router
                        MainContentRouter(
                            currentScreen = currentScreen,
                            showFileManager = showFileManager,
                            openTabs = openTabs,
                            activeTabId = activeTabId,
                            activeTab = activeTab,
                            rootFiles = rootFiles,
                            workspaceName = workspaceName,
                            gitState = gitState,
                            terminalLines = terminalLines,
                            settings = settings,
                            viewModel = viewModel
                        )
                    }
                }
            }
        } else {
            // Handheld / Compact Screen (< 700dp) with Standard Bottom Navigation
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("arnix_compact_scaffold"),
                containerColor = IdeBackground,
                contentWindowInsets = WindowInsets.statusBars,
                bottomBar = {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        // Quick Symbol Bar for mobile coding
                        if (currentScreen == IdeScreen.EDITOR && !showFileManager && activeTab != null) {
                            QuickSymbolBar(
                                onInsertSymbol = { symbol -> viewModel.insertSymbolAtCursor(symbol) }
                            )
                        }

                        // Bottom Navigation Bar with fluid height
                        Surface(
                            color = IdeSurface,
                            border = BorderStroke(1.dp, IdeBorderSubtle)
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == IdeScreen.EDITOR && !showFileManager,
                                    onClick = {
                                        viewModel.closeFileManager()
                                        viewModel.setScreen(IdeScreen.EDITOR)
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_file_code),
                                            contentDescription = stringResource(R.string.nav_editor),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_editor), fontSize = 10.sp, fontWeight = if (currentScreen == IdeScreen.EDITOR) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = IdePrimary,
                                        selectedTextColor = IdePrimary,
                                        indicatorColor = IdePrimary.copy(alpha = 0.15f),
                                        unselectedIconColor = IdeTextMuted,
                                        unselectedTextColor = IdeTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_editor")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == IdeScreen.EXPLORER,
                                    onClick = { viewModel.setScreen(IdeScreen.EXPLORER) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_folder),
                                            contentDescription = stringResource(R.string.nav_explorer),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_explorer), fontSize = 10.sp, fontWeight = if (currentScreen == IdeScreen.EXPLORER) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = IdeSecondary,
                                        selectedTextColor = IdeSecondary,
                                        indicatorColor = IdeSecondary.copy(alpha = 0.15f),
                                        unselectedIconColor = IdeTextMuted,
                                        unselectedTextColor = IdeTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_explorer")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == IdeScreen.GIT && !showFileManager,
                                    onClick = {
                                        viewModel.closeFileManager()
                                        viewModel.setScreen(IdeScreen.GIT)
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_git_branch),
                                            contentDescription = stringResource(R.string.nav_git),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_git), fontSize = 10.sp, fontWeight = if (currentScreen == IdeScreen.GIT) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = IdeAccentViolet,
                                        selectedTextColor = IdeAccentViolet,
                                        indicatorColor = IdeAccentViolet.copy(alpha = 0.15f),
                                        unselectedIconColor = IdeTextMuted,
                                        unselectedTextColor = IdeTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_git")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == IdeScreen.TERMINAL && !showFileManager,
                                    onClick = {
                                        viewModel.closeFileManager()
                                        viewModel.setScreen(IdeScreen.TERMINAL)
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_terminal),
                                            contentDescription = stringResource(R.string.nav_terminal),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    },
                                    label = { Text(stringResource(R.string.nav_terminal), fontSize = 10.sp, fontWeight = if (currentScreen == IdeScreen.TERMINAL) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = TerminalCyan,
                                        selectedTextColor = TerminalCyan,
                                        indicatorColor = TerminalCyan.copy(alpha = 0.15f),
                                        unselectedIconColor = IdeTextMuted,
                                        unselectedTextColor = IdeTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_terminal")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == IdeScreen.SETTINGS && !showFileManager,
                                    onClick = {
                                        viewModel.closeFileManager()
                                        viewModel.setScreen(IdeScreen.SETTINGS)
                                    },
                                    icon = {
                                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings), modifier = Modifier.size(19.dp))
                                    },
                                    label = { Text(stringResource(R.string.nav_settings), fontSize = 10.sp, fontWeight = if (currentScreen == IdeScreen.SETTINGS) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = IdeTertiary,
                                        selectedTextColor = IdeTertiary,
                                        indicatorColor = IdeTertiary.copy(alpha = 0.15f),
                                        unselectedIconColor = IdeTextMuted,
                                        unselectedTextColor = IdeTextMuted
                                    ),
                                    modifier = Modifier.testTag("nav_item_settings")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    MainContentRouter(
                        currentScreen = currentScreen,
                        showFileManager = showFileManager,
                        openTabs = openTabs,
                        activeTabId = activeTabId,
                        activeTab = activeTab,
                        rootFiles = rootFiles,
                        workspaceName = workspaceName,
                        gitState = gitState,
                        terminalLines = terminalLines,
                        settings = settings,
                        viewModel = viewModel
                    )
                }
            }
        }

        // Create New File Dialog Modal (Responsive scrollable dialog)
        if (showNewFileDialog) {
            var newFileName by remember { mutableStateOf("") }
            var isFolderType by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { viewModel.closeNewFileDialog() },
                title = {
                    Text(
                        text = if (isFolderType) "Create New Directory" else "Create New File",
                        color = IdeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (targetFolderPath.isNotEmpty()) {
                            Text(
                                text = "Location: $targetFolderPath",
                                color = IdeTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2
                            )
                        }
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            placeholder = {
                                Text(
                                    text = if (isFolderType) "directory_name" else "filename.kt",
                                    color = IdeTextMuted,
                                    fontSize = 12.sp
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_file_name"),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(color = IdeTextPrimary, fontSize = 13.sp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !isFolderType,
                                onClick = { isFolderType = false },
                                label = { Text("File") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_file_code),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            FilterChip(
                                selected = isFolderType,
                                onClick = { isFolderType = true },
                                label = { Text("Folder") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_new_folder),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFileName.isNotBlank()) {
                                viewModel.createNewFile(newFileName.trim(), isFolderType)
                                viewModel.closeNewFileDialog()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = IdePrimary)
                    ) {
                        Text("Create", color = IdeBackground, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeNewFileDialog() }) {
                        Text("Cancel", color = IdeTextSecondary)
                    }
                },
                containerColor = IdeSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // In-App Native File Manager Screen (Scoped to Files tab)
        if (showFileManager && currentScreen == IdeScreen.EXPLORER) {
            FileManagerScreen(
                currentPath = fileManagerPath,
                onNavigateTo = { viewModel.navigateFileManagerTo(it) },
                onNavigateUp = { viewModel.navigateFileManagerUp() },
                onSelectWorkspaceFolder = { viewModel.selectWorkspaceFolder(it) },
                onOpenFileInEditor = { viewModel.openExternalFileInEditor(it) },
                onClose = { viewModel.closeFileManager() }
            )
        }
    }
}

@Composable
private fun MainContentRouter(
    currentScreen: IdeScreen,
    showFileManager: Boolean,
    openTabs: List<com.arnix.ide.data.model.CodeTab>,
    activeTabId: String?,
    activeTab: com.arnix.ide.data.model.CodeTab?,
    rootFiles: List<com.arnix.ide.data.model.FileItem>,
    workspaceName: String,
    gitState: com.arnix.ide.data.model.GitState,
    terminalLines: List<com.arnix.ide.data.model.TerminalLine>,
    settings: com.arnix.ide.data.model.IdeSettings,
    viewModel: IdeViewModel
) {
    when (currentScreen) {
        IdeScreen.EDITOR -> {
            Column(modifier = Modifier.fillMaxSize()) {
                EditorTopBar(
                    tabs = openTabs,
                    activeTabId = activeTabId,
                    onSelectTab = { viewModel.selectTab(it) },
                    onCloseTab = { viewModel.closeTab(it) },
                    onSave = { viewModel.saveCurrentFile() },
                    onRun = { viewModel.runProject() },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onToggleFind = { viewModel.toggleFindBar() }
                )

                if (activeTab != null) {
                    val ext = activeTab.fileName.substringAfterLast(".", "")
                    CodeEditorView(
                        code = activeTab.content,
                        extension = ext,
                        settings = settings,
                        onCodeChange = { viewModel.updateCurrentCode(it) }
                    )
                } else {
                    EmptyEditorState(onOpenExplorer = { viewModel.setScreen(IdeScreen.EXPLORER) })
                }
            }
        }

        IdeScreen.EXPLORER -> {
            ExplorerScreen(
                files = rootFiles,
                workspaceName = workspaceName,
                onBrowseClick = { viewModel.openFileManager() },
                onOpenFile = { viewModel.openFileInEditor(it) },
                onToggleFolder = { viewModel.toggleFolder(it) },
                onNewFileClick = { viewModel.openNewFileDialog(it) },
                onDeleteFile = { viewModel.deleteFile(it) }
            )
        }

        IdeScreen.GIT -> {
            GitScreen(
                gitState = gitState,
                onStageFile = { viewModel.stageFile(it) },
                onUnstageFile = { viewModel.unstageFile(it) },
                onStageAll = { viewModel.stageAll() },
                onCommit = { viewModel.commit(it) },
                onSwitchBranch = { viewModel.switchBranch(it) },
                onCreateBranch = { viewModel.createBranch(it) }
            )
        }

        IdeScreen.TERMINAL -> {
            TerminalScreen(
                lines = terminalLines,
                onExecuteCommand = { viewModel.executeTerminalCommand(it) }
            )
        }

        IdeScreen.SETTINGS -> {
            SettingsScreen(
                settings = settings,
                githubUser = gitState.githubUser,
                githubRepo = gitState.remoteRepoUrl,
                onUpdateSettings = { viewModel.updateSettings(it) },
                onUpdateGithub = { repo, user, pat ->
                    viewModel.configureGithub(repo, user, pat)
                }
            )
        }
    }
}

@Composable
private fun EmptyEditorState(onOpenExplorer: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Surface(
                color = IdeSurfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, IdeBorderSubtle),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_file_code),
                        contentDescription = null,
                        tint = IdePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = "No open files",
                color = IdeTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Select a file from the Project Explorer or browse storage to start editing.",
                color = IdeTextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onOpenExplorer,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IdePrimary, contentColor = IdeBackground)
            ) {
                Text("Open Files in Explorer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
