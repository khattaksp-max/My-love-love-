package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.AssistantState
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.MysticViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SoftRose
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.WarmAmber
import kotlin.math.cos
import kotlin.math.sin

private data class AmbientParticle(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val alpha: Float,
    val speed: Float,
    val phase: Float
)

@Composable
fun AuraBackgroundCanvas(
    state: AssistantState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    // Animated glow colors based on assistant state
    val glowColor1 by animateColorAsState(
        targetValue = when (state) {
            AssistantState.LISTENING -> NeonCyan.copy(alpha = 0.28f)
            AssistantState.THINKING -> MysticViolet.copy(alpha = 0.35f)
            AssistantState.SPEAKING -> VioletGlow.copy(alpha = 0.32f)
            AssistantState.ERROR -> WarmAmber.copy(alpha = 0.25f)
            AssistantState.IDLE -> CyanGlow.copy(alpha = 0.12f)
        },
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "glowColor1"
    )

    val glowColor2 by animateColorAsState(
        targetValue = when (state) {
            AssistantState.LISTENING -> DeepSpace
            AssistantState.THINKING -> SoftRose.copy(alpha = 0.18f)
            AssistantState.SPEAKING -> CyanGlow.copy(alpha = 0.22f)
            AssistantState.ERROR -> SoftRose.copy(alpha = 0.2f)
            AssistantState.IDLE -> SurfaceDark.copy(alpha = 0.3f)
        },
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "glowColor2"
    )

    val particles = remember {
        List(30) { index ->
            AmbientParticle(
                xRatio = (index * 37 % 100) / 100f,
                yRatio = (index * 73 % 100) / 100f,
                radius = 1.2f + (index % 3) * 0.8f,
                alpha = 0.2f + (index % 5) * 0.12f,
                speed = 0.5f + (index % 4) * 0.4f,
                phase = (index * 0.4f)
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Deep space base gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(CosmicBlack, DeepSpace, CosmicBlack)
            )
        )

        // Dynamic drifting atmospheric glow orbs
        val orb1X = width * (0.5f + 0.2f * sin(time * 0.7f))
        val orb1Y = height * (0.35f + 0.15f * cos(time * 0.5f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor1, Color.Transparent),
                center = Offset(orb1X, orb1Y),
                radius = width * 0.85f
            ),
            radius = width * 0.85f,
            center = Offset(orb1X, orb1Y)
        )

        val orb2X = width * (0.5f - 0.22f * cos(time * 0.6f))
        val orb2Y = height * (0.65f + 0.18f * sin(time * 0.8f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor2, Color.Transparent),
                center = Offset(orb2X, orb2Y),
                radius = width * 0.75f
            ),
            radius = width * 0.75f,
            center = Offset(orb2X, orb2Y)
        )

        // Delicate floating star particles
        particles.forEach { p ->
            val particleAlpha = (p.alpha * (0.6f + 0.4f * sin(time * p.speed + p.phase))).coerceIn(0f, 1f)
            val px = (p.xRatio * width + 15f * sin(time * p.speed + p.phase)) % width
            val py = (p.yRatio * height + 20f * cos(time * p.speed + p.phase)) % height

            drawCircle(
                color = Color.White.copy(alpha = particleAlpha),
                radius = p.radius,
                center = Offset(px, py)
            )
        }
    }
}
