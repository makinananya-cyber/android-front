package com.hisaabwise.feature.expenses

import com.hisaabwise.core.model.AuthoringCurrency
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.model.MoneyAmount
import com.hisaabwise.core.model.PicklistOption
import com.hisaabwise.core.model.Picklists
import com.hisaabwise.core.model.TypedAmount
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState
import java.util.UUID

/**
 * One contract for both Expenses destinations (the root and a category page), because one view model
 * serves both, as on iOS. Port of `ExpensesViewModel`'s presentation state.
 */
data class ExpensesState(
    val content: LoadState<ExpensesScreen> = LoadState.Loading,
    /** The entry being typed. Outlives the payload, so an offline write does not cost the user their typing. */
    val draft: EntryDraft = EntryDraft(),
    /** Whether the open `lines`/`fixed` category is in edit mode. */
    val isEditing: Boolean = false,
    /** Bills as they read in edit mode — amounts in major units, as typed. */
    val billDrafts: List<BillDraft> = emptyList(),
    /** The `fixed` amount as it reads in edit mode, in major units. */
    val fixedAmount: String = "",
    /** The last write that landed, for the toast. */
    val notice: ExpensesNotice? = null,
    /** The offer to file a refused create into the live month. */
    val refileOffer: RefileOffer? = null,
    val isWriting: Boolean = false,
    /** Cacheable content, fetched once on demand. */
    val picklists: Picklists? = null,
    /** What the user is authoring in — held so a retry after an offline write still knows the currency. */
    val authoring: AuthoringCurrency? = null,
) : UiState {
    fun category(id: String): ExpensesScreen.Category? = content.valueOrNull?.category(id)
    val monthLabel: String get() = content.valueOrNull?.monthLabel.orEmpty()
    val entrySymbol: String get() = authoring?.symbol.orEmpty()
    val entryCode: String get() = authoring?.displayCode.orEmpty()

    fun options(picklist: ExpensesScreen.Picklist): List<PicklistOption> = picklists?.options(picklist).orEmpty()
    fun option(id: String): PicklistOption? = picklists?.let { (it.transport + it.other).firstOrNull { o -> o.id == id } }
    val chosenOptionName: String? get() = draft.optionId?.let { option(it)?.name }
}

sealed interface ExpensesIntent : UiIntent {
    data object Load : ExpensesIntent
    data object LoadPicklists : ExpensesIntent

    /** Arriving at a category page: leaves edit mode; starts a new draft if this is a different category. */
    data class Open(val categoryId: String) : ExpensesIntent
    data class BeginEditing(val categoryId: String) : ExpensesIntent
    data object CancelEditing : ExpensesIntent

    data class EditAmount(val text: String) : ExpensesIntent
    data class EditLabel(val text: String) : ExpensesIntent
    data class Choose(val optionId: String) : ExpensesIntent
    data object AddEntry : ExpensesIntent
    data class DeleteEntry(val id: String) : ExpensesIntent

    data class EditFixedAmount(val text: String) : ExpensesIntent
    data class SaveFixedAmount(val categoryId: String) : ExpensesIntent

    data object AddBill : ExpensesIntent
    data class RemoveBill(val index: Int) : ExpensesIntent
    data class EditBill(val index: Int, val name: String? = null, val amount: String? = null) : ExpensesIntent
    data class SaveBills(val categoryId: String) : ExpensesIntent

    data class SetWantsShare(val percent: Int) : ExpensesIntent

    data object Refile : ExpensesIntent
    data object DismissRefile : ExpensesIntent
    data object DismissNotice : ExpensesIntent
}

/** Expenses has no one-shot effects: navigation follows state (a non-loaded payload pops the category page). */
sealed interface ExpensesEffect : UiEffect

/** The entry being typed. Port of iOS `EntryDraft`. */
data class EntryDraft(
    val categoryId: String? = null,
    /** Snapshotted when the category was opened, so a retry can validate with no payload in hand. */
    val field: ExpensesScreen.Field? = null,
    val amount: String = "",
    val optionId: String? = null,
    val label: String = "",
    val needsFreeText: Boolean = false,
    val failure: EntryFailure? = null,
    /** One key per user intent, minted on the first attempt and reused across retries. */
    val idempotencyKey: String? = null,
) {
    val isEmpty: Boolean get() = amount.isEmpty() && optionId == null && label.isEmpty()

    /** Empties the form, keeping the category and its field shape. */
    fun reset() = copy(amount = "", optionId = null, label = "", needsFreeText = false, failure = null, idempotencyKey = null)
}

enum class EntryFailure { AmountMissing, OptionMissing, LabelMissing }

/** One bill in edit mode. [key] is a stable identity for rows that have no server id yet. */
data class BillDraft(
    val id: String?,
    val name: String,
    val amount: String,
    val icon: ExpensesScreen.Icon,
    val key: String = UUID.randomUUID().toString(),
)

enum class ExpensesNotice { EntryAdded, EntryRemoved, FixedUpdated, BillsUpdated, WantsShareUpdated }

/** [monthLabel] is the live month, from the payload reloaded after the refusal. */
data class RefileOffer(val categoryId: String, val monthLabel: String)

/** Local input validation — whether there is something to send, never what any figure is. */
sealed interface EntryValidation {
    data class Valid(val body: NewExpense) : EntryValidation
    data class Invalid(val failure: EntryFailure) : EntryValidation
    /** No category open or no authoring currency yet: nothing to send and nothing to say. */
    data object NotReady : EntryValidation
}

/** Pure ports of the iOS view model's form rules, so they can be tested without a network. */
object ExpensesForms {

    fun validate(draft: EntryDraft, authoring: AuthoringCurrency?): EntryValidation {
        val categoryId = draft.categoryId ?: return EntryValidation.NotReady
        val currency = authoring ?: return EntryValidation.NotReady
        val minor = TypedAmount.minor(draft.amount, currency.exponent)
            ?: return EntryValidation.Invalid(EntryFailure.AmountMissing)
        val label = draft.label.trim()
        val field = draft.field
        if (field?.picklist != null) {
            if (draft.optionId == null) return EntryValidation.Invalid(EntryFailure.OptionMissing)
            if (draft.needsFreeText && label.isEmpty()) return EntryValidation.Invalid(EntryFailure.LabelMissing)
        } else if (field != null && !field.isOptional && label.isEmpty()) {
            return EntryValidation.Invalid(EntryFailure.LabelMissing)
        }
        return EntryValidation.Valid(
            NewExpense(
                categoryId = categoryId,
                amount = MoneyAmount(minor, currency.code),
                optionId = draft.optionId,
                label = label.ifEmpty { null },
            ),
        )
    }

    /** Drafts for edit mode: each figure read with its own exponent; zero shows as an empty box. */
    fun billDrafts(category: ExpensesScreen.Category): List<BillDraft> = category.lines.map {
        BillDraft(
            id = it.id,
            name = it.name,
            amount = if (it.amount.minor == 0L) "" else TypedAmount.major(it.amount.minor, it.amount.exponent),
            icon = it.icon,
        )
    }

    fun fixedAmount(category: ExpensesScreen.Category): String =
        if (category.total.minor == 0L) "" else TypedAmount.major(category.total.minor, category.total.exponent)

    /** Blank-named rows are dropped; a named bill with nothing typed is zero. */
    fun billLines(drafts: List<BillDraft>, currency: AuthoringCurrency): BillLinesUpdate =
        BillLinesUpdate(
            drafts.mapNotNull { draft ->
                val name = draft.name.trim()
                if (name.isEmpty()) null
                else BillLinesUpdate.BillLine(
                    id = draft.id,
                    name = name,
                    amount = MoneyAmount(TypedAmount.minor(draft.amount, currency.exponent) ?: 0, currency.code),
                )
            },
        )

    /** Zero is a legitimate fixed cost, and an empty box reads as zero. */
    fun fixedCost(text: String, currency: AuthoringCurrency): FixedCostUpdate =
        FixedCostUpdate(MoneyAmount(TypedAmount.scaled(text, currency.exponent) ?: 0, currency.code))
}
