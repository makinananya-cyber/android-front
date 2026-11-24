package com.hisaabwise.feature.auth

import com.hisaabwise.core.mvi.MviViewModel

class LandingViewModel : MviViewModel<LandingState, LandingIntent, LandingEffect>(LandingState()) {
    override suspend fun handle(intent: LandingIntent) {
        when (intent) {
            LandingIntent.Advance -> reduce { advanced() }
            LandingIntent.GetStarted -> emit(LandingEffect.GoToSignIn)
        }
    }
}
