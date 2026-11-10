package com.arnix.ide.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Responsive Centered Header
            Surface(
                color = IdeSurface,
                border = BorderStroke(1.dp, IdeBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 840.dp)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "GIT & GITHUB",
                                    color = IdeTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = gitState.remoteRepoUrl,
                                    color = IdePrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
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
                                            fontSize = 11.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_git_branch),
                                            contentDescription = "Current branch",
                                            modifier = Modifier.size(15.dp)
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
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, IdeBorderSubtle),
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
                                    HorizontalDivider(color = IdeBorderSubtle)
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Remote Sync Buttons (Push / Pull)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { toastMessage = "Pulled latest changes from origin/${gitState.currentBranch}" },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 34.dp, max = 40.dp)
                                    .testTag("btn_git_pull"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, IdeBorderSubtle),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(15.dp), tint = IdeTextPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pull", fontSize = 12.sp, color = IdeTextPrimary)
                            }

                            Button(
                                onClick = { toastMessage = "Pushed commits to GitHub: ${gitState.githubUser}/ARNIX-IDE" },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 34.dp, max = 40.dp)
                                    .testTag("btn_git_push"),
                                colors = ButtonDefaults.buttonColors(containerColor = IdePrimary, contentColor = IdeBackground),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(15.dp), tint = IdeBackground)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Push", fontSize = 12.sp, color = IdeBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Scrollable Section: Changes & Commit History (Centered max 840dp for wide screens)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Commit Box
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IdeSurface),
                        border = BorderStroke(1.dp, IdeBorderSubtle),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = IdePrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "COMMIT",
                                        color = IdePrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "RECORD WORKING TREE STATE",
                                    color = IdeTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = commitMessage,
                                onValueChange = { commitMessage = it },
                                placeholder = { Text("e.g. feat: modernize theme & responsive controls", color = IdeTextMuted, fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_commit_message"),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(color = IdeTextPrimary, fontSize = 13.sp),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IdePrimary,
                                    unfocusedBorderColor = IdeBorderSubtle,
                                    focusedContainerColor = IdeSurfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = IdeSurfaceVariant.copy(alpha = 0.2f)
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = onStageAll,
                                    modifier = Modifier.testTag("btn_stage_all")
                                ) {
                                    Text("Stage All (${gitState.unstagedFiles.size})", color = IdePrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
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
                        border = BorderStroke(1.dp, IdeBorderSubtle),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${commit.author} • ${commit.timestamp}",
                                    color = IdeTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Surface(
                                color = IdeSurfaceVariant,
                                border = BorderStroke(1.dp, IdeBorderSubtle),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = commit.shortHash,
                                    color = IdePrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // New Branch Dialog (Responsive scrollable)
        if (showNewBranchDialog) {
            AlertDialog(
                onDismissRequest = { showNewBranchDialog = false },
                title = { Text("Create New Branch", color = IdeTextPrimary) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = newBranchName,
                            onValueChange = { newBranchName = it },
                            placeholder = { Text("e.g. feature/new-tool") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, IdeBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
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

            OutlinedButton(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, IdeBorderSubtle),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.heightIn(min = 28.dp)
            ) {
                Text(actionLabel, color = IdePrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
