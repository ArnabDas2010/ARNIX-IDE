package com.arnix.ide.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.arnix.ide.data.model.IdeSettings
import com.arnix.ide.ui.theme.*

@Composable
fun SettingsScreen(
    settings: IdeSettings,
    githubUser: String,
    githubRepo: String,
    onUpdateSettings: (IdeSettings) -> Unit,
    onUpdateGithub: (repo: String, user: String, pat: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var userField by remember { mutableStateOf(githubUser) }
    var repoField by remember { mutableStateOf(githubRepo) }
    var patField by remember { mutableStateOf("") }
    var savedFeedback by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Surface(
                color = IdeSurface,
                border = BorderStroke(1.dp, IdeBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 800.dp)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "IDE SETTINGS & PREFERENCES",
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Customize your mobile coding environment & repository sync",
                            color = IdeTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 800.dp)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section: Code Editor Preferences
                item {
                    Text(
                        text = "EDITOR PREFERENCES",
                        color = IdePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IdeSurface),
                        border = BorderStroke(1.dp, IdeBorderSubtle),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Font Size Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Editor Font Size", color = IdeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Surface(
                                    color = IdeSurfaceVariant,
                                    border = BorderStroke(1.dp, IdeBorderSubtle),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "${settings.fontSizeSp.toInt()} sp",
                                        color = IdeSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = settings.fontSizeSp,
                                onValueChange = { onUpdateSettings(settings.copy(fontSizeSp = it)) },
                                valueRange = 10f..22f,
                                steps = 11,
                                modifier = Modifier.testTag("slider_font_size"),
                                colors = SliderDefaults.colors(
                                    thumbColor = IdePrimary,
                                    activeTrackColor = IdePrimary,
                                    inactiveTrackColor = IdeSurfaceVariant
                                )
                            )

                            HorizontalDivider(color = IdeBorderSubtle, modifier = Modifier.padding(vertical = 10.dp))

                            // Show Line Numbers Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Line Numbers", color = IdeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Show code gutter numbers on the left", color = IdeTextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = settings.showLineNumbers,
                                    onCheckedChange = { onUpdateSettings(settings.copy(showLineNumbers = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = IdeBackground,
                                        checkedTrackColor = IdeSecondary,
                                        uncheckedThumbColor = IdeTextSecondary,
                                        uncheckedTrackColor = IdeSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("switch_line_numbers")
                                )
                            }

                            HorizontalDivider(color = IdeBorderSubtle, modifier = Modifier.padding(vertical = 10.dp))

                            // Word Wrap Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Word Wrap", color = IdeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Wrap long code lines to prevent horizontal scroll", color = IdeTextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = settings.wordWrap,
                                    onCheckedChange = { onUpdateSettings(settings.copy(wordWrap = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = IdeBackground,
                                        checkedTrackColor = IdePrimary,
                                        uncheckedThumbColor = IdeTextSecondary,
                                        uncheckedTrackColor = IdeSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("switch_word_wrap")
                                )
                            }

                            HorizontalDivider(color = IdeBorderSubtle, modifier = Modifier.padding(vertical = 10.dp))

                            // Auto Save Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto Save", color = IdeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Automatically persist file edits to disk", color = IdeTextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = settings.autoSave,
                                    onCheckedChange = { onUpdateSettings(settings.copy(autoSave = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = IdeBackground,
                                        checkedTrackColor = IdeTertiary,
                                        uncheckedThumbColor = IdeTextSecondary,
                                        uncheckedTrackColor = IdeSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("switch_auto_save")
                                )
                            }
                        }
                    }
                }

                // Section: GitHub & Remote Config
                item {
                    Text(
                        text = "GITHUB SYNC & AUTHENTICATION",
                        color = IdePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IdeSurface),
                        border = BorderStroke(1.dp, IdeBorderSubtle),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("GitHub Username", color = IdeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = userField,
                                onValueChange = { userField = it },
                                modifier = Modifier.fillMaxWidth().testTag("input_github_user"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(color = IdeTextPrimary, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IdePrimary,
                                    unfocusedBorderColor = IdeBorderSubtle,
                                    focusedContainerColor = IdeSurfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = IdeSurfaceVariant.copy(alpha = 0.2f)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Repository URL", color = IdeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = repoField,
                                onValueChange = { repoField = it },
                                modifier = Modifier.fillMaxWidth().testTag("input_github_repo"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(color = IdeTextPrimary, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IdePrimary,
                                    unfocusedBorderColor = IdeBorderSubtle,
                                    focusedContainerColor = IdeSurfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = IdeSurfaceVariant.copy(alpha = 0.2f)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Personal Access Token (PAT)", color = IdeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = patField,
                                onValueChange = { patField = it },
                                placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx", color = IdeTextMuted, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("input_github_pat"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = LocalTextStyle.current.copy(color = IdeTextPrimary, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IdePrimary,
                                    unfocusedBorderColor = IdeBorderSubtle,
                                    focusedContainerColor = IdeSurfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = IdeSurfaceVariant.copy(alpha = 0.2f)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    onUpdateGithub(repoField, userField, patField)
                                    savedFeedback = true
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).testTag("btn_save_github"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IdePrimary)
                            ) {
                                Text(
                                    if (savedFeedback) "Saved & Connected" else "Save GitHub Configuration",
                                    color = IdeBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Section: App Metadata
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IdeSurfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, IdeBorderSubtle),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = IdePrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "CORE",
                                        color = IdePrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text("ARNIX IDE v1.0.0", color = IdeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Package: com.arnix.ide", color = IdeTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            Text("Engine: Jetpack Compose + Material 3", color = IdeTextSecondary, fontSize = 11.sp)
                            Text("Environment: Android 36 (Java 21 / Kotlin 2.2.10)", color = IdeTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
