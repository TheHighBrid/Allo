package com.kenza.callsim.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.config.ConfigRepository
import com.kenza.callsim.script.DemoScriptGenerator
import com.kenza.callsim.script.GeminiScriptGenerator
import com.kenza.callsim.script.ScriptMode
import com.kenza.callsim.script.ScriptStudioDraft
import com.kenza.callsim.script.ScriptStudioDraftStore
import com.kenza.callsim.script.ScriptStudioEditorState
import com.kenza.callsim.script.ScriptGenerationSource
import com.kenza.callsim.script.ScriptGenerationUiState
import com.kenza.callsim.script.ScriptTextExport
import com.kenza.callsim.ui.theme.IOSColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * An editable local Script Studio workspace. It intentionally starts with the
 * deterministic demo provider so people can explore the editor without a key,
 * network connection, or paid request.
 */
@Composable
fun ScriptStudioScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val draftStore = remember { ScriptStudioDraftStore(context) }
    val restoredDraft = remember(draftStore) { draftStore.load() }
    val clipboard = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    var requestedMinutesText by remember { mutableStateOf(restoredDraft?.requestedMinutes?.toString() ?: "10") }
    var mode by remember { mutableStateOf(restoredDraft?.mode ?: ScriptMode.CASUAL_DAILY) }
    var language by remember { mutableStateOf(restoredDraft?.language ?: "English") }
    var callReason by remember { mutableStateOf(restoredDraft?.callReason.orEmpty()) }
    var scriptText by remember { mutableStateOf(restoredDraft?.scriptText.orEmpty()) }
    var generatedTitle by remember { mutableStateOf(restoredDraft?.generatedTitle) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var generationState by remember { mutableStateOf(ScriptGenerationUiState.idle()) }
    var generationJob by remember { mutableStateOf<Job?>(null) }
    val isGenerating = generationState.isGenerating
    val scope = rememberCoroutineScope()

    val editor = ScriptStudioEditorState(
        requestedMinutes = requestedMinutesText.toIntOrNull() ?: 0,
        mode = mode,
        language = language,
        callReason = callReason,
        scriptText = scriptText,
    )
    val duration = editor.duration
    val warnings = editor.warnings

    LaunchedEffect(requestedMinutesText, mode, language, callReason, scriptText, generatedTitle) {
        requestedMinutesText.toIntOrNull()?.let { requestedMinutes ->
            draftStore.save(
                ScriptStudioDraft(
                    requestedMinutes = requestedMinutes,
                    mode = mode,
                    language = language,
                    callReason = callReason,
                    scriptText = scriptText,
                    generatedTitle = generatedTitle,
                ),
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IOSColors.GroupedBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                text = "Script Studio",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Spacer(Modifier.height(1.dp))
        }

        Text(
            text = "Create Kenza's audible side of a call, then refine every line before sharing or rendering.",
            color = IOSColors.SecondaryLabel,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Drafts save automatically on this device.",
            color = IOSColors.TertiaryLabel,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(16.dp))

        StudioSection(title = "Call setup") {
            OutlinedTextField(
                value = requestedMinutesText,
                onValueChange = { requestedMinutesText = it.filter(Char::isDigit) },
                label = { Text("Duration in minutes") },
                supportingText = { Text("Choose between 10 and 45 minutes.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("Call mode", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScriptMode.entries.forEach { candidate ->
                    FilterChip(
                        selected = candidate == mode,
                        onClick = { mode = candidate },
                        label = { Text(candidate.displayName()) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text("Language") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = callReason,
                onValueChange = { callReason = it },
                label = {
                    Text(
                        if (mode == ScriptMode.CUSTOM) "Scenario or call reason (required)"
                        else "Scenario or call reason (optional)",
                    )
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val request = editor.toRequest()
                    val validation = com.kenza.callsim.script.ScriptRequestValidator.validate(request)
                    if (!validation.isValid) {
                        errorMessage = validation.errors.first()
                        return@Button
                    }
                    generationJob = scope.launch {
                        generationState = generationState.start(ScriptGenerationSource.DEMO)
                        errorMessage = null
                        try {
                            DemoScriptGenerator().generate(request)
                                .onSuccess { generated ->
                                    generatedTitle = generated.title
                                    scriptText = generated.ttsText
                                }
                                .onFailure { error -> errorMessage = error.message ?: "Could not create the demo script." }
                        } catch (_: CancellationException) {
                            errorMessage = "Generation cancelled. Your draft is safe."
                        } finally {
                            generationState = generationState.finish()
                            generationJob = null
                        }
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (generationState.activeSource == ScriptGenerationSource.DEMO) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(18.dp),
                    )
                    Spacer(Modifier.height(1.dp))
                }
                Text(generationState.demoButtonLabel)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val request = editor.toRequest()
                    val validation = com.kenza.callsim.script.ScriptRequestValidator.validate(request)
                    if (!validation.isValid) {
                        errorMessage = validation.errors.first()
                        return@OutlinedButton
                    }
                    val apiKey = ConfigRepository(context).geminiApiKey
                    generationJob = scope.launch {
                        generationState = generationState.start(ScriptGenerationSource.GEMINI)
                        errorMessage = null
                        try {
                            GeminiScriptGenerator(apiKey = apiKey).generate(request)
                                .onSuccess { generated ->
                                    generatedTitle = generated.title
                                    scriptText = generated.ttsText
                                }
                                .onFailure { error ->
                                    errorMessage = error.message ?: "Could not create a Gemini script."
                                }
                        } catch (_: CancellationException) {
                            errorMessage = "Generation cancelled. Your draft is safe."
                        } finally {
                            generationState = generationState.finish()
                            generationJob = null
                        }
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (generationState.activeSource == ScriptGenerationSource.GEMINI) {
                    CircularProgressIndicator(
                        color = IOSColors.Label,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(18.dp),
                    )
                    Spacer(Modifier.height(1.dp))
                }
                Text(generationState.geminiButtonLabel)
            }
            if (isGenerating) {
                TextButton(
                    onClick = { generationJob?.cancel() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Cancel generation") }
            }
            Text(
                text = "Uses the Gemini key configured in Settings. The key is never saved in the draft.",
                color = IOSColors.TertiaryLabel,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
            )
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color(0xFFFF6961),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        StudioSection(title = generatedTitle ?: "Editable script") {
            Text(
                text = if (scriptText.isBlank()) {
                    "Create a no-network demo to begin editing. You can replace all text once it appears."
                } else {
                    "Demo output — edit freely. Duration and warnings update as you type."
                },
                color = IOSColors.SecondaryLabel,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = scriptText,
                onValueChange = { scriptText = it },
                label = { Text("Kenza's audible side") },
                placeholder = { Text("Your editable script will appear here.") },
                minLines = 12,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Estimated Kenza speaking time: ${duration.speechSeconds.asClock()} · total call time: ${duration.totalSeconds.asClock()}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
            if (warnings.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                warnings.forEach { warning ->
                    Text(
                        text = "Review: ${warning.message}",
                        color = Color(0xFFFFC107),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (scriptText.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val cleanText = ScriptTextExport.clean(scriptText)
                            if (cleanText.isBlank()) {
                                errorMessage = "There is no spoken text to copy."
                            } else {
                                clipboard.setPrimaryClip(ClipData.newPlainText("Kenza script", cleanText))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Copy clean text") }
                    OutlinedButton(
                        onClick = {
                            val cleanText = ScriptTextExport.clean(scriptText)
                            if (cleanText.isBlank()) {
                                errorMessage = "There is no spoken text to export."
                            } else {
                                val shareIntent = Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, cleanText)
                                context.startActivity(Intent.createChooser(shareIntent, "Export Script Studio text"))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Export text") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = IOSColors.SecondaryBackground),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "Synthetic voice reminder: only use a real person's voice or identity with their clear, informed consent. This editor does not render or share audio.",
                color = IOSColors.SecondaryLabel,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(14.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StudioSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = IOSColors.SecondaryBackground),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

private fun ScriptMode.displayName(): String = name
    .lowercase()
    .split('_')
    .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }

private fun Int.asClock(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%d:%02d".format(minutes, seconds)
}
