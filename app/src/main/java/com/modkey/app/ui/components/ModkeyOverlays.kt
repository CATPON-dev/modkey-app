package com.modkey.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.modkeyPress

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  TEXT — foundation-only text with Modkey default styling (no Material)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = ModkeyDesign.Typography.bodyMedium(),
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    fontSize: androidx.compose.ui.unit.TextUnit? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) {
    val resolved = style.copy(
        color = when {
            color != Color.Unspecified -> color
            style.color != Color.Unspecified -> style.color
            else -> ModkeyDesign.Colors.onSurface
        },
        fontWeight = fontWeight ?: style.fontWeight,
        fontFamily = fontFamily ?: style.fontFamily,
        fontSize = fontSize ?: style.fontSize,
        textAlign = textAlign ?: style.textAlign
    )
    androidx.compose.foundation.text.BasicText(
        text = text,
        modifier = modifier,
        style = resolved,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = onTextLayout
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  SWITCH — custom toggle (replaces Material3 Switch)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = ModkeyDesign.Colors.primary
) {
    val view = LocalView.current
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 28.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "modkeySwitchThumb"
    )
    val trackColor = if (checked) activeColor else ModkeyDesign.Colors.surfaceContainerHighest
    val borderColor = if (checked) activeColor.copy(alpha = 0.6f) else ModkeyDesign.Colors.outline

    Box(
        modifier = modifier
            .width(58.dp)
            .height(32.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(ModkeyDesign.Shapes.full)
            .background(trackColor)
            .border(1.dp, borderColor, ModkeyDesign.Shapes.full)
            .modkeyPress(enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(26.dp)
                .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(ModkeyDesign.Colors.onSurface)
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  CHECKBOX — custom check (replaces Material3 Checkbox)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeyCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(ModkeyDesign.Shapes.extraSmall)
            .background(if (checked) ModkeyDesign.Colors.primary else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (checked) ModkeyDesign.Colors.primary
                else ModkeyDesign.Colors.onSurfaceVariant.copy(alpha = 0.6f),
                shape = ModkeyDesign.Shapes.extraSmall
            )
            .modkeyPress(enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            ModkeyIcon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = ModkeyDesign.Colors.onPrimary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  FILTER CHIP — selectable chip (replaces Material3 FilterChip)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeyFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    val view = LocalView.current
    val container = if (selected) ModkeyDesign.Colors.primaryContainer else ModkeyDesign.Colors.surfaceContainerHigh
    val contentColor = if (selected) ModkeyDesign.Colors.primary else ModkeyDesign.Colors.onSurfaceVariant
    val borderColor = if (selected) ModkeyDesign.Colors.primary.copy(alpha = 0.45f)
    else ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .clip(ModkeyDesign.Shapes.small)
            .background(container, ModkeyDesign.Shapes.small)
            .border(1.dp, borderColor, ModkeyDesign.Shapes.small)
            .modkeyPress(enabled = enabled) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                ModkeyIcon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  LINEAR PROGRESS (replaces Material3 LinearProgressIndicator)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeyLinearProgress(
    modifier: Modifier = Modifier,
    color: Color = ModkeyDesign.Colors.primary,
    isIndeterminate: Boolean = true
) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "modkeyLinear")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "modkeyLinearProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(ModkeyDesign.Shapes.full)
            .background(ModkeyDesign.Colors.surfaceContainerHighest)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(3.dp)
                .graphicsLayer {
                    val shift = if (isIndeterminate) progress else 0.5f
                    translationX = (size.width * (shift * 2f - 1f)).coerceAtLeast(0f)
                }
                .clip(ModkeyDesign.Shapes.full)
                .background(color)
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  FAB — floating action button (replaces Material3 FloatingActionButton)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeyFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable () -> Unit = {
        if (icon != null) {
            ModkeyIcon(
                imageVector = icon,
                contentDescription = null,
                tint = ModkeyDesign.Colors.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(ModkeyDesign.Colors.primary, CircleShape)
            .modkeyPress {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  TEXT BUTTON — ghost label button (replaces Material3 TextButton)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeyTextButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = ModkeyDesign.Colors.primary,
    enabled: Boolean = true
) {
    Text(
        text = label,
        style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.Bold),
        color = color,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .modkeyPress(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  DIALOG — custom modal (replaces Material3 AlertDialog)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeyDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmContainerColor: Color = ModkeyDesign.Colors.primary,
    confirmContentColor: Color = ModkeyDesign.Colors.onPrimary,
    dismissLabel: String = "Отмена",
    showDismiss: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.modkey.app.ui.components.Surface(
            shape = ModkeyDesign.Shapes.large,
            color = ModkeyDesign.Colors.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = title,
                    style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.Bold),
                    color = ModkeyDesign.Colors.onSurface
                )
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showDismiss) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(ModkeyDesign.Shapes.medium)
                                .background(ModkeyDesign.Colors.surfaceContainerHigh, ModkeyDesign.Shapes.medium)
                                .modkeyPress(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dismissLabel,
                                style = ModkeyDesign.Typography.labelLarge(),
                                color = ModkeyDesign.Colors.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                    val view = LocalView.current
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(ModkeyDesign.Shapes.medium)
                            .background(confirmContainerColor, ModkeyDesign.Shapes.medium)
                            .modkeyPress {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onConfirm()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmLabel,
                            style = ModkeyDesign.Typography.labelLarge(),
                            color = confirmContentColor,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  BOTTOM SHEET — custom modal sheet (replaces Material3 ModalBottomSheet)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun ModkeyBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val slideProgress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            slideProgress.animateTo(
                1f,
                spring(
                    dampingRatio = 0.9f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(slideProgress.value * 0.75f)
                    .background(Color(0xFF050508))
                    .clickable { onDismiss() }
            )

            Column(
                modifier = modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = (1f - slideProgress.value) * 800f
                        alpha = 0.35f + 0.65f * slideProgress.value
                    }
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(ModkeyDesign.Colors.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(top = 18.dp, bottom = 20.dp, start = 0.dp, end = 0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                content = content
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  SHEET HEADER — standard sheet top row w/ drag handle + close
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 2.dp)
                .size(width = 44.dp, height = 5.dp)
                .clip(ModkeyDesign.Shapes.full)
                .background(ModkeyDesign.Colors.outline.copy(alpha = 0.9f))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (leading != null) {
                    leading()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.Bold),
                        color = ModkeyDesign.Colors.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = ModkeyDesign.Typography.bodySmall(),
                            color = ModkeyDesign.Colors.onSurfaceVariant
                        )
                    }
                }
            }
            ModkeyIconButton(onClick = onClose, size = 36.dp, iconSize = 20.dp) {
                ModkeyIcon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Закрыть",
                    tint = ModkeyDesign.Colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}
