package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class NarrationPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var tickerJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    fun loadAudio(file: File) {
        release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
                _durationMs.value = duration.toLong()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = _durationMs.value
                    stopTicker()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun play() {
        val player = mediaPlayer ?: return
        if (!_isPlaying.value) {
            if (_currentPositionMs.value >= _durationMs.value) {
                player.seekTo(0)
            }
            player.start()
            _isPlaying.value = true
            startTicker()
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        if (_isPlaying.value) {
            player.pause()
            _isPlaying.value = false
            stopTicker()
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        player.seekTo(positionMs.toInt())
        _currentPositionMs.value = positionMs
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    _currentPositionMs.value = it.currentPosition.toLong()
                }
                delay(100)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun release() {
        stopTicker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
    }
}
