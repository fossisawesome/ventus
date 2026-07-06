package com.fossisawesome.ventus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fossisawesome.ventus.ui.theme.LocalAppColors
import com.fossisawesome.ventus.ui.theme.LocalAppFontFamily

// Convenience Text composable backed by BasicText — replaces material3 Text() throughout the app.
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    textDecoration: TextDecoration? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textAlign = textAlign ?: TextAlign.Unspecified,
            textDecoration = textDecoration,
            lineHeight = lineHeight,
        ),
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
    )
}

// Renders an ImageVector with a colour tint — replaces material3 Icon().
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberVectorPainter(imageVector),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}

// Renders a drawable vector resource with a colour tint — used for the app's custom
// Phosphor-style icons (weather glyphs plus the UI chrome icons), sharing a call-site
// shape with the ImageVector overload above.
@Composable
fun AppIcon(
    @DrawableRes id: Int,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}

@Composable
private fun rememberPressAnimations(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedAlpha: Float,
    pressedScale: Float,
    label: String,
): Pair<Float, Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedAlpha else 0f,
        animationSpec = tween(durationMillis = 80),
        label = "${label}Press",
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = if (isPressed && enabled) {
            tween(durationMillis = 80, easing = LinearEasing)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "${label}Scale",
    )
    return overlayAlpha to scale
}

// Tap-target Box that wraps icon content — replaces material3 IconButton().
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val pressColor = LocalAppColors.current.text
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val (overlayAlpha, scale) = rememberPressAnimations(
        interactionSource = interactionSource,
        enabled = enabled,
        pressedAlpha = 0.12f,
        pressedScale = 0.82f,
        label = "iconBtn",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        // Haptic feedback is best-effort — devices/emulators without a
                        // vibrator must never have that break the actual click action.
                        runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                        onClick()
                    },
                ) else Modifier
            )
            .background(pressColor.copy(alpha = overlayAlpha), shape = CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// 1dp horizontal rule — replaces material3 HorizontalDivider().
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Color = LocalAppColors.current.border,
) {
    Box(modifier = modifier.fillMaxWidth().height(1.dp).background(color))
}

// Spinning arc — replaces material3 CircularProgressIndicator().
@Composable
fun Spinner(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )
    Canvas(modifier = modifier) {
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
        )
    }
}

// Minimal toggle switch — used for the units auto/override control.
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val thumbX by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "toggleThumb",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.accent.copy(alpha = 0.4f) else colors.border,
        animationSpec = tween(150),
        label = "toggleTrack",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.muted,
        animationSpec = tween(150),
        label = "toggleThumbColor",
    )
    Box(
        modifier = modifier
            .size(width = 40.dp, height = 22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(18.dp)
                .offset(x = (thumbX * 18).dp)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}

// Clickable text — replaces material3 TextButton().
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pressColor = LocalAppColors.current.text
    val interactionSource = remember { MutableInteractionSource() }
    val (overlayAlpha, scale) = rememberPressAnimations(
        interactionSource = interactionSource,
        pressedAlpha = 0.08f,
        pressedScale = 0.93f,
        label = "textBtn",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(pressColor.copy(alpha = overlayAlpha), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

// Small bordered stat tile — feels-like/humidity/wind/UV/precip row on the main screen.
@Composable
fun StatChip(
    @DrawableRes icon: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 84.dp)
            .background(colors.surface, RoundedCornerShape(14.dp))
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppIcon(icon, null, tint = colors.accent.copy(alpha = 0.75f), modifier = Modifier.size(16.dp))
        Text(value, color = colors.text, fontFamily = font, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            label.uppercase(),
            color = colors.muted,
            fontFamily = font,
            fontSize = 10.5.sp,
            letterSpacing = 0.4.sp,
        )
    }
}

// Bordered card wrapper — hourly/sun/air-quality/7-day sections on the main screen.
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    label: String? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val font = LocalAppFontFamily.current
    Column(
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(18.dp))
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(contentPadding),
    ) {
        if (label != null) {
            Text(
                label.uppercase(),
                color = colors.muted,
                fontFamily = font,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

// Shallow sunrise-to-sunset arc with an elapsed-daylight segment and a sun-position dot.
// The elapsed segment and the dot are derived from the same PathMeasure length so they
// can never drift apart the way independently-computed values would.
@Composable
fun SunArc(
    sunriseEpochSeconds: Long,
    sunsetEpochSeconds: Long,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val nowEpochSeconds = remember { System.currentTimeMillis() / 1000 }
    val t = if (sunsetEpochSeconds > sunriseEpochSeconds) {
        ((nowEpochSeconds - sunriseEpochSeconds).toFloat() / (sunsetEpochSeconds - sunriseEpochSeconds)).coerceIn(0f, 1f)
    } else 0f

    Canvas(modifier = modifier) {
        val margin = 4.dp.toPx()
        val start = Offset(margin, size.height - margin)
        val end = Offset(size.width - margin, size.height - margin)
        val control = Offset(size.width / 2f, -size.height * 0.4f)

        val path = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(control.x, control.y, end.x, end.y)
        }
        drawPath(path, color = colors.border, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        val measure = PathMeasure().apply { setPath(path, false) }
        val elapsedLength = measure.length * t
        val elapsed = Path()
        measure.getSegment(0f, elapsedLength, elapsed, true)
        drawPath(elapsed, color = colors.accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        val sunPosition = measure.getPosition(elapsedLength)
        drawCircle(color = colors.text, radius = 6.dp.toPx(), center = sunPosition)
    }
}

// Min-max range track — 7-day forecast row, filled segment scaled to the week's temp range.
@Composable
fun RangeBar(
    leftFraction: Float,
    widthFraction: Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    BoxWithConstraints(
        modifier = modifier
            .height(4.dp)
            .background(colors.surface2, RoundedCornerShape(2.dp)),
    ) {
        Box(
            modifier = Modifier
                .offset(x = maxWidth * leftFraction)
                .width(maxWidth * widthFraction)
                .fillMaxHeight()
                .background(colors.accent, RoundedCornerShape(2.dp)),
        )
    }
}

// Swipeable-pager position indicator — one dot per saved location, the active one widened and
// tinted accent. No Material3 PagerIndicator equivalent exists in this codebase, so this is a
// from-scratch primitive matching the rest of this file's plain Box/Row + CompositionLocal style.
@Composable
fun PageDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) return
    val colors = LocalAppColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val width by animateFloatAsState(
                targetValue = if (isActive) 18f else 6f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (isActive) colors.accent else colors.border,
                animationSpec = tween(150),
                label = "dotColor",
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
    }
}
