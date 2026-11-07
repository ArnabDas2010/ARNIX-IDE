package com.arnix.ide.data.model

data class GitCommit(
    val id: String,
    val shortHash: String,
    val message: String,
    val author: String,
    val timestamp: String,
    val changedFilesCount: Int
)

data class GitFileChange(
    val filePath: String,
    val status: GitChangeType,
    val isStaged: Boolean = false
)

enum class GitChangeType {
    MODIFIED,
    ADDED,
    DELETED,
    UNTRACKED
}

data class GitState(
    val currentBranch: String = "main",
    val availableBranches: List<String> = listOf("main", "feature/dev", "release/v1.0"),
    val stagedFiles: List<GitFileChange> = emptyList(),
    val unstagedFiles: List<GitFileChange> = emptyList(),
    val commitHistory: List<GitCommit> = emptyList(),
    val remoteRepoUrl: String = "https://github.com/ArnabDas2010/ARNIX-IDE",
    val isConnected: Boolean = true,
    val githubUser: String = "ArnabDas2010",
    val personalAccessToken: String = ""
)
