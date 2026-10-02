package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantState
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraBackgroundCanvas
import com.example.ui.components.AuraOrbCanvas
import com.example.ui.components.ConversationHistoryDialog
import com.example.ui.components.ConversationTranscript
import com.example.ui.components.MemoryBottomSheet
import com.example.ui.components.PermissionExplanationDialog
import com.example.ui.components.SoundWaveVisualizer
import com.example.ui.components.TextInputDialog
import com.example.ui.components.VoiceControlDock
import com.example.ui.components.VoiceSettingsBottomSheet
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.MysticViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorderSubtle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.WarmAmber

@Composable
fun AuraHomeScreen(
    viewModel: AuraViewModel
) {
    val context = LocalContext.current
    val assistantState by viewModel.assistantState.collectAsState()
    val visualizerAmplitude by viewModel.visualizerAmplitude.collectAsState()
    val currentStatus by viewModel.currentStatus.collectAsState()
    val partialText by viewModel.partialSpeechText.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val detectedTone by viewModel.detectedTone.collectAsState()
    val lastActionNote by viewModel.lastActionResult.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val voiceSettings by viewModel.voiceSettings.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()

    var showMemorySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicToggled()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Handled silently for background notifications */ }

    fun checkAndRequestMicPermission() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            viewModel.onMicToggled()
        } else {
            showPermissionDialog = true
        }

        // Request notification permission on Android 13+ if background listening is enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CosmicBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Ambient Dynamic Background Canvas
            AuraBackgroundCanvas(state = assistantState)

            // 2. Foreground Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Brand Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo & Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(NeonCyan, MysticViolet))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Aura AI",
                                tint = CosmicBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AURA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Intelligent Companion",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Mode Pills (Continuous & Background Indicators)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (voiceSettings.continuousConversation) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Hearing,
                                        contentDescription = "Hands-free continuous conversation",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Continuous",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Center Stage: Interactive AI Orb & Status Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceGlass)
                            .border(1.dp, SurfaceGlassBorderSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (assistantState) {
                                            AssistantState.LISTENING -> NeonCyan
                                            AssistantState.SPEAKING -> VioletGlow
                                            AssistantState.THINKING -> MysticViolet
                                            AssistantState.ERROR -> WarmAmber
                                            AssistantState.IDLE -> CyanGlow
                                        }
                                    )
                            )
                            Text(
                                text = currentStatus,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Dynamic Interactive AI Orb
                    AuraOrbCanvas(
                        state = assistantState,
                        amplitude = visualizerAmplitude,
                        onClick = { checkAndRequestMicPermission() },
                        modifier = Modifier.testTag("aura_orb_canvas")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio Waveform Visualizer
                    SoundWaveVisualizer(
                        state = assistantState,
                        amplitude = visualizerAmplitude
                    )
                }

                // Minimal Glass Conversation Transcript
                ConversationTranscript(
                    state = assistantState,
                    partialUserText = partialText,
                    messages = messages,
                    detectedTone = detectedTone,
                    lastActionNote = lastActionNote,
                    onViewHistory = { showHistoryDialog = true },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .testTag("conversation_transcript")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Floating Control Dock
                VoiceControlDock(
                    state = assistantState,
                    onMicClick = { checkAndRequestMicPermission() },
                    onInterruptClick = { viewModel.interruptSpeech() },
                    onOpenMemory = { showMemorySheet = true },
                    onOpenSettings = { showSettingsSheet = true },
                    onOpenTextInput = { showTextInputDialog = true },
                    onPromptSelected = { prompt -> viewModel.processUserSpeech(prompt) }
                )
            }
        }
    }

    // Modal Sheets and Dialogs
    if (showMemorySheet) {
        MemoryBottomSheet(
            memories = memories,
            isMemoryEnabled = voiceSettings.memoryEnabled,
            onToggleMemoryEnabled = { viewModel.toggleMemoryEnabled(it) },
            onAddMemory = { title, content, cat -> viewModel.addMemory(title, content, cat) },
            onDeleteMemory = { viewModel.deleteMemory(it) },
            onToggleMemoryActive = { viewModel.toggleMemory(it) },
            onClearAllMemories = { viewModel.clearAllMemories() },
            onDismiss = { showMemorySheet = false }
        )
    }

    if (showSettingsSheet) {
        VoiceSettingsBottomSheet(
            settings = voiceSettings,
            availableVoices = availableVoices,
            onUpdatePitch = { viewModel.updatePitch(it) },
            onUpdateSpeed = { viewModel.updateSpeed(it) },
            onUpdateVoice = { viewModel.updateVoice(it) },
            onToggleContinuous = { viewModel.toggleContinuousConversation(it) },
            onToggleBackground = { viewModel.toggleBackgroundListening(it) },
            onPreviewVoice = { viewModel.previewVoice(it) },
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showHistoryDialog) {
        ConversationHistoryDialog(
            messages = messages,
            onClearHistory = { viewModel.clearConversation() },
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showTextInputDialog) {
        TextInputDialog(
            onSendText = { viewModel.sendTextMessage(it) },
            onDismiss = { showTextInputDialog = false }
        )
    }

    if (showPermissionDialog) {
        PermissionExplanationDialog(
            onRequestPermission = {
                showPermissionDialog = false
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDismiss = { showPermissionDialog = false }
        )
    }
}
