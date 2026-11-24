package com.hisaabwise.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.BottomSpacer
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWButtonStyle
import com.hisaabwise.core.designsystem.components.HWTopBar
import com.hisaabwise.core.designsystem.components.PageHorizontalPadding
import com.hisaabwise.core.designsystem.components.StateView
import com.hisaabwise.core.designsystem.components.SurfaceGround
import com.hisaabwise.core.walkthrough.WalkthroughTarget
import com.hisaabwise.core.walkthrough.walkthroughTarget
import com.hisaabwise.feature.account.components.AccountProfileHeader
import com.hisaabwise.feature.account.components.AccountRefusalNote
import com.hisaabwise.feature.account.components.AccountRow
import com.hisaabwise.feature.account.components.AccountSettingsTray

/**
 * Account root — the settings list: profile card, the server's rows, the tour, contact, log out
 * and delete. Stateless; port of iOS `AccountView` / `AccountPage`.
 */
@Composable
fun AccountRootScreen(
    state: AccountState,
    onIntent: (AccountIntent) -> Unit,
    onOpenSection: (AccountScreen.Section) -> Unit,
    onReplayTour: () -> Unit,
    contactEmail: String,
) {
    SurfaceGround {
        Column(Modifier.fillMaxSize()) {
            HWTopBar(stringResource(R.string.account_title), eyebrow = stringResource(R.string.account_eyebrow))
            StateView(state.content, onRetry = { onIntent(AccountIntent.Load) }) { screen ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = PageHorizontalPadding)
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AccountProfileHeader(
                        initials = screen.profile.initials,
                        name = screen.profile.displayName,
                        email = screen.profile.email,
                        summary = screen.profile.summaryLabel,
                    )
                    SettingsRows(screen, onOpenSection, onReplayTour, contactEmail, onContact = { onIntent(AccountIntent.ContactSupport) })
                    LogoutControl(onConfirm = { onIntent(AccountIntent.SignOut) })
                    DeleteAccountControl(state, onConfirm = { onIntent(AccountIntent.DeleteAccount) })
                    BottomSpacer()
                }
            }
        }
    }
}

@Composable
private fun SettingsRows(
    screen: AccountScreen,
    onOpenSection: (AccountScreen.Section) -> Unit,
    onReplayTour: () -> Unit,
    contactEmail: String,
    onContact: () -> Unit,
) {
    AccountSettingsTray(stringResource(R.string.account_tray_caption)) {
        val mask = stringResource(R.string.account_row_passwordMask)
        screen.drawnRows.forEach { row ->
            val section = row.section ?: return@forEach
            AccountRow(
                icon = glyph(section),
                name = row.name,
                subtitle = row.hint,
                value = if (section.masksValue) mask else row.value,
                onClick = { onOpenSection(section) },
                modifier = Modifier.walkthroughTarget(walkthroughTarget(section)),
            )
        }
        AccountRow(
            icon = Icons.Outlined.Map,
            name = stringResource(R.string.walkthrough_replay),
            subtitle = stringResource(R.string.walkthrough_replay_subtitle),
            showsDisclosure = false,
            onClick = onReplayTour,
        )
        AccountRow(
            icon = Icons.Outlined.Mail,
            name = contactEmail,
            subtitle = stringResource(R.string.account_contact),
            showsDisclosure = false,
            onClick = onContact,
        )
    }
}

private fun glyph(section: AccountScreen.Section): ImageVector = when (section) {
    AccountScreen.Section.Personal -> Icons.Outlined.Person
    AccountScreen.Section.Language -> Icons.Outlined.Language
    AccountScreen.Section.Currency -> Icons.Outlined.Payments
    AccountScreen.Section.Password -> Icons.Outlined.Lock
}

private fun walkthroughTarget(section: AccountScreen.Section): WalkthroughTarget = when (section) {
    AccountScreen.Section.Personal -> WalkthroughTarget.AccountPersonal
    AccountScreen.Section.Language -> WalkthroughTarget.AccountLanguage
    AccountScreen.Section.Currency -> WalkthroughTarget.AccountCurrency
    AccountScreen.Section.Password -> WalkthroughTarget.AccountPassword
}

/** The only user-initiated way out, with the question asked first. Port of iOS `LogoutControl`. */
@Composable
private fun LogoutControl(onConfirm: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    HWButton(
        stringResource(R.string.shell_logout_action),
        onClick = { confirming = true },
        style = HWButtonStyle.Destructive,
        icon = Icons.AutoMirrored.Outlined.Logout,
    )
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.shell_logout_confirm_title)) },
            text = { Text(stringResource(R.string.shell_logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { confirming = false; onConfirm() }) {
                    Text(stringResource(R.string.shell_logout_action), color = HWColors.Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(R.string.shell_logout_confirm_cancel), color = HWColors.Accent)
                }
            },
            containerColor = HWColors.Surface,
            titleContentColor = HWColors.Ink,
            textContentColor = HWColors.InkSecondary,
        )
    }
}

/** Delete the account (soft delete, 30-day grace), confirmed first. App Store 5.1.1(v) / Play policy. */
@Composable
private fun DeleteAccountControl(state: AccountState, onConfirm: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HWButton(
            stringResource(R.string.account_delete_action),
            onClick = { confirming = true },
            style = HWButtonStyle.Destructive,
            icon = Icons.Outlined.Delete,
            isLoading = state.isDeletingAccount,
        )
        Text(stringResource(R.string.account_delete_blurb), style = HWType.Micro.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary)
        AccountRefusalNote(state.refusal(Refusal.Subject.DeleteAccount))
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.account_delete_confirm_title)) },
            text = { Text(stringResource(R.string.account_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { confirming = false; onConfirm() }) {
                    Text(stringResource(R.string.account_delete_confirm_action), color = HWColors.Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) {
                    Text(stringResource(R.string.account_delete_confirm_cancel), color = HWColors.Accent)
                }
            },
            containerColor = HWColors.Surface,
            titleContentColor = HWColors.Ink,
            textContentColor = HWColors.InkSecondary,
        )
    }
}

/** Copy for a landed write's toast. */
fun noticeCopy(notice: AccountNotice): Int = when (notice) {
    AccountNotice.PersonalUpdated -> R.string.account_notice_personalUpdated
    AccountNotice.CurrencyChanged -> R.string.account_notice_currencyChanged
    AccountNotice.LanguageChanged -> R.string.account_notice_languageChanged
    AccountNotice.PasswordChanged -> R.string.account_notice_passwordChanged
}
