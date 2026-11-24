package com.hisaabwise.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType

/** White rounded card with a hairline border and soft ink shadow — the basic block of every tab. */
@Composable
fun HWCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, HWRadius.Card, ambientColor = HWColors.Ink.copy(alpha = 0.10f), spotColor = HWColors.Ink.copy(alpha = 0.10f))
            .clip(HWRadius.Card)
            .background(HWColors.Surface)
            .border(1.dp, HWColors.Separator, HWRadius.Card)
            .padding(padding),
        content = content,
    )
}

/** The deep-navy hero card (Expenses summary, Reports hero, Account profile). */
@Composable
fun HWHeroCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, HWRadius.Card, ambientColor = HWColors.Ink.copy(alpha = 0.24f), spotColor = HWColors.Ink.copy(alpha = 0.24f))
            .clip(HWRadius.Card)
            .background(Brush.linearGradient(listOf(HWColors.AccentDeep, HWColors.StreakMid, HWColors.Accent)))
            .padding(padding),
        content = content,
    )
}

/** Tinted tray that groups rows (Expenses categories, Account settings). */
@Composable
fun HWTray(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(HWRadius.Card)
            .background(HWColors.AccentSoft.copy(alpha = 0.55f))
            .border(1.dp, HWColors.Separator, HWRadius.Card)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** "WHERE YOUR MONEY WENT ............ August" */
@Composable
fun HWEyebrowRow(
    eyebrow: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    color: Color = HWColors.AccentMuted,
    trailingColor: Color = HWColors.InkSecondary,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            eyebrow.uppercase(),
            style = HWType.Eyebrow,
            color = color,
            modifier = Modifier.weight(1f).semantics { heading() },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(trailing, style = HWType.Caption, color = trailingColor)
        }
    }
}

/**
 * Tab-root header: the mark, a small "HISAABWISE" eyebrow and the screen title.
 * [actions] sits at the end (Learn's stat chips, etc).
 */
@Composable
fun HWTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = stringResource(R.string.app_name),
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HWMark(48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) Text(eyebrow.uppercase(), style = HWType.Eyebrow, color = HWColors.AccentMuted)
            Text(title, style = HWType.Title, color = HWColors.Ink, modifier = Modifier.semantics { heading() })
        }
        actions()
    }
}
