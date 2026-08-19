package com.notiask.ui.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.notiask.ui.theme.NotiGlassStroke
import com.notiask.ui.theme.NotiGlassStrokeSoft
import com.notiask.ui.theme.NotiInk
import com.notiask.ui.theme.NotiInkMuted

val GlassCardShape = RoundedCornerShape(24.dp)
val GlassButtonShape = RoundedCornerShape(50)

fun glassFillBrush(prominent: Boolean = false): Brush = Brush.verticalGradient(
    colors = if (prominent) {
        listOf(
            Color.White.copy(alpha = 0.70f),
            Color.White.copy(alpha = 0.30f),
            Color.White.copy(alpha = 0.46f),
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.52f),
            Color.White.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.32f),
        )
    }
)

fun glassStrokeBrush(): Brush = Brush.verticalGradient(
    colors = listOf(NotiGlassStroke, NotiGlassStrokeSoft),
)

fun Modifier.notiGlass(
    shape: Shape,
    prominent: Boolean = false,
): Modifier = this
    .clip(shape)
    .drawBehind {
        drawRect(
            color = Color.White.copy(alpha = if (prominent) 0.10f else 0.06f),
        )
    }
    .background(glassFillBrush(prominent), shape)
    .border(BorderStroke(1.dp, glassStrokeBrush()), shape)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .notiGlass(shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prominent: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .notiGlass(GlassButtonShape, prominent = prominent)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = ripple(color = Color.White.copy(alpha = 0.55f)),
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            CompositionLocalProvider(
                LocalContentColor provides if (enabled) NotiInk else NotiInkMuted,
                content = { content() },
            )
        },
    )
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
            .notiGlass(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.55f)),
                enabled = enabled,
                role = Role.Button,
                onClick = { onClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides NotiInk, content = content)
    }
}

@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .notiGlass(RoundedCornerShape(50), prominent = false)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = NotiInk)
    }
}

@Composable
fun GlassStatusChip(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.18f))
            .border(1.dp, tint.copy(alpha = 0.38f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}
