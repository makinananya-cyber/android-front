package com.hisaabwise.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.MinTouchTarget

enum class HWButtonStyle {
    /** Filled: deep navy on surface screens, cream on brand screens. */
    Primary,
    /** Outlined white pill ("Edit goal", "Log out"). */
    Secondary,
    /** Text only ("Register", "Sign in instead"). */
    Ghost,
    /** Red text in a white pill ("Log out", "Delete my account"). */
    Destructive,
    /** Green success button (lesson Continue). */
    Success,
}

/**
 * The one button. Shows a spinner and blocks taps while [isLoading]. Presses scale to 0.97
 * (none under Reduce Motion).
 */
@Composable
fun HWButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HWButtonStyle = HWButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    fillWidth: Boolean = true,
) {
    val brand = LocalAppearance.current == Appearance.Brand
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = LocalReduceMotion.current
    val scale by animateFloatAsState(if (pressed && !reduceMotion) 0.97f else 1f, label = "press")

    val (container, content, border) = when (style) {
        HWButtonStyle.Primary -> if (brand) Triple(HWColors.BrandInk, HWColors.BrandBackground, null)
        else Triple(HWColors.AccentDeep, HWColors.Surface, null)
        HWButtonStyle.Secondary -> if (brand) Triple(HWColors.BrandSurface, HWColors.BrandInk, BorderStroke(1.dp, HWColors.BrandSeparatorStrong))
        else Triple(HWColors.Surface, HWColors.AccentDeep, BorderStroke(1.dp, HWColors.SeparatorStrong))
        HWButtonStyle.Destructive -> Triple(HWColors.Surface, HWColors.Danger, BorderStroke(1.dp, HWColors.Separator))
        HWButtonStyle.Success -> Triple(Color(0xFF2FA37C), HWColors.Surface, null)
        HWButtonStyle.Ghost -> Triple(Color.Transparent, if (brand) HWColors.BrandInkAccent else HWColors.Accent, null)
    }
    val inFlight = stringResource(R.string.component_button_inFlight)
    val widthModifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier
    val semantics = Modifier.semantics {
        contentDescription = text
        if (isLoading) stateDescription = inFlight
    }

    if (style == HWButtonStyle.Ghost) {
        TextButton(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier.then(widthModifier).heightIn(min = MinTouchTarget).scale(scale).then(semantics),
            interactionSource = interaction,
            colors = ButtonDefaults.textButtonColors(contentColor = content),
        ) { ButtonContent(text, icon, isLoading, content) }
        return
    }
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.then(widthModifier).heightIn(min = 54.dp).scale(scale).then(semantics),
        shape = HWRadius.Large,
        border = border,
        interactionSource = interaction,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = if (container == Color.Transparent) 0f else 0.5f),
            disabledContentColor = content.copy(alpha = 0.6f),
        ),
    ) { ButtonContent(text, icon, isLoading, content) }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, isLoading: Boolean, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = color, strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = HWType.BodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    }
}

/** Small round icon button (sheet close, back). */
@Composable
fun HWIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val brand = LocalAppearance.current == Appearance.Brand
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier
            .size(MinTouchTarget)
            .background(if (brand) HWColors.BrandSurface else HWColors.Surface, HWRadius.Medium),
    ) {
        Icon(icon, contentDescription, tint = if (brand) HWColors.BrandInk else HWColors.Ink)
    }
}
