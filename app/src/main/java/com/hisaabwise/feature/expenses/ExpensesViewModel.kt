package com.hisaabwise.feature.expenses

import androidx.lifecycle.viewModelScope
import com.hisaabwise.core.model.ContentResource
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.model.Picklists
import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.attempt
import com.hisaabwise.core.mvi.loadState
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.ApiError
import com.hisaabwise.core.network.ContentLoader
import com.hisaabwise.core.network.Endpoint
import com.hisaabwise.core.session.ScreenRepaint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Expenses: one read, five writes, and nothing derived from any of them. Port of iOS `ExpensesViewModel`.
 *
 * - Every write answers with the whole [ExpensesScreen], which replaces state.
 * - A write that fails offline becomes [LoadState.Offline]; the draft survives for the retry.
 * - `MONTH_CLOSED` on a create reloads and offers to file the entry into the live month with a new key.
 */
class ExpensesViewModel(
    private val api: ApiClient,
    private val content: ContentLoader,
    repaint: ScreenRepaint? = null,
) : MviViewModel<ExpensesState, ExpensesIntent, ExpensesEffect>(ExpensesState()) {

    init {
        dispatch(ExpensesIntent.Load)
        if (repaint != null) viewModelScope.launch { repaint.requests.collect { dispatch(ExpensesIntent.Load) } }
    }

    override suspend fun handle(intent: ExpensesIntent) {
        when (intent) {
            ExpensesIntent.Load -> load()
            ExpensesIntent.LoadPicklists -> loadPicklists()
            is ExpensesIntent.Open -> open(intent.categoryId)
            is ExpensesIntent.BeginEditing -> beginEditing(intent.categoryId)
            ExpensesIntent.CancelEditing -> reduce { cancelledEditing() }

            is ExpensesIntent.EditAmount -> reduce { copy(draft = draft.copy(amount = intent.text, failure = null)) }
            is ExpensesIntent.EditLabel -> reduce { copy(draft = draft.copy(label = intent.text, failure = null)) }
            is ExpensesIntent.Choose -> reduce {
                // From the option's own flag, never its name.
                val needsFreeText = option(intent.optionId)?.opensFreeText ?: false
                copy(
                    draft = draft.copy(
                        optionId = intent.optionId,
                        needsFreeText = needsFreeText,
                        label = if (needsFreeText) draft.label else "",
                        failure = null,
                    ),
                )
            }
            ExpensesIntent.AddEntry -> addEntry()
            is ExpensesIntent.DeleteEntry -> deleteEntry(intent.id)

            is ExpensesIntent.EditFixedAmount -> reduce { copy(fixedAmount = intent.text) }
            is ExpensesIntent.SaveFixedAmount -> saveFixedAmount(intent.categoryId)

            ExpensesIntent.AddBill -> reduce {
                copy(billDrafts = billDrafts + BillDraft(id = null, name = "", amount = "", icon = ExpensesScreen.Icon.Bolt))
            }
            is ExpensesIntent.RemoveBill -> reduce {
                if (intent.index !in billDrafts.indices) this
                else copy(billDrafts = billDrafts.filterIndexed { i, _ -> i != intent.index })
            }
            is ExpensesIntent.EditBill -> reduce {
                if (intent.index !in billDrafts.indices) this
                else copy(
                    billDrafts = billDrafts.mapIndexed { i, bill ->
                        if (i != intent.index) bill
                        else bill.copy(name = intent.name ?: bill.name, amount = intent.amount ?: bill.amount)
                    },
                )
            }
            is ExpensesIntent.SaveBills -> saveBills(intent.categoryId)

            is ExpensesIntent.SetWantsShare -> setWantsShare(intent.percent)

            ExpensesIntent.Refile -> refile()
            ExpensesIntent.DismissRefile -> reduce { copy(refileOffer = null, draft = draft.reset()) }
            ExpensesIntent.DismissNotice -> reduce { copy(notice = null) }
        }
    }

    // region Read

    private suspend fun load() {
        reduce { copy(content = LoadState.Loading) }
        val loaded = loadState { api.get<ExpensesScreen>(Endpoint.SCREEN_EXPENSES) }
        reduce { copy(content = loaded, authoring = loaded.valueOrNull?.entry ?: authoring) }
    }

    /** Fetched once, on demand. A list that will not load leaves the screen alone. */
    private suspend fun loadPicklists() {
        if (current.picklists != null) return
        val lists = try {
            content.load<Picklists>(ContentResource.Picklists)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return
        reduce { copy(picklists = lists) }
    }

    // endregion

    // region The form

    private fun open(categoryId: String) = reduce {
        val left = cancelledEditing()
        if (left.draft.categoryId == categoryId) left
        else left.copy(draft = EntryDraft(categoryId = categoryId, field = category(categoryId)?.field))
    }

    private fun beginEditing(categoryId: String) = reduce {
        val category = category(categoryId) ?: return@reduce this
        copy(
            isEditing = true,
            billDrafts = ExpensesForms.billDrafts(category),
            fixedAmount = ExpensesForms.fixedAmount(category),
        )
    }

    private fun ExpensesState.cancelledEditing() = copy(isEditing = false, billDrafts = emptyList(), fixedAmount = "")

    private suspend fun addEntry() {
        val body = when (val result = ExpensesForms.validate(current.draft, current.authoring)) {
            EntryValidation.NotReady -> return
            is EntryValidation.Invalid -> {
                reduce { copy(draft = draft.copy(failure = result.failure)) }
                return
            }
            is EntryValidation.Valid -> result.body
        }
        if (current.isWriting) return
        reduce { copy(draft = draft.copy(failure = null, idempotencyKey = draft.idempotencyKey ?: UUID.randomUUID().toString())) }
        val key = current.draft.idempotencyKey ?: return
        write(WriteKind.Entry(body.categoryId), ExpensesNotice.EntryAdded) {
            api.post<NewExpense, ExpensesScreen>(Endpoint.EXPENSES, body, idempotencyKey = key)
        }
    }

    private suspend fun deleteEntry(id: String) {
        // An id a path segment would mangle is not sent at all: it would address a different resource.
        if (Endpoint.expense(id) != "${Endpoint.EXPENSES}/$id") return
        write(WriteKind.Other, ExpensesNotice.EntryRemoved) { api.delete<ExpensesScreen>(Endpoint.expense(id)) }
    }

    private suspend fun saveFixedAmount(categoryId: String) {
        val currency = current.authoring ?: return
        val body = ExpensesForms.fixedCost(current.fixedAmount, currency)
        val landed = write(WriteKind.Other, ExpensesNotice.FixedUpdated) {
            api.put<FixedCostUpdate, ExpensesScreen>(Endpoint.fixedCost(categoryId), body)
        }
        if (landed) reduce { cancelledEditing() }
    }

    private suspend fun saveBills(categoryId: String) {
        val currency = current.authoring ?: return
        val body = ExpensesForms.billLines(current.billDrafts, currency)
        val landed = write(WriteKind.Other, ExpensesNotice.BillsUpdated) {
            api.put<BillLinesUpdate, ExpensesScreen>(Endpoint.billLines(categoryId), body)
        }
        if (landed) reduce { cancelledEditing() }
    }

    /** Nothing is sent when the choice is the one already in force. */
    private suspend fun setWantsShare(percent: Int) {
        if (percent == current.content.valueOrNull?.wants?.sharePercent) return
        write(WriteKind.Other, ExpensesNotice.WantsShareUpdated) {
            api.put<WantsShareUpdate, ExpensesScreen>(Endpoint.WANTS_SHARE, WantsShareUpdate(percent))
        }
    }

    /** A new key: the first attempt was refused, and filing into a different month is a new intent. */
    private suspend fun refile() {
        if (current.refileOffer == null) return
        reduce { copy(refileOffer = null, draft = draft.copy(idempotencyKey = UUID.randomUUID().toString())) }
        addEntry()
    }

    // endregion

    // region One write

    private sealed interface WriteKind {
        /** The form's own create — the only write with something to re-file, and the only one that empties the form. */
        data class Entry(val categoryId: String) : WriteKind
        data object Other : WriteKind
    }

    /** Sends a write and re-renders from its answer. Returns whether it landed. */
    private suspend fun write(kind: WriteKind, notice: ExpensesNotice, send: suspend () -> ExpensesScreen): Boolean {
        if (current.isWriting) return false
        reduce { copy(isWriting = true) }
        val result = try {
            attempt { send() }
        } finally {
            reduce { copy(isWriting = false) }
        }
        result.fold(
            onSuccess = { screen ->
                reduce {
                    copy(
                        content = LoadState.Loaded(screen),
                        authoring = screen.entry,
                        draft = if (kind is WriteKind.Entry) draft.reset() else draft,
                        refileOffer = null,
                        notice = notice,
                    )
                }
            },
            onFailure = { apply(it, kind) },
        )
        return result.isSuccess
    }

    private suspend fun apply(error: Throwable, kind: WriteKind) {
        val apiError = error as? ApiError ?: run {
            reduce { copy(content = LoadState.Failed(ErrorCode.Unknown)) }
            return
        }
        if (apiError.errorCode == ErrorCode.MonthClosed && kind is WriteKind.Entry) {
            offerRefiling(kind.categoryId)
            return
        }
        reduce {
            copy(
                content = when (apiError) {
                    ApiError.Offline -> LoadState.Offline
                    else -> LoadState.Failed(apiError.errorCode ?: ErrorCode.Unknown)
                },
            )
        }
    }

    /** Reloads first, so the offer names the month that is now live. */
    private suspend fun offerRefiling(categoryId: String) {
        load()
        val screen = current.content.valueOrNull ?: return
        reduce { copy(refileOffer = RefileOffer(categoryId, screen.monthLabel)) }
    }

    // endregion
}
