package com.arnix.ide.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.ide.R
import com.arnix.ide.data.model.GitChangeType
import com.arnix.ide.data.model.GitFileChange
import com.arnix.ide.data.model.GitState
import com.arnix.ide.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitScreen(
    gitState: GitState,
    onStageFile: (String) -> Unit,
    onUnstageFile: (String) -> Unit,
    onStageAll: () -> Unit,
    onCommit: (String) -> Boolean,
    onSwitchBranch: (String) -> Unit,
    onCreateBranch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var commitMessage by remember { mutableStateOf("") }
    var showBranchMenu by remember { mutableStateOf(false) }
    var showNewBranchDialog by remember { mutableStateOf(false) }
    var newBranchName by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
            .testTag("git_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = IdeSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GIT & GITHUB INTEGRATION",
                                color = IdeTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = gitState.remoteRepoUrl,
                                color = IdePrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Branch Switcher Chip
                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { showBranchMenu = true },
                                label = {
                                    Text(
                                        text = gitState.currentBranch,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_git_branch),
                                        contentDescription = "Current branch",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IdeSurfaceVariant,
                                    selectedLabelColor = IdeTextPrimary,
                                    selectedLeadingIconColor = IdeSecondary
                                ),
                                modifier = Modifier.testTag("chip_current_branch")
                            )

                            DropdownMenu(
                                expanded = showBranchMenu,
                                onDismissRequest = { showBranchMenu = false },
                                modifier = Modifier.background(IdeSurface)
                            ) {
                                gitState.availableBranches.forEach { branch ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = branch,
                                                color = if (branch == gitState.currentBranch) IdeSecondary else IdeTextPrimary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        },
                                        onClick = {
                                            onSwitchBranch(branch)
                                            showBranchMenu = false
                                        }
                                    )
                                }
                                HorizontalDivider(color = IdeBorder)
                                DropdownMenuItem(
                                    text = { Text("+ New Branch...", color = IdePrimary) },
                                    onClick = {
                                        showBranchMenu = false
                                        showNewBranchDialog = true
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Remote Sync Buttons (Push / Pull)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { toastMessage = "Pulled latest changes from origin/${gitState.currentBranch}" },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("btn_git_pull"),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = IdeTextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pull", fontSize = 12.sp, color = IdeTextPrimary)
                        }

                        Button(
                            onClick = { toastMessage = "Pushed commits to GitHub: ${gitState.githubUser}/ARNIX-IDE" },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("btn_git_push"),
                            colors = ButtonDefaults.buttonColors(containerColor = IdePrimary),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = IdeBackground)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Push", fontSize = 12.sp, color = IdeBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Scrollable Section: Changes & Commit History
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Commit Box
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IdeSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "COMMIT CHANGES",
                                color = IdeTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = commitMessage,
                                onValueChange = { commitMessage = it },
                                placeholder = { Text("Enter commit message (e.g. feature: implement editor)", color = IdeTextMuted, fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_commit_message"),
                                textStyle = LocalTextStyle.current.copy(color = IdeTextPrimary, fontSize = 13.sp),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IdePrimary,
                                    unfocusedBorderColor = IdeBorder
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = onStageAll,
                                    modifier = Modifier.testTag("btn_stage_all")
                                ) {
                                    Text("Stage All (${gitState.unstagedFiles.size})", color = IdePrimary, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (commitMessage.isNotBlank()) {
                                            val ok = onCommit(commitMessage)
                                            if (ok) {
                                                commitMessage = ""
                                                toastMessage = "Committed successfully!"
                                            } else {
                                                toastMessage = "Stage some files before committing."
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IdeSecondary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("btn_commit")
                                ) {
                                    Text("Commit", color = IdeBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Staged Files Section
                if (gitState.stagedFiles.isNotEmpty()) {
                    item {
                        Text(
                            text = "STAGED CHANGES (${gitState.stagedFiles.size})",
                            color = IdeSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    items(gitState.stagedFiles) { change ->
                        GitChangeRow(
                            change = change,
                            onActionClick = { onUnstageFile(change.filePath) },
                            actionLabel = "Unstage"
                        )
                    }
                }

                // Unstaged Files Section
                if (gitState.unstagedFiles.isNotEmpty()) {
                    item {
                        Text(
                            text = "WORKING TREE CHANGES (${gitState.unstagedFiles.size})",
                            color = IdeTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    items(gitState.unstagedFiles) { change ->
                        GitChangeRow(
                            change = change,
                            onActionClick = { onStageFile(change.filePath) },
                            actionLabel = "Stage"
                        )
                    }
                }

                // Commit History
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "COMMIT HISTORY (${gitState.commitHistory.size})",
                        color = IdeTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
                items(gitState.commitHistory) { commit ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IdeSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = commit.message,
                                    color = IdeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${commit.author} | ${commit.timestamp}",
                                    color = IdeTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                color = IdeSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = commit.shortHash,
                                    color = IdePrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // New Branch Dialog
        if (showNewBranchDialog) {
            AlertDialog(
                onDismissRequest = { showNewBranchDialog = false },
                title = { Text("Create New Branch", color = IdeTextPrimary) },
                text = {
                    OutlinedTextField(
                        value = newBranchName,
                        onValueChange = { newBranchName = it },
                        placeholder = { Text("e.g. feature/new-tool") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newBranchName.isNotBlank()) {
                                onCreateBranch(newBranchName.trim())
                                newBranchName = ""
                                showNewBranchDialog = false
                            }
                        }
                    ) {
                        Text("Create & Switch")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewBranchDialog = false }) {
                        Text("Cancel", color = IdeTextSecondary)
                    }
                },
                containerColor = IdeSurface
            )
        }

        // Toast Snackbar
        if (toastMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { toastMessage = null }) {
                        Text("Dismiss", color = IdePrimary)
                    }
                },
                containerColor = IdeSurfaceVariant,
                contentColor = IdeTextPrimary
            ) {
                Text(toastMessage ?: "")
            }
        }
    }
}

@Composable
private fun GitChangeRow(
    change: GitFileChange,
    onActionClick: () -> Unit,
    actionLabel: String
) {
    Surface(
        color = IdeSurface,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
        modifier = Modifier.fillMaxWidth()
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
                val statusText = when (change.status) {
                    GitChangeType.MODIFIED -> "M"
                    GitChangeType.ADDED -> "A"
                    GitChangeType.DELETED -> "D"
                    GitChangeType.UNTRACKED -> "?"
                }
                val statusColor = when (change.status) {
                    GitChangeType.MODIFIED -> IdeTertiary
                    GitChangeType.ADDED -> IdeSecondary
                    GitChangeType.DELETED -> IdeError
                    GitChangeType.UNTRACKED -> IdePrimary
                }

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = change.filePath,
                    color = IdeTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(actionLabel, color = IdePrimary, fontSize = 12.sp)
            }
        }
    }
}
