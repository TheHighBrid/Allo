package com.kenza.callsim.call

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import android.view.Window
import android.view.WindowManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages proximity sensor detection for iOS-style screen-off behavior during calls.
 * When the proximity sensor detects a close object (like a user's face), the screen
 * can be turned off to save battery and prevent accidental touches.
 */
class ProximitySensorManager(
    private val context: Context,
    private val window: Window? = null,
) : SensorEventListener {
    
    companion object {
        private const val TAG = "ProximitySensor"
        private const val PROXIMITY_THRESHOLD = 5.0f // cm - typical threshold for "near"
    }
    
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    
    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    private var proximitySensor: Sensor? = null
    private var isRegistered = false
    private var isNear = false
    private var wasScreenOn = true
    
    // Track whether we should be managing the screen state
    private val enabled = AtomicBoolean(false)
    
    // Wake lock to prevent CPU sleep while we're managing screen state
    private var proximityWakeLock: PowerManager.WakeLock? = null
    
    // For testing - allows mocking the sensor state
    private var testMode = false
    private var testIsNear = false
    
    /**
     * Initialize the proximity sensor manager.
     * Call this once when the call starts.
     */
    fun initialize() {
        if (isRegistered) return
        
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if (proximitySensor == null) {
            Log.w(TAG, "Proximity sensor not available on this device")
            return
        }
        
        // Create a wake lock to ensure CPU stays awake while we're managing proximity
        proximityWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "${TAG}::ProximityWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
        
        Log.i(TAG, "Proximity sensor initialized: ${proximitySensor?.name}")
    }
    
    /**
     * Enable proximity sensor monitoring and screen management.
     * Call this when a call becomes active.
     */
    fun enable() {
        if (!enabled.compareAndSet(false, true)) return
        if (proximitySensor == null) return
        
        // Acquire wake lock
        proximityWakeLock?.acquire()
        
        // Register sensor listener
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        isRegistered = true
        
        Log.i(TAG, "Proximity sensor enabled")
    }
    
    /**
     * Disable proximity sensor monitoring and restore screen state.
     * Call this when a call ends.
     */
    fun disable() {
        if (!enabled.compareAndSet(true, false)) return
        
        // Unregister sensor listener
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
        }
        
        // Release wake lock
        proximityWakeLock?.release()
        
        // Restore screen state if we had turned it off
        if (!wasScreenOn && window != null) {
            restoreScreenState()
        }
        
        isNear = false
        wasScreenOn = true
        
        Log.i(TAG, "Proximity sensor disabled")
    }
    
    /**
     * Check if proximity sensor is currently detecting a close object.
     */
    fun isProximityNear(): Boolean = if (testMode) testIsNear else isNear
    
    /**
     * Check if proximity sensor is available on this device.
     */
    fun isAvailable(): Boolean = proximitySensor != null
    
    /**
     * Check if proximity management is currently enabled.
     */
    fun isEnabled(): Boolean = enabled.get()
    
    /**
     * Manually set proximity state (for testing).
     */
    fun setTestMode(near: Boolean) {
        testMode = true
        testIsNear = near
        if (testMode) {
            onProximityChanged(near)
        }
    }
    
    /**
     * Clear test mode.
     */
    fun clearTestMode() {
        testMode = false
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return
        
        val distance = event.values[0]
        val newIsNear = distance < PROXIMITY_THRESHOLD
        
        if (newIsNear != isNear) {
            isNear = newIsNear
            onProximityChanged(newIsNear)
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used for proximity sensor
    }
    
    private fun onProximityChanged(near: Boolean) {
        Log.d(TAG, "Proximity changed: near=$near")
        
        if (near) {
            // Object is near - turn screen off
            turnScreenOff()
        } else {
            // Object is far - turn screen on
            restoreScreenState()
        }
    }
    
    private fun turnScreenOff() {
        if (window == null) return
        
        // Remember current screen state
        if (wasScreenOn) {
            wasScreenOn = false
        }
        
        // Turn screen off using window flags
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            
            // For Android 10+, we need to use a different approach
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            
            Log.i(TAG, "Screen turned off due to proximity")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn screen off", e)
        }
    }
    
    private fun restoreScreenState() {
        if (window == null) return
        
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            
            // Keep screen on while in call
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            wasScreenOn = true
            Log.i(TAG, "Screen restored")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore screen state", e)
        }
    }
    
    /**
     * Clean up resources.
     */
    fun release() {
        disable()
        proximityWakeLock?.release()
        proximityWakeLock = null
        sensorManager.let { sm ->
            if (isRegistered) {
                sm.unregisterListener(this)
                isRegistered = false
            }
        }
    }
}
