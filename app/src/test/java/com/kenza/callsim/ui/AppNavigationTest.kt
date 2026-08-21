package com.kenza.callsim.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationTest {
    @Test
    fun selectingScheduleChangesTheTopLevelTab() {
        val initial = AppNavigationState()

        val next = reduceNavigation(initial, NavigationIntent.SelectTab(AppTab.SCHEDULE))

        assertEquals(AppTab.SCHEDULE, next.selectedTab)
        assertEquals(false, next.settingsPresented)
    }

    @Test
    fun selectingMemoryDoesNotOpenSettings() {
        val next = reduceNavigation(
            AppNavigationState(settingsPresented = true),
            NavigationIntent.SelectTab(AppTab.MEMORY),
        )

        assertEquals(AppTab.MEMORY, next.selectedTab)
        assertEquals(false, next.settingsPresented)
    }

    @Test
    fun backFromSettingsReturnsToThePreviouslySelectedTab() {
        val next = reduceNavigation(
            AppNavigationState(selectedTab = AppTab.MEMORY, settingsPresented = true),
            NavigationIntent.DismissSettings,
        )

        assertEquals(AppTab.MEMORY, next.selectedTab)
        assertEquals(false, next.settingsPresented)
    }

    @Test
    fun openingAndDismissingScriptStudioPreservesTheHomeTab() {
        val opened = reduceNavigation(
            AppNavigationState(selectedTab = AppTab.CALL, settingsPresented = true),
            NavigationIntent.PresentScriptStudio,
        )
        val dismissed = reduceNavigation(opened, NavigationIntent.DismissScriptStudio)

        assertEquals(AppTab.CALL, opened.selectedTab)
        assertEquals(false, opened.settingsPresented)
        assertEquals(true, opened.scriptStudioPresented)
        assertEquals(AppTab.CALL, dismissed.selectedTab)
        assertEquals(false, dismissed.scriptStudioPresented)
    }
}
