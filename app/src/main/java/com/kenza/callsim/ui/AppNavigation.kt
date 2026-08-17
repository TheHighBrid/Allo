package com.kenza.callsim.ui

enum class AppTab {
    CALL,
    SCHEDULE,
    MEMORY,
}

data class AppNavigationState(
    val selectedTab: AppTab = AppTab.CALL,
    val settingsPresented: Boolean = false,
)

sealed interface NavigationIntent {
    data class SelectTab(val tab: AppTab) : NavigationIntent
    data object PresentSettings : NavigationIntent
    data object DismissSettings : NavigationIntent
}

fun reduceNavigation(
    state: AppNavigationState,
    intent: NavigationIntent,
): AppNavigationState = when (intent) {
    is NavigationIntent.SelectTab -> state.copy(
        selectedTab = intent.tab,
        settingsPresented = false,
    )
    NavigationIntent.PresentSettings -> state.copy(settingsPresented = true)
    NavigationIntent.DismissSettings -> state.copy(settingsPresented = false)
}
