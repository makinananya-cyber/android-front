package com.hisaabwise.feature.learn.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWMotion
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.MinTouchTarget
import com.hisaabwise.core.designsystem.UnitTint
import com.hisaabwise.core.designsystem.components.HWIconButton
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** Font scale above which the zig-zag path is replaced by rows (iOS: accessibility Dynamic Type sizes). */
internal const val ACCESSIBILITY_FONT_SCALE = 1.6f

@Composable
internal fun isAccessibilityFontScale(): Boolean = LocalDensity.current.fontScale >= ACCESSIBILITY_FONT_SCALE

/** One node on the path; everything both the path and its row replacement draw. */
data class LessonNode(
    val id: String,
    val number: String,
    val title: String,
    val progressLabel: String,
    val icon: ImageVector,
    val state: LessonNodeState,
    val segments: Int,
    val filledSegments: Int,
    val isNext: Boolean,
)

@Composable
private fun lessonHint(state: LessonNodeState, isNext: Boolean): String = stringResource(
    when {
        state == LessonNodeState.Locked -> R.string.component_lesson_hint_locked
        isNext -> R.string.component_lesson_hint_next
        else -> R.string.component_lesson_hint_open
    },
)

/** A continuous decorative loop (breathe / bob / drift). Removed under reduce motion. */
@Composable
private fun ambientLoop(suppressed: Boolean, millis: Int): Float {
    if (suppressed) return 0f
    val transition = rememberInfiniteTransition(label = "ambient")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(millis, easing = HWMotion.EaseInOut), RepeatMode.Reverse),
        label = "ambientValue",
    )
    return t
}

// region Unit header

/** The design's `.unit-head` — the accent band that opens a unit, with the guide button. */
@Composable
fun UnitHeader(
    number: String,
    caption: String,
    title: String,
    subtitle: String,
    tint: UnitTint,
    isUnlocked: Boolean,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = tint.accent
    val ink = if (isUnlocked) HWColors.BrandInk else HWColors.Ink
    val reduceMotion = LocalReduceMotion.current
    val drift = ambientLoop(reduceMotion || !isUnlocked, 9000)
    val ground = if (isUnlocked) Brush.linearGradient(listOf(accent.base, accent.deep)) else Brush.linearGradient(listOf(HWColors.Locked, HWColors.Locked))
    Row(
        modifier
            .fillMaxWidth()
            .shadow(if (isUnlocked) 10.dp else 4.dp, HWRadius.Card, ambientColor = HWColors.Ink.copy(alpha = 0.18f), spotColor = HWColors.Ink.copy(alpha = 0.18f))
            .clip(HWRadius.Card)
            .background(ground)
            .drawBehind {
                if (isUnlocked) {
                    // `.unit-head::after` — the soft light in the band's top corner, drifting.
                    val diameter = 170.dp.toPx()
                    val rtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
                    val cx = if (rtl) -46.dp.toPx() + diameter / 2 else size.width + 46.dp.toPx() - diameter / 2
                    val cy = -92.dp.toPx() + diameter / 2 + drift * 12.dp.toPx()
                    val radius = diameter / 2 * (1f + 0.16f * drift)
                    drawCircle(
                        Brush.radialGradient(listOf(HWColors.BrandInk.copy(alpha = 0.3f), Color.Transparent), Offset(cx, cy), radius),
                        radius, Offset(cx, cy),
                    )
                }
            }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier
                .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                .clip(HWRadius.Medium)
                .background(HWColors.Surface)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Text(number, style = HWType.Subheading, color = if (isUnlocked) accent.deep else HWColors.Ink, modifier = Modifier.padding(horizontal = 8.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(caption.uppercase(), style = HWType.Micro.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp), color = ink)
            Text(title, style = HWType.Subheading, color = ink)
            Text(subtitle, style = HWType.Caption, color = ink)
        }
        HWIconButton(Icons.AutoMirrored.Outlined.MenuBook, stringResource(R.string.component_unit_guide), onOpenGuide)
    }
}

// endregion

// region Track

/**
 * The design's `.track` — the zig-zag path with dotted connectors. At accessibility font scales the
 * path is replaced by the same lessons as rows.
 */
@Composable
fun LessonTrack(
    tint: UnitTint,
    nodes: List<LessonNode>,
    startLabel: String,
    onSelect: (LessonNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isAccessibilityFontScale()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            nodes.forEach { node ->
                LessonRow(node.number, node.title, node.progressLabel, node.state, node.isNext, tint) { onSelect(node) }
            }
        }
        return
    }
    val accent = tint.accent
    val centres = remember { mutableStateMapOf<String, Offset>() }
    // Each centre is measured in root space, and so is the track: both are state, so the connectors redraw
    // whichever arrives last. The frames come back already mirrored under RTL, so the path follows for free.
    var origin by remember { mutableStateOf<Offset?>(null) }
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 34.dp, bottom = 18.dp)
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawBehind {
                val base = origin ?: return@drawBehind
                val reach = (NODE_DIAMETER / 2 + 8.dp).toPx()
                val dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 15.dp.toPx()))
                val stroke = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash)
                nodes.zipWithNext().forEach { (above, below) ->
                    val from = (centres[above.id] ?: return@forEach) - base
                    val to = (centres[below.id] ?: return@forEach) - base
                    val path = connector(from, to, reach)
                    val done = above.state == LessonNodeState.Completed
                    drawPath(path, if (done) accent.base.copy(alpha = 0.65f) else HWColors.SeparatorStrong, style = stroke)
                }
            },
        verticalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        nodes.forEachIndexed { index, node ->
            val leading = index % 2 == 0
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = if (node.isNext) 20.dp else 0.dp)
                    .padding(start = if (leading) 56.dp else 0.dp, end = if (leading) 0.dp else 56.dp),
                contentAlignment = if (leading) Alignment.TopStart else Alignment.TopEnd,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(contentAlignment = Alignment.TopCenter) {
                        LessonNodeButton(
                            node, tint,
                            Modifier.onGloballyPositioned { coords ->
                                val centre = coords.localToRoot(Offset(coords.size.width / 2f, coords.size.height / 2f))
                                if (centres[node.id] != centre) centres[node.id] = centre
                            },
                        ) { onSelect(node) }
                        if (node.isNext) StartFlag(startLabel, tint)
                    }
                    Text(
                        node.title,
                        style = HWType.Caption,
                        color = if (node.state == LessonNodeState.Locked) HWColors.InkTertiary else HWColors.InkSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 130.dp).clearAndSetSemantics {},
                    )
                }
            }
        }
    }
}

/** The design's bowed connector between two ring centres (iOS `HWLessonTrack.curves`). */
private fun connector(from: Offset, to: Offset, reach: Float): Path {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val distance = max(hypot(dx, dy), 1f)
    val ux = dx / distance
    val uy = dy / distance
    val side = if (dx >= 0) 1f else -1f
    val outbound = turned(ux, uy, -side * OUTBOUND_TURN)
    val inbound = turned(-ux, -uy, side * INBOUND_TURN)
    val start = Offset(from.x + outbound.x * reach, from.y + outbound.y * reach)
    val end = Offset(to.x + inbound.x * reach, to.y + inbound.y * reach)
    val nx = side * uy
    val ny = -side * ux
    val bow = distance * 0.25f
    val lead = distance * 0.3f
    return Path().apply {
        moveTo(start.x, start.y)
        cubicTo(
            start.x + outbound.x * lead + nx * bow, start.y + outbound.y * lead + ny * bow,
            end.x + inbound.x * lead + nx * bow, end.y + inbound.y * lead + ny * bow,
            end.x, end.y,
        )
    }
}

internal const val OUTBOUND_TURN = 1.0
internal const val INBOUND_TURN = 0.75

internal fun turned(x: Float, y: Float, angle: Double): Offset {
    val c = cos(angle).toFloat()
    val s = sin(angle).toFloat()
    return Offset(x * c - y * s, x * s + y * c)
}

/** `.start` — the hopping START flag above the cursor, hung out of a zero-height box. */
@Composable
private fun StartFlag(label: String, tint: UnitTint) {
    val accent = tint.accent
    val bob = ambientLoop(LocalReduceMotion.current, 1600)
    val density = LocalDensity.current
    Column(
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val gap = with(density) { 6.dp.roundToPx() }
                layout(placeable.width, 0) { placeable.place(0, -placeable.height - gap) }
            }
            .graphicsLayer { translationY = -bob * 5.dp.toPx() }
            .clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = HWType.Micro.copy(fontWeight = FontWeight.Black),
            color = accent.deep,
            modifier = Modifier
                .shadow(6.dp, HWRadius.Small, ambientColor = HWColors.Ink.copy(alpha = 0.18f), spotColor = HWColors.Ink.copy(alpha = 0.18f))
                .clip(HWRadius.Small)
                .background(HWColors.Surface)
                .border(2.dp, accent.base, HWRadius.Small)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
        Canvas(Modifier.size(12.dp, 6.dp)) {
            drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close() }, accent.base)
        }
    }
}

internal val NODE_DIAMETER = 78.dp
private val RING_WIDTH = 6.dp
private val FACE_DIAMETER = 54.dp
private val FACE_LIFT = 4.dp

/** One `.node` — the segmented progress ring and the face inside it. A locked node stays pressable. */
@Composable
fun LessonNodeButton(node: LessonNode, tint: UnitTint, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = tint.accent
    val locked = node.state == LessonNodeState.Locked
    val hint = lessonHint(node.state, node.isNext)
    val breathe = ambientLoop(LocalReduceMotion.current || node.state != LessonNodeState.Available, 2400)
    Box(
        modifier
            .size(NODE_DIAMETER)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = hint, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = node.title
                stateDescription = node.progressLabel
                role = Role.Button
                onClick(label = hint) { onClick(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val segments = max(node.segments, 1)
            val strokePx = RING_WIDTH.toPx()
            val inset = strokePx / 2
            val slot = 360f / segments
            val gap = if (segments > 1) 0.031f * 360f else 0f
            for (i in 0 until segments) {
                val lit = i < node.filledSegments && !locked
                drawArc(
                    color = if (lit) accent.base else HWColors.LockedSoft,
                    startAngle = -90f + i * slot + gap / 2,
                    sweepAngle = slot - gap,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        Box(Modifier.graphicsLayer { translationY = -breathe * 4.dp.toPx() }, contentAlignment = Alignment.Center) {
            Box(Modifier.offset(y = FACE_LIFT).size(FACE_DIAMETER).clip(CircleShape).background(if (locked) HWColors.Locked else accent.deep))
            Box(
                Modifier
                    .size(FACE_DIAMETER)
                    .clip(CircleShape)
                    .background(if (locked) Brush.linearGradient(listOf(HWColors.LockedSoft, HWColors.LockedSoft)) else Brush.linearGradient(listOf(accent.base, accent.deep))),
                contentAlignment = Alignment.Center,
            ) {
                val glyph = when (node.state) {
                    LessonNodeState.Completed -> statusGlyph(LessonNodeState.Completed)
                    LessonNodeState.Available -> node.icon
                    LessonNodeState.Locked -> statusGlyph(LessonNodeState.Locked)
                }
                Icon(glyph, null, tint = if (locked) HWColors.InkSecondary else HWColors.BrandInk, modifier = Modifier.size(26.dp))
            }
        }
    }
}

/** The one table of state glyphs, shared by the node and the row. */
fun statusGlyph(state: LessonNodeState): ImageVector = when (state) {
    LessonNodeState.Completed -> Icons.Filled.Check
    LessonNodeState.Available -> Icons.Filled.Star
    LessonNodeState.Locked -> Icons.Filled.Lock
}

// endregion

// region Row

/** The design's `.guide-item` — the guide sheet's row, and the path's replacement at large font scales. */
@Composable
fun LessonRow(
    number: String,
    title: String,
    detail: String,
    state: LessonNodeState,
    isNext: Boolean,
    tint: UnitTint,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = tint.accent
    val hint = lessonHint(state, isNext)
    val statusInk = when (state) {
        LessonNodeState.Completed -> HWColors.MeterReached
        LessonNodeState.Available -> accent.deep
        LessonNodeState.Locked -> HWColors.Locked
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .shadow(3.dp, HWRadius.Medium, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Medium)
            .background(HWColors.Surface)
            .border(1.dp, HWColors.Separator, HWRadius.Medium)
            .clickable(role = Role.Button, onClickLabel = hint, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = title
                stateDescription = detail
                role = Role.Button
                onClick(label = hint) { onClick(); true }
            }
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.sizeIn(minWidth = 32.dp, minHeight = 32.dp).clip(HWRadius.Small).background(accent.soft), contentAlignment = Alignment.Center) {
            Text(number, style = HWType.Caption.copy(fontWeight = FontWeight.Black), color = HWColors.Ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = HWType.BodyLarge.copy(fontWeight = FontWeight.Bold), color = if (state == LessonNodeState.Locked) HWColors.InkSecondary else HWColors.Ink)
            Text(detail, style = HWType.Caption, color = HWColors.InkTertiary)
        }
        Icon(statusGlyph(state), null, tint = statusInk, modifier = Modifier.size(20.dp))
    }
}

// endregion
