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
import androidx.compose.ui.res.painterResource
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
    onPreview: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_top_bar"),
        color = IdeSurface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorderSubtle)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main App Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Logo & Title (Flexible & auto-fit)
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(IdePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "{ }",
                            color = IdePrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "ARNIX IDE",
                        color = IdeTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        color = IdeAccentViolet.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "v1.0",
                            color = IdeAccentViolet,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Action Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    IconButton(
                        onClick = onUndo,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_undo")
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_undo),
                            contentDescription = stringResource(R.string.action_undo),
                            tint = IdeTextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onRedo,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_redo")
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_redo),
                            contentDescription = stringResource(R.string.action_redo),
                            tint = IdeTextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleFind,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_find")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.action_find),
                            tint = IdeTextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onSave,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_save")
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_save),
                            contentDescription = stringResource(R.string.action_save),
                            tint = IdePrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    val activeTab = tabs.find { it.id == activeTabId }
                    val isPreviewable = activeTab?.fileName?.let {
                        it.endsWith(".html", ignoreCase = true) ||
                        it.endsWith(".htm", ignoreCase = true) ||
                        it.endsWith(".md", ignoreCase = true) ||
                        it.endsWith(".markdown", ignoreCase = true)
                    } == true

                    if (isPreviewable && onPreview != null) {
                        Button(
                            onClick = onPreview,
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("btn_preview"),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IdeAccentViolet,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_document),
                                contentDescription = "Preview",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "PREVIEW",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    Button(
                        onClick = {
                            if (isPreviewable && onPreview != null) {
                                onPreview()
                            } else {
                                onRun()
                            }
                        },
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_run"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IdeSecondary,
                            contentColor = IdeBackground
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.action_run),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isPreviewable) "RUN" else "RUN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
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
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab.id == activeTabId
                        val tabBg = if (isSelected) IdeSurface else IdeSurfaceVariant.copy(alpha = 0.4f)
                        val tabTextColor = if (isSelected) IdeTextPrimary else IdeTextSecondary
                        val ext = tab.fileName.substringAfterLast(".", "").lowercase()

                        val tabIconRes = when (ext) {
                            "kt", "java" -> R.drawable.ic_file_code
                            "md", "txt" -> R.drawable.ic_document
                            "zip", "apk" -> R.drawable.ic_archive
                            else -> R.drawable.ic_file_code
                        }

                        val tabIconColor = when (ext) {
                            "kt", "java" -> IdePrimary
                            "py" -> IdeSecondary
                            "json", "xml" -> IdeTertiary
                            "md" -> IdeAccentViolet
                            else -> IdeTextMuted
                        }

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(tabBg)
                                .clickable { onSelectTab(tab.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 32.dp, max = 38.dp)
                                    .widthIn(min = 70.dp, max = 220.dp)
                                    .padding(start = 10.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = tabIconRes),
                                    contentDescription = null,
                                    tint = tabIconColor,
                                    modifier = Modifier.size(14.dp)
                                )

                                Text(
                                    text = tab.fileName,
                                    color = tabTextColor,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                if (tab.isDirty) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(IdeTertiary)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { onCloseTab(tab.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.action_close),
                                        tint = if (isSelected) IdeTextSecondary else IdeTextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Active tab indicator underline
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (isSelected) IdePrimary else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    }
}
