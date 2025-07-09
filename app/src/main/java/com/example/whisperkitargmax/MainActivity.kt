package com.example.whisperkitargmax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.whisperkitargmax.ui.theme.WhisperKitArgmaxTheme

class MainActivity : ComponentActivity() {
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var whisperKitManager: WhisperKitManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            WhisperKitArgmaxTheme {
                TranscriptionScreen()
            }
        }
    }
    
    @Composable
    fun TranscriptionScreen() {
        var transcriptionText by remember { mutableStateOf("") }
        var statusText by remember { mutableStateOf("Ready to start") }
        var isRecording by remember { mutableStateOf(false) }
        var hasPermission by remember { mutableStateOf(false) }
        
        // Initialize WhisperKit manager
        LaunchedEffect(Unit) {
            whisperKitManager = WhisperKitManager(
                context = this@MainActivity,
                onTranscriptionUpdate = { text ->
                    transcriptionText = text
                },
                onStatusUpdate = { status ->
                    statusText = status
                }
            )
            whisperKitManager.initialize()
        }
        
        // Initialize audio recorder
        LaunchedEffect(Unit) {
            audioRecorder = AudioRecorder { audioData ->
                if (whisperKitManager.isReady()) {
                    whisperKitManager.transcribeAudio(audioData)
                }
            }
        }
        
        // Permission launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
            if (!isGranted) {
                statusText = "Microphone permission is required for recording"
            }
        }
        
        // Check permission on launch
        LaunchedEffect(Unit) {
            hasPermission = ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "WhisperKit Transcription",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Status
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRecording) 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Record/Stop Button
                FloatingActionButton(
                    onClick = {
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@FloatingActionButton
                        }
                        
                        if (!whisperKitManager.isReady()) {
                            statusText = "WhisperKit is not ready yet. Please wait..."
                            return@FloatingActionButton
                        }
                        
                        if (isRecording) {
                            audioRecorder.stopRecording()
                            isRecording = false
                            statusText = "Recording stopped"
                        } else {
                            transcriptionText = ""
                            audioRecorder.startRecording()
                            isRecording = true
                            statusText = "Recording... Speak now!"
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 24.dp),
                    containerColor = if (isRecording) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }
                
                // Transcription Text
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Transcription:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        val scrollState = rememberScrollState()
                        
                        LaunchedEffect(transcriptionText) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                        
                        Text(
                            text = if (transcriptionText.isEmpty()) 
                                "Your transcribed text will appear here..." 
                            else 
                                transcriptionText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            fontSize = 16.sp,
                            color = if (transcriptionText.isEmpty()) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::audioRecorder.isInitialized) {
            audioRecorder.stopRecording()
        }
        if (::whisperKitManager.isInitialized) {
            whisperKitManager.cleanup()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TranscriptionScreenPreview() {
    WhisperKitArgmaxTheme {
        // Preview content would go here
    }
}
