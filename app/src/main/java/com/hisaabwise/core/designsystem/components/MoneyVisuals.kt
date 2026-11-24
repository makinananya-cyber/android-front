package com.hisaabwise.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWMotion
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.VerdictTint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

/** One slice/row of spending. [share] is 0…1 from the server; [slot] picks the category colour. */
data class SpendSlice(
    val id: String,
    val name: String,
    val amountDisplay: String,
    val share: Double,
    val shareLabel: String,
    val slot: Int,
)

/**
 * The spending donut. Tap a slice to isolate it (the others fade); tap it again to clear.
 * An empty [slices] list (or all zero) draws the grey first-run ring.
 */
@Composable
fun HWDonut(
    slices: List<SpendSlice>,
    isolatedId: String?,
    onIsolate: (String?) -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 150.dp,
    thickness: Dp = 26.dp,
    center: @Composable () -> Unit = {},
) {
    val total = slices.sumOf { it.share }.takeIf { it > 0 } ?: 0.0
    val gapDegrees = if (slices.count { it.share > 0 } > 1) 3f else 0f
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(diameter)
                .pointerInput(slices, isolatedId) {
                    detectTapOnRing(slices, total) { id -> onIsolate(if (id == isolatedId) null else id) }
                },
        ) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            if (total <= 0.0) {
                drawArc(HWColors.EmptyTrack, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                return@Canvas
            }
            var start = -90f
            slices.filter { it.share > 0 }.forEach { slice ->
                val sweep = (slice.share / total * 360f).toFloat()
                val alpha = if (isolatedId == null || isolatedId == slice.id) 1f else 0.25f
                drawArc(
                    HWColors.categorySlot(slice.slot).copy(alpha = alpha),
                    start + gapDegrees / 2, (sweep - gapDegrees).coerceAtLeast(0.5f), false,
                    Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        center()
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapOnRing(
    slices: List<SpendSlice>,
    total: Double,
    onTap: (String) -> Unit,
) {
    detectTapGestures { offset ->
        if (total <= 0.0) return@detectTapGestures
        val cx = size.width / 2f
        val cy = size.height / 2f
        val dx = offset.x - cx
        val dy = offset.y - cy
        if (hypot(dx, dy) < size.width * 0.25f) return@detectTapGestures
        var angle = (atan2(dy, dx) * 180 / PI + 90).toFloat()
        if (angle < 0) angle += 360f
        var start = 0f
        for (slice in slices.filter { it.share > 0 }) {
            val sweep = (slice.share / total * 360f).toFloat()
            if (angle >= start && angle < start + sweep) {
                onTap(slice.id); return@detectTapGestures
            }
            start += sweep
        }
    }
}

/** The key beside the donut: dot, name, share. Tapping a row isolates it too. */
@Composable
fun HWCategoryKey(
    slices: List<SpendSlice>,
    isolatedId: String?,
    onIsolate: (String?) -> Unit,
    modifier: Modifier = Modifier,
    showAmounts: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        slices.forEach { slice ->
            val dim = isolatedId != null && isolatedId != slice.id
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clip(HWRadius.Small)
                    .clickable(role = Role.Button) { onIsolate(if (isolatedId == slice.id) null else slice.id) }
                    .semantics(mergeDescendants = true) { selected = isolatedId == slice.id }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(HWRadius.Hairline).background(HWColors.categorySlot(slice.slot).copy(alpha = if (dim) 0.3f else 1f)))
                Spacer(Modifier.width(10.dp))
                Text(slice.name, style = HWType.BodyLarge.copy(fontWeight = FontWeight.SemiBold), color = HWColors.Ink.copy(alpha = if (dim) 0.4f else 1f), modifier = Modifier.weight(1f))
                if (showAmounts) {
                    Text(slice.amountDisplay, style = HWType.Caption, color = HWColors.InkSecondary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(slice.shareLabel, style = HWType.BodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.Ink.copy(alpha = if (dim) 0.4f else 1f))
            }
        }
    }
}

/** hit / near / miss (or met / onTrack / low) pill: "120% of goal". */
@Composable
fun HWVerdictBadge(label: String, tint: VerdictTint, modifier: Modifier = Modifier) {
    Text(
        label,
        style = HWType.Caption.copy(fontWeight = FontWeight.Bold),
        color = tint.ink,
        modifier = modifier
            .clip(HWRadius.Small)
            .background(tint.soft)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * The savings meter: a red→green gradient track with a sliding pin at [position] (0…1, from the
 * server), the saved label riding above the pin, zero and goal labels below.
 */
@Composable
fun HWSavingsMeter(
    position: Double,
    savedLabel: String,
    zeroLabel: String,
    goalLabel: String,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val animated by animateFloatAsState(
        position.coerceIn(0.0, 1.0).toFloat(),
        animationSpec = if (reduceMotion) tween(0) else tween(HWMotion.LONG, easing = HWMotion.EaseOut),
        label = "meter",
    )
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "$savedLabel. $zeroLabel – $goalLabel" }) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val trackWidth = maxWidth
            val density = LocalDensity.current
            // Saved label pill, clamped so it never leaves the card.
            Box(Modifier.fillMaxWidth().height(34.dp)) {
                val pillOffset = (trackWidth * animated) - 70.dp
                Text(
                    savedLabel,
                    style = HWType.Caption.copy(fontWeight = FontWeight.Bold),
                    color = HWColors.Surface,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = pillOffset.coerceIn(0.dp, (trackWidth - 140.dp).coerceAtLeast(0.dp)))
                        .wrapContentWidth()
                        .clip(HWRadius.Small)
                        .background(HWColors.AccentDeep)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // The track reads from zero to goal in the reading direction, so the gradient mirrors in RTL with the pin.
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        BoxWithConstraints(Modifier.fillMaxWidth().height(30.dp)) {
            val w = maxWidth
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(HWRadius.Pill)
                    .background(Brush.horizontalGradient(if (rtl) HWColors.meterStops.reversed() else HWColors.meterStops)),
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (w * animated - 4.dp).coerceIn(0.dp, w - 8.dp))
                    .width(8.dp)
                    .fillMaxHeight()
                    .clip(HWRadius.Pill)
                    .background(HWColors.AccentDeep),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(zeroLabel.uppercase(), style = HWType.Eyebrow, color = HWColors.InkSecondary, modifier = Modifier.weight(1f))
            Text(goalLabel.uppercase(), style = HWType.Eyebrow, color = HWColors.Ink)
        }
    }
}

/**
 * A progress bar for the wants allowance. [fill] 0…1 from the server; [isOver] turns it red.
 * [onHero] draws it light-on-navy for the Expenses summary card.
 */
@Composable
fun HWBudgetBar(
    fill: Double,
    isOver: Boolean,
    modifier: Modifier = Modifier,
    onHero: Boolean = false,
    height: Dp = 12.dp,
) {
    val reduceMotion = LocalReduceMotion.current
    val animated by animateFloatAsState(
        fill.coerceIn(0.0, 1.0).toFloat(),
        animationSpec = if (reduceMotion) tween(0) else tween(HWMotion.LONG, easing = HWMotion.EaseOut),
        label = "budget",
    )
    val track = if (onHero) HWColors.BrandSurface.copy(alpha = 0.25f) else HWColors.EmptyTrack
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    fun directed(colors: List<Color>) = Brush.horizontalGradient(if (rtl) colors.reversed() else colors)
    val bar: Brush = when {
        isOver -> directed(listOf(HWColors.MeterLow, HWColors.Danger))
        onHero -> directed(listOf(HWColors.BrandInkAccent, HWColors.AccentMuted))
        else -> directed(listOf(HWColors.Accent, HWColors.AccentMuted))
    }
    Box(modifier.fillMaxWidth().height(height).clip(HWRadius.Pill).background(track)) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(HWRadius.Pill).background(bar))
    }
}

/** One segment of a proportion bar. */
data class BarSegment(val share: Double, val color: Color)

/** A horizontal stacked bar (month proportion bars, the needs/wants/saved/surplus split). */
@Composable
fun HWSplitBar(segments: List<BarSegment>, modifier: Modifier = Modifier, height: Dp = 10.dp, gap: Dp = 3.dp) {
    val visible = segments.filter { it.share > 0 }
    if (visible.isEmpty()) {
        Box(modifier.fillMaxWidth().height(height).clip(HWRadius.Pill).background(HWColors.EmptyTrack))
        return
    }
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(gap)) {
        visible.forEach { segment ->
            Box(Modifier.weight(segment.share.toFloat().coerceAtLeast(0.001f)).fillMaxHeight().clip(HWRadius.Pill).background(segment.color))
        }
    }
}

/** A stat tile ("6 MONTHS", "+60 XP EARNED"). */
@Composable
fun HWStatTile(value: String, label: String, modifier: Modifier = Modifier, onHero: Boolean = false, valueColor: Color? = null) {
    Column(
        modifier
            .clip(HWRadius.Large)
            .background(if (onHero) Color.White.copy(alpha = 0.08f) else HWColors.Surface)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(value, style = HWType.Subheading.copy(fontWeight = FontWeight.ExtraBold), color = valueColor ?: if (onHero) HWColors.BrandInk else HWColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(label.uppercase(), style = HWType.Eyebrow, color = if (onHero) HWColors.BrandInkSecondary else HWColors.AccentMuted)
    }
}

/** A small rounded chip with optional leading content (streak 🔥 1, XP ⭐ 60). */
@Composable
fun HWChip(text: String, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null, contentDescription: String? = null) {
    Row(
        modifier
            .clip(HWRadius.Large)
            .background(HWColors.Surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { if (contentDescription != null) this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        if (leading != null) Spacer(Modifier.width(6.dp))
        Text(text, style = HWType.BodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.Ink)
    }
}
