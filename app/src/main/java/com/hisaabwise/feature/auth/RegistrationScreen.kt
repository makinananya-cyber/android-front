package com.hisaabwise.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabwise.R
import com.hisaabwise.app.LocalAppContainer
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWMotion
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWButtonStyle
import com.hisaabwise.core.designsystem.components.HWIconButton
import com.hisaabwise.core.designsystem.components.HWMark
import com.hisaabwise.core.designsystem.components.HWStepBar
import com.hisaabwise.core.model.ContentResource
import com.hisaabwise.core.model.CountryList
import com.hisaabwise.core.model.CurrencyList
import com.hisaabwise.core.model.SecurityQuestionList
import com.hisaabwise.feature.auth.components.AuthHairline
import com.hisaabwise.feature.auth.components.AuthHead
import com.hisaabwise.feature.auth.components.AuthNote
import com.hisaabwise.feature.auth.components.AuthPage
import com.hisaabwise.feature.auth.components.FlexSpace
import com.hisaabwise.feature.auth.components.authEnters
import com.hisaabwise.feature.auth.registration.RegistrationDetailsStep
import com.hisaabwise.feature.auth.registration.RegistrationGoalStep
import com.hisaabwise.feature.auth.registration.RegistrationMoneyStep

@Composable
fun RegistrationRoute(onBack: () -> Unit, onSignInInstead: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel {
        RegistrationViewModel(
            lists = object : RegistrationLists {
                override suspend fun countries() = container.content.load<CountryList>(ContentResource.Countries)
                override suspend fun currencies() = container.content.load<CurrencyList>(ContentResource.Currencies)
                override suspend fun questions() = container.content.load<SecurityQuestionList>(ContentResource.SecurityQuestions)
            },
            register = container.session::register,
            legal = container.legal,
            language = { container.language.acceptLanguage },
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    val view = LocalView.current
    val context = LocalContext.current
    LaunchedEffect(vm) {
        vm.effects.collect {
            when (it) {
                is RegistrationEffect.OpenUrl -> runCatching { uri.openUri(it.url) }
                // A step change or a refusal changes nothing TalkBack would otherwise notice.
                is RegistrationEffect.Announce -> announce(view, context.getString(it.text))
                RegistrationEffect.GoToSignIn -> onSignInInstead()
                RegistrationEffect.GoBack -> onBack()
            }
        }
    }
    RegistrationScreen(state, vm::dispatch)
}

/**
 * Port of iOS `RegistrationView`: one screen whose step is state, so a back gesture can never
 * discard a step's typing. System back on steps 2–3 goes back a step; on step 1 it leaves.
 */
@Composable
fun RegistrationScreen(state: RegistrationState, onIntent: (RegistrationIntent) -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    BackHandler(enabled = state.step != RegistrationStep.One) { onIntent(RegistrationIntent.GoBack) }

    AuthPage(onBack = { onIntent(RegistrationIntent.Leave) }) {
        HWMark(34.dp, Modifier.authEnters(0))
        FlexSpace(20.dp)
        AuthHead(
            stringResource(state.step.titleFirst),
            stringResource(state.step.titleSecond),
            stringResource(state.step.subtitle),
            Modifier.authEnters(1),
        )
        FlexSpace(22.dp)
        StepHeader(state.step, onBack = { onIntent(RegistrationIntent.GoBack) }, modifier = Modifier.authEnters(2))
        FlexSpace(18.dp)

        if (state.hasReferenceLists) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    if (reduceMotion) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    else (fadeIn(tween(HWMotion.SHORT)) + slideInHorizontally(tween(HWMotion.SHORT, easing = HWMotion.EaseOut)) { it / 12 }) togetherWith
                        fadeOut(tween(HWMotion.QUICK))
                },
                label = "registration-step",
            ) { step ->
                when (step) {
                    RegistrationStep.One -> RegistrationDetailsStep(state, onIntent)
                    RegistrationStep.Two -> RegistrationMoneyStep(state, onIntent)
                    RegistrationStep.Three -> RegistrationGoalStep(state, onIntent)
                }
            }
        } else {
            ListsMissing(state, onIntent)
        }

        if (state.step == RegistrationStep.One) {
            FlexSpace(24.dp)
            Column(
                Modifier.fillMaxWidth().authEnters(6),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AuthHairline()
                AuthNote(stringResource(R.string.registration_signIn_prompt), style = HWType.Body)
                HWButton(
                    stringResource(R.string.registration_signIn_action),
                    onClick = { onIntent(RegistrationIntent.SignInInstead) },
                    style = HWButtonStyle.Ghost,
                )
            }
        }
    }
}

/** The step bar with "Step 2 of 3" and, after step 1, the design's `.stepback`. */
@Composable
private fun StepHeader(step: RegistrationStep, onBack: () -> Unit, modifier: Modifier) {
    val label = stringResource(step.label)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (step != RegistrationStep.One) {
                HWIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(step.backLabel), onBack)
            }
            Text(
                label.uppercase(),
                style = HWType.Eyebrow,
                color = HWColors.BrandInkSecondary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (step != RegistrationStep.One) 12.dp else 0.dp)
                    .semantics {
                        contentDescription = label
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
        HWStepBar(step.ordinal, RegistrationStep.entries.size)
    }
}

/** Spinner first; after a failure, the reason and a retry. Never an empty form of dead pickers. */
@Composable
private fun ListsMissing(state: RegistrationState, onIntent: (RegistrationIntent) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
        val failure = state.referenceFailure
        if (failure != null && !state.isLoadingReferenceLists) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AuthNote(stringResource(failure.copy), style = HWType.Body)
                HWButton(
                    stringResource(R.string.registration_reference_retry),
                    onClick = { onIntent(RegistrationIntent.LoadReferenceLists) },
                    style = HWButtonStyle.Ghost,
                )
            }
        } else {
            val loading = stringResource(R.string.state_loading)
            CircularProgressIndicator(
                color = HWColors.BrandInkAccent,
                modifier = Modifier.size(32.dp).semantics { contentDescription = loading },
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun announce(view: android.view.View, text: String) = view.announceForAccessibility(text)
