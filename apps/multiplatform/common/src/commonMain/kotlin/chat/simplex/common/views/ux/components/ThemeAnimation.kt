package chat.simplex.common.views.ux.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import chat.simplex.common.model.ChatController
import chat.simplex.common.ui.theme.*
import chat.simplex.common.ui.theme.DefaultTheme
import chat.simplex.common.ui.theme.ThemeManager
import chat.simplex.res.MR
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import chat.simplex.common.ui.theme.CurrentColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot

/** Fallback reveal origin (approx. app bar area) used when the call site has no real control bounds. */
val DEFAULT_REVEAL_ORIGIN = Offset(1000f, 150f)

/**
 * Toggle decision for the theme reveal: the darkness AFTER the toggle, or null when a reveal
 * is already running and the new trigger must be ignored.
 *
 * Re-entry policy (issue #14): while a reveal is running, new triggers are IGNORED rather
 * than cancel-and-restarted. The running reveal applies the theme at its 180 ms mark;
 * restarting would race that and could flip the theme twice or leave [ThemeAnimationController.targetIsDark]
 * inconsistent with the actually applied theme. Ignoring cannot leave stale state either,
 * because the running reveal always resets [ThemeAnimationController.isAnimating] in its finally block.
 */
fun nextRevealTarget(isAnimating: Boolean, currentIsDark: Boolean): Boolean? =
    if (isAnimating) null else !currentIsDark

/** [originOffset] when provided, the documented fallback otherwise. */
fun resolveRevealOrigin(originOffset: Offset?): Offset =
    originOffset ?: DEFAULT_REVEAL_ORIGIN

object ThemeAnimationController {
    val isAnimating = mutableStateOf(false)
    val origin = mutableStateOf(DEFAULT_REVEAL_ORIGIN)
    val targetIsDark = mutableStateOf(false)
    val animProgress = Animatable(0f)

    // Own scope so trigger() can be called without a caller-provided scope; the reveal then
    // also survives the calling screen being disposed, so the theme still applies.
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Start the circular theme reveal expanding from [originOffset] (window coordinates of
     * the tapped control; null = [DEFAULT_REVEAL_ORIGIN]).
     *
     * @param currentlyDark darkness BEFORE the toggle; null = derived from the live theme
     *   ([CurrentColors]) instead of a value captured at composition time.
     * @param scope scope running the reveal; null = controller-owned scope. A screen-scoped
     *   scope is safe: cancellation resets [isAnimating] via finally.
     *
     * Triggers while a reveal is already running are ignored - see [nextRevealTarget].
     */
    fun trigger(
        originOffset: Offset? = null,
        currentlyDark: Boolean? = null,
        scope: CoroutineScope? = null
    ) {
        val currentIsDark = currentlyDark ?: !CurrentColors.value.colors.isLight
        val newIsDark = nextRevealTarget(isAnimating.value, currentIsDark) ?: return
        val revealScope = scope ?: controllerScope
        // A dead scope would never run the coroutine body, so it could also never run the
        // finally that resets isAnimating - never arm the overlay in that case.
        if (!revealScope.isActive) return
        origin.value = resolveRevealOrigin(originOffset)
        targetIsDark.value = newIsDark
        isAnimating.value = true

        revealScope.launch {
            try {
                animProgress.snapTo(0f)
                // #82: apply the theme BEFORE the reveal draws, and without the
                // system night-mode sync. setApplicationNightMode recreates the
                // activity (uiMode config change) - that teardown under a
                // running full-screen animation was the entire jank; the Compose
                // tree restyles from CurrentColors alone, and the sync still
                // happens on the next activity create. The opaque overlay disc
                // (target background) masks the flip near the origin.
                val targetTheme: String = if (newIsDark) {
                    ChatController.appPrefs.systemDarkTheme.get() ?: DefaultTheme.DARK.themeName
                } else {
                    DefaultTheme.LIGHT.themeName
                }
                ThemeManager.applyTheme(targetTheme, syncSystemNightMode = false)
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            } finally {
                // ALWAYS reset - also when a screen-scoped scope cancels this coroutine
                // (e.g. back navigation mid-reveal) - otherwise the hosted overlay
                // (zIndex 9999) keeps drawing a frozen partial circle over the app.
                isAnimating.value = false
            }
        }
    }
}

@Composable
fun AnimatedThemeIcon(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isDark) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f)
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
            },
        contentAlignment = Alignment.Center
    ) {
        if (isDark) {
            Icon(
                painter = painterResource(MR.images.ic_light_mode),
                contentDescription = stringResource(MR.strings.theme_mode_light_descr),
                tint = AmberGold,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                painter = painterResource(MR.images.ic_bedtime_moon),
                contentDescription = stringResource(MR.strings.theme_mode_dark_descr),
                tint = Amber600,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ThemeCircularRevealOverlay() {
    if (ThemeAnimationController.isAnimating.value) {
        val progress = ThemeAnimationController.animProgress.value
        val origin = ThemeAnimationController.origin.value
        val targetIsDark = ThemeAnimationController.targetIsDark.value
        val targetBgColor = if (targetIsDark) Slate900 else Slate50
        val ringColor = if (targetIsDark) AmberGold else Amber600

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(9999f)
        ) {
            val maxRadius = hypot(size.width, size.height) * 1.25f
            val currentRadius = maxRadius * progress

            // Expanding circle filling the screen with new theme color
            drawCircle(
                color = targetBgColor,
                radius = currentRadius,
                center = origin
            )

            // Dynamic glow ring propagating at the wave edge
            if (progress in 0.01f..0.96f) {
                drawCircle(
                    color = ringColor.copy(alpha = (1f - progress * 0.8f).coerceIn(0f, 1f)),
                    radius = currentRadius,
                    center = origin,
                    style = Stroke(width = 6.dp.toPx() * (1f - progress * 0.5f))
                )
            }
        }
    }
}
