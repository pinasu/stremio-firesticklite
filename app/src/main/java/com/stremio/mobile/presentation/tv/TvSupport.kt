package com.stremio.mobile.presentation.tv

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Android TV / Amazon Fire TV support.
 *
 * Everything in this file is a no-op on phones and tablets: the TV behaviour is switched on only
 * when [TvDevice.isTv] detects a leanback / Fire TV / non-touch device, so the original mobile
 * experience is left untouched.
 */
object TvDevice {
    /** Extra font scaling applied on TV for 10-foot readability (see MainActivity.attachBaseContext). */
    const val TV_FONT_SCALE = 1.15f

    @Volatile
    private var cached: Boolean? = null

    fun isTv(context: Context): Boolean {
        cached?.let { return it }
        val appContext = context.applicationContext ?: context
        val pm = appContext.packageManager
        val uiModeManager = appContext.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val result = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            pm.hasSystemFeature(AMAZON_FIRE_TV_FEATURE) ||
            pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            Build.MODEL.orEmpty().startsWith("AFT") ||
            !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        cached = result
        return result
    }

    private const val AMAZON_FIRE_TV_FEATURE = "amazon.hardware.fire_tv"
}

/** True when the app runs on a TV (Fire TV Stick, Android TV, Google TV). */
val LocalIsTvDevice = staticCompositionLocalOf { false }

object TvFocusDefaults {
    val RingColor = Color.White
    val RingShadowColor = Color.Black.copy(alpha = 0.55f)
    val FocusedOverlay = Color.White.copy(alpha = 0.16f)
    val PressedOverlay = Color.White.copy(alpha = 0.22f)
    val RingWidth = 3.dp
    val Shape: Shape = RoundedCornerShape(14.dp)
}

/**
 * Indication used by every `Modifier.clickable` / `selectable` / `toggleable` on TV (provided
 * through `LocalIndication`). It draws a bright overlay plus a thick white ring around the
 * focused element so the D-pad position is obvious from the sofa.
 */
object TvFocusIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        TvFocusIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = javaClass.hashCode()
}

private class TvFocusIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {
    private var focused = false
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            var focusCount = 0
            var pressCount = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> focusCount++
                    is FocusInteraction.Unfocus -> focusCount = (focusCount - 1).coerceAtLeast(0)
                    is PressInteraction.Press -> pressCount++
                    is PressInteraction.Release -> pressCount = (pressCount - 1).coerceAtLeast(0)
                    is PressInteraction.Cancel -> pressCount = (pressCount - 1).coerceAtLeast(0)
                }
                val newFocused = focusCount > 0
                val newPressed = pressCount > 0
                if (newFocused != focused || newPressed != pressed) {
                    focused = newFocused
                    pressed = newPressed
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (pressed) {
            drawRect(TvFocusDefaults.PressedOverlay)
        }
        if (focused) {
            drawRect(TvFocusDefaults.FocusedOverlay)
            drawFocusRingInBounds()
        }
    }
}

/**
 * Draws the focus ring inside the bounds. The corner radius is chosen generously (a larger radius
 * always stays inside a less-rounded clip) so the ring is never cut by the element's own clip.
 */
private fun DrawScope.drawFocusRingInBounds() {
    val ring = TvFocusDefaults.RingWidth.toPx()
    val radius = if (size.height <= 64.dp.toPx()) {
        size.minDimension / 2f
    } else {
        minOf(size.minDimension / 2f, 22.dp.toPx())
    }
    // Dark halo first so the white ring stays visible on light surfaces too.
    drawRoundRect(
        color = TvFocusDefaults.RingShadowColor,
        topLeft = Offset(ring, ring),
        size = Size(size.width - ring * 2f, size.height - ring * 2f),
        cornerRadius = CornerRadius((radius - ring).coerceAtLeast(0f)),
        style = Stroke(width = ring),
    )
    drawRoundRect(
        color = TvFocusDefaults.RingColor,
        topLeft = Offset(ring / 2f, ring / 2f),
        size = Size(size.width - ring, size.height - ring),
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = ring),
    )
}

/**
 * Focus ring for components that do not use `LocalIndication` (Material3 Button, IconButton,
 * Switch, Slider, FilterChip, DropdownMenuItem… and clickables declared with `indication = null`).
 * Apply it as the OUTERMOST modifier of the component: it observes the focus state of the focus
 * target below it and draws a ring following [shape], optionally scaling the element up.
 */
fun Modifier.tvFocusRing(
    shape: Shape = TvFocusDefaults.Shape,
    focusedScale: Float = 1f,
): Modifier = composed {
    if (!LocalIsTvDevice.current) return@composed Modifier
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        label = "tvFocusScale",
    )
    Modifier
        .onFocusChanged { focused = it.hasFocus }
        .then(
            if (focusedScale != 1f) {
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            } else {
                Modifier
            },
        )
        .drawWithContent {
            drawContent()
            if (focused) {
                val ring = TvFocusDefaults.RingWidth.toPx()
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline, color = TvFocusDefaults.RingShadowColor, style = Stroke(width = ring * 2.2f))
                drawOutline(outline, color = TvFocusDefaults.RingColor, style = Stroke(width = ring))
            }
        }
}

/** Scales an element up while it (or one of its children) has D-pad focus. TV only. */
fun Modifier.tvFocusScale(focusedScale: Float = 1.08f): Modifier = composed {
    if (!LocalIsTvDevice.current) return@composed Modifier
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        label = "tvFocusScale",
    )
    Modifier
        .onFocusChanged { focused = it.hasFocus }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

/**
 * Turns an overlay (sheet, full-screen panel) into a focus trap on TV:
 *  - when [active] becomes true, D-pad focus is moved inside the overlay (first focusable child);
 *  - while [active], arrow keys can never move focus to the screen drawn underneath.
 *
 * [active] must be true only for the top-most overlay: a programmatic `requestFocus()` on another
 * overlay also runs `onExit`, so an inactive trap must let focus go.
 */
fun Modifier.tvFocusTrap(active: Boolean = true): Modifier = composed {
    if (!LocalIsTvDevice.current) return@composed Modifier
    val requester = remember { FocusRequester() }
    val currentActive by rememberUpdatedState(active)
    var hasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        // Content may need a few frames to be laid out (lazy lists, async data): retry briefly.
        repeat(20) {
            if (hasFocus) return@LaunchedEffect
            runCatching { requester.requestFocus() }
            delay(50)
        }
    }

    Modifier
        .focusRequester(requester)
        .onFocusChanged { hasFocus = it.hasFocus }
        .focusProperties {
            onExit = {
                if (currentActive) cancelFocusChange()
            }
        }
        .focusGroup()
}

/** Makes a clickable (e.g. a scrim or a click-blocking container) unreachable by the D-pad. */
fun Modifier.notFocusable(): Modifier = focusProperties { canFocus = false }

tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}
