package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MysticViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftRose
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.WarmAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuraOrbCanvas(
    state: AssistantState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbLoop")

    // Smooth rotational angle
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 3500
                    AssistantState.SPEAKING -> 6000
                    AssistantState.LISTENING -> 8000
                    else -> 15000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Smooth amplitude interpolation
    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(120, easing = LinearEasing),
        label = "amp"
    )

    val coreScale = when (state) {
        AssistantState.LISTENING -> (1f + animatedAmp * 0.45f) * breathingPulse
        AssistantState.SPEAKING -> (1f + animatedAmp * 0.4f) * breathingPulse
        AssistantState.THINKING -> breathingPulse * 1.08f
        AssistantState.ERROR -> 1.0f
        AssistantState.IDLE -> breathingPulse
    }

    val primaryColor = when (state) {
        AssistantState.LISTENING -> NeonCyan
        AssistantState.THINKING -> MysticViolet
        AssistantState.SPEAKING -> VioletGlow
        AssistantState.ERROR -> WarmAmber
        AssistantState.IDLE -> CyanGlow
    }

    val secondaryColor = when (state) {
        AssistantState.LISTENING -> CyanGlow
        AssistantState.THINKING -> SoftRose
        AssistantState.SPEAKING -> NeonCyan
        AssistantState.ERROR -> SoftRose
        AssistantState.IDLE -> MysticViolet
    }

    Box(
        modifier = modifier
            .size(280.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = coreScale,
                    scaleY = coreScale
                )
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.width * 0.28f

            // 1. Ambient outer aura diffusion
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f + animatedAmp * 0.3f),
                        secondaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * (1.75f + animatedAmp * 0.4f)
                ),
                radius = baseRadius * (1.75f + animatedAmp * 0.4f),
                center = center
            )

            // 2. Dynamic acoustic harmonic ripple rings
            val ringCount = 3
            for (i in 1..ringCount) {
                val ringOffset = (rotationAngle * (if (i % 2 == 0) 1 else -1) + i * 45f) * (Math.PI.toFloat() / 180f)
                val ringRadius = baseRadius * (1.1f + i * 0.18f + (animatedAmp * 0.35f * (i / 2f)))
                val ringAlpha = (0.45f - i * 0.1f + animatedAmp * 0.25f).coerceIn(0.08f, 0.7f)

                val path = Path()
                val steps = 36
                for (s in 0..steps) {
                    val angle = (s.toFloat() / steps) * 2 * Math.PI.toFloat()
                    val waveFactor = sin(angle * (i + 2) + ringOffset) * (8f + animatedAmp * 22f)
                    val r = ringRadius + waveFactor
                    val x = center.x + r * cos(angle)
                    val y = center.y + r * sin(angle)
                    if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()

                drawPath(
                    path = path,
                    color = if (i % 2 == 0) primaryColor.copy(alpha = ringAlpha) else secondaryColor.copy(alpha = ringAlpha),
                    style = Stroke(width = (2f + (animatedAmp * 2.5f)))
                )
            }

            // 3. Inner fluid energy core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.7f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // 4. Shimmering fluid internal crest
            val crestPath = Path()
            val crestRadius = baseRadius * 0.7f
            val crestRad = rotationAngle * (Math.PI.toFloat() / 180f)
            for (s in 0..24) {
                val theta = (s.toFloat() / 24) * 2 * Math.PI.toFloat()
                val offsetDist = sin(theta * 3f + crestRad * 2f) * (6f + animatedAmp * 14f)
                val cr = crestRadius + offsetDist
                val cx = center.x + cr * cos(theta)
                val cy = center.y + cr * sin(theta)
                if (s == 0) crestPath.moveTo(cx, cy) else crestPath.lineTo(cx, cy)
            }
            crestPath.close()

            drawPath(
                path = crestPath,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        primaryColor.copy(alpha = 0.6f),
                        secondaryColor.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.8f)
                    ),
                    center = center
                ),
                blendMode = BlendMode.Screen
            )

            // 5. Central crystalline spark
            drawCircle(
                color = Color.White.copy(alpha = 0.85f + animatedAmp * 0.15f),
                radius = baseRadius * 0.18f,
                center = center
            )
        }
    }
}
