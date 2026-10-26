/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.speaker

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.lineageos.settings.R
import org.lineageos.settings.ui.ExpressiveFooter
import org.lineageos.settings.ui.ExpressiveGroup
import org.lineageos.settings.ui.ExpressiveIcon
import org.lineageos.settings.ui.ExpressiveScaffold
import org.lineageos.settings.ui.ExpressiveScreen
import org.lineageos.settings.ui.ExpressiveSwitchRow
import org.lineageos.settings.ui.ExpressiveTheme
import java.io.IOException

/** Plays a 30 second tone through the earpiece cleaning path to push dust out of the speaker. */
class ClearSpeakerActivity : ComponentActivity() {

    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var playing by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        setContent {
            ExpressiveTheme {
                ExpressiveScaffold(
                    title = stringResource(R.string.clear_speaker_title),
                    onBack = { finish() },
                ) { padding ->
                    ExpressiveScreen(modifier = Modifier.padding(padding)) {
                        ExpressiveGroup(title = "") {
                            item {
                                ExpressiveSwitchRow(
                                    titleText = stringResource(R.string.clear_speaker_title),
                                    summaryText = stringResource(R.string.clear_speaker_summary),
                                    isChecked = playing,
                                    // Like the preference it replaces, locked while the tone plays.
                                    isEnabled = !playing,
                                    leading = { ExpressiveIcon(R.drawable.ic_clear_speaker) },
                                    onChange = { if (it) startPlaying() },
                                )
                            }
                        }
                        ExpressiveFooter(stringResource(R.string.clear_speaker_description))
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stopPlaying()
    }

    private fun startPlaying() {
        audioManager.setParameters("status_earpiece_clean=on")
        volumeControlStream = AudioManager.STREAM_MUSIC
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.isLooping = true
            resources.openRawResourceFd(R.raw.clear_speaker_sound).use {
                player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            player.setVolume(1.0f, 1.0f)
            player.prepare()
            player.start()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to play speaker clean sound!", e)
            player.release()
            audioManager.setParameters("status_earpiece_clean=off")
            return
        }
        mediaPlayer = player
        playing = true
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stopPlaying() }, DURATION_MS)
    }

    private fun stopPlaying() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.reset()
            it.release()
        }
        mediaPlayer = null
        audioManager.setParameters("status_earpiece_clean=off")
        playing = false
    }

    companion object {
        private const val TAG = "ClearSpeaker"
        private const val DURATION_MS = 30_000L
    }
}
