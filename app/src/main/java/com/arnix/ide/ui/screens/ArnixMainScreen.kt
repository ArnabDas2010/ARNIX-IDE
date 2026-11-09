package com.arnix.ide.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("arnix_main_scaffold"),
        containerColor = IdeBackground,
        bottomBar = {
            Column {
                // Show mobile coding symbols keyboard helper only on Editor when file manager is closed
                if (currentScreen == IdeScreen.EDITOR && !showFileManager && activeTab != null) {
                    QuickSymbolBar(
                        onInsertSymbol = { symbol -> viewModel.insertSymbolAtCursor(symbol) }
                    )
                }

                // Bottom Navigation Bar
                NavigationBar(
                    containerColor = IdeSurface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == IdeScreen.EDITOR && !showFileManager,
                        onClick = {
                            viewModel.closeFileManager()
                            viewModel.setScreen(IdeScreen.EDITOR)
                        },
                        icon = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_file_code),
                                contentDescription = stringResource(R.string.nav_editor),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_editor), fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IdePrimary,
                            selectedTextColor = IdePrimary,
                            indicatorColor = IdePrimaryContainer.copy(alpha = 0.3f),
                            unselectedIconColor = IdeTextSecondary,
                            unselectedTextColor = IdeTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_item_editor")
                    )

                    NavigationBarItem(
                        selected = currentScreen == IdeScreen.EXPLORER,
                        onClick = { viewModel.setScreen(IdeScreen.EXPLORER) },
                        icon = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_folder),
                                contentDescription = stringResource(R.string.nav_explorer),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_explorer), fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IdePrimary,
                            selectedTextColor = IdePrimary,
                            indicatorColor = IdePrimaryContainer.copy(alpha = 0.3f),
                            unselectedIconColor = IdeTextSecondary,
                            unselectedTextColor = IdeTextSecondary
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
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_git_branch),
                                contentDescription = stringResource(R.string.nav_git),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_git), fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IdePrimary,
                            selectedTextColor = IdePrimary,
                            indicatorColor = IdePrimaryContainer.copy(alpha = 0.3f),
                            unselectedIconColor = IdeTextSecondary,
                            unselectedTextColor = IdeTextSecondary
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
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_terminal),
                                contentDescription = stringResource(R.string.nav_terminal),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.nav_terminal), fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IdePrimary,
                            selectedTextColor = IdePrimary,
                            indicatorColor = IdePrimaryContainer.copy(alpha = 0.3f),
                            unselectedIconColor = IdeTextSecondary,
                            unselectedTextColor = IdeTextSecondary
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
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings))
                        },
                        label = { Text(stringResource(R.string.nav_settings), fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IdePrimary,
                            selectedTextColor = IdePrimary,
                            indicatorColor = IdePrimaryContainer.copy(alpha = 0.3f),
                            unselectedIconColor = IdeTextSecondary,
                            unselectedTextColor = IdeTextSecondary
                        ),
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                            // Empty state when no tabs are open
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(IdeBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_file_code),
                                        contentDescription = null,
                                        tint = IdeTextMuted,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "No open files",
                                        color = IdeTextSecondary,
                                        fontSize = 16.sp
                                    )
                                    Button(
                                        onClick = { viewModel.setScreen(IdeScreen.EXPLORER) },
                                        colors = ButtonDefaults.buttonColors(containerColor = IdePrimaryContainer)
                                    ) {
                                        Text("Open Files in Explorer")
                                    }
                                }
                            }
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

            // Create New File Dialog Modal
            if (showNewFileDialog) {
                var newFileName by remember { mutableStateOf("") }
                var isFolderType by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { viewModel.closeNewFileDialog() },
                    title = {
                        Text(
                            text = if (isFolderType) "Create New Directory" else "Create New File",
                            color = IdeTextPrimary
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (targetFolderPath.isNotEmpty()) {
                                Text(
                                    text = "Location: $targetFolderPath",
                                    color = IdeTextMuted,
                                    fontSize = 11.sp
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
                                    .testTag("input_new_file_name")
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
                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_file_code),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                                FilterChip(
                                    selected = isFolderType,
                                    onClick = { isFolderType = true },
                                    label = { Text("Folder") },
                                    leadingIcon = {
                                        Icon(
                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_folder),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newFileName.isNotBlank()) {
                                    viewModel.createNewFile(newFileName.trim(), isFolderType)
                                }
                            },
                            modifier = Modifier.testTag("btn_confirm_create_file")
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.closeNewFileDialog() }) {
                            Text("Cancel", color = IdeTextSecondary)
                        }
                    },
                    containerColor = IdeSurface
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
}
