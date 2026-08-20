package chat.simplex.common.views.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.simplex.common.BuildConfigCommon
import chat.simplex.common.ui.theme.DEFAULT_PADDING
import chat.simplex.common.ui.theme.isInDarkTheme
import chat.simplex.common.views.helpers.ModalManager
import chat.simplex.common.views.helpers.mixWith
import chat.simplex.common.views.newchat.darkStops
import chat.simplex.common.views.newchat.gradientPoints
import chat.simplex.common.views.newchat.lightStops
import chat.simplex.res.MR
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.painterResource

/**
 * A layout for onboarding screens: image + content + spacer + button.
 * The spacer shrinks first (down to [minSpacerHeight]), then the image shrinks.
 * Button is always at the bottom.
 */
@Composable
fun OnboardingShrinkingLayout(
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    minSpacerHeight: Dp = 20.dp,
    image: @Composable () -> Unit,
    content: @Composable () -> Unit,
    button: @Composable () -> Unit
) {
    Layout(
        contents = listOf(image, content, button),
        modifier = modifier
    ) { (imageMeasurables, contentMeasurables, buttonMeasurables), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // 1. Measure fixed content (texts) and button first
        val contentPlaceable = contentMeasurables.first().measure(childConstraints)
        val buttonPlaceable = buttonMeasurables.first().measure(childConstraints)
        val minSpacer = minSpacerHeight.roundToPx()

        // 2. Image gets remaining after top padding + content + button + minimum spacer
        val topPad = topPadding.roundToPx()
        val reservedHeight = topPad + contentPlaceable.height + buttonPlaceable.height + minSpacer
        val imageMaxHeight = (height - reservedHeight).coerceAtLeast(0)
        val imagePlaceable = imageMeasurables.first().measure(
            childConstraints.copy(maxWidth = width, maxHeight = imageMaxHeight)
        )

        // 3. Spacer fills whatever is left between content and button
        val usedHeight = topPad + imagePlaceable.height + contentPlaceable.height + buttonPlaceable.height
        val spacerHeight = (height - usedHeight).coerceAtLeast(minSpacer)

        // 4. Place: image centered horizontally, rest below
        layout(width, height) {
            var y = topPad
            imagePlaceable.placeRelative((width - imagePlaceable.width) / 2, y)
            y += imagePlaceable.height
            contentPlaceable.placeRelative((width - contentPlaceable.width) / 2, y)
            y += contentPlaceable.height
            y += spacerHeight
            buttonPlaceable.placeRelative((width - buttonPlaceable.width) / 2, y)
        }
    }
}

@Composable
fun OnboardingHeroJewel(
    icon: ImageResource,
    accentColor: androidx.compose.ui.graphics.Color? = null,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1.15f
) {
    val isDark = isInDarkTheme()

    // Haute Horlogerie & Mineral Jewels Palettes
    val resolvedAccent = accentColor ?: when (icon) {
        MR.images.ic_forum -> if (isDark) androidx.compose.ui.graphics.Color(0xFF38BDF8) else androidx.compose.ui.graphics.Color(0xFF0284C7)
        MR.images.ic_person -> if (isDark) androidx.compose.ui.graphics.Color(0xFFE2B755) else androidx.compose.ui.graphics.Color(0xFFD97706)
        MR.images.ic_dns -> if (isDark) androidx.compose.ui.graphics.Color(0xFF2DD4BF) else androidx.compose.ui.graphics.Color(0xFF0F766E)
        MR.images.ic_shield -> if (isDark) androidx.compose.ui.graphics.Color(0xFF10B981) else androidx.compose.ui.graphics.Color(0xFF059669)
        else -> if (isDark) androidx.compose.ui.graphics.Color(0xFFE2B755) else androidx.compose.ui.graphics.Color(0xFF0284C7)
    }

    val cardBg = if (isDark) {
        Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF1E2533), androidx.compose.ui.graphics.Color(0xFF111722)))
    } else {
        Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFFFFFFFF), androidx.compose.ui.graphics.Color(0xFFF1F5F9)))
    }

    val specularRim = if (isDark) {
        Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0x45FFFFFF), androidx.compose.ui.graphics.Color(0x10FFFFFF)))
    } else {
        Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0x250F172A), androidx.compose.ui.graphics.Color(0x0C0F172A)))
    }

    val cardShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(cardShape)
            .background(cardBg)
            .border(1.dp, specularRim, cardShape),
        contentAlignment = Alignment.Center
    ) {
        // Concentric precision glow and inner ring
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(resolvedAccent.copy(alpha = if (isDark) 0.12f else 0.08f))
                .border(
                    1.dp,
                    resolvedAccent.copy(alpha = if (isDark) 0.25f else 0.18f),
                    androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(
                        if (isDark) androidx.compose.ui.graphics.Color(0xFF0B0F17) else androidx.compose.ui.graphics.Color(0xFFFFFFFF)
                    )
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(resolvedAccent.copy(alpha = 0.8f), resolvedAccent.copy(alpha = 0.2f))
                        ),
                        androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(46.dp),
                    tint = resolvedAccent
                )
            }
        }
    }
}

@Composable
fun OnboardingImage(
    lightImage: ImageResource,
    darkImage: ImageResource,
    fallbackIcon: ImageResource,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1.15f
) {
    if (BuildConfigCommon.SIMPLEX_ASSETS) {
        Image(
            painterResource(if (isInDarkTheme()) darkImage else lightImage),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().then(modifier)
        )
    } else {
        OnboardingHeroJewel(
            icon = fallbackIcon,
            modifier = modifier,
            aspectRatio = aspectRatio
        )
    }
}

@Composable
fun DesktopOnboardingShell(stage: OnboardingStage, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxSize()) {
        Box(
            Modifier.weight(0.382f).fillMaxHeight()
                .background(MaterialTheme.colors.background.mixWith(MaterialTheme.colors.onBackground, 0.985f))
                .padding(horizontal = DEFAULT_PADDING),
            contentAlignment = Alignment.Center
        ) {
            when (stage) {
                OnboardingStage.Step1_SimpleXInfo ->
                    OnboardingImage(MR.images.intro, MR.images.intro_light, MR.images.ic_forum, Modifier.fillMaxWidth())
                OnboardingStage.Step2_CreateProfile,
                OnboardingStage.Step2_5_SetupDatabasePassphrase,
                OnboardingStage.LinkAMobile ->
                    OnboardingImage(MR.images.your_profile, MR.images.your_profile_light, MR.images.ic_person, Modifier.fillMaxWidth())
                OnboardingStage.Step3_ChooseServerOperators,
                OnboardingStage.Step3_CreateSimpleXAddress,
                OnboardingStage.Step4_SetNotificationsMode ->
                    OnboardingImage(MR.images.your_network, MR.images.your_network_light, MR.images.ic_dns, Modifier.fillMaxWidth())
                OnboardingStage.Step4_NetworkCommitments ->
                    OnboardingImage(MR.images.network_commitments, MR.images.network_commitments_light, MR.images.ic_shield, Modifier.fillMaxWidth(), aspectRatio = 1.5f)
                else -> {}
            }
        }
        Divider(Modifier.fillMaxHeight().width(1.dp))
        Box(Modifier.weight(0.618f).fillMaxHeight().clipToBounds()) {
            content()
            ModalManager.fullscreen.showInView()
        }
    }
}
