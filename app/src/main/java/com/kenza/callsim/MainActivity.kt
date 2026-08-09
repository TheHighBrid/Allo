package com.kenza.callsim

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kenza.callsim.call.CallViewModel
import com.kenza.callsim.schedule.IncomingCallNotifier
import com.kenza.callsim.ui.CallApp
import com.kenza.callsim.ui.IncomingCallPresentation
import com.kenza.callsim.ui.theme.KenzaCallTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels()
    private val incomingCallPresentation = mutableStateOf(IncomingCallPresentation.UNLOCKED_BANNER)

    private val requestMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onMicPermissionResult(granted)
        }

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        volumeControlStream = android.media.AudioManager.STREAM_VOICE_CALL

        // Ask for notification permission up front so scheduled calls can ring.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Resolve lock state before drawing the incoming UI. This prevents an
        // unlocked-style banner from flashing before a scheduled locked call.
        handleIncomingIntent(intent)

        setContent {
            KenzaCallTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    CallApp(
                        viewModel = viewModel,
                        incomingCallPresentation = incomingCallPresentation.value,
                        onSimulateIncoming = {
                            incomingCallPresentation.value = IncomingCallPresentation.UNLOCKED_BANNER
                            viewModel.simulateIncomingCall()
                        },
                        onCallFinished = ::releaseLockScreenPresentation,
                        onNeedMicPermission = { requestMic.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * A fired scheduled call chooses its iPhone-style presentation from the
     * actual Android keyguard state at the instant the call arrives.
     *
     * Locked device: full-screen incoming call + slide to answer.
     * Unlocked device: compact incoming-call banner + tap controls.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_INCOMING_CALL, false) != true) return

        val locked = isDeviceLocked()
        incomingCallPresentation.value = if (locked) {
            IncomingCallPresentation.LOCKED_FULL_SCREEN
        } else {
            IncomingCallPresentation.UNLOCKED_BANNER
        }

        if (locked) showWhenLockedAndTurnScreenOn()
        IncomingCallNotifier.cancel(this)
        viewModel.onScheduledIncomingCall(intent.getStringExtra(EXTRA_INITIATIVE_TOKEN))
    }

    private fun isDeviceLocked(): Boolean =
        (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true

    /**
     * Show the call over Android's keyguard without dismissing it. The previous
     * implementation requested keyguard dismissal, which erased the very
     * locked/unlocked distinction needed to mirror native iPhone behavior.
     */
    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    /** Return to the real lock screen after a locked incoming call is dismissed. */
    private fun releaseLockScreenPresentation() {
        if (incomingCallPresentation.value != IncomingCallPresentation.LOCKED_FULL_SCREEN) return

        incomingCallPresentation.value = IncomingCallPresentation.UNLOCKED_BANNER

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        } else {
            @Suppress("DEPRECATION")
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        if (isDeviceLocked()) moveTaskToBack(true)
    }

    companion object {
        const val EXTRA_INCOMING_CALL = "extra_incoming_call"
        const val EXTRA_INITIATIVE_TOKEN = "extra_initiative_token"
    }
}
