package com.hisaabwise.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType

/** Bottom sheet with a title and close button, always on the light surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HWSheet(
    title: String,
    onDismiss: () -> Unit,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = HWColors.Background) {
        CompositionLocalProvider(LocalAppearance provides Appearance.Surface) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = HWType.Heading, color = HWColors.Ink, modifier = Modifier.weight(1f).semantics { heading() })
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, stringResource(R.string.component_sheet_close), tint = HWColors.Ink)
                    }
                }
                content()
            }
        }
    }
}

/**
 * A searchable list picker (currencies, countries, security questions, picklist options).
 * A search box appears when there are more than 12 options, as on iOS.
 */
@Composable
fun <T> HWPickerSheet(
    title: String,
    options: List<T>,
    label: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    detail: ((T) -> String?)? = null,
    searchPlaceholder: String = "",
) {
    var query by remember { mutableStateOf("") }
    HWSheet(title, onDismiss) {
        if (options.size > 12) {
            HWTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = searchPlaceholder,
                imeAction = ImeAction.Search,
                leading = { Icon(Icons.Filled.Search, null, tint = HWColors.InkSecondary) },
                trailing = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.component_picker_clearSearch), tint = HWColors.InkSecondary)
                    }
                },
            )
        }
        val filtered = remember(query, options) {
            if (query.isBlank()) options
            else options.filter { label(it).contains(query.trim(), ignoreCase = true) || detail?.invoke(it)?.contains(query.trim(), ignoreCase = true) == true }
        }
        if (filtered.isEmpty()) {
            Text(stringResource(R.string.component_picker_empty), style = HWType.Body, color = HWColors.InkSecondary, modifier = Modifier.padding(vertical = 24.dp))
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            items(filtered) { option ->
                val selected = isSelected(option)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(role = Role.RadioButton) { onSelect(option) }
                        .semantics { this.selected = selected }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label(option), style = HWType.BodyLarge, color = HWColors.Ink)
                        detail?.invoke(option)?.let { Text(it, style = HWType.Caption, color = HWColors.InkSecondary) }
                    }
                    if (selected) Icon(Icons.Filled.Check, null, tint = HWColors.Accent)
                }
                HorizontalDivider(color = HWColors.Separator)
            }
        }
    }
}
