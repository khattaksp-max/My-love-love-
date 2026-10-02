package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.MysticViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftRose
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorderSubtle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.WarmAmber

@Composable
fun VoiceControlDock(
    state: AssistantState,
    onMicClick: () -> Unit,
    onInterruptClick: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTextInput: () -> Unit,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickPrompts = remember {
        listOf(
            "How can I de-stress?",
            "Plan my morning",
            "Open YouTube",
            "Open WhatsApp",
            "Set a 10m timer",
            "Open Camera",
            "Remember my coffee order"
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseHalo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val micButtonColor by animateColorAsState(
        targetValue = when (state) {
            AssistantState.LISTENING -> NeonCyan
            AssistantState.SPEAKING -> VioletGlow
            AssistantState.THINKING -> MysticViolet
            AssistantState.ERROR -> WarmAmber
            AssistantState.IDLE -> NeonCyan
        },
        animationSpec = tween(400),
        label = "micButtonColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick suggestions carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickPrompts.forEach { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceGlass)
                        .border(1.dp, SurfaceGlassBorderSubtle, RoundedCornerShape(16.dp))
                        .clickable { onPromptSelected(prompt) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Action dock bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(SurfaceGlass)
                .border(1.dp, SurfaceGlassBorderSubtle, RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Memory button
            IconButton(
                onClick = onOpenMemory,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("memory_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Personal Memory Controls",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Keyboard / text entry button
            IconButton(
                onClick = onOpenTextInput,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("text_input_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Type a message",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Central Glowing Microphone Button with interactive halo
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(micButtonColor.copy(alpha = 0.22f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    micButtonColor,
                                    if (state == AssistantState.SPEAKING) MysticViolet else CyanGlow
                                )
                            )
                        )
                        .clickable(onClick = onMicClick)
                        .testTag("mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (state) {
                            AssistantState.SPEAKING -> Icons.Default.Stop
                            AssistantState.LISTENING -> Icons.Default.GraphicEq
                            AssistantState.THINKING -> Icons.Default.MicOff
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Microphone Action",
                        tint = CosmicBlack,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Interrupt button (when speaking) or secondary action
            if (state == AssistantState.SPEAKING) {
                IconButton(
                    onClick = onInterruptClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("interrupt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Interrupt Assistant",
                        tint = SoftRose,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Voice settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("voice_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsVoice,
                    contentDescription = "Voice and Speech Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
