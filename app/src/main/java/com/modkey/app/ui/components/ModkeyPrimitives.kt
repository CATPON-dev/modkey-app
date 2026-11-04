package com.modkey.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.NataSans
import com.modkey.app.ui.theme.modkeyPress

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  ICON
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeyIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color = ModkeyDesign.Colors.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    val painter = rememberVectorPainter(imageVector)
    androidx.compose.foundation.Image(
        painter = painter,
        contentDescription = contentDescription,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
        modifier = modifier
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  BUTTON
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

enum class ButtonVariant { Primary, Secondary, Outline, Ghost, Danger }

@Composable
fun ModkeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Primary,
    shape: Shape = ModkeyDesign.Shapes.medium,
    minHeight: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val bg = when (variant) {
        ButtonVariant.Primary -> ModkeyDesign.Colors.primary
        ButtonVariant.Secondary -> ModkeyDesign.Colors.surfaceContainerHigh
        ButtonVariant.Outline, ButtonVariant.Ghost -> Color.Transparent
        ButtonVariant.Danger -> ModkeyDesign.Colors.error
    }
    val border = when (variant) {
        ButtonVariant.Primary -> BorderStroke(1.dp, ModkeyDesign.Colors.primary)
        ButtonVariant.Outline -> BorderStroke(1.dp, ModkeyDesign.Colors.primary.copy(alpha = 0.6f))
        ButtonVariant.Secondary -> BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.6f))
        else -> null
    }

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .alpha(if (enabled) 1f else 0.45f)
            .background(bg, shape)
            .clip(shape)
            .then(if (border != null) Modifier.border(border.width, border.brush, shape) else Modifier)
            .modkeyPress(enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  ICON BUTTON
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeyIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    tint: Color = ModkeyDesign.Colors.onSurfaceVariant,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(ModkeyDesign.Shapes.extraSmall)
            .background(containerColor, ModkeyDesign.Shapes.extraSmall)
            .modkeyPress(enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  TEXT FIELD (outlined, Foundation — replaces Material3 OutlinedTextField)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeyOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    shape: Shape = ModkeyDesign.Shapes.medium,
    keyboardOptions: KeyboardOptions? = null,
    keyboardType: KeyboardType? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    error: Boolean = false
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = if (error) ModkeyDesign.Colors.error
    else if (isFocused) ModkeyDesign.Colors.primary
    else ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.55f)
    val textColor = if (enabled) ModkeyDesign.Colors.onSurface else ModkeyDesign.Colors.onSurfaceDisabled

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (label != null) {
            Text(
                text = label,
                style = ModkeyDesign.Typography.labelSmall().copy(fontSize = 12.sp),
                color = if (isFocused || value.isNotEmpty()) ModkeyDesign.Colors.primary
                else ModkeyDesign.Colors.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(if (enabled) ModkeyDesign.Colors.surface else ModkeyDesign.Colors.surfaceContainer, shape)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (leadingIcon != null) {
                    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        leadingIcon()
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = ModkeyDesign.Typography.bodyMedium(),
                            color = ModkeyDesign.Colors.onSurfaceVariant.copy(alpha = 0.55f),
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        textStyle = TextStyle(
                            fontFamily = NataSans,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = textColor
                        ),
                        keyboardOptions = keyboardOptions
                            ?: KeyboardOptions(
                                keyboardType = keyboardType ?: KeyboardType.Text
                            ),
                        visualTransformation = visualTransformation,
                        singleLine = singleLine,
                        interactionSource = interactionSource,
                        cursorBrush = SolidColor(ModkeyDesign.Colors.primary)
                    )
                }
                if (trailingIcon != null) {
                    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        trailingIcon()
                    }
                }
            }
        }
    }
}
