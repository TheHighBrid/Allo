package com.kenza.callsim.voice

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Tracks conversation flow state for improved realism and safer latency management.
 * This provides content-free turn state tracking that helps manage barge-in, interruptions,
 * and conversation pacing without accessing transcript content.
 */
class ConversationFlowTracker(
    private val clockMs: () -> Long = { System.currentTimeMillis() },
) {
    
    /** Current conversation phase from the provider perspective */
    enum class FlowPhase {
        IDLE,           // No active conversation
        USER_SPEAKING,  // User is speaking, provider should be listening
        AGENT_SPEAKING, // Agent is speaking, user may barge-in
        AGENT_THINKING, // Agent is generating (between turns)
        INTERRUPTED,    // Agent was interrupted by user
        WAITING,        // Waiting for user response
    }
    
    private val currentPhase = AtomicReference(FlowPhase.IDLE)
    private val lastPhaseChangeMs = AtomicLong(0L)
    private val turnStartMs = AtomicLong(0L)
    private val lastUserSpeechEndMs = AtomicLong(0L)
    private val lastAgentSpeechEndMs = AtomicLong(0L)
    private val bargeInDetected = AtomicBoolean(false)
    private val consecutiveBargeIns = AtomicLong(0L)
    
    // Timing thresholds (configurable)
    var bargeInSensitivityMs: Long = 300L  // Min gap for barge-in detection
    var turnTimeoutMs: Long = 5000L       // Max time waiting for response
    var interruptionRecoveryMs: Long = 1000L // Time after interruption before normal flow
    
    /** Call when user starts speaking */
    fun onUserSpeechStart() {
        val now = clockMs()
        val previous = currentPhase.getAndSet(FlowPhase.USER_SPEAKING)
        val previousChange = lastPhaseChangeMs.getAndSet(now)
        turnStartMs.set(now)
        
        // Detect barge-in if agent was speaking
        if (previous == FlowPhase.AGENT_SPEAKING) {
            bargeInDetected.set(true)
            consecutiveBargeIns.incrementAndGet()
            Log.d("ConversationFlow", "Barge-in detected at ${(now - previousChange).coerceAtLeast(0)}ms")
        } else {
            consecutiveBargeIns.set(0)
        }
        
        Log.d("ConversationFlow", "User speech started, previous=$previous")
    }
    
    /** Call when user ends speaking */
    fun onUserSpeechEnd() {
        val now = clockMs()
        lastUserSpeechEndMs.set(now)
        
        // If we were in USER_SPEAKING, transition to WAITING for agent response
        if (currentPhase.get() == FlowPhase.USER_SPEAKING) {
            currentPhase.set(FlowPhase.WAITING)
            lastPhaseChangeMs.set(now)
            Log.d("ConversationFlow", "User speech ended, waiting for agent")
        }
    }
    
    /** Call when agent starts speaking */
    fun onAgentSpeechStart() {
        val now = clockMs()
        val previous = currentPhase.getAndSet(FlowPhase.AGENT_SPEAKING)
        if (previous == FlowPhase.AGENT_SPEAKING) return
        lastPhaseChangeMs.set(now)
        turnStartMs.set(now)
        bargeInDetected.set(false)
        
        Log.d("ConversationFlow", "Agent speech started, previous=$previous")
    }
    
    /** Call when agent ends speaking */
    fun onAgentSpeechEnd() {
        val now = clockMs()
        lastAgentSpeechEndMs.set(now)
        
        // Transition to WAITING if not interrupted
        if (currentPhase.get() == FlowPhase.AGENT_SPEAKING && !bargeInDetected.get()) {
            currentPhase.set(FlowPhase.WAITING)
            lastPhaseChangeMs.set(now)
        }
        Log.d("ConversationFlow", "Agent speech ended")
    }
    
    /** Call when agent is interrupted */
    fun onInterrupted() {
        val now = clockMs()
        val previous = currentPhase.getAndSet(FlowPhase.INTERRUPTED)
        lastPhaseChangeMs.set(now)
        
        Log.d("ConversationFlow", "Agent interrupted, previous=$previous")
    }
    
    /** Call when agent starts thinking/generating */
    fun onAgentThinking() {
        val now = clockMs()
        val previous = currentPhase.getAndSet(FlowPhase.AGENT_THINKING)
        lastPhaseChangeMs.set(now)
        
        Log.d("ConversationFlow", "Agent thinking, previous=$previous")
    }
    
    /** Call when turn completes */
    fun onTurnComplete() {
        val now = clockMs()
        currentPhase.set(FlowPhase.WAITING)
        lastPhaseChangeMs.set(now)
        bargeInDetected.set(false)
        
        Log.d("ConversationFlow", "Turn completed")
    }
    
    /** Call to reset conversation state */
    fun reset() {
        currentPhase.set(FlowPhase.IDLE)
        lastPhaseChangeMs.set(0L)
        turnStartMs.set(0L)
        lastUserSpeechEndMs.set(0L)
        lastAgentSpeechEndMs.set(0L)
        bargeInDetected.set(false)
        consecutiveBargeIns.set(0L)
        
        Log.d("ConversationFlow", "Conversation state reset")
    }
    
    /** Get current phase */
    fun getCurrentPhase(): FlowPhase = currentPhase.get()
    
    /** Check if barge-in was detected in current turn */
    fun isBargeInDetected(): Boolean = bargeInDetected.get()
    
    /** Get consecutive barge-in count */
    fun getConsecutiveBargeIns(): Long = consecutiveBargeIns.get()
    
    /** Check if conversation is in a state where barge-in is allowed */
    fun isBargeInAllowed(): Boolean {
        val phase = currentPhase.get()
        return phase == FlowPhase.AGENT_SPEAKING || phase == FlowPhase.AGENT_THINKING
    }
    
    /** Check if we're waiting too long for a response */
    fun isTurnTimeout(): Boolean {
        val phase = currentPhase.get()
        if (phase != FlowPhase.WAITING) return false
        
        val waitTime = clockMs() - lastPhaseChangeMs.get()
        return waitTime > turnTimeoutMs
    }
    
    /** Get time since last user speech ended */
    fun timeSinceUserSpeechEndMs(): Long {
        val endTime = lastUserSpeechEndMs.get()
        return if (endTime > 0L) clockMs() - endTime else 0L
    }
    
    /** Get time since last agent speech ended */
    fun timeSinceAgentSpeechEndMs(): Long {
        val endTime = lastAgentSpeechEndMs.get()
        return if (endTime > 0L) clockMs() - endTime else 0L
    }
    
    /** Get current turn duration */
    fun currentTurnDurationMs(): Long {
        val startTime = turnStartMs.get()
        return if (startTime > 0L) clockMs() - startTime else 0L
    }
    
    /**
     * Check if the conversation flow is healthy (no excessive barge-ins or timeouts)
     */
    fun isConversationHealthy(): Boolean {
        val consecutive = consecutiveBargeIns.get()
        val timeout = isTurnTimeout()
        
        // Too many consecutive barge-ins may indicate echo or other issues
        if (consecutive > 3) return false
        
        // Timeout is not healthy
        if (timeout) return false
        
        return true
    }
    
    /**
     * Get a snapshot of the current conversation flow state for diagnostics
     */
    fun getFlowSnapshot(): FlowSnapshot {
        return FlowSnapshot(
            phase = currentPhase.get(),
            phaseDurationMs = if (lastPhaseChangeMs.get() > 0L) clockMs() - lastPhaseChangeMs.get() else 0L,
            turnDurationMs = currentTurnDurationMs(),
            timeSinceUserSpeechEndMs = timeSinceUserSpeechEndMs(),
            timeSinceAgentSpeechEndMs = timeSinceAgentSpeechEndMs(),
            bargeInDetected = bargeInDetected.get(),
            consecutiveBargeIns = consecutiveBargeIns.get(),
            isHealthy = isConversationHealthy()
        )
    }
    
    /** Snapshot of conversation flow state for diagnostics */
    data class FlowSnapshot(
        val phase: FlowPhase,
        val phaseDurationMs: Long,
        val turnDurationMs: Long,
        val timeSinceUserSpeechEndMs: Long,
        val timeSinceAgentSpeechEndMs: Long,
        val bargeInDetected: Boolean,
        val consecutiveBargeIns: Long,
        val isHealthy: Boolean
    ) {
        override fun toString(): String = buildString {
            append("FlowSnapshot(")
            append("phase=$phase")
            append(", phaseDurationMs=$phaseDurationMs")
            append(", turnDurationMs=$turnDurationMs")
            append(", timeSinceUserSpeechEndMs=$timeSinceUserSpeechEndMs")
            append(", timeSinceAgentSpeechEndMs=$timeSinceAgentSpeechEndMs")
            append(", bargeInDetected=$bargeInDetected")
            append(", consecutiveBargeIns=$consecutiveBargeIns")
            append(", isHealthy=$isHealthy")
            append(")")
        }
    }
}
