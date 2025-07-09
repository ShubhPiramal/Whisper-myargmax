package com.example.whisperkitargmax

import android.content.Context
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.argmaxinc.whisperkit.WhisperKit
import com.argmaxinc.whisperkit.ExperimentalWhisperKit

@OptIn(ExperimentalWhisperKit::class)
class WhisperKitManager(
    private val context: Context,
    private val onTranscriptionUpdate: (String) -> Unit,
    private val onStatusUpdate: (String) -> Unit
) {
    private var whisperKit: WhisperKit? = null
    private var isInitialized = false
    private var isModelLoaded = false

    fun initialize() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                onStatusUpdate("Initializing WhisperKit...")
                
                whisperKit = WhisperKit.Builder()
                    .setModel(WhisperKit.OPENAI_TINY_EN)
                    .setApplicationContext(context.applicationContext)
                    .setCallback { what, result ->
                        when (what) {
                            WhisperKit.TextOutputCallback.MSG_INIT -> {
                                onStatusUpdate("WhisperKit initialized successfully")
                                isInitialized = true
                            }
                            WhisperKit.TextOutputCallback.MSG_TEXT_OUT -> {
                                val fullText = result.text
                                onTranscriptionUpdate(fullText)
                            }
                            WhisperKit.TextOutputCallback.MSG_CLOSE -> {
                                onStatusUpdate("WhisperKit closed")
                            }
                        }
                    }
                    .build()

                loadModel()
            } catch (e: Exception) {
                onStatusUpdate("Error initializing WhisperKit: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadModel() {
        try {
            onStatusUpdate("Loading model...")
            
            withContext(Dispatchers.IO) {
                whisperKit?.loadModel()?.collect { progress ->
                    withContext(Dispatchers.Main) {
                        onStatusUpdate("Loading model: ${(progress * 100).toInt()}%")
                    }
                }
            }

            // Initialize with audio parameters
            whisperKit?.init(frequency = 16000, channels = 1, duration = 0)
            isModelLoaded = true
            onStatusUpdate("Model loaded successfully. Ready to transcribe!")
            
        } catch (e: Exception) {
            onStatusUpdate("Error loading model: ${e.message}")
            e.printStackTrace()
        }
    }

    fun transcribeAudio(audioData: ByteArray) {
        if (!isInitialized || !isModelLoaded) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                whisperKit?.transcribe(audioData)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onStatusUpdate("Error transcribing audio: ${e.message}")
                }
                e.printStackTrace()
            }
        }
    }

    fun cleanup() {
        try {
            whisperKit?.deinitialize()
            isInitialized = false
            isModelLoaded = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isReady(): Boolean = isInitialized && isModelLoaded
}
