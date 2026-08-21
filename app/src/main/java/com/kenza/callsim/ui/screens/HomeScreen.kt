package com.kenza.callsim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kenza.callsim.call.CallUiState
import com.kenza.callsim.ui.components.Keypad
import com.kenza.callsim.ui.components.RoundControl
import com.kenza.callsim.ui.theme.IOSColors

@Composable
fun HomeScreen(
    state: CallUiState,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onSimulateIncoming: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenScriptStudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(IOSColors.CallScreenTop, IOSColors.GroupedBackground)
                )
            )
    ) {
        val isCompactHeight = maxHeight < 760.dp
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .let { baseModifier ->
                if (isCompactHeight) {
                    baseModifier.verticalScroll(rememberScrollState())
                } else {
                    baseModifier
                }
            }

        Column(
            modifier = contentModifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(54.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Allo",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeQuickAction(Icons.Filled.Memory, "Memory", onOpenMemory)
                    HomeQuickAction(Icons.Filled.CalendarMonth, "Schedule", onOpenSchedule)
                    HomeQuickAction(Icons.Filled.Settings, "Settings", onOpenSettings)
                }
            }
            Spacer(Modifier.height(18.dp))

            ConnectionStatus(isConfigured = state.isConfigured)

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(IOSColors.SecondaryBackground)
                    .clickable(role = Role.Button, onClick = onOpenScriptStudio)
                    .semantics { contentDescription = "Open Script Studio" }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = IOSColors.Blue,
                    modifier = Modifier.size(22.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text("Script Studio", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Create and edit a one-sided call script",
                        color = IOSColors.SecondaryLabel,
                        fontSize = 12.sp,
                    )
                }
                Text("Open", color = IOSColors.Blue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.dialedNumber.ifEmpty { state.contactName },
                    color = Color.White,
                    fontSize = if (state.dialedNumber.isEmpty()) 32.sp else 40.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!state.isConfigured) {
                Text(
                    text = "Calls use a guided demo until a voice provider is connected in Settings.",
                    color = IOSColors.SecondaryLabel,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            Keypad(
                onKey = onDigit,
                keyBackground = IOSColors.KeypadBackground,

                modifier = Modifier.widthIn(max = 360.dp)
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(Modifier.size(64.dp))

                RoundControl(onClick = onCall, background = IOSColors.Green, size = 72) {
                    Icon(
                        Icons.Filled.Call,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable(enabled = state.dialedNumber.isNotEmpty(), onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.dialedNumber.isNotEmpty()) {
                        Icon(
                            Icons.Filled.Backspace,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(
                if (isCompactHeight) {
                    Modifier.height(1.dp)
                } else {
                    Modifier.height(1.dp).weight(1f)
                }
            )

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(IOSColors.SecondaryBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSimulateIncoming
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PhoneCallback,
                    contentDescription = null,
                    tint = IOSColors.Green,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Simulate incoming call", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun HomeQuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(IOSColors.SecondaryBackground)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun ConnectionStatus(isConfigured: Boolean) {
    val statusColor = if (isConfigured) IOSColors.Green else Color(0xFFFF9F0A)
    val statusText = if (isConfigured) "Live voice ready" else "Demo mode"
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .semantics { contentDescription = statusText },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
        Text(
            text = statusText,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
