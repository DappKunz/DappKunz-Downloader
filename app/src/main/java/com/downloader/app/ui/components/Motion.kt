package com.downloader.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.downloader.app.core.util.Formatters
import com.downloader.app.ui.theme.DownloaderColors
import com.downloader.app.ui.theme.DownloaderRadius
import kotlinx.coroutines.delay

/**
 * Motion language for Fluxen.
 *
 * The Figma prototype is deliberately calm: surfaces lift, content settles, and
 * menus arrive from the edge they belong to. These helpers keep that language
 * consistent instead of sprinkling unrelated durations through every screen.
 */
object DownloaderMotion {
    const val Fast = 160
    const val Normal = 280
    const val Slow = 460

    val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val EaseInOut = FastOutSlowInEasing

    fun <T> lift() = spring<T>(
        dampingRatio = 0.74f,
        stiffness = 420f,
    )

    fun <T> gentleSpring() = spring<T>(
        dampingRatio = 0.82f,
        stiffness = 260f,
    )

    val dialogEnter: EnterTransition =
        fadeIn(tween(180)) + scaleIn(tween(260, easing = EaseOut), initialScale = 0.9f) +
            slideInVertically(tween(260, easing = EaseOut)) { it / 18 }

    val dialogExit: ExitTransition =
        fadeOut(tween(140)) + scaleOut(tween(180, easing = EaseInOut), targetScale = 0.94f) +
            slideOutVertically(tween(180, easing = EaseInOut)) { it / 24 }
}

/**
 * Staggered entrance for cards, rows and sections. The animation runs once per
 * composition; LazyColumn items animate again when they are first scrolled in.
 */
fun Modifier.entrance(
    index: Int = 0,
    stagger: Int = 38,
    duration: Int = 420,
    offsetY: Int = 18,
    scaleFrom: Float = 0.97f,
): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtLeast(0) * stagger).toLong())
        progress.animateTo(1f, tween(duration, easing = DownloaderMotion.EaseOut))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * offsetY.dp.toPx()
        scaleX = scaleFrom + (1f - scaleFrom) * p
        scaleY = scaleFrom + (1f - scaleFrom) * p
        transformOrigin = TransformOrigin(0.5f, 0.45f)
    }
}

/**
 * Press feedback that works with any clickable surface without consuming the
 * gesture. Existing Surface(onClick) / clickable handlers keep working while
 * the whole card gently compresses under the finger.
 */
fun Modifier.pressScale(
    enabled: Boolean = true,
    scaleDown: Float = 0.965f,
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            try {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when (event.type) {
                            PointerEventType.Press -> pressed = true
                            PointerEventType.Release, PointerEventType.Exit -> pressed = false
                            else -> Unit
                        }
                    }
                }
            } finally {
                pressed = false
            }
        }
}

/** Slow rotation for decorative album rings / artwork orbits. */
fun Modifier.slowRotation(active: Boolean = true, periodMillis: Int = 22000): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "slowRotation")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
        ),
        label = "slowRotationAngle",
    )
    graphicsLayer {
        rotationZ = if (active) angle else 0f
        transformOrigin = TransformOrigin(0.5f, 0.5f)
    }
}

/** Gentle breathing scale for the Now Playing artwork glow. */
fun Modifier.breathing(active: Boolean = true, scale: Float = 1.015f): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "breathing")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = scale,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "breathingPulse",
    )
    graphicsLayer {
        scaleX = if (active) pulse else 1f
        scaleY = if (active) pulse else 1f
    }
}

@Composable
fun AnimatedIntText(
    value: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    padStart: Int = 0,
    durationMillis: Int = 760,
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis, easing = DownloaderMotion.EaseOut),
        label = "animatedInt",
    )
    Text(
        text = animated.coerceAtLeast(0).toString().padStart(padStart, '0'),
        style = style,
        color = color,
        modifier = modifier,
        maxLines = 1,
    )
}

@Composable
fun AnimatedBytesText(
    value: Long,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    durationMillis: Int = 900,
) {
    val animated by animateFloatAsState(
        targetValue = value.coerceAtLeast(0L).toFloat(),
        animationSpec = tween(durationMillis, easing = DownloaderMotion.EaseOut),
        label = "animatedBytes",
    )
    Text(
        text = Formatters.bytes(animated.toLong()),
        style = style,
        color = color,
        modifier = modifier,
        maxLines = 1,
    )
}

/**
 * Figma-style dialog: the window exists only while the enter/exit transition is
 * running, so menus and confirmations no longer pop in rigidly.
 */
@Composable
fun MotionDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (state.currentState || state.targetState) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            AnimatedVisibility(
                visibleState = state,
                enter = DownloaderMotion.dialogEnter,
                exit = DownloaderMotion.dialogExit,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DownloaderColors.AppearanceScrim)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(DownloaderRadius.dialog),
                        color = DownloaderColors.SurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            DownloaderColors.Border,
                        ),
                        modifier = modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            content = content,
                        )
                    }
                }
            }
        }
    }
}
