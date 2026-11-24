package com.hisaabwise.feature.expenses.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.hisaabwise.feature.expenses.ExpensesScreen

/** The server's category and bill glyph names, as Material icons (iOS `ExpensesView.symbol`). */
fun ExpensesScreen.Icon.vector(): ImageVector = when (this) {
    ExpensesScreen.Icon.Groceries -> Icons.Outlined.ShoppingBasket
    ExpensesScreen.Icon.Transport -> Icons.Outlined.DirectionsBus
    ExpensesScreen.Icon.Entertainment -> Icons.Outlined.Movie
    ExpensesScreen.Icon.Other -> Icons.AutoMirrored.Outlined.HelpOutline
    // Money coming in; down carries no direction to mirror.
    ExpensesScreen.Icon.Income -> Icons.Outlined.Download
    ExpensesScreen.Icon.Utilities, ExpensesScreen.Icon.Bolt -> Icons.Outlined.Bolt
    ExpensesScreen.Icon.Rent -> Icons.Outlined.Home
    ExpensesScreen.Icon.Drop -> Icons.Outlined.WaterDrop
    ExpensesScreen.Icon.Signal -> Icons.Outlined.Smartphone
    ExpensesScreen.Icon.Tag -> Icons.Outlined.LocalOffer
}

/** The leading glyph per field shape. */
fun ExpensesScreen.Field.glyph(): ImageVector = when (this) {
    ExpensesScreen.Field.Place, ExpensesScreen.Field.Source -> Icons.Outlined.LocalOffer
    ExpensesScreen.Field.TransportMode -> Icons.Outlined.DirectionsBus
    ExpensesScreen.Field.OtherType -> Icons.AutoMirrored.Outlined.HelpOutline
}
