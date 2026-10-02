package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.MysticViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VioletGlow
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun SoundWaveVisualizer(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 7
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val active = state == AssistantState.LISTENING || state == AssistantState.SPEAKING

    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val distFromCenter = abs(i - (barCount - 1) / 2f) / ((barCount - 1) / 2f)
            val baseHeight = if (active) {
                val wave = (sin(phase + i * 0.8f) + 1f) / 2f
                val factor = amplitude * (1f - distFromCenter * 0.4f) * wave
                val dynamic = 8.dp + (30.dp * factor)
                dynamic.coerceIn(6.dp, 36.dp)
            } else {
                (6.dp + (2.dp * distFromCenter)).coerceIn(4.dp, 8.dp)
            }

            val brush = when (state) {
                AssistantState.LISTENING -> Brush.verticalGradient(
                    listOf(NeonCyan, CyanGlow)
                )
                AssistantState.SPEAKING -> Brush.verticalGradient(
                    listOf(VioletGlow, MysticViolet)
                )
                AssistantState.THINKING -> Brush.verticalGradient(
                    listOf(MysticViolet, NeonCyan)
                )
                else -> Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.1f))
                )
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(baseHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(brush)
            )
        }
    }
}
