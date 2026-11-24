package com.hisaabwise.feature.learn.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.hisaabwise.core.designsystem.UnitAccent
import com.hisaabwise.core.designsystem.UnitTint
import com.hisaabwise.feature.learn.LessonIcon
import com.hisaabwise.feature.learn.LessonState
import com.hisaabwise.feature.learn.UnitAccentName

/** Payload accent name → design-system tint (iOS `LearnView.tint`). */
fun UnitAccentName.tint(): UnitTint = when (this) {
    UnitAccentName.Sun -> UnitTint.Sun
    UnitAccentName.Mint -> UnitTint.Mint
    UnitAccentName.Coral -> UnitTint.Coral
    UnitAccentName.Sky -> UnitTint.Sky
    UnitAccentName.Violet -> UnitTint.Violet
}

/** Mint means right, coral means wrong — for the whole player (iOS `verdict(isCorrect:)`). */
fun verdictAccent(isCorrect: Boolean): UnitAccent = if (isCorrect) UnitTint.Mint.accent else UnitTint.Coral.accent

/** What a lesson node looks like. */
enum class LessonNodeState { Completed, Available, Locked }

fun LessonState.nodeState(): LessonNodeState = when (this) {
    LessonState.Completed -> LessonNodeState.Completed
    LessonState.Available -> LessonNodeState.Available
    LessonState.Locked -> LessonNodeState.Locked
}

/** The design's fifteen glyphs, as Material icons (readings of the iOS SF Symbols). */
fun LessonIcon.vector(): ImageVector = when (this) {
    LessonIcon.Wallet -> Icons.Filled.Payments
    LessonIcon.Clock -> Icons.Filled.Schedule
    LessonIcon.Split -> Icons.Filled.Shuffle
    LessonIcon.Flow -> Icons.Filled.SwapVert
    LessonIcon.Pie -> Icons.Filled.PieChart
    LessonIcon.Shield -> Icons.Filled.VerifiedUser
    LessonIcon.Bank -> Icons.Filled.AccountBalance
    LessonIcon.Gauge -> Icons.Filled.Speed
    LessonIcon.Scales -> Icons.Filled.Exposure
    LessonIcon.Steps -> Icons.Filled.Stairs
    LessonIcon.Umbrella -> Icons.Filled.Umbrella
    LessonIcon.Lock -> Icons.Filled.Security
    LessonIcon.Spark -> Icons.Filled.AutoAwesome
    LessonIcon.Layers -> Icons.Filled.Layers
    LessonIcon.Grid -> Icons.Filled.GridView
    LessonIcon.Book -> Icons.AutoMirrored.Filled.MenuBook
}

/** Inline markdown (`**bold**`, `*italic*` / `_italic_`) — what iOS `HWMarkdown` interprets in lesson content. */
fun markdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    var bold = false
    var italic = false
    val run = StringBuilder()
    fun flush() {
        if (run.isEmpty()) return
        val style = SpanStyle(
            fontWeight = if (bold) FontWeight.ExtraBold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
        )
        withStyle(style) { append(run.toString()) }
        run.clear()
    }
    while (i < text.length) {
        val c = text[i]
        when {
            c == '\\' && i + 1 < text.length -> { run.append(text[i + 1]); i += 2; continue }
            c == '*' && i + 1 < text.length && text[i + 1] == '*' && hasCloser(text, i + 2, "**", bold) -> {
                flush(); bold = !bold; i += 2; continue
            }
            (c == '*' || c == '_') && hasCloser(text, i + 1, c.toString(), italic) && !(c == '_' && i > 0 && text[i - 1].isLetterOrDigit()) -> {
                flush(); italic = !italic; i += 1; continue
            }
            else -> run.append(c)
        }
        i++
    }
    flush()
}

/** An opening marker needs a matching closer later; a closing marker always closes. */
private fun hasCloser(text: String, from: Int, marker: String, isOpen: Boolean): Boolean =
    isOpen || text.indexOf(marker, from) >= 0

/** Haptic on a verdict or a landed completion (iOS `.sensoryFeedback(.success / .error)`). */
fun View.verdictHaptic(success: Boolean) {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (success) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
    } else {
        if (success) HapticFeedbackConstants.VIRTUAL_KEY else HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}

/** One-off TalkBack announcement (iOS `HWAnnouncement.post`). */
@Suppress("DEPRECATION")
fun View.announce(text: String) = announceForAccessibility(text)
