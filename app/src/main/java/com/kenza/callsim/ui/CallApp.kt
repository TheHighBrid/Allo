package com.kenza.callsim.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kenza.callsim.call.CallPhase
import com.kenza.callsim.call.CallViewModel
import com.kenza.callsim.ui.screens.HomeScreen
import com.kenza.callsim.ui.screens.InCallScreen
import com.kenza.callsim.ui.screens.IncomingCallBanner
import com.kenza.callsim.ui.screens.IncomingCallScreen
import com.kenza.callsim.ui.screens.MemoryScreen
import com.kenza.callsim.ui.screens.ScheduleScreen
import com.kenza.callsim.ui.screens.ScriptStudioHubScreen
import com.kenza.callsim.ui.screens.SettingsScreen
import com.kenza.callsim.ui.theme.IOSColors

@Composable
fun CallApp(
    viewModel: CallViewModel,
    incomingCallPresentation: IncomingCallPresentation,
    onSimulateIncoming: () -> Unit,
    onCallFinished: () -> Unit,
    onNeedMicPermission: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var navigation by remember { mutableStateOf(AppNavigationState()) }
    var showConsent by remember { mutableStateOf(!viewModel.isConsentAccepted()) }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage == CallViewModel.NEED_MIC) onNeedMicPermission()
    }

    if (navigation.settingsPresented) {
        SettingsScreen(
            initial = viewModel.currentSettings(),
            voiceId = viewModel.voiceId(),
            onSave = viewModel::saveSettings,
            onBack = { navigation = reduceNavigation(navigation, NavigationIntent.DismissSettings) },
        )
        return
    }

    if (navigation.scriptStudioPresented) {
        ScriptStudioHubScreen(
            onBack = { navigation = reduceNavigation(navigation, NavigationIntent.DismissScriptStudio) },
        )
        return
    }

    AnimatedContent(
        targetState = state.phase,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "callPhase",
    ) { phase ->
        when (phase) {
            CallPhase.IDLE -> TopLevelTabs(
                selectedTab = navigation.selectedTab,
                onSelectTab = { navigation = reduceNavigation(navigation, NavigationIntent.SelectTab(it)) },
                state = state,
                onDigit = viewModel::appendDigit,
                onDelete = viewModel::deleteDigit,
                onCall = viewModel::placeCall,
                onSimulateIncoming = onSimulateIncoming,
                onOpenSettings = { navigation = reduceNavigation(navigation, NavigationIntent.PresentSettings) },
                onOpenScriptStudio = { navigation = reduceNavigation(navigation, NavigationIntent.PresentScriptStudio) },
            )

            CallPhase.INCOMING -> when (incomingCallPresentation) {
                IncomingCallPresentation.LOCKED_FULL_SCREEN -> IncomingCallScreen(
                    state = state,
                    onAccept = viewModel::answerIncoming,
                    onDecline = {
                        viewModel.declineIncoming()
                        onCallFinished()
                    },
                )

                IncomingCallPresentation.UNLOCKED_BANNER -> Box(Modifier.fillMaxSize()) {
                    TopLevelTabs(
                        selectedTab = navigation.selectedTab,
                        onSelectTab = {},
                        state = state,
                        onDigit = {},
                        onDelete = {},
                        onCall = {},
                        onSimulateIncoming = {},
                        onOpenSettings = {},
                        onOpenScriptStudio = {},
                        interactive = false,
                    )
                    IncomingCallBanner(
                        state = state,
                        onAccept = viewModel::answerIncoming,
                        onDecline = viewModel::declineIncoming,
                    )
                }
            }

            else -> InCallScreen(
                state = state,
                onToggleMute = viewModel::toggleMute,
                onToggleSpeaker = viewModel::toggleSpeaker,
                onToggleKeypad = viewModel::toggleKeypad,
                onKeypadKey = viewModel::pressKeypadKey,
                onEndCall = {
                    viewModel.endCall()
                    onCallFinished()
                },
            )
        }
    }

    if (showConsent) {
        ConsentDialog(
            onAccept = {
                viewModel.acceptConsent()
                showConsent = false
            },
        )
    }

    val err = state.errorMessage
    val isRealError = err != null &&
        err != CallViewModel.NEED_MIC &&
        err != CallViewModel.DEMO_NOTICE
    if (isRealError && (state.phase == CallPhase.IDLE || state.phase == CallPhase.ENDED)) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } },
            title = { Text("Call could not connect") },
            text = { Text(err ?: "", color = Color.White) },
        )
    }
}

@Composable
private fun TopLevelTabs(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    state: com.kenza.callsim.call.CallUiState,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onSimulateIncoming: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenScriptStudio: () -> Unit,
    interactive: Boolean = true,
) {
    Column(Modifier.fillMaxSize().background(IOSColors.GroupedBackground)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "topLevelTab",
            ) { tab ->
                when (tab) {
                    AppTab.CALL -> HomeScreen(
                        state = state,
                        onDigit = onDigit,
                        onDelete = onDelete,
                        onCall = onCall,
                        onSimulateIncoming = onSimulateIncoming,
                        onOpenSettings = onOpenSettings,
                        onOpenSchedule = { onSelectTab(AppTab.SCHEDULE) },
                        onOpenMemory = { onSelectTab(AppTab.MEMORY) },
                        onOpenScriptStudio = onOpenScriptStudio,
                        modifier = Modifier,
                    )
                    AppTab.SCHEDULE -> ScheduleScreen()
                    AppTab.MEMORY -> MemoryScreen()
                }
            }
        }
        IOSBottomBar(selectedTab = selectedTab, onSelectTab = onSelectTab, enabled = interactive)
    }
}

@Composable
private fun IOSBottomBar(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    enabled: Boolean,
) {
    val items = listOf(
        AppTab.CALL to ("Call" to Icons.Filled.Phone),
        AppTab.SCHEDULE to ("Schedule" to Icons.Filled.CalendarMonth),
        AppTab.MEMORY to ("Memory" to Icons.Filled.Memory),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IOSColors.SecondaryBackground.copy(alpha = 0.97f))
            .navigationBarsPadding()
            .height(72.dp)
            .padding(horizontal = 30.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (tab, descriptor) ->
            val selected = tab == selectedTab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = enabled, role = Role.Button, onClick = { onSelectTab(tab) })
                    .semantics {
                        role = Role.Button
                        contentDescription = descriptor.first
                    }
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = descriptor.second,
                    contentDescription = null,
                    tint = if (selected) IOSColors.Blue else IOSColors.TertiaryLabel,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = descriptor.first,
                    color = if (selected) IOSColors.Blue else IOSColors.TertiaryLabel,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun ConsentDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* must accept to continue */ },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("I understand & agree") }
        },
        title = { Text("Before you start") },
        text = {
            Text(
                "This app simulates a phone call with an AI voice. Only use a cloned " +
                    "voice with the clear consent of the person it belongs to, and never " +
                    "to deceive or impersonate them to others. By continuing you confirm " +
                    "you have permission to use this voice for personal use.",
                color = Color.White,
            )
        },
    )
}
