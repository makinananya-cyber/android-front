package com.hisaabwise.feature.account

import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.MoneyAmount
import com.hisaabwise.core.model.PhoneNumber
import com.hisaabwise.core.model.TypedAmount
import com.hisaabwise.core.network.HWJson
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountModelsTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")) { "missing fixture $name" }.readText()

    private fun screen(name: String) = HWJson.decodeFromString(AccountScreen.serializer(), fixture(name))

    @Test
    fun decodesInrAccount() {
        val screen = screen("account-inr.json")
        assertEquals("N", screen.profile.initials)
        assertEquals("INR · English", screen.profile.summaryLabel)
        assertEquals(
            listOf(AccountScreen.Section.Personal, AccountScreen.Section.Language, AccountScreen.Section.Currency, AccountScreen.Section.Password),
            screen.drawnRows.map { it.section },
        )
        assertNull(screen.row(AccountScreen.Section.Password)?.value)
        assertEquals("INR", screen.row(AccountScreen.Section.Currency)?.value)
        assertEquals("₹65,000", screen.personal.salary.display)
        assertEquals(6_500_000L, screen.personal.salary.minor)
        assertEquals("₹", screen.personal.salaryCurrency.symbol)
        assertEquals("+91 98765 43210", screen.personal.phone?.display)
        assertEquals("en", screen.language)
        assertEquals("INR", screen.currency)
        assertEquals(listOf("sq01", "sq02"), screen.password.questions.map { it.id })
        assertTrue(screen.personal.isEmailVerified)
    }

    @Test
    fun decodesAedAccount() {
        val screen = screen("account-aed.json")
        assertEquals("AED", screen.currency)
        assertEquals("AED · English", screen.profile.summaryLabel)
    }

    @Test
    fun decodesUnverifiedAccountWithNoPhone() {
        val screen = screen("account-unverified.json")
        assertFalse(screen.personal.isEmailVerified)
        assertNull(screen.personal.phone)
    }

    @Test
    fun unknownSectionIsDroppedNotGuessed() {
        val json = fixture("account-inr.json").replaceFirst("\"section\": \"password\"", "\"section\": \"twoFactor\"")
        val screen = HWJson.decodeFromString(AccountScreen.serializer(), json)
        assertEquals(3, screen.drawnRows.size)
        assertNull(screen.row(AccountScreen.Section.Password))
    }

    @Test
    fun decodesPasswordCheckAccepted() {
        assertNotNull(HWJson.decodeFromString(PasswordCheckAccepted.serializer(), fixture("password-check-ok.json")))
    }

    @Test
    fun salaryRoundTripsThroughMinorNotDisplay() {
        val salary = screen("account-inr.json").personal.salary
        val typed = TypedAmount.major(salary.minor, salary.exponent)
        assertEquals("65000", typed)
        assertEquals(salary.minor, TypedAmount.minor(typed, 2))
    }

    @Test
    fun personalUpdateNeverCarriesEmailAndOmitsAbsentPhone() {
        val body = HWJson.encodeToString(PersonalDetailsUpdate.serializer(), PersonalDetailsUpdate("Neeraj", MoneyAmount(6_500_000, "INR"), null))
        val obj = HWJson.parseToJsonElement(body).jsonObject
        assertFalse("email" in obj)
        assertFalse("phone" in obj)

        val withPhone = HWJson.encodeToString(
            PersonalDetailsUpdate.serializer(),
            PersonalDetailsUpdate("Neeraj", MoneyAmount(1, "INR"), PhoneNumber.of("IN", "+91", "9876543210")),
        )
        assertTrue("\"e164\":\"+919876543210\"" in withPhone)
    }

    @Test
    fun passwordCheckOmitsAnswersOnStepOne() {
        val body = HWJson.encodeToString(PasswordCheck.serializer(), PasswordCheck("secret123"))
        assertFalse("securityAnswers" in body)
    }

    @Test
    fun onlyTheTwoFieldCodesBecomePasswordFailures() {
        assertEquals(PasswordDraft.Failure.CurrentPasswordRejected, PasswordDraft.Failure.of(ErrorCode.InvalidCredentials))
        assertEquals(PasswordDraft.Failure.AnswersRejected, PasswordDraft.Failure.of(ErrorCode.SecurityAnswersInvalid))
        assertNull(PasswordDraft.Failure.of(ErrorCode.RateLimited))
        assertNull(PasswordDraft.Failure.of(null))
    }
}
