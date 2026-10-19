package com.kenza.callsim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.script.ScriptProjectStore
import com.kenza.callsim.script.ScriptProjectSummary
import com.kenza.callsim.ui.components.IosFooterNote
import com.kenza.callsim.ui.components.IosGroupedCard
import com.kenza.callsim.ui.components.IosNavHeader
import com.kenza.callsim.ui.components.IosSectionCaption
import com.kenza.callsim.ui.theme.IOSColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Library-first Script Studio entry point. The existing editor remains the single editing surface. */
@Composable
fun ScriptStudioHubScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val projects = remember { ScriptProjectStore(context) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var editorOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ScriptProjectSummary?>(null) }

    LaunchedEffect(Unit) {
        projects.migrateLegacyDraftIfNeeded()
        refreshKey++
    }

    if (editorOpen) {
        ScriptStudioScreen(
            onBack = {
                editorOpen = false
                refreshKey++
            },
            modifier = modifier,
        )
        return
    }

    val library = remember(refreshKey) { projects.projects() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IOSColors.GroupedBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        IosNavHeader(
            title = "Script Studio",
            onBack = onBack,
            trailing = {
                TextButton(
                    onClick = {
                        val id = projects.create()
                        projects.activate(id)
                        editorOpen = true
                    },
                ) { Text("New", color = IOSColors.Blue) }
            },
        )
        IosFooterNote(
            "Projects save automatically on this device. Open one to continue exactly where you left it.",
        )

        Button(
            onClick = {
                val id = projects.create()
                projects.activate(id)
                editorOpen = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = IOSColors.Green),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Text("Create new script", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(18.dp))
        IosSectionCaption("Your scripts")
        if (library.isEmpty()) {
            IosGroupedCard {
                Column(Modifier.padding(18.dp)) {
                    Text("No scripts yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Start with a 10 to 45 minute request, then edit and keep the result as a project.",
                        color = IOSColors.SecondaryLabel,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
        } else {
            library.forEach { project ->
                ScriptProjectCard(
                    project = project,
                    onOpen = {
                        if (projects.activate(project.id) != null) editorOpen = true
                    },
                    onRename = { title ->
                        projects.rename(project.id, title)
                        refreshKey++
                    },
                    onDuplicate = {
                        projects.duplicate(project.id)
                        refreshKey++
                    },
                    onDelete = { pendingDelete = project },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(30.dp))
    }

    pendingDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete script?") },
            text = { Text("${project.title} will be permanently removed from this device.") },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        projects.delete(project.id)
                        pendingDelete = null
                        refreshKey++
                    },
                ) { Text("Delete", color = IOSColors.Red) }
            },
        )
    }
}

@Composable
private fun ScriptProjectCard(
    project: ScriptProjectSummary,
    onOpen: () -> Unit,
    onRename: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember(project.id, project.title) { mutableStateOf(project.title) }
    Card(
        colors = CardDefaults.cardColors(containerColor = IOSColors.SecondaryBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Project title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Updated ${formatProjectDate(project.updatedAt)}",
                color = IOSColors.TertiaryLabel,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(onClick = onOpen, modifier = Modifier.weight(1f)) { Text("Open") }
                OutlinedButton(
                    onClick = { onRename(title) },
                    enabled = title.trim() != project.title,
                    modifier = Modifier.weight(1f),
                ) { Text("Rename") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDuplicate) { Text("Duplicate") }
                TextButton(onClick = onDelete) { Text("Delete", color = IOSColors.Red) }
            }
        }
    }
}

private fun formatProjectDate(timestamp: Long): String = SimpleDateFormat(
    "MMM d, h:mm a",
    Locale.getDefault(),
).format(Date(timestamp))
