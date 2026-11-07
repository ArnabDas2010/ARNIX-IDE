package com.arnix.ide.ui.screens

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
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                color = IdeSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "IDE SETTINGS & PREFERENCES",
                        color = IdeTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Customize your mobile coding environment",
                        color = IdeTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Code Editor Preferences
                item {
                    Text(
                        text = "EDITOR PREFERENCES",
                        color = IdePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IdeSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Font Size Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Font Size: ${settings.fontSizeSp.toInt()} sp", color = IdeTextPrimary, fontSize = 13.sp)
                                Text("${settings.fontSizeSp.toInt()}", color = IdeSecondary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            }
                            Slider(
                                value = settings.fontSizeSp,
                                onValueChange = { onUpdateSettings(settings.copy(fontSizeSp = it)) },
                                valueRange = 10f..22f,
                                steps = 11,
                                modifier = Modifier.testTag("slider_font_size"),
                                colors = SliderDefaults.colors(
                                    thumbColor = IdePrimary,
                                    activeTrackColor = IdePrimary
                                )
                            )

                            HorizontalDivider(color = IdeBorder, modifier = Modifier.padding(vertical = 10.dp))

                            // Show Line Numbers Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Show Line Numbers", color = IdeTextPrimary, fontSize = 13.sp)
                                    Text("Gutter numbering on left margin", color = IdeTextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = settings.showLineNumbers,
                                    onCheckedChange = { onUpdateSettings(settings.copy(showLineNumbers = it)) },
                                    modifier = Modifier.testTag("switch_line_numbers")
                                )
                            }

                            HorizontalDivider(color = IdeBorder, modifier = Modifier.padding(vertical = 10.dp))

                            // Word Wrap Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Word Wrap", color = IdeTextPrimary, fontSize = 13.sp)
                                    Text("Wrap long code lines to fit screen", color = IdeTextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = settings.wordWrap,
                                    onCheckedChange = { onUpdateSettings(settings.copy(wordWrap = it)) },
                                    modifier = Modifier.testTag("switch_word_wrap")
                                )
                            }

                            HorizontalDivider(color = IdeBorder, modifier = Modifier.padding(vertical = 10.dp))

                            // Auto Save Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Auto Save", color = IdeTextPrimary, fontSize = 13.sp)
                                    Text("Persist code changes automatically", color = IdeTextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = settings.autoSave,
                                    onCheckedChange = { onUpdateSettings(settings.copy(autoSave = it)) },
                                    modifier = Modifier.testTag("switch_auto_save")
                                )
                            }
                        }
                    }
                }

                // Section: GitHub & Remote Config
                item {
                    Text(
                        text = "GITHUB INTEGRATION",
                        color = IdePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IdeSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("GitHub Username", color = IdeTextSecondary, fontSize = 11.sp)
                            OutlinedTextField(
                                value = userField,
                                onValueChange = { userField = it },
                                modifier = Modifier.fillMaxWidth().testTag("input_github_user"),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = IdeTextPrimary, fontSize = 13.sp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Repository URL", color = IdeTextSecondary, fontSize = 11.sp)
                            OutlinedTextField(
                                value = repoField,
                                onValueChange = { repoField = it },
                                modifier = Modifier.fillMaxWidth().testTag("input_github_repo"),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = IdeTextPrimary, fontSize = 13.sp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Personal Access Token (PAT)", color = IdeTextSecondary, fontSize = 11.sp)
                            OutlinedTextField(
                                value = patField,
                                onValueChange = { patField = it },
                                placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx", color = IdeTextMuted, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("input_github_pat"),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = IdeTextPrimary, fontSize = 13.sp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    onUpdateGithub(repoField, userField, patField)
                                    savedFeedback = true
                                },
                                modifier = Modifier.fillMaxWidth().testTag("btn_save_github"),
                                colors = ButtonDefaults.buttonColors(containerColor = IdePrimary)
                            ) {
                                Text(if (savedFeedback) "Saved & Connected!" else "Save GitHub Configuration", color = IdeBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section: App Metadata
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IdeSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("ARNIX IDE v1.0.0", color = IdeTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
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
