package com.hisaabwise.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.LoadState
import kotlinx.coroutines.delay

/** Maps a server code to the copy the user sees. The server's own `message` is never shown. */
object ErrorCopy {
    fun message(code: ErrorCode): Int = when (code) {
        ErrorCode.MalformedResponse -> R.string.state_failed_malformedResponse
        ErrorCode.RateLimited -> R.string.state_failed_rateLimited
        ErrorCode.MonthClosed -> R.string.state_failed_monthClosed
        else -> R.string.state_failed_generic
    }
}

/**
 * Renders any [LoadState]: spinner, offline and failed (with retry), empty, or [content].
 * Port of iOS `StateView` / `ScreenChrome`. Every tab screen goes through this.
 */
@Composable
fun <T> StateView(
    state: LoadState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    empty: @Composable () -> Unit = {},
    content: @Composable (T) -> Unit,
) {
    when (state) {
        LoadState.Loading -> Centered(modifier) {
            CircularProgressIndicator(color = HWColors.Accent)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.state_loading), style = HWType.Body, color = HWColors.InkSecondary)
        }
        LoadState.Offline -> Centered(modifier) {
            Icon(Icons.Outlined.CloudOff, null, tint = HWColors.AccentMuted)
            Spacer(Modifier.height(12.dp))
            Message(stringResource(R.string.state_offline))
            Spacer(Modifier.height(16.dp))
            HWButton(stringResource(R.string.state_retry), onRetry, style = HWButtonStyle.Secondary, fillWidth = false)
        }
        is LoadState.Failed -> Centered(modifier) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = HWColors.Danger)
            Spacer(Modifier.height(12.dp))
            Message(stringResource(ErrorCopy.message(state.code)))
            Spacer(Modifier.height(16.dp))
            HWButton(stringResource(R.string.state_retry), onRetry, style = HWButtonStyle.Secondary, fillWidth = false)
        }
        LoadState.Empty -> empty()
        is LoadState.Loaded -> content(state.value)
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

@Composable
private fun Message(text: String) {
    Text(
        text, style = HWType.Body, color = HWColors.InkSecondary, textAlign = TextAlign.Center,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/**
 * A transient confirmation ("Added", "Bills updated"). Shown while [message] is non-null and
 * auto-dismissed after 2.4 s, like iOS `.hwToast`.
 */
@Composable
fun BoxScope.HWToast(message: String?, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2400)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp),
    ) {
        Row(
            Modifier
                .clip(HWRadius.Pill)
                .background(HWColors.AccentDeep)
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message.orEmpty(), style = HWType.Caption, color = HWColors.Surface)
        }
    }
}

/** An inline notice (unverified email, refused write). */
@Composable
fun HWBanner(text: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(HWRadius.Medium)
            .background(if (isError) HWColors.DangerSoft else HWColors.AccentSoft)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, null, tint = if (isError) HWColors.Danger else HWColors.Accent)
        Spacer(Modifier.width(10.dp))
        Text(text, style = HWType.Caption, color = if (isError) HWColors.Danger else HWColors.Ink)
    }
}

/** Back button + centred title for pushed pages (month report, category, account detail). */
@Composable
fun HWBackBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backDescription: String = stringResource(R.string.walkthrough_back),
    trailing: @Composable () -> Unit = {},
) {
    Box(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        HWIconButton(Icons.AutoMirrored.Filled.ArrowBack, backDescription, onBack, Modifier.align(Alignment.CenterStart))
        Text(
            title,
            style = HWType.BodyLarge,
            color = if (LocalAppearance.current == Appearance.Brand) HWColors.BrandInk else HWColors.Ink,
            modifier = Modifier.align(Alignment.Center).semantics { heading() },
        )
        Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

/** Standard scrolling page padding: 20 dp sides, room for the tab bar at the bottom. */
val PageHorizontalPadding = 20.dp

@Composable
fun BottomSpacer() = Spacer(Modifier.navigationBarsPadding().height(110.dp))
