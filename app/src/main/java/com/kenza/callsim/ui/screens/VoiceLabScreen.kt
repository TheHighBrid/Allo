package com.kenza.callsim.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Developer-facing blind A/B lab for Gemini Live voices.
 *
 * Voice names are hidden until the user rates a pair, so preference is driven by
 * how Kenza actually sounds rather than marketing labels.
 */
data class VoiceLabCandidate(
    val id: String,
    val displayWhenRevealed: String,
)

data class VoiceLabRating(
    val voiceId: String,
    val naturalness: Int,
    val latencyFeel: Int,
    val likeKenza: Int,
)

val DefaultGeminiVoiceCandidates = listOf(
    VoiceLabCandidate("Kore", "Kore"),
    VoiceLabCandidate("Aoede", "Aoede"),
    VoiceLabCandidate("Charon", "Charon"),
    VoiceLabCandidate("Fenrir", "Fenrir"),
    VoiceLabCandidate("Puck", "Puck"),
)

@Composable
fun VoiceLabScreen(
    currentVoiceId: String,
    candidates: List<VoiceLabCandidate> = DefaultGeminiVoiceCandidates,
    onApplyVoice: (String) -> Unit,
    onClose: () -> Unit,
    onSaveRating: (VoiceLabRating) -> Unit = {},
) {
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var naturalness by remember { mutableFloatStateOf(3f) }
    var latencyFeel by remember { mutableFloatStateOf(3f) }
    var likeKenza by remember { mutableFloatStateOf(3f) }

    val candidate = candidates.getOrNull(index % candidates.size.coerceAtLeast(1))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Kenza Voice Lab", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Blind A/B: rate naturalness, turn-taking, and how much the sample sounds like Kenza. " +
                "Names stay hidden until you reveal them.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("Active voice in Settings: $currentVoiceId", style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(8.dp))
        Text(
            if (revealed) "Voice: ${candidate?.displayWhenRevealed ?: "—"}"
            else "Sample ${index + 1} of ${candidates.size} (name hidden)",
            style = MaterialTheme.typography.titleMedium,
        )

        RatingRow("Naturalness", naturalness) { naturalness = it }
        RatingRow("Latency / turn-taking", latencyFeel) { latencyFeel = it }
        RatingRow("Sounds like Kenza", likeKenza) { likeKenza = it }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { revealed = !revealed }) {
                Text(if (revealed) "Hide name" else "Reveal name")
            }
            OutlinedButton(
                onClick = {
                    index = (index + 1) % candidates.size.coerceAtLeast(1)
                    revealed = false
                    naturalness = 3f
                    latencyFeel = 3f
                    likeKenza = 3f
                },
            ) { Text("Next sample") }
        }

        Button(
            onClick = {
                val id = candidate?.id ?: return@Button
                onSaveRating(
                    VoiceLabRating(
                        voiceId = id,
                        naturalness = naturalness.toInt(),
                        latencyFeel = latencyFeel.toInt(),
                        likeKenza = likeKenza.toInt(),
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save rating") }

        Button(
            onClick = { candidate?.id?.let(onApplyVoice) },
            modifier = Modifier.fillMaxWidth(),
            enabled = candidate != null,
        ) { Text("Apply this voice in Settings") }

        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Close")
        }
    }
}

@Composable
private fun RatingRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ${value.toInt()}/5", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 1f..5f,
            steps = 3,
        )
    }
}
