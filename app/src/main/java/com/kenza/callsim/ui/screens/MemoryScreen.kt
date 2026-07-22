package com.kenza.callsim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.memory.CallSummary
import com.kenza.callsim.memory.MemoryItem
import com.kenza.callsim.memory.MemoryKind
import com.kenza.callsim.memory.MemoryOwner
import com.kenza.callsim.memory.MemoryPolicy
import com.kenza.callsim.memory.MemoryStore
import com.kenza.callsim.memory.PersonalityProfiles
import com.kenza.callsim.ui.theme.IOSColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Reviewable and editable private memory, stored encrypted on this device. */
@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { MemoryStore(context) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val snapshot = remember(refreshKey) { store.snapshot() }
    val initialProfiles = remember { snapshot.profiles }

    var kenzaProfile by remember { mutableStateOf(initialProfiles.kenzaProfile) }
    var listenerProfile by remember { mutableStateOf(initialProfiles.listenerProfile) }
    var relationshipProfile by remember { mutableStateOf(initialProfiles.relationshipProfile) }
    var ambitions by remember { mutableStateOf(initialProfiles.ambitionsAndGoals) }
    var boundaries by remember { mutableStateOf(initialProfiles.boundariesAndContext) }
    var newMemory by remember { mutableStateOf("") }
    var newOwner by remember { mutableStateOf(MemoryOwner.USER) }
    var newKind by remember { mutableStateOf(MemoryKind.FACT) }
    var showClearConfirm by remember { mutableStateOf(false) }

    fun refresh() { refreshKey++ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = IOSColors.Blue,
                modifier = Modifier.size(28.dp).clickable(onClick = onBack)
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text("Kenza Memory", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Encrypted on this device. Raw call transcripts are not kept.",
                    color = IOSColors.SecondaryLabel,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("Personality and relationship profiles")
        Text(
            "These stable profiles are loaded automatically before every outgoing, incoming, or scheduled call.",
            color = IOSColors.SecondaryLabel,
            fontSize = 13.sp,
        )
        ProfileField("About Kenza", kenzaProfile, { kenzaProfile = it }, "Personality, background, habits, communication style")
        ProfileField("About Mohamed", listenerProfile, { listenerProfile = it }, "Work, interests, preferences, current priorities")
        ProfileField("Relationship and shared history", relationshipProfile, { relationshipProfile = it }, "Important history, rituals, inside jokes, meaningful dates")
        ProfileField("Kenza's ambitions and future goals", ambitions, { ambitions = it }, "Career, family, personal growth, travel, long-term plans")
        ProfileField("Boundaries and important context", boundaries, { boundaries = it }, "Consent, sensitive topics, facts that must not be invented")
        Button(
            onClick = {
                store.saveProfiles(
                    PersonalityProfiles(
                        kenzaProfile = kenzaProfile,
                        listenerProfile = listenerProfile,
                        relationshipProfile = relationshipProfile,
                        ambitionsAndGoals = ambitions,
                        boundariesAndContext = boundaries,
                    )
                )
                refresh()
            },
            colors = ButtonDefaults.buttonColors(containerColor = IOSColors.Green),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("Save profiles", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(30.dp))
        SectionTitle("Add an important memory")
        OutlinedTextField(
            value = newMemory,
            onValueChange = { newMemory = it },
            placeholder = { Text("Something Kenza should remember", color = IOSColors.SecondaryLabel) },
            minLines = 2,
            colors = memoryFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text("Belongs to", color = IOSColors.SecondaryLabel, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemoryOwner.entries.forEach { owner ->
                ChoiceChip(
                    label = when (owner) {
                        MemoryOwner.USER -> "Mohamed"
                        MemoryOwner.KENZA -> "Kenza"
                        MemoryOwner.SHARED -> "Shared"
                    },
                    selected = newOwner == owner,
                    modifier = Modifier.weight(1f),
                ) { newOwner = owner }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Type", color = IOSColors.SecondaryLabel, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(MemoryKind.FACT, MemoryKind.PREFERENCE, MemoryKind.PLAN, MemoryKind.RELATIONSHIP).forEach { kind ->
                ChoiceChip(
                    label = kind.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = newKind == kind,
                    modifier = Modifier.weight(1f),
                ) { newKind = kind }
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                if (store.addManualMemory(newMemory, newOwner, newKind) != null) {
                    newMemory = ""
                    refresh()
                }
            },
            enabled = newMemory.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = IOSColors.Blue),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            Text("Remember this", color = Color.White)
        }

        Spacer(Modifier.height(30.dp))
        SectionTitle("Recent calls")
        if (snapshot.calls.isEmpty()) {
            EmptyText("Call summaries will appear here after a real two-way call ends.")
        } else {
            snapshot.calls.sortedByDescending { it.startedAt }.take(12).forEach { call ->
                CallSummaryCard(call = call, onDelete = {
                    store.deleteCall(call.id)
                    refresh()
                })
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("Durable memories")
        val sortedMemories = snapshot.items.sortedByDescending {
            MemoryPolicy.score(it, System.currentTimeMillis())
        }
        if (sortedMemories.isEmpty()) {
            EmptyText("Important facts, preferences, plans and shared moments will collect here.")
        } else {
            sortedMemories.forEach { item ->
                MemoryCard(
                    item = item,
                    onPin = { store.togglePinned(item.id); refresh() },
                    onDone = { store.toggleDone(item.id); refresh() },
                    onDelete = { store.deleteMemory(item.id); refresh() },
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = Color(0xFF2C2C2E))
        TextButton(onClick = { showClearConfirm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Delete all Kenza memory", color = IOSColors.Red)
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Delete all memory?") },
            text = { Text("This permanently removes profiles, summaries, plans and memories from this device.") },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.clearAll()
                    kenzaProfile = ""
                    listenerProfile = ""
                    relationshipProfile = ""
                    ambitions = ""
                    boundaries = ""
                    showClearConfirm = false
                    refresh()
                }) { Text("Delete", color = IOSColors.Red) }
            },
        )
    }
}

@Composable
private fun CallSummaryCard(call: CallSummary, onDelete: () -> Unit) {
    Surface(color = Color(0xFF1C1C1E), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(formatCallDate(call.startedAt), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(formatDuration(call.durationSeconds), color = IOSColors.SecondaryLabel, fontSize = 12.sp)
                }
                TextButton(onClick = onDelete) { Text("Delete", color = IOSColors.Red, fontSize = 12.sp) }
            }
            Text(call.summary, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
            if (call.processing) {
                Spacer(Modifier.height(6.dp))
                Text("Processing summary…", color = IOSColors.Blue, fontSize = 12.sp)
            }
            if (call.mood.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Mood: ${call.mood}", color = IOSColors.SecondaryLabel, fontSize = 12.sp)
            }
            call.highlights.take(4).forEach { Text("• $it", color = IOSColors.SecondaryLabel, fontSize = 12.sp) }
            if (call.unresolvedTopics.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Follow up", color = IOSColors.Blue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                call.unresolvedTopics.take(4).forEach { Text("• $it", color = Color.White, fontSize = 12.sp) }
            }
            call.followUp?.let {
                Spacer(Modifier.height(6.dp))
                Text("Next call: $it", color = IOSColors.Green, fontSize = 12.sp)
            }
            call.processingError?.let {
                Spacer(Modifier.height(6.dp))
                Text("Automatic analysis was limited: $it", color = IOSColors.SecondaryLabel, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MemoryCard(
    item: MemoryItem,
    onPin: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(color = Color(0xFF1C1C1E), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${ownerLabel(item.owner)} • ${item.kind.name.lowercase()}",
                    color = IOSColors.SecondaryLabel,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
                if (item.pinned) Text("PINNED", color = IOSColors.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.text,
                color = if (item.done) IOSColors.SecondaryLabel else Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Importance ${item.importance}/5 • Confidence ${(item.confidence * 100).toInt()}%",
                color = IOSColors.SecondaryLabel,
                fontSize = 11.sp,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onPin) { Text(if (item.pinned) "Unpin" else "Pin", color = IOSColors.Blue) }
                if (item.kind == MemoryKind.PLAN || item.kind == MemoryKind.GOAL) {
                    TextButton(onClick = onDone) { Text(if (item.done) "Reopen" else "Done", color = IOSColors.Green) }
                }
                TextButton(onClick = onDelete) { Text("Delete", color = IOSColors.Red) }
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, onChange: (String) -> Unit, help: String) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(5.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            minLines = 3,
            maxLines = 8,
            colors = memoryFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(help, color = IOSColors.SecondaryLabel, fontSize = 11.sp)
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) IOSColors.Green else Color(0xFF1C1C1E),
        shape = RoundedCornerShape(11.dp),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EmptyText(text: String) {
    Text(text, color = IOSColors.SecondaryLabel, fontSize = 13.sp, modifier = Modifier.padding(vertical = 10.dp))
}

@Composable
private fun memoryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = IOSColors.Blue,
    unfocusedBorderColor = Color(0xFF3A3A3C),
    cursorColor = IOSColors.Blue,
)

private fun ownerLabel(owner: MemoryOwner): String = when (owner) {
    MemoryOwner.USER -> "Mohamed"
    MemoryOwner.KENZA -> "Kenza"
    MemoryOwner.SHARED -> "Shared"
}

private fun formatCallDate(timestamp: Long): String = SimpleDateFormat(
    "EEE, MMM d • h:mm a",
    Locale.getDefault(),
).format(Date(timestamp))

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return if (minutes > 0) "$minutes min $remaining sec" else "$remaining sec"
}
