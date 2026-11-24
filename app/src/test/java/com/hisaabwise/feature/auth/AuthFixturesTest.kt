package com.hisaabwise.feature.auth

import com.hisaabwise.core.model.Acknowledgement
import com.hisaabwise.core.model.CountryList
import com.hisaabwise.core.model.CurrencyList
import com.hisaabwise.core.model.MoneyAmount
import com.hisaabwise.core.model.PhoneNumber
import com.hisaabwise.core.model.RegistrationRequest
import com.hisaabwise.core.model.SecurityAnswer
import com.hisaabwise.core.model.SecurityQuestionList
import com.hisaabwise.core.model.SessionTokensWire
import com.hisaabwise.core.network.HWJson
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthFixturesTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")) { "missing fixture $name" }.readText()

    @Test
    fun referenceListsDecodeWithTheirAcceptanceCounts() {
        val countries = HWJson.decodeFromString<CountryList>(fixture("reference-countries.json")).countries
        val currencies = HWJson.decodeFromString<CurrencyList>(fixture("reference-currencies.json")).currencies
        val questions = HWJson.decodeFromString<SecurityQuestionList>(fixture("reference-security-questions.json")).questions
        assertEquals(251, countries.size)
        assertEquals(160, currencies.size)
        assertEquals(14, questions.size)
        assertTrue(countries.any { it.code == RegistrationState.DEFAULT_COUNTRY_CODE && it.dialCode == "+971" })
        assertTrue(currencies.any { it.code == RegistrationState.DEFAULT_CURRENCY_CODE })
    }

    @Test
    fun recoveryPayloadsDecode() {
        val questions = HWJson.decodeFromString<SecurityQuestionList>(fixture("recovery-questions.json")).questions
        assertEquals(listOf("sq01", "sq02"), questions.map { it.id })

        val ticket = HWJson.decodeFromString<RecoveryTicket>(fixture("recovery-ticket.json"))
        assertTrue(ticket.ticket.startsWith("rt_"))
        assertEquals(600, ticket.expiresInSeconds)

        // The reset answers with a token pair, which the client discards: any 2xx body decodes.
        HWJson.decodeFromString<Acknowledgement>(fixture("recovery-reset.json"))
        HWJson.decodeFromString<SessionTokensWire>(fixture("session-tokens.json"))
    }

    @Test
    fun registrationBodyEncodesTheWireShape() {
        val body = RegistrationRequest(
            name = "Neeraj",
            email = "n@example.ae",
            dateOfBirth = "1990-03-12",
            phone = PhoneNumber.of("AE", "+971", "501234567"),
            password = "a-long-password",
            displayCurrency = "AED",
            salary = MoneyAmount(800_000, "AED"),
            savingsGoal = MoneyAmount(160_000, "AED"),
            goalWasSkipped = true,
            securityAnswers = listOf(SecurityAnswer("sq01", "Rex"), SecurityAnswer("sq02", "Dubai")),
            acceptedTerms = true,
            timeZone = "Asia/Dubai",
            language = "en",
        )
        val json = HWJson.parseToJsonElement(HWJson.encodeToString(RegistrationRequest.serializer(), body)).jsonObject
        assertEquals("+971501234567", json["phone"]!!.jsonObject["e164"]!!.jsonPrimitive.content)
        assertEquals("sq01", (json["securityAnswers"] as kotlinx.serialization.json.JsonArray)[0].jsonObject["questionId"]!!.jsonPrimitive.content)
        assertEquals("800000", json["salary"]!!.jsonObject["minor"]!!.jsonPrimitive.content)
    }

    @Test
    fun recoveryVerifyBodyEncodesQuestionIds() {
        val body = RecoveryVerifyRequest("a@b.co", "1990-01-02", listOf(RecoverySubmittedAnswer("sq01", "rex")))
        val json = HWJson.encodeToString(RecoveryVerifyRequest.serializer(), body)
        assertEquals("""{"email":"a@b.co","dateOfBirth":"1990-01-02","answers":[{"questionId":"sq01","answer":"rex"}]}""", json)
    }
}
