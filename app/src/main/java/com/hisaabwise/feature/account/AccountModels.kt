package com.hisaabwise.feature.account

import com.hisaabwise.core.model.AuthoringCurrency
import com.hisaabwise.core.model.Money
import com.hisaabwise.core.model.MoneyAmount
import com.hisaabwise.core.model.PhoneNumber
import com.hisaabwise.core.model.SecurityAnswer
import com.hisaabwise.core.model.SecurityQuestion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /v1/screens/account` — everything Account draws, fully computed by the server.
 * Port of iOS `AccountScreen`. Every write on this screen answers with the whole payload.
 */
@Serializable
data class AccountScreen(
    val profile: Profile,
    /** The rows, in the order they are drawn. */
    val rows: List<Row>,
    val personal: Personal,
    /** The stored language tag ("en", "ar"…). The picker's tick follows this, not the optimistic manager. */
    val language: String,
    /** The display currency's ISO code. */
    val currency: String,
    val password: Password,
) {
    @Serializable
    data class Profile(
        val initials: String,
        val displayName: String,
        val email: String,
        /** "INR · English" — one server string, never joined on the client. */
        val summaryLabel: String,
    )

    @Serializable
    data class Row(
        /**
         * Which page the row opens. iOS fails the whole screen on an unknown section; here an unknown
         * value coerces to `null` and the row is dropped ([drawnRows]), so a newer server never makes
         * a row open the wrong page.
         */
        val section: Section? = null,
        val name: String,
        val hint: String,
        /** `null` on the rows drawn without one. The password row's mask is client copy. */
        val value: String? = null,
    )

    @Serializable
    enum class Section {
        @SerialName("personal") Personal,
        @SerialName("language") Language,
        @SerialName("currency") Currency,
        @SerialName("password") Password;

        /** The password row draws a client-side mask instead of a value. */
        val masksValue: Boolean get() = this == Password
    }

    @Serializable
    data class Personal(
        val displayName: String,
        /** Locked: email is the identity (invariant 4). */
        val email: String,
        val isEmailVerified: Boolean = true,
        /** `minor` for the field the user edits, `display` for the line they read (defect D16). */
        val salary: Money,
        val salaryCurrency: AuthoringCurrency,
        val phone: Phone? = null,
    )

    @Serializable
    data class Phone(
        val country: String,
        val dialCode: String,
        val national: String,
        /** The server's grouping — rendered as-is. */
        val display: String,
    )

    @Serializable
    data class Password(val questions: List<SecurityQuestion> = emptyList())

    /** Rows this build knows how to open, in server order. */
    val drawnRows: List<Row> get() = rows.filter { it.section != null }

    fun row(section: Section): Row? = rows.firstOrNull { it.section == section }
}

/** `PUT /v1/me` — the three editable personal details. Email is deliberately absent (invariant 4). */
@Serializable
data class PersonalDetailsUpdate(
    val displayName: String,
    val salary: MoneyAmount,
    /** Absent rather than empty when not given (HWJson drops nulls). */
    val phone: PhoneNumber? = null,
)

/** `PUT /v1/me/currency` — the ISO code and nothing else. */
@Serializable
data class DisplayCurrencyUpdate(val currency: String)

/** `POST /v1/me/password` — the whole change, in one request. Answers with [AccountScreen]. */
@Serializable
data class PasswordChange(
    val currentPassword: String,
    val securityAnswers: List<SecurityAnswer>,
    val newPassword: String,
)

/** `POST /v1/me/password/check` — advisory. `securityAnswers` is omitted on step one. */
@Serializable
data class PasswordCheck(
    val currentPassword: String,
    val securityAnswers: List<SecurityAnswer>? = null,
)

/** What the check answers with on acceptance. The status is the answer; the body is not read. */
@Serializable
data class PasswordCheckAccepted(val ok: Boolean = true)

/** What `DELETE /v1/me` answers with. Decoded and discarded; the screen signs out. */
@Serializable
data class AccountDeletion(val deletedAt: String = "", val purgeAfterDays: Int = 30)
