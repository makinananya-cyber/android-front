package com.hisaabwise.feature.learn.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

/** How an answer is drawn: untouched, chosen, or graded. */
enum class AnswerState { Offered, Chosen, Right, Wrong }

private fun AnswerState.verdict() = when (this) {
    AnswerState.Right -> verdictAccent(true)
    AnswerState.Wrong -> verdictAccent(false)
    else -> null
}

/** `.p-top` — close, progress through the lesson, and the hearts. Counts are read in words. */
@Composable
fun RunHeader(
    stepsDone: Int,
    stepCount: Int,
    heartsRemaining: Int,
    heartCount: Int,
    tint: UnitTint,
    progressLabel: String,
    heartsLabel: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val accent = tint.accent
    val target = if (stepCount > 0) (stepsDone.toFloat() / stepCount).coerceIn(0f, 1f) else 0f
    val fill by animateFloatAsState(target, if (reduceMotion) tween(0) else tween(HWMotion.MEDIUM, easing = HWMotion.EaseOut), label = "runFill")
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HWIconButton(Icons.Filled.Close, stringResource(R.string.component_lesson_close), onClose)
        Box(
            Modifier
                .weight(1f)
                .height(14.dp)
                .clip(HWRadius.Pill)
                .background(HWColors.LockedSoft)
                .clearAndSetSemantics { contentDescription = progressLabel },
        ) {
            if (fill > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .clip(HWRadius.Pill)
                        .background(Brush.horizontalGradient(listOf(accent.base, accent.deep))),
                )
            }
        }
        Row(Modifier.clearAndSetSemantics { contentDescription = heartsLabel }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(heartCount.coerceAtLeast(0)) { index ->
                val spent = index >= heartsRemaining
                val alpha by animateFloatAsState(if (spent) 0.32f else 1f, if (reduceMotion) tween(0) else tween(HWMotion.SHORT), label = "heart")
                Icon(
                    if (spent) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite,
                    null,
                    tint = UnitTint.Coral.accent.base,
                    modifier = Modifier.size(22.dp).alpha(alpha),
                )
            }
        }
    }
}

/** `.combo` — "COMBO ×3". Hidden from TalkBack; the screen announces it instead. */
@Composable
fun ComboBadge(text: String, isPresented: Boolean, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedVisibility(
        visible = isPresented,
        enter = if (reduceMotion) fadeIn() else fadeIn() + scaleIn(initialScale = 0.85f),
        exit = if (reduceMotion) fadeOut() else fadeOut() + scaleOut(targetScale = 0.85f),
        modifier = modifier.clearAndSetSemantics {},
    ) {
        Text(
            text.uppercase(),
            style = HWType.Micro.copy(fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
            color = UnitTint.Sun.accent.deep,
            modifier = Modifier
                .shadow(4.dp, HWRadius.Medium)
                .clip(HWRadius.Medium)
                .background(UnitTint.Sun.accent.base)
                .padding(horizontal = 13.dp, vertical = 5.dp),
        )
    }
}

/** What the run button is coloured by. */
sealed interface RunButtonTone {
    data class Lesson(val tint: UnitTint) : RunButtonTone
    data object Right : RunButtonTone
    data object Wrong : RunButtonTone
}

/** `.btn-go` / `.btn-ok` / `.btn-no` — the player's one primary control. */
@Composable
fun RunButton(title: String, tone: RunButtonTone, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val fill: Brush = when {
        !enabled -> SolidColor(HWColors.LockedSoft)
        tone is RunButtonTone.Lesson -> Brush.linearGradient(listOf(tone.tint.accent.base, tone.tint.accent.deep))
        tone is RunButtonTone.Right -> SolidColor(verdictAccent(true).base)
        else -> SolidColor(verdictAccent(false).base)
    }
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .then(if (enabled) Modifier.shadow(4.dp, HWRadius.Large) else Modifier)
            .clip(HWRadius.Large)
            .background(fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title.uppercase(),
            style = HWType.BodyLarge.copy(fontWeight = FontWeight.Black),
            color = if (enabled) HWColors.BrandInk else HWColors.InkTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/** `.kicker` — the small tinted chip saying what kind of step this is. */
@Composable
fun Kicker(text: String, icon: ImageVector, tint: UnitTint, modifier: Modifier = Modifier) {
    val accent = tint.accent
    Row(
        modifier.clip(HWRadius.Small).background(accent.soft).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, null, tint = accent.deep, modifier = Modifier.size(14.dp))
        Text(text.uppercase(), style = HWType.Micro.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = accent.deep)
    }
}

/** `.opt` — one answer. Stays a live element after grading so its state is still announced. */
@Composable
fun AnswerOption(text: String, state: AnswerState, allowsMany: Boolean, stateLabel: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    val verdict = state.verdict()
    val targetFill = verdict?.soft ?: if (state == AnswerState.Chosen) HWColors.Tint else HWColors.Surface
    val targetBorder = verdict?.base ?: if (state == AnswerState.Chosen) HWColors.Accent else HWColors.Separator
    val markFill = verdict?.base ?: if (state == AnswerState.Chosen) HWColors.Accent else Color.Transparent
    val spec = if (reduceMotion) tween<Color>(0) else tween(HWMotion.QUICK, easing = HWMotion.EaseOut)
    val fill by animateColorAsState(targetFill, spec, label = "optFill")
    val border by animateColorAsState(targetBorder, spec, label = "optBorder")
    val boxShape = if (allowsMany) HWRadius.Hairline else CircleShape
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(HWRadius.Large)
            .background(fill)
            .border(2.dp, border, HWRadius.Large)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = text
                if (stateLabel != null) stateDescription = stateLabel
                role = Role.Button
                onClick { onClick(); true }
            }
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(boxShape)
                .background(markFill)
                .border(2.dp, if (state == AnswerState.Offered) HWColors.SeparatorStrong else markFill, boxShape),
            contentAlignment = Alignment.Center,
        ) {
            if (state != AnswerState.Offered) {
                Icon(if (state == AnswerState.Wrong) Icons.Filled.Close else Icons.Filled.Check, null, tint = HWColors.BrandInk, modifier = Modifier.size(16.dp))
            }
        }
        Text(markdown(text), style = HWType.BodyLarge.copy(fontWeight = FontWeight.Normal), color = HWColors.Ink, modifier = Modifier.weight(1f))
    }
}

/** `.numwrap` — the box a figure is typed into, with the currency symbol beside it on money questions. */
@Composable
fun AnswerField(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    symbol: String?,
    state: AnswerState,
    hint: String?,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val verdict = state.verdict()
    val fill = verdict?.soft ?: HWColors.Surface
    val border = verdict?.base ?: if (focused) HWColors.AccentMuted else HWColors.Separator
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(HWRadius.Card)
                .background(fill)
                .border(2.dp, border, HWRadius.Card)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (symbol != null) {
                Text(symbol.trim(), style = HWType.Heading, color = HWColors.AccentDeep, modifier = Modifier.clearAndSetSemantics {})
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) {
                    Text(label, style = HWType.Heading.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary, modifier = Modifier.clearAndSetSemantics {})
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = isEditable,
                    singleLine = true,
                    textStyle = HWType.Heading.copy(color = HWColors.Ink),
                    cursorBrush = SolidColor(HWColors.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, autoCorrectEnabled = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = label },
                )
            }
        }
        if (hint != null) Text(hint, style = HWType.Caption, color = HWColors.InkTertiary)
    }
}

/** `.fb` — the verdict footer's note: headline, the right answer after a miss, and the explanation. */
@Composable
fun FeedbackNote(isCorrect: Boolean, headline: String, answerCaption: String?, answers: List<String>, explanation: String, modifier: Modifier = Modifier) {
    val accent = verdictAccent(isCorrect)
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(accent.base), contentAlignment = Alignment.Center) {
                Icon(if (isCorrect) Icons.Filled.Check else Icons.Filled.Close, null, tint = HWColors.BrandInk, modifier = Modifier.size(18.dp))
            }
            Text(headline, style = HWType.Subheading, color = accent.deep)
        }
        if (answerCaption != null && answers.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(HWRadius.Medium)
                    .background(HWColors.Surface)
                    .border(1.dp, HWColors.Separator, HWRadius.Medium)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(answerCaption, style = HWType.Caption, color = HWColors.InkSecondary)
                answers.forEach { Text(markdown(it), style = HWType.Caption.copy(fontWeight = FontWeight.Bold), color = accent.deep) }
            }
        }
        Text(markdown(explanation), style = HWType.Body, color = HWColors.InkSecondary)
    }
}

/** `.t-li` — one term-and-explanation row on a teaching page. */
@Composable
fun TeachingEntry(term: String, detail: String, tint: UnitTint, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .shadow(3.dp, HWRadius.Medium, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Medium)
            .background(HWColors.Surface)
            .border(1.dp, HWColors.Separator, HWRadius.Medium)
            .semantics(mergeDescendants = true) {}
            .padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(Modifier.padding(top = 7.dp).size(8.dp).clip(CircleShape).background(tint.accent.base))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(markdown(term), style = HWType.BodyLarge.copy(fontWeight = FontWeight.Black), color = HWColors.Ink)
            Text(markdown(detail), style = HWType.Body, color = HWColors.InkSecondary)
        }
    }
}

/** `.eg` — a worked example in the unit's wash. */
@Composable
fun WorkedExample(heading: String, text: String, tint: UnitTint, modifier: Modifier = Modifier) {
    val accent = tint.accent
    Column(
        modifier
            .fillMaxWidth()
            .clip(HWRadius.Large)
            .background(accent.soft)
            .semantics(mergeDescendants = true) {}
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Filled.AutoAwesome, null, tint = accent.deep, modifier = Modifier.size(14.dp))
            Text(markdown(heading.uppercase()), style = HWType.Micro.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp), color = accent.deep)
        }
        Text(markdown(text), style = HWType.Body, color = HWColors.InkSecondary)
    }
}

/** `.tip` — the dashed "Remember it:" box. */
@Composable
fun TeachingTip(caption: String, text: String, modifier: Modifier = Modifier) {
    val sun = UnitTint.Sun.accent
    Row(
        modifier
            .fillMaxWidth()
            .clip(HWRadius.Large)
            .background(sun.soft)
            .drawBehind {
                val w = 1.dp.toPx()
                drawRoundRect(
                    color = sun.base,
                    topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .semantics(mergeDescendants = true) {}
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(Icons.Outlined.Lightbulb, null, tint = sun.deep, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(caption, style = HWType.Caption.copy(fontWeight = FontWeight.Black), color = sun.deep)
            Text(markdown(text), style = HWType.Body, color = HWColors.Ink)
        }
    }
}
