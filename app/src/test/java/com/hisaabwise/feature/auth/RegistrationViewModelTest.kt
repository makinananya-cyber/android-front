package com.hisaabwise.feature.auth

import com.hisaabwise.app.LegalLinks
import com.hisaabwise.core.model.CountryList
import com.hisaabwise.core.model.CurrencyList
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.RegistrationRequest
import com.hisaabwise.core.model.SecurityQuestionList
import com.hisaabwise.core.network.ApiError
import com.hisaabwise.core.network.HWJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun fixture(name: String) = requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")).readText()

    private val lists = object : RegistrationLists {
        override suspend fun countries() = HWJson.decodeFromString<CountryList>(fixture("reference-countries.json"))
        override suspend fun currencies() = HWJson.decodeFromString<CurrencyList>(fixture("reference-currencies.json"))
        override suspend fun questions() = HWJson.decodeFromString<SecurityQuestionList>(fixture("reference-security-questions.json"))
    }

    private val sent = mutableListOf<RegistrationRequest>()
    private var refusal: Throwable? = null

    private fun viewModel() = RegistrationViewModel(
        lists = lists,
        register = { request -> sent += request; refusal?.let { throw it } },
        legal = LegalLinks("https://example.invalid/terms", "https://example.invalid/privacy"),
        language = { "en" },
    )

    private fun RegistrationViewModel.fillToStepThree() {
        dispatch(RegistrationIntent.NameChanged("Neeraj"))
        dispatch(RegistrationIntent.EmailChanged("n@example.ae"))
        dispatch(RegistrationIntent.DateOfBirthChosen(LocalDate.of(1990, 3, 12)))
        dispatch(RegistrationIntent.PasswordChanged("a-long-password"))
        dispatch(RegistrationIntent.ConfirmPasswordChanged("a-long-password"))
        dispatch(RegistrationIntent.TermsChanged(true))
        dispatch(RegistrationIntent.Advance)
        dispatch(RegistrationIntent.SalaryChanged("8000"))
        val questions = state.value.questions
        dispatch(RegistrationIntent.FirstQuestionChosen(questions[0].id))
        dispatch(RegistrationIntent.FirstAnswerChanged("Rex"))
        dispatch(RegistrationIntent.SecondQuestionChosen(questions[1].id))
        dispatch(RegistrationIntent.SecondAnswerChanged("Dubai"))
        dispatch(RegistrationIntent.Advance)
    }

    @Test
    fun listsLoadWithTheUaeDefaults() {
        val vm = viewModel()
        assertTrue(vm.state.value.hasReferenceLists)
        assertEquals("AE", vm.state.value.country?.code)
        assertEquals("AED", vm.state.value.currency?.code)
    }

    @Test
    fun stepThreeIsPrefilledAndReDerivedFromACorrectedSalary() {
        val vm = viewModel()
        vm.fillToStepThree()
        assertEquals(RegistrationStep.Three, vm.state.value.step)
        assertEquals("1600", vm.state.value.goalText)

        vm.dispatch(RegistrationIntent.GoBack)
        vm.dispatch(RegistrationIntent.SalaryChanged("10000"))
        vm.dispatch(RegistrationIntent.Advance)
        assertEquals("2000", vm.state.value.goalText)
    }

    @Test
    fun skipSendsTheSuggestionAndSaysSo() {
        val vm = viewModel()
        vm.fillToStepThree()
        vm.dispatch(RegistrationIntent.GoalChanged(""))
        vm.dispatch(RegistrationIntent.SkipGoal)
        val request = sent.single()
        assertTrue(request.goalWasSkipped)
        assertEquals(160_000L, request.savingsGoal.minor)
        assertEquals("AED", request.displayCurrency)
    }

    @Test
    fun submitWithAnEmptiedGoalIsRefusedLocally() {
        val vm = viewModel()
        vm.fillToStepThree()
        vm.dispatch(RegistrationIntent.GoalChanged(""))
        vm.dispatch(RegistrationIntent.Submit)
        assertTrue(sent.isEmpty())
        assertEquals(listOf(RegistrationFailure.GoalMissing), vm.state.value.failures)
    }

    @Test
    fun emailTakenReturnsToStepOne() {
        refusal = ApiError.Server(409, ErrorCode.EmailTaken)
        val vm = viewModel()
        vm.fillToStepThree()
        vm.dispatch(RegistrationIntent.GoalChanged("2000"))
        vm.dispatch(RegistrationIntent.Submit)
        assertEquals(false, sent.single().goalWasSkipped)
        assertEquals(RegistrationStep.One, vm.state.value.step)
        assertEquals(RegistrationFailure.EmailTaken, vm.state.value.failure(RegistrationField.Email))
        assertEquals(false, vm.state.value.isSubmitting)
    }
}
