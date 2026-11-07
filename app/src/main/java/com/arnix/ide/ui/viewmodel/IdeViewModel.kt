package com.arnix.ide.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arnix.ide.data.model.*
import com.arnix.ide.data.repository.GitRepository
import com.arnix.ide.data.repository.WorkspaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class IdeScreen {
    EDITOR,
    EXPLORER,
    GIT,
    TERMINAL,
    SETTINGS
}

class IdeViewModel(
    private val workspaceRepo: WorkspaceRepository = WorkspaceRepository(),
    private val gitRepo: GitRepository = GitRepository()
) : ViewModel() {

    val rootFiles: StateFlow<List<FileItem>> = workspaceRepo.rootFiles
    val gitState: StateFlow<GitState> = gitRepo.gitState

    private val _currentScreen = MutableStateFlow(IdeScreen.EDITOR)
    val currentScreen: StateFlow<IdeScreen> = _currentScreen.asStateFlow()

    private val _openTabs = MutableStateFlow<List<CodeTab>>(emptyList())
    val openTabs: StateFlow<List<CodeTab>> = _openTabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val _settings = MutableStateFlow(IdeSettings())
    val settings: StateFlow<IdeSettings> = _settings.asStateFlow()

    // Undo / Redo stacks
    private val undoStack = mutableMapOf<String, MutableList<String>>()
    private val redoStack = mutableMapOf<String, MutableList<String>>()

    // Terminal State
    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(
        listOf(
            TerminalLine("init_1", "ARNIX Android IDE Terminal v1.0.0", TerminalLineType.INFO),
            TerminalLine("init_2", "Workspace: /storage/emulated/0/ARNIX-Projects/app", TerminalLineType.INFO),
            TerminalLine("init_3", "Ready. Type 'help' to see available tools.", TerminalLineType.SUCCESS)
        )
    )
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()

    // UI Dialogs
    private val _showNewFileDialog = MutableStateFlow(false)
    val showNewFileDialog: StateFlow<Boolean> = _showNewFileDialog.asStateFlow()

    private val _targetFolderPath = MutableStateFlow("")
    val targetFolderPath: StateFlow<String> = _targetFolderPath.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showFindBar = MutableStateFlow(false)
    val showFindBar: StateFlow<Boolean> = _showFindBar.asStateFlow()

    init {
        // Open default file in editor tab on start
        viewModelScope.launch {
            rootFiles.collect { files ->
                if (_openTabs.value.isEmpty()) {
                    val defaultFile = workspaceRepo.findFileByPath("app/src/main/java/com/arnix/app/MainActivity.kt")
                        ?: files.firstOrNull { !it.isDirectory }
                    if (defaultFile != null) {
                        openFileInEditor(defaultFile)
                    }
                }
            }
        }
    }

    fun setScreen(screen: IdeScreen) {
        _currentScreen.value = screen
    }

    fun openFileInEditor(file: FileItem) {
        if (file.isDirectory) return

        val existingTab = _openTabs.value.find { it.filePath == file.path }
        if (existingTab != null) {
            _activeTabId.value = existingTab.id
        } else {
            val newTab = CodeTab(
                id = UUID.randomUUID().toString(),
                filePath = file.path,
                fileName = file.name,
                content = file.content,
                isDirty = false
            )
            _openTabs.value = _openTabs.value + newTab
            _activeTabId.value = newTab.id
        }
        _currentScreen.value = IdeScreen.EDITOR
    }

    fun closeTab(tabId: String) {
        val currentTabs = _openTabs.value
        val newTabs = currentTabs.filter { it.id != tabId }
        _openTabs.value = newTabs

        if (_activeTabId.value == tabId) {
            _activeTabId.value = newTabs.lastOrNull()?.id
        }
    }

    fun selectTab(tabId: String) {
        _activeTabId.value = tabId
    }

    fun updateCurrentCode(newContent: String) {
        val tabId = _activeTabId.value ?: return
        val currentTabs = _openTabs.value
        val tab = currentTabs.find { it.id == tabId } ?: return

        // Push to undo stack
        val fileUndo = undoStack.getOrPut(tabId) { mutableListOf() }
        if (fileUndo.lastOrNull() != tab.content) {
            fileUndo.add(tab.content)
            if (fileUndo.size > 50) fileUndo.removeAt(0)
        }
        redoStack[tabId]?.clear()

        _openTabs.value = currentTabs.map {
            if (it.id == tabId) it.copy(content = newContent, isDirty = true) else it
        }

        if (_settings.value.autoSave) {
            saveCurrentFile()
        }
    }

    fun undo() {
        val tabId = _activeTabId.value ?: return
        val fileUndo = undoStack[tabId] ?: return
        if (fileUndo.isEmpty()) return

        val prevContent = fileUndo.removeAt(fileUndo.size - 1)
        val currentContent = _openTabs.value.find { it.id == tabId }?.content ?: ""

        val fileRedo = redoStack.getOrPut(tabId) { mutableListOf() }
        fileRedo.add(currentContent)

        _openTabs.value = _openTabs.value.map {
            if (it.id == tabId) it.copy(content = prevContent, isDirty = true) else it
        }
    }

    fun redo() {
        val tabId = _activeTabId.value ?: return
        val fileRedo = redoStack[tabId] ?: return
        if (fileRedo.isEmpty()) return

        val nextContent = fileRedo.removeAt(fileRedo.size - 1)
        val currentContent = _openTabs.value.find { it.id == tabId }?.content ?: ""

        val fileUndo = undoStack.getOrPut(tabId) { mutableListOf() }
        fileUndo.add(currentContent)

        _openTabs.value = _openTabs.value.map {
            if (it.id == tabId) it.copy(content = nextContent, isDirty = true) else it
        }
    }

    fun saveCurrentFile() {
        val tabId = _activeTabId.value ?: return
        val tab = _openTabs.value.find { it.id == tabId } ?: return

        workspaceRepo.updateFileContent(tab.filePath, tab.content)
        _openTabs.value = _openTabs.value.map {
            if (it.id == tabId) it.copy(isDirty = false) else it
        }
    }

    fun insertSymbolAtCursor(symbol: String) {
        val tabId = _activeTabId.value ?: return
        val tab = _openTabs.value.find { it.id == tabId } ?: return
        updateCurrentCode(tab.content + symbol)
    }

    fun toggleFolder(folderPath: String) {
        workspaceRepo.toggleFolderExpanded(folderPath)
    }

    fun openNewFileDialog(parentPath: String = "") {
        _targetFolderPath.value = parentPath
        _showNewFileDialog.value = true
    }

    fun closeNewFileDialog() {
        _showNewFileDialog.value = false
    }

    fun createNewFile(fileName: String, isDirectory: Boolean) {
        val created = workspaceRepo.createFile(_targetFolderPath.value, fileName, isDirectory)
        _showNewFileDialog.value = false
        if (!isDirectory) {
            openFileInEditor(created)
        }
    }

    fun deleteFile(path: String) {
        workspaceRepo.deleteFile(path)
        val openTab = _openTabs.value.find { it.filePath == path }
        if (openTab != null) {
            closeTab(openTab.id)
        }
    }

    // Git Actions
    fun stageFile(filePath: String) = gitRepo.stageFile(filePath)
    fun unstageFile(filePath: String) = gitRepo.unstageFile(filePath)
    fun stageAll() = gitRepo.stageAll()
    fun commit(message: String): Boolean = gitRepo.commitStaged(message)
    fun switchBranch(branch: String) = gitRepo.switchBranch(branch)
    fun createBranch(branch: String) = gitRepo.createBranch(branch)
    fun configureGithub(repo: String, user: String, pat: String) = gitRepo.updateGitHubConfig(repo, user, pat)

    // Terminal Commands Execution
    fun executeTerminalCommand(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        val lines = _terminalLines.value.toMutableList()
        lines.add(TerminalLine(UUID.randomUUID().toString(), "$ $trimmed", TerminalLineType.PROMPT))

        val parts = trimmed.split("\\s+".toRegex())
        val cmd = parts[0].lowercase()

        when (cmd) {
            "help" -> {
                lines.add(TerminalLine(UUID.randomUUID().toString(), "ARNIX Built-in Commands:", TerminalLineType.INFO))
                lines.add(TerminalLine(UUID.randomUUID().toString(), "  run / build       Build and execute current project", TerminalLineType.OUTPUT))
                lines.add(TerminalLine(UUID.randomUUID().toString(), "  ls / dir          List workspace directory files", TerminalLineType.OUTPUT))
                lines.add(TerminalLine(UUID.randomUUID().toString(), "  cat <file>        Display file contents", TerminalLineType.OUTPUT))
                lines.add(TerminalLine(UUID.randomUUID().toString(), "  git status        Show working tree status", TerminalLineType.OUTPUT))
                lines.add(TerminalLine(UUID.randomUUID().toString(), "  git branch        List and switch branches", TerminalLineType.OUTPUT))
                lines.add(TerminalLine(UUID.randomUUID().toString(), "  clear             Clear terminal screen", TerminalLineType.OUTPUT))
                lines.add(TerminalLine(UUID.randomUUID().toString(), "  echo <text>       Print message to console", TerminalLineType.OUTPUT))
            }
            "clear", "cls" -> {
                _terminalLines.value = emptyList()
                return
            }
            "ls", "dir" -> {
                val files = _rootFiles.value
                val formatted = files.joinToString("  ") { if (it.isDirectory) "${it.name}/" else it.name }
                lines.add(TerminalLine(UUID.randomUUID().toString(), formatted, TerminalLineType.OUTPUT))
            }
            "git" -> {
                val subCmd = parts.getOrNull(1)?.lowercase() ?: ""
                when (subCmd) {
                    "status" -> {
                        val git = gitState.value
                        lines.add(TerminalLine(UUID.randomUUID().toString(), "On branch ${git.currentBranch}", TerminalLineType.INFO))
                        if (git.stagedFiles.isNotEmpty()) {
                            lines.add(TerminalLine(UUID.randomUUID().toString(), "Changes to be committed:", TerminalLineType.SUCCESS))
                            git.stagedFiles.forEach { change ->
                                lines.add(TerminalLine(UUID.randomUUID().toString(), "  modified: ${change.filePath}", TerminalLineType.SUCCESS))
                            }
                        }
                        if (git.unstagedFiles.isNotEmpty()) {
                            lines.add(TerminalLine(UUID.randomUUID().toString(), "Changes not staged for commit:", TerminalLineType.ERROR))
                            git.unstagedFiles.forEach { change ->
                                lines.add(TerminalLine(UUID.randomUUID().toString(), "  modified: ${change.filePath}", TerminalLineType.ERROR))
                            }
                        }
                        if (git.stagedFiles.isEmpty() && git.unstagedFiles.isEmpty()) {
                            lines.add(TerminalLine(UUID.randomUUID().toString(), "nothing to commit, working tree clean", TerminalLineType.OUTPUT))
                        }
                    }
                    "branch" -> {
                        val git = gitState.value
                        git.availableBranches.forEach { b ->
                            val prefix = if (b == git.currentBranch) "* " else "  "
                            lines.add(TerminalLine(UUID.randomUUID().toString(), "$prefix$b", if (b == git.currentBranch) TerminalLineType.SUCCESS else TerminalLineType.OUTPUT))
                        }
                    }
                    else -> {
                        lines.add(TerminalLine(UUID.randomUUID().toString(), "git: '$subCmd' is supported via the Git tab.", TerminalLineType.INFO))
                    }
                }
            }
            "run", "build", "gradle" -> {
                runProject()
                return
            }
            "echo" -> {
                val text = parts.drop(1).joinToString(" ")
                lines.add(TerminalLine(UUID.randomUUID().toString(), text, TerminalLineType.OUTPUT))
            }
            else -> {
                lines.add(TerminalLine(UUID.randomUUID().toString(), "bash: $cmd: command simulated. Enter 'help' for available commands.", TerminalLineType.INFO))
            }
        }
        _terminalLines.value = lines
    }

    private val _rootFiles = workspaceRepo.rootFiles

    fun runProject() {
        val currentTab = _openTabs.value.find { it.id == _activeTabId.value }
        val lines = _terminalLines.value.toMutableList()
        lines.add(TerminalLine(UUID.randomUUID().toString(), "$ ./gradlew assembleDebug", TerminalLineType.PROMPT))
        lines.add(TerminalLine(UUID.randomUUID().toString(), "> Starting Gradle Daemon...", TerminalLineType.INFO))
        lines.add(TerminalLine(UUID.randomUUID().toString(), "> Task :app:preBuild UP-TO-DATE", TerminalLineType.OUTPUT))
        lines.add(TerminalLine(UUID.randomUUID().toString(), "> Task :app:compileDebugKotlin", TerminalLineType.OUTPUT))
        lines.add(TerminalLine(UUID.randomUUID().toString(), "> Task :app:mergeDebugResources", TerminalLineType.OUTPUT))
        lines.add(TerminalLine(UUID.randomUUID().toString(), "> Task :app:packageDebug", TerminalLineType.OUTPUT))
        lines.add(TerminalLine(UUID.randomUUID().toString(), "BUILD SUCCESSFUL in 1s", TerminalLineType.SUCCESS))
        lines.add(TerminalLine(UUID.randomUUID().toString(), "APK: app-debug.apk (Target: ${currentTab?.fileName ?: "com.arnix.ide"})", TerminalLineType.SUCCESS))

        _terminalLines.value = lines
        _currentScreen.value = IdeScreen.TERMINAL
    }

    fun updateSettings(newSettings: IdeSettings) {
        _settings.value = newSettings
    }

    fun toggleFindBar() {
        _showFindBar.value = !_showFindBar.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
