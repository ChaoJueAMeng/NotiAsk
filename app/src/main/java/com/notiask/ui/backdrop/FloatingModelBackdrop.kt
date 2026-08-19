package com.notiask.ui.backdrop

import android.os.Build
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.notiask.ui.theme.NotiSkyEnd
import com.notiask.ui.theme.NotiSkyMid
import com.notiask.ui.theme.NotiSkyStart
import kotlin.math.PI

private const val TwoPi = (PI * 2.0).toFloat()

@Composable
fun FloatingModelBackdrop(modifier: Modifier = Modifier) {
    val marks = ModelMark.entries
    val painters = marks.map { painterResource(it.iconRes) }
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
        val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(14.dp, BlurredEdgeTreatment.Unbounded)
        } else {
            Modifier
        }
        FloatingMarkField(
            painters = painters,
            glows = marks.map { it.glow },
            drift = drift,
            wave = wave,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .then(blurModifier),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.22f),
                        )
                    )
                )
        )
    }
}

@Composable
private fun FloatingMarkField(
    painters: List<Painter>,
    glows: List<Color>,
    drift: Float,
    wave: Float,
    modifier: Modifier = Modifier,
) {
    val markCount = painters.size
    Canvas(modifier) {
        val tile = 118.dp.toPx()
        val icon = 56.dp.toPx()
        val amplitude = 13.dp.toPx()
        val period = BackdropLattice.patternPeriodPx(tile, markCount)
        val shift = BackdropLattice.wrap(drift * period, period)
        val cols = BackdropLattice.columnCount(size.width, tile, markCount)
        val rows = BackdropLattice.rowCount(size.height, tile, markCount)
        val glowRadius = icon * 0.92f

        for (row in -1 until rows) {
            for (col in -1 until cols) {
                val index = BackdropLattice.markIndex(col, row, markCount)
                val seed = row * 37 + col * 17
                val (ox, oy) = BackdropLattice.cellOrigin(col, row, tile, shift)
                val (fx, fy) = BackdropLattice.floatDisplacement(wave, seed, amplitude)
                val left = ox + (tile - icon) / 2f + fx
                val top = oy + (tile - icon) / 2f + fy
                val cx = left + icon / 2f
                val cy = top + icon / 2f
                val glow = glows[index]
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = Offset(cx, cy),
                )
                val tilt = BackdropLattice.tiltDegrees(wave, seed)
                val breathe = BackdropLattice.breatheScale(wave, seed)
                rotate(tilt, pivot = Offset(cx, cy)) {
                    scale(breathe, pivot = Offset(cx, cy)) {
                        translate(left, top) {
                            with(painters[index]) {
                                draw(Size(icon, icon), alpha = 0.72f)
                            }
                        }
                    }
                }
            }
        }
    }
}
