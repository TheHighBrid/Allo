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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.ui.theme.IOSColors

@Composable
fun SettingsScreen(
    initialAgentId: String,
    initialApiKey: String,
    initialContactName: String,
    voiceId: String,
    onSave: (agentId: String, apiKey: String, contactName: String) -> Unit,
    onBack: () -> Unit,
) {
    var agentId by remember { mutableStateOf(initialAgentId) }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var contactName by remember { mutableStateOf(initialContactName) }

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
            Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))

        Field(
            label = "Agent ID",
            value = agentId,
            onChange = { agentId = it },
            placeholder = "ElevenLabs Conversational AI agent id",
            help = "Required for live voice. Create an agent at elevenlabs.io → Conversational AI, set its voice to Kenza, then paste its ID here."
        )
        Field(
            label = "API key (optional)",
            value = apiKey,
            onChange = { apiKey = it },
            placeholder = "Only for a PRIVATE agent",
            help = "Leave blank if your agent is public. Embedding a key in an installed app is not fully secure — keep the agent public for personal use."
        )
        Field(
            label = "Contact name",
            value = contactName,
            onChange = { contactName = it },
            placeholder = "Kenza",
            help = "Shown on the call screen."
        )

        Spacer(Modifier.height(8.dp))
        Text("Voice ID", color = IOSColors.SecondaryLabel, fontSize = 13.sp)
        Text(voiceId.ifEmpty { "—" }, color = Color.White, fontSize = 15.sp)
        Text(
            "Set inside your agent on ElevenLabs (read-only here).",
            color = IOSColors.SecondaryLabel,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { onSave(agentId, apiKey, contactName); onBack() },
            colors = ButtonDefaults.buttonColors(containerColor = IOSColors.Green),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    help: String,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = IOSColors.SecondaryLabel.copy(alpha = 0.6f)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = IOSColors.Blue,
                unfocusedBorderColor = Color(0xFF3A3A3C),
                cursorColor = IOSColors.Blue,
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(help, color = IOSColors.SecondaryLabel, fontSize = 12.sp)
    }
}
