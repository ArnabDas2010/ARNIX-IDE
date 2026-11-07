package com.arnix.ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arnix.ide.R
import com.arnix.ide.data.model.CodeTab
import com.arnix.ide.ui.theme.*

@Composable
fun EditorTopBar(
    tabs: List<CodeTab>,
    activeTabId: String?,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onSave: () -> Unit,
    onRun: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleFind: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_top_bar"),
        color = IdeSurface,
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main App Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(IdePrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "{ }",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Action Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onUndo,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_undo")
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_undo),
                            contentDescription = stringResource(R.string.action_undo),
                            tint = IdeTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onRedo,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_redo")
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_redo),
                            contentDescription = stringResource(R.string.action_redo),
                            tint = IdeTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleFind,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_find")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.action_find),
                            tint = IdeTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onSave,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_save")
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_save),
                            contentDescription = stringResource(R.string.action_save),
                            tint = IdePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = onRun,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_run"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = IdeSecondary,
                            contentColor = IdeBackground
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.action_run),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RUN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Tab Strip (if tabs are open)
            if (tabs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IdeBackground)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab.id == activeTabId
                        val tabBg = if (isSelected) IdeSurface else IdeBackground
                        val tabTextColor = if (isSelected) IdeTextPrimary else IdeTextSecondary

                        Row(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(tabBg)
                                .clickable { onSelectTab(tab.id) }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tab.fileName,
                                color = tabTextColor,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Unsaved changes indicator
                            if (tab.isDirty) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(IdeTertiary)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_close),
                                tint = IdeTextMuted,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onCloseTab(tab.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
