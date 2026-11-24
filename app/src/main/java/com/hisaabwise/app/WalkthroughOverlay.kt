package com.hisaabwise.app

import android.annotation.SuppressLint
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWButtonStyle
import com.hisaabwise.core.walkthrough.WalkthroughModel

/**
 * The guided tour: a navy scrim with a punched-out spotlight and a pulsing ring around the
 * current target, plus a card with Back / Next / Skip. A step whose target is not on screen
 * is drawn centred. Port of iOS `WalkthroughOverlay`.
 */
@Composable
fun WalkthroughOverlay(model: WalkthroughModel, state: WalkthroughModel.State) {
    val step = model.current(state) ?: return
    val firstRun = step.whenPresent != null && state.rects.containsKey(step.whenPresent)
    val targetKey = if (step.whenPresent != null && state.rects.containsKey(step.whenPresent)) step.instead else step.target
    val rect: Rect? = targetKey?.let { state.rects[it] }
    val reduceMotion = LocalReduceMotion.current
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, if (reduceMotion) 0f else 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse",
    )

    val title = stepString(step.slug, "title", firstRun && (step.slug == "spend" || step.slug == "add"))
    val body = stepString(step.slug, "body", firstRun && (step.slug == "spend" || step.slug == "add"))
    val isFirst = state.index == 0
    val isLast = state.index == model.steps.lastIndex
    val tourLabel = stringResource(R.string.walkthrough_a11y_label)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } } // blocks the screen underneath
            .semantics { paneTitle = tourLabel },
    ) {
        val density = LocalDensity.current
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(HWColors.BrandBackgroundDeep.copy(alpha = 0.72f))
            if (rect != null) {
                val pad = 8.dp.toPx()
                val r = Rect(rect.left - pad, rect.top - pad, rect.right + pad, rect.bottom + pad)
                drawRoundRect(
                    color = HWColors.Surface, topLeft = r.topLeft, size = r.size,
                    cornerRadius = CornerRadius(22.dp.toPx()), blendMode = BlendMode.Clear,
                )
                val grow = 6.dp.toPx() * pulse
                drawRoundRect(
                    color = HWColors.BrandInkAccent.copy(alpha = 0.9f - 0.5f * pulse),
                    topLeft = Offset(r.left - grow, r.top - grow), size = Size(r.width + grow * 2, r.height + grow * 2),
                    cornerRadius = CornerRadius(24.dp.toPx() + grow), style = Stroke(2.dp.toPx()),
                )
            }
        }

        // Card below the target if it sits in the top half, above it otherwise; centred with no target.
        val heightPx = with(density) { maxHeight.toPx() }
        val cardModifier = when {
            rect == null -> Modifier.align(Alignment.Center)
            rect.center.y < heightPx / 2 -> Modifier.align(Alignment.TopCenter).offset(y = with(density) { (rect.bottom + 24.dp.toPx()).toDp() })
            else -> Modifier.align(Alignment.BottomCenter).padding(bottom = with(density) { (heightPx - rect.top + 24.dp.toPx()).toDp() })
        }
        Column(
            cardModifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(HWRadius.Card)
                .background(HWColors.Surface)
                .padding(20.dp)
                .semantics(mergeDescendants = false) { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.walkthrough_count, (state.index + 1).toString(), model.steps.size.toString()),
                style = HWType.Eyebrow, color = HWColors.AccentMuted,
            )
            Text(title, style = HWType.Heading, color = HWColors.Ink, modifier = Modifier.semantics { contentDescription = title })
            Text(body, style = HWType.Body, color = HWColors.InkSecondary)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isFirst) HWButton(stringResource(R.string.walkthrough_back), model::back, Modifier.weight(1f), style = HWButtonStyle.Secondary)
                HWButton(
                    stringResource(
                        when {
                            isFirst -> R.string.walkthrough_start
                            isLast -> R.string.walkthrough_finish
                            else -> R.string.walkthrough_next
                        },
                    ),
                    model::next, Modifier.weight(1f),
                )
            }
            if (!isLast) HWButton(stringResource(R.string.walkthrough_skip), model::skip, style = HWButtonStyle.Ghost)
        }
    }
}

/** `walkthrough_<slug>_<part>`, or the `_firstrun_` variant on a first-run month. */
@SuppressLint("DiscouragedApi", "LocalContextGetResourceValueCall")
@Composable
private fun stepString(slug: String, part: String, firstRun: Boolean): String {
    val context = LocalContext.current
    val res = context.resources
    fun id(name: String) = res.getIdentifier(name, "string", context.packageName)
    val preferred = if (firstRun) id("walkthrough_${slug}_firstrun_$part") else 0
    val resolved = if (preferred != 0) preferred else id("walkthrough_${slug}_$part")
    return if (resolved != 0) res.getString(resolved) else ""
}
