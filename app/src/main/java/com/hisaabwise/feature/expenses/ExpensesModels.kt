package com.hisaabwise.feature.expenses

import com.hisaabwise.core.model.AuthoringCurrency
import com.hisaabwise.core.model.Money
import com.hisaabwise.core.model.MoneyAmount
import com.hisaabwise.core.model.PicklistOption
import com.hisaabwise.core.model.Picklists
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /v1/screens/expenses` — everything Expenses draws, fully computed by the server (ADR-0020).
 * Port of iOS `ExpensesScreen`. Every write answers with this whole payload, which replaces state.
 */
@Serializable
data class ExpensesScreen(
    /** "August" — names the live month, including in the re-filing offer. */
    val monthLabel: String,
    val summary: Summary,
    val wants: Wants,
    /** What the user types amounts in. */
    val entry: AuthoringCurrency,
    /** In the server's order. */
    val categories: List<Category>,
) {
    @Serializable
    data class Summary(val total: Money, val fixed: Money, val variable: Money, val income: Money)

    @Serializable
    data class Wants(
        val used: Money,
        val allowance: Money,
        /** "88%" — the readout, so the ratio and the bar cannot disagree about rounding. */
        val percentageLabel: String,
        /** Geometry only, `0…1`, already clamped. */
        val fill: Double,
        /** The server's verdict; never `used > allowance` worked out here. */
        val isOver: Boolean,
        /** The share in force, whole percent. `null` = no row is ticked. */
        val sharePercent: Int? = null,
        /** The shares the Edit budget sheet offers, each with its allowance. `null` → the full 10–40 range. */
        val options: List<ShareOption>? = null,
    )

    @Serializable
    data class ShareOption(val percent: Int, val allowance: Money)

    @Serializable
    data class Category(
        val id: String,
        val name: String,
        val hint: String,
        /** Read, never computed. Already signed ("+₹900") for an incoming category. */
        val total: Money,
        /** "2 entries" — `null` for `lines` and `fixed`. */
        val entryCountLabel: String? = null,
        /**
         * **No default, deliberately** (as on iOS): `kind` decides which write the page offers, and guessing
         * would offer the wrong one. An unknown kind fails the decode and the screen renders as failed.
         */
        val kind: Kind,
        val flow: Flow = Flow.Outgoing,
        val icon: Icon = Icon.Tag,
        /** An unknown shape reads as no field at all (the Groceries case). */
        val field: Field? = null,
        /** `log` only, newest first in the server's order. */
        val entries: List<Entry> = emptyList(),
        /** `lines` only. */
        val lines: List<Line> = emptyList(),
    )

    @Serializable
    enum class Kind {
        @SerialName("log") Log,
        @SerialName("lines") Lines,
        @SerialName("fixed") Fixed,
    }

    @Serializable
    enum class Flow {
        @SerialName("out") Outgoing,
        @SerialName("in") Incoming,
    }

    @Serializable
    enum class Field {
        @SerialName("place") Place,
        @SerialName("source") Source,
        @SerialName("transportMode") TransportMode,
        @SerialName("otherType") OtherType;

        /** Which server-served list the field picks from, or `null` where it is typed. */
        val picklist: Picklist?
            get() = when (this) {
                Place, Source -> null
                TransportMode -> Picklist.Transport
                OtherType -> Picklist.Other
            }

        /** Whether an entry may be filed with this field left blank. */
        val isOptional: Boolean get() = this == Source
    }

    enum class Picklist { Transport, Other }

    @Serializable
    enum class Icon {
        @SerialName("groceries") Groceries,
        @SerialName("transport") Transport,
        @SerialName("entertainment") Entertainment,
        @SerialName("other") Other,
        @SerialName("income") Income,
        @SerialName("utilities") Utilities,
        @SerialName("rent") Rent,
        @SerialName("bolt") Bolt,
        @SerialName("drop") Drop,
        @SerialName("signal") Signal,
        @SerialName("tag") Tag,
    }

    @Serializable
    data class Entry(val id: String, val label: String, val amount: Money, val dateLabel: String)

    @Serializable
    data class Line(val id: String, val name: String, val amount: Money, val icon: Icon = Icon.Tag)

    fun category(id: String): Category? = categories.firstOrNull { it.id == id }

    /** Incoming categories first, then the rest in the server's order (a stable partition). */
    val orderedCategories: List<Category>
        get() = categories.filter { it.flow == Flow.Incoming } + categories.filter { it.flow != Flow.Incoming }
}

fun Picklists.options(picklist: ExpensesScreen.Picklist): List<PicklistOption> = when (picklist) {
    ExpensesScreen.Picklist.Transport -> transport
    ExpensesScreen.Picklist.Other -> other
}

// region Writes

/** `POST /v1/expenses`. No date and no month: the server derives both. */
@Serializable
data class NewExpense(
    val categoryId: String,
    val amount: MoneyAmount,
    /** The pick-list option's id — never its name. */
    val optionId: String? = null,
    val label: String? = null,
)

/** `PUT /v1/expenses/fixed/{categoryId}`. */
@Serializable
data class FixedCostUpdate(val amount: MoneyAmount)

/** `PUT /v1/me/budget/wants` — a setting (whole percent), never an allowance. */
@Serializable
data class WantsShareUpdate(val percent: Int)

/** `PUT /v1/expenses/lines/{categoryId}` — the whole set of bills, replaced. */
@Serializable
data class BillLinesUpdate(val lines: List<BillLine>) {
    /** [id] is `null` for a bill the user has just added. */
    @Serializable
    data class BillLine(val id: String? = null, val name: String, val amount: MoneyAmount)
}

// endregion

/** The shares the wants budget may be taken from — 10% to 40% in fives. */
enum class WantsShare(val percent: Int) {
    Tenth(10), Fifteen(15), Fifth(20), Quarter(25), RuleOfThumb(30), ThirtyFive(35), TwoFifths(40);

    companion object {
        fun of(percent: Int): WantsShare? = entries.firstOrNull { it.percent == percent }

        /** The server's feasible options in order; none (older server) → the whole range. */
        fun offered(wants: ExpensesScreen.Wants): List<WantsShare> {
            val options = wants.options
            if (options.isNullOrEmpty()) return entries
            return options.mapNotNull { of(it.percent) }
        }
    }
}
