package com.arnix.ide.data.repository

import com.arnix.ide.data.model.GitChangeType
import com.arnix.ide.data.model.GitCommit
import com.arnix.ide.data.model.GitFileChange
import com.arnix.ide.data.model.GitState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GitRepository {

    private val _gitState = MutableStateFlow(
        GitState(
            currentBranch = "main",
            availableBranches = listOf("main", "feature/dev", "release/v1.0"),
            unstagedFiles = listOf(
                GitFileChange("app/src/main/java/com/arnix/app/MainActivity.kt", GitChangeType.MODIFIED),
                GitFileChange("README.md", GitChangeType.MODIFIED)
            ),
            commitHistory = listOf(
                GitCommit(
                    id = UUID.randomUUID().toString(),
                    shortHash = "9a7f31c",
                    message = "Initial commit: ARNIX IDE baseline project structure",
                    author = "ArnabDas2010",
                    timestamp = "Just now",
                    changedFilesCount = 4
                ),
                GitCommit(
                    id = UUID.randomUUID().toString(),
                    shortHash = "e3d10ab",
                    message = "Setup Jetpack Compose & Material 3 architecture",
                    author = "majumdermitali1986",
                    timestamp = "2 hours ago",
                    changedFilesCount = 7
                )
            )
        )
    )
    val gitState: StateFlow<GitState> = _gitState.asStateFlow()

    fun stageFile(filePath: String) {
        val current = _gitState.value
        val fileToStage = current.unstagedFiles.find { it.filePath == filePath } ?: return
        _gitState.value = current.copy(
            unstagedFiles = current.unstagedFiles.filter { it.filePath != filePath },
            stagedFiles = current.stagedFiles + fileToStage.copy(isStaged = true)
        )
    }

    fun unstageFile(filePath: String) {
        val current = _gitState.value
        val fileToUnstage = current.stagedFiles.find { it.filePath == filePath } ?: return
        _gitState.value = current.copy(
            stagedFiles = current.stagedFiles.filter { it.filePath != filePath },
            unstagedFiles = current.unstagedFiles + fileToUnstage.copy(isStaged = false)
        )
    }

    fun stageAll() {
        val current = _gitState.value
        val newlyStaged = current.unstagedFiles.map { it.copy(isStaged = true) }
        _gitState.value = current.copy(
            stagedFiles = current.stagedFiles + newlyStaged,
            unstagedFiles = emptyList()
        )
    }

    fun commitStaged(message: String): Boolean {
        if (message.isBlank()) return false
        val current = _gitState.value
        if (current.stagedFiles.isEmpty()) return false

        val newCommit = GitCommit(
            id = UUID.randomUUID().toString(),
            shortHash = UUID.randomUUID().toString().take(7),
            message = message,
            author = current.githubUser.ifEmpty { "ARNIX Developer" },
            timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            changedFilesCount = current.stagedFiles.size
        )

        _gitState.value = current.copy(
            commitHistory = listOf(newCommit) + current.commitHistory,
            stagedFiles = emptyList()
        )
        return true
    }

    fun switchBranch(branchName: String) {
        val current = _gitState.value
        if (current.availableBranches.contains(branchName)) {
            _gitState.value = current.copy(currentBranch = branchName)
        }
    }

    fun createBranch(branchName: String) {
        val current = _gitState.value
        if (branchName.isNotBlank() && !current.availableBranches.contains(branchName)) {
            _gitState.value = current.copy(
                availableBranches = current.availableBranches + branchName,
                currentBranch = branchName
            )
        }
    }

    fun updateGitHubConfig(repoUrl: String, user: String, patToken: String) {
        val current = _gitState.value
        _gitState.value = current.copy(
            remoteRepoUrl = repoUrl,
            githubUser = user,
            personalAccessToken = patToken,
            isConnected = true
        )
    }
}
