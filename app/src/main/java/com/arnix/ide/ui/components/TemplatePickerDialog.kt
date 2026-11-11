package com.arnix.ide.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arnix.ide.R
import com.arnix.ide.data.repository.ProjectTemplate
import com.arnix.ide.data.repository.TemplateProjects
import com.arnix.ide.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TemplatePickerDialog(
    targetFolder: File,
    onDismiss: () -> Unit,
    onProjectCreated: (createdFolder: File, primaryFile: File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTemplate by remember { mutableStateOf<ProjectTemplate?>(TemplateProjects.templates.firstOrNull()) }
    var projectNameInput by remember { mutableStateOf("MyWebApp") }
    var isCreating by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTemplate) {
        selectedTemplate?.let { tmpl ->
            projectNameInput = when (tmpl.id) {
                "web_starter" -> "MyWebApp"
                "kotlin_starter" -> "MyKotlinApp"
                "python_starter" -> "MyPythonTool"
                "docs_starter" -> "MyDocumentation"
                else -> "MyProject"
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isCreating,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(14.dp))
                .testTag("template_picker_dialog"),
            color = IdeSurface,
            border = BorderStroke(1.dp, IdeBorderSubtle)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IdeSecondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = IdeSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Project Starter Templates",
                                color = IdeTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Generate complete ready-to-run projects",
                                color = IdeTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isCreating,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = IdeTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Templates List
                Text(
                    text = "SELECT TEMPLATE",
                    color = IdeTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TemplateProjects.templates, key = { it.id }) { template ->
                        val isSelected = template.id == selectedTemplate?.id
                        val borderColor = if (isSelected) IdePrimary else IdeBorderSubtle
                        val bgColor = if (isSelected) IdePrimary.copy(alpha = 0.08f) else IdeBackground

                        val catIconRes = when (template.id) {
                            "web_starter" -> R.drawable.ic_file_code
                            "kotlin_starter" -> R.drawable.ic_android
                            "python_starter" -> R.drawable.ic_file_code
                            else -> R.drawable.ic_document
                        }

                        val catColor = when (template.id) {
                            "web_starter" -> IdeSecondary
                            "kotlin_starter" -> IdePrimary
                            "python_starter" -> IdeTertiary
                            else -> IdeAccentViolet
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = bgColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTemplate = template }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(catColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = catIconRes),
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.name,
                                        color = IdeTextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = template.description,
                                        color = IdeTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        template.files.take(3).forEach { (fileName, _) ->
                                            Surface(
                                                color = IdeSurfaceVariant,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = fileName,
                                                    color = IdeTextMuted,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedTemplate = template },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = IdePrimary,
                                        unselectedColor = IdeTextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Project Name Input Field
                OutlinedTextField(
                    value = projectNameInput,
                    onValueChange = { projectNameInput = it.replace(Regex("[^a-zA-Z0-9_-]"), "") },
                    label = { Text("Project / Folder Name") },
                    placeholder = { Text("e.g. MyWebApp") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IdeBackground,
                        unfocusedContainerColor = IdeBackground,
                        focusedTextColor = IdeTextPrimary,
                        unfocusedTextColor = IdeTextPrimary,
                        focusedBorderColor = IdePrimary,
                        unfocusedBorderColor = IdeBorderSubtle,
                        focusedLabelColor = IdePrimary,
                        unfocusedLabelColor = IdeTextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Creating in: ${targetFolder.absolutePath}/$projectNameInput",
                    color = IdeTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isCreating,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, IdeBorderSubtle)
                    ) {
                        Text("Cancel", color = IdeTextSecondary)
                    }

                    Button(
                        onClick = {
                            val template = selectedTemplate ?: return@Button
                            if (projectNameInput.isBlank()) {
                                Toast.makeText(context, "Enter a project name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isCreating = true
                            coroutineScope.launch {
                                val projectFolder = File(targetFolder, projectNameInput)
                                val success = withContext(Dispatchers.IO) {
                                    TemplateProjects.createTemplate(projectFolder, template)
                                }
                                isCreating = false
                                if (success) {
                                    val primaryName = template.files.firstOrNull()?.first ?: "README.md"
                                    val primaryFile = File(projectFolder, primaryName)
                                    Toast.makeText(context, "Project created: ${projectFolder.name}", Toast.LENGTH_SHORT).show()
                                    onProjectCreated(projectFolder, primaryFile)
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Failed to create project template", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isCreating && projectNameInput.isNotBlank(),
                        modifier = Modifier.weight(1.5f).height(44.dp).testTag("btn_create_template_project"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IdeSecondary,
                            contentColor = IdeBackground
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(color = IdeBackground, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(imageVector = Icons.Default.Create, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Project", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
