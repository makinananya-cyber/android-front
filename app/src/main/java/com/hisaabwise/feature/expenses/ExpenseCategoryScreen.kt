package com.hisaabwise.feature.expenses

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.components.HWBackBar
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWCard
import com.hisaabwise.core.designsystem.components.HWComboField
import com.hisaabwise.core.designsystem.components.HWMoneyField
import com.hisaabwise.core.designsystem.components.HWPickerSheet
import com.hisaabwise.core.designsystem.components.HWTextField
import com.hisaabwise.core.designsystem.components.SurfaceGround
import com.hisaabwise.feature.expenses.components.BillLine
import com.hisaabwise.feature.expenses.components.BillLineEditor
import com.hisaabwise.feature.expenses.components.CategoryHero
import com.hisaabwise.feature.expenses.components.DashedButton
import com.hisaabwise.feature.expenses.components.EditToggle
import com.hisaabwise.feature.expenses.components.EmptyNote
import com.hisaabwise.feature.expenses.components.EntryRow
import com.hisaabwise.feature.expenses.components.FixedAmount
import com.hisaabwise.feature.expenses.components.glyph
import com.hisaabwise.feature.expenses.components.vector

/**
 * One category's page, in whichever structural kind it is. A projection of the screen payload: it re-reads
 * the category from [state] on every draw, so a write's answer re-renders it from server truth. Stateless
 * apart from which pick-list sheet is open.
 */
@Composable
fun ExpenseCategoryScreen(
    state: ExpensesState,
    categoryId: String,
    onIntent: (ExpensesIntent) -> Unit,
    onBack: () -> Unit,
) {
    val category = state.category(categoryId)
    var picker by rememberSaveable { mutableStateOf<ExpensesScreen.Picklist?>(null) }

    SurfaceGround {
        Column(Modifier.fillMaxSize()) {
            HWBackBar(
                title = category?.name.orEmpty(),
                onBack = onBack,
                backDescription = stringResource(R.string.expenses_nav_back),
                trailing = {
                    if (category != null && category.kind != ExpensesScreen.Kind.Log) {
                        EditToggle(isOn = state.isEditing, isLoading = state.isWriting, onClick = {
                            if (!state.isEditing) onIntent(ExpensesIntent.BeginEditing(categoryId))
                            else when (category.kind) {
                                ExpensesScreen.Kind.Lines -> onIntent(ExpensesIntent.SaveBills(categoryId))
                                ExpensesScreen.Kind.Fixed -> onIntent(ExpensesIntent.SaveFixedAmount(categoryId))
                                ExpensesScreen.Kind.Log -> Unit
                            }
                        })
                    }
                },
            )
            // Reachable only if a reload came back without this category: draw nothing rather than a page about nothing.
            if (category == null) return@Column
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val incoming = category.flow == ExpensesScreen.Flow.Incoming
                CategoryHero(
                    name = category.name,
                    hint = category.hint,
                    total = category.total.display,
                    monthLabel = state.monthLabel,
                    icon = category.icon.vector(),
                    isIncoming = incoming,
                )
                when (category.kind) {
                    ExpensesScreen.Kind.Log -> LogKind(state, category, onIntent, onOpenPicker = { picker = it })
                    ExpensesScreen.Kind.Lines -> LinesKind(state, category, onIntent)
                    ExpensesScreen.Kind.Fixed -> FixedKind(state, category, onIntent)
                }
            }
        }
    }

    picker?.let { list ->
        HWPickerSheet(
            title = stringResource(if (list == ExpensesScreen.Picklist.Transport) R.string.expenses_field_transportMode_title else R.string.expenses_field_otherType_title),
            options = state.options(list),
            label = { it.name },
            isSelected = { it.id == state.draft.optionId },
            onSelect = {
                onIntent(ExpensesIntent.Choose(it.id))
                picker = null
            },
            onDismiss = { picker = null },
            searchPlaceholder = stringResource(if (list == ExpensesScreen.Picklist.Transport) R.string.expenses_field_transportMode_search else R.string.expenses_field_otherType_search),
        )
    }
}

@Composable
private fun CardCaption(text: String) {
    Text(text.uppercase(), style = HWType.Eyebrow, color = HWColors.AccentMuted, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Explain(text: String) {
    Text(text, style = HWType.Body, color = HWColors.InkSecondary)
}

// region log

@Composable
private fun ColumnScope.LogKind(
    state: ExpensesState,
    category: ExpensesScreen.Category,
    onIntent: (ExpensesIntent) -> Unit,
    onOpenPicker: (ExpensesScreen.Picklist) -> Unit,
) {
    val incoming = category.flow == ExpensesScreen.Flow.Incoming
    val draft = state.draft
    HWCard {
        CardCaption(stringResource(if (incoming) R.string.expenses_add_income_caption else R.string.expenses_add_caption))
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // The label is not drawn (the caption says it), but it is still the field's spoken name.
            val amountLabel = stringResource(R.string.expenses_amount_label)
            HWMoneyField(
                text = draft.amount,
                onTextChange = { onIntent(ExpensesIntent.EditAmount(it)) },
                currencyCode = state.entryCode,
                error = if (draft.failure == EntryFailure.AmountMissing) stringResource(R.string.expenses_error_amount) else null,
                prominent = true,
                modifier = Modifier.semantics { contentDescription = amountLabel },
            )
            category.field?.let { ExtraField(it, state, onIntent, onOpenPicker) }
            HWButton(
                text = stringResource(if (incoming) R.string.expenses_add_income_action else R.string.expenses_add_action),
                onClick = { onIntent(ExpensesIntent.AddEntry) },
                icon = Icons.Filled.Add,
                isLoading = state.isWriting,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    // `.list-head` — the month and how many entries are in it, both the server's.
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(state.monthLabel.uppercase(), style = HWType.Eyebrow, color = HWColors.AccentMuted, modifier = Modifier.weight(1f))
        category.entryCountLabel?.let { Text(it, style = HWType.Caption, color = HWColors.InkTertiary) }
    }

    if (category.entries.isEmpty()) {
        EmptyNote(stringResource(R.string.expenses_entries_empty))
    } else {
        val reduceMotion = LocalReduceMotion.current
        val removeLabel = stringResource(R.string.expenses_entry_remove)
        Column(
            (if (reduceMotion) Modifier else Modifier.animateContentSize()).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            category.entries.forEach { entry ->
                key(entry.id) {
                    EntryRow(
                        label = entry.label,
                        dateLabel = entry.dateLabel,
                        amount = entry.amount.display,
                        icon = category.icon.vector(),
                        isIncoming = incoming,
                        removeLabel = removeLabel,
                        onRemove = if (state.isWriting) null else { { onIntent(ExpensesIntent.DeleteEntry(entry.id)) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtraField(
    field: ExpensesScreen.Field,
    state: ExpensesState,
    onIntent: (ExpensesIntent) -> Unit,
    onOpenPicker: (ExpensesScreen.Picklist) -> Unit,
) {
    val draft = state.draft
    val picklist = field.picklist
    if (picklist != null) {
        HWComboField(
            value = state.chosenOptionName,
            placeholder = stringResource(field.placeholderRes),
            onClick = { onOpenPicker(picklist) },
            label = stringResource(field.labelRes),
            // Only its own message: a missing free-text label belongs beside the text box.
            error = if (draft.failure == EntryFailure.OptionMissing) stringResource(R.string.expenses_error_option) else null,
        )
        if (draft.needsFreeText) {
            HWTextField(
                value = draft.label,
                onValueChange = { onIntent(ExpensesIntent.EditLabel(it)) },
                label = stringResource(R.string.expenses_field_custom_label),
                placeholder = stringResource(R.string.expenses_field_custom_placeholder),
                error = if (draft.failure == EntryFailure.LabelMissing) stringResource(R.string.expenses_error_custom) else null,
                leading = { Icon(Icons.Outlined.LocalOffer, null, tint = HWColors.InkSecondary) },
            )
        }
    } else {
        HWTextField(
            value = draft.label,
            onValueChange = { onIntent(ExpensesIntent.EditLabel(it)) },
            label = stringResource(field.labelRes),
            placeholder = stringResource(field.placeholderRes),
            error = if (draft.failure == EntryFailure.LabelMissing) stringResource(R.string.expenses_error_label) else null,
            leading = { Icon(field.glyph(), null, tint = HWColors.InkSecondary) },
        )
    }
}

private val ExpensesScreen.Field.labelRes: Int
    get() = when (this) {
        ExpensesScreen.Field.Place -> R.string.expenses_field_place_label
        ExpensesScreen.Field.Source -> R.string.expenses_field_source_label
        ExpensesScreen.Field.TransportMode -> R.string.expenses_field_transportMode_label
        ExpensesScreen.Field.OtherType -> R.string.expenses_field_otherType_label
    }

private val ExpensesScreen.Field.placeholderRes: Int
    get() = when (this) {
        ExpensesScreen.Field.Place -> R.string.expenses_field_place_placeholder
        ExpensesScreen.Field.Source -> R.string.expenses_field_source_placeholder
        ExpensesScreen.Field.TransportMode -> R.string.expenses_field_transportMode_placeholder
        ExpensesScreen.Field.OtherType -> R.string.expenses_field_otherType_placeholder
    }

// endregion

// region lines

@Composable
private fun LinesKind(state: ExpensesState, category: ExpensesScreen.Category, onIntent: (ExpensesIntent) -> Unit) {
    HWCard {
        CardCaption(stringResource(R.string.expenses_bills_caption))
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Explain(stringResource(R.string.expenses_bills_explain))
            if (state.isEditing) {
                val nameLabel = stringResource(R.string.expenses_bill_name)
                val amountLabel = stringResource(R.string.expenses_bill_amount)
                val removeLabel = stringResource(R.string.expenses_bill_remove)
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    state.billDrafts.forEachIndexed { index, bill ->
                        key(bill.key) {
                            BillLineEditor(
                                name = bill.name,
                                onNameChange = { onIntent(ExpensesIntent.EditBill(index, name = it)) },
                                amount = bill.amount,
                                onAmountChange = { onIntent(ExpensesIntent.EditBill(index, amount = it)) },
                                symbol = state.entrySymbol,
                                icon = bill.icon.vector(),
                                nameLabel = nameLabel,
                                amountLabel = amountLabel,
                                removeLabel = removeLabel,
                                onRemove = { onIntent(ExpensesIntent.RemoveBill(index)) },
                            )
                        }
                    }
                    DashedButton(stringResource(R.string.expenses_bills_add), Icons.Filled.Add, onClick = { onIntent(ExpensesIntent.AddBill) })
                    HWButton(
                        text = stringResource(R.string.expenses_bills_action),
                        onClick = { onIntent(ExpensesIntent.SaveBills(category.id)) },
                        icon = Icons.Filled.Check,
                        isLoading = state.isWriting,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    category.lines.forEach { line ->
                        key(line.id) { BillLine(line.name, line.amount.display, line.icon.vector()) }
                    }
                }
            }
        }
    }
}

// endregion

// region fixed

@Composable
private fun FixedKind(state: ExpensesState, category: ExpensesScreen.Category, onIntent: (ExpensesIntent) -> Unit) {
    HWCard {
        CardCaption(stringResource(R.string.expenses_fixed_caption))
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Explain(stringResource(R.string.expenses_fixed_explain))
            if (state.isEditing) {
                val label = stringResource(R.string.expenses_fixed_label)
                HWMoneyField(
                    text = state.fixedAmount,
                    onTextChange = { onIntent(ExpensesIntent.EditFixedAmount(it)) },
                    currencyCode = state.entryCode,
                    prominent = true,
                    modifier = Modifier.semantics { contentDescription = label },
                )
                HWButton(
                    text = stringResource(R.string.expenses_fixed_action),
                    onClick = { onIntent(ExpensesIntent.SaveFixedAmount(category.id)) },
                    icon = Icons.Filled.Check,
                    isLoading = state.isWriting,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                FixedAmount(amount = category.total.display, label = category.name)
            }
        }
    }
}

// endregion

