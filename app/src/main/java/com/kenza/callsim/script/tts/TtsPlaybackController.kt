package com.kenza.callsim.script.tts

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class TtsPlaybackStatus { IDLE, PLAYING, PAUSED, COMPLETED, ERROR }

data class TtsPlaybackState(
    val status: TtsPlaybackStatus = TtsPlaybackStatus.IDLE,
    val positionMs: Long = 0L,
    val totalMs: Long = 0L,
    val errorMessage: String? = null,
)

/**
 * Local-file timeline player. Listener silences are first-class time ranges, so seek and resume
 * remain consistent with the production pause metadata instead of treating rendered clips as one
 * uninterrupted monologue.
 */
class TtsPlaybackController(private val result: TtsRenderResult) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(TtsPlaybackState(totalMs = result.totalDurationMs))
    val state: StateFlow<TtsPlaybackState> = _state.asStateFlow()

    private var playbackJob: Job? = null
    private var player: MediaPlayer? = null

    fun play() {
        if (_state.value.status == TtsPlaybackStatus.PLAYING) return
        val from = if (_state.value.status == TtsPlaybackStatus.COMPLETED) 0L else _state.value.positionMs
        startFrom(from)
    }

    fun pause() {
        if (_state.value.status != TtsPlaybackStatus.PLAYING) return
        playbackJob?.cancel()
        playbackJob = null
        releasePlayer()
        _state.value = _state.value.copy(status = TtsPlaybackStatus.PAUSED, errorMessage = null)
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, result.totalDurationMs)
        val resume = _state.value.status == TtsPlaybackStatus.PLAYING
        playbackJob?.cancel()
        playbackJob = null
        releasePlayer()
        _state.value = _state.value.copy(
            status = if (target >= result.totalDurationMs) TtsPlaybackStatus.COMPLETED else TtsPlaybackStatus.PAUSED,
            positionMs = target,
            errorMessage = null,
        )
        if (resume && target < result.totalDurationMs) startFrom(target)
    }

    fun cancel() {
        playbackJob?.cancel()
        playbackJob = null
        releasePlayer()
        _state.value = TtsPlaybackState(totalMs = result.totalDurationMs)
    }

    fun release() {
        playbackJob?.cancel()
        playbackJob = null
        releasePlayer()
        scope.cancel()
    }

    private fun startFrom(positionMs: Long) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            try {
                _state.value = _state.value.copy(
                    status = TtsPlaybackStatus.PLAYING,
                    positionMs = positionMs,
                    errorMessage = null,
                )
                var timelineStart = 0L
                var cursor = positionMs

                for (clip in result.segments) {
                    if (!isActive) return@launch
                    val silenceStart = timelineStart
                    val audioStart = silenceStart + clip.pauseBeforeMs
                    val clipEnd = audioStart + clip.durationMs
                    timelineStart = clipEnd

                    if (cursor >= clipEnd) continue

                    if (cursor < audioStart) {
                        var remaining = audioStart - cursor.coerceAtLeast(silenceStart)
                        while (remaining > 0 && isActive) {
                            val step = minOf(100L, remaining)
                            delay(step)
                            remaining -= step
                            cursor += step
                            _state.value = _state.value.copy(positionMs = cursor.coerceAtMost(audioStart))
                        }
                    }

                    if (!isActive) return@launch
                    val file = File(clip.artifactRef)
                    require(file.isFile) { "Rendered audio file is missing." }
                    val offset = (cursor - audioStart).coerceAtLeast(0L).coerceAtMost(clip.durationMs)
                    val currentPlayer = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        prepare()
                        if (offset > 0) seekTo(offset.toInt())
                        start()
                    }
                    player = currentPlayer

                    while (isActive && currentPlayer.isPlaying) {
                        cursor = (audioStart + currentPlayer.currentPosition).coerceAtMost(clipEnd)
                        _state.value = _state.value.copy(positionMs = cursor)
                        delay(100L)
                    }
                    releasePlayer()
                    cursor = clipEnd
                    _state.value = _state.value.copy(positionMs = cursor)
                }

                _state.value = _state.value.copy(
                    status = TtsPlaybackStatus.COMPLETED,
                    positionMs = result.totalDurationMs,
                )
            } catch (error: Throwable) {
                if (!isActive) return@launch
                releasePlayer()
                _state.value = _state.value.copy(
                    status = TtsPlaybackStatus.ERROR,
                    errorMessage = error.message ?: "Playback failed.",
                )
            }
        }
    }

    private fun releasePlayer() {
        val current = player
        player = null
        runCatching { current?.stop() }
        runCatching { current?.release() }
    }
}
