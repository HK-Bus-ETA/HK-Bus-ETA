package com.loohp.hkbuseta.presentation.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.wear.compose.material.ButtonColors
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.LocalContentAlpha
import androidx.wear.compose.material.LocalContentColor
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.MaterialTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdvanceButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.primaryButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(
                minWidth = ButtonDefaults.DefaultButtonSize,
                minHeight = ButtonDefaults.DefaultButtonSize
            )
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = rememberRipple(),
            )
            .combinedClickable(
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = Role.Button,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onDoubleClick = onDoubleClick,
                onClick = onClick
            )
            .background(
                color = colors.backgroundColor(enabled = enabled).value,
                shape = CircleShape
            )
    ) {
        val contentColor = colors.contentColor(enabled = enabled).value
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalContentAlpha provides contentColor.alpha,
            LocalTextStyle provides MaterialTheme.typography.button,
        ) {
            content()
        }
    }
}