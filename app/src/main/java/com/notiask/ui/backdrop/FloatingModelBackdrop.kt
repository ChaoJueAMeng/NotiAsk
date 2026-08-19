package com.notiask.ui.backdrop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.notiask.ui.theme.NotiSkyEnd
import com.notiask.ui.theme.NotiSkyMid
import com.notiask.ui.theme.NotiSkyStart
import kotlin.math.PI

private const val TwoPi = (PI * 2.0).toFloat()

@Composable
fun FloatingModelBackdrop(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val tilePx = with(density) { 108.dp.roundToPx() }
    val pattern = remember(tilePx) { createModelPatternBitmap(context, tilePx) }
    val transition = rememberInfiniteTransition(label = "model-backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "diagonal-drift",
    )
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = TwoPi,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "float-wave",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(NotiSkyStart, NotiSkyMid, NotiSkyEnd),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize().clipToBounds()) {
            val period = pattern.width.toFloat()
            val shift = BackdropLattice.wrap(drift * period, period)
            val (fx, fy) = BackdropLattice.floatDisplacement(wave, 18.dp.toPx())
            val originX = -shift + fx
            val originY = -shift + fy
            val cols = BackdropLattice.copiesNeeded(size.width, period)
            val rows = BackdropLattice.copiesNeeded(size.height, period)
            val breathe = BackdropLattice.breatheScale(wave)
            scale(breathe, pivot = Offset(size.width / 2f, size.height / 2f)) {
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        translate(originX + col * period, originY + row * period) {
                            drawImage(
                                image = pattern,
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(pattern.width, pattern.height),
                                alpha = 0.82f,
                            )
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.16f),
                        )
                    )
                )
        )
    }
}
