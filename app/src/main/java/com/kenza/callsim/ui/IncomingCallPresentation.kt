package com.kenza.callsim.ui

/**
 * Mirrors the two native iPhone incoming-call presentations.
 *
 * LOCKED_FULL_SCREEN is used when the device is keyguard-locked when the call
 * arrives. UNLOCKED_BANNER is used when the device is already unlocked/in use.
 */
enum class IncomingCallPresentation {
    LOCKED_FULL_SCREEN,
    UNLOCKED_BANNER,
}
