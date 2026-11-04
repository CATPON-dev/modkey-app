package com.modkey.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import com.modkey.app.MainDestination
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.modkeyPress

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  Modkey Components — pure Compose Foundation + custom design tokens.
//  No Material imports.
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.ExtraBold),
                color = ModkeyDesign.Colors.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = ModkeyDesign.Typography.bodySmall(),
                    color = ModkeyDesign.Colors.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Box(
                modifier = Modifier
                    .clip(ModkeyDesign.Shapes.small)
                    .background(ModkeyDesign.Colors.primaryContainer)
                    .border(1.dp, ModkeyDesign.Colors.primary.copy(alpha = 0.35f), ModkeyDesign.Shapes.small)
                    .modkeyPress(onClick = onActionClick)
            ) {
                Text(
                    text = actionLabel,
                    style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.Bold),
                    color = ModkeyDesign.Colors.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    containerColor: Color = ModkeyDesign.Colors.surfaceContainerHigh,
    contentColor: Color = ModkeyDesign.Colors.onSurface,
    badgeColor: Color = ModkeyDesign.Colors.primary,
    onClick: (() -> Unit)? = null
) {
    val view = LocalView.current
    Surface(
        onClick = if (onClick != null) {
            {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        } else null,
        enabled = onClick != null,
        shape = ModkeyDesign.Shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.SemiBold),
                    color = ModkeyDesign.Colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(ModkeyDesign.Shapes.small)
                        .background(badgeColor.copy(alpha = 0.15f), ModkeyDesign.Shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    ModkeyIcon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = ModkeyDesign.Typography.headlineLarge().copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                ),
                color = contentColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = ModkeyDesign.Typography.bodySmall(),
                    color = ModkeyDesign.Colors.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(ModkeyDesign.Shapes.extraSmall)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = ModkeyDesign.Typography.labelSmall().copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ModkeyDesign.Colors.surfaceContainerHigh,
    contentColor: Color = ModkeyDesign.Colors.onSurface
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .clip(ModkeyDesign.Shapes.medium)
            .background(containerColor, ModkeyDesign.Shapes.medium)
            .border(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f), ModkeyDesign.Shapes.medium)
            .modkeyPress {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModkeyIcon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

@Composable
fun LoadingBlock(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularLoader(modifier = Modifier.size(42.dp))
        if (message != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                style = ModkeyDesign.Typography.bodyMedium(),
                color = ModkeyDesign.Colors.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(ModkeyDesign.Colors.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            ModkeyIcon(
                imageVector = icon,
                contentDescription = null,
                tint = ModkeyDesign.Colors.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = title,
            style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = ModkeyDesign.Typography.bodySmall(),
            color = ModkeyDesign.Colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  SURFACE (replaces Material3 Surface)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun Surface(
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = ModkeyDesign.Shapes.medium,
    color: Color = ModkeyDesign.Colors.surface,
    border: BorderStroke? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val clickModifier = if (onClick != null && enabled) {
        modifier.modkeyPress(enabled = enabled, onClick = onClick)
    } else {
        modifier
    }
    val drawn = clickModifier
        .clip(shape)
        .background(color, shape)
    val bordered = if (border != null) {
        drawn.border(border.width, border.brush, shape)
    } else drawn

    Box(modifier = bordered) {
        content()
    }
}

@Composable
fun SurfaceChip(
    onClick: (() -> Unit)? = null,
    shape: Shape = ModkeyDesign.Shapes.medium,
    color: Color = ModkeyDesign.Colors.surfaceContainer,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val clickModifier = if (onClick != null) modifier.modkeyPress(onClick = onClick) else modifier
    Box(
        modifier = clickModifier
            .clip(shape)
            .background(color, shape)
    ) {
        content()
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  LOADER (replaces Material3 CircularProgressIndicator)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun CircularLoader(
    modifier: Modifier = Modifier,
    color: Color = ModkeyDesign.Colors.primary,
    size: Dp = 40.dp
) {
    val transition = rememberInfiniteTransition(label = "modkeyLoader")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing)
        ),
        label = "modkeyLoaderAngle"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .rotate(angle)
        ) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 100f,
                useCenter = false,
                style = Stroke(
                    width = size.toPx() * 0.085f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  BOTTOM NAVIGATION SCAFFOLD (replaces Material3 Scaffold + NavigationBar)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun BottomScaffold(
    currentDestination: MainDestination,
    onDestinationClick: (MainDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ModkeyDesign.Colors.background)
    ) {
        content(PaddingValues(bottom = 84.dp))

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(ModkeyDesign.Shapes.extraLarge)
                    .background(ModkeyDesign.Colors.surfaceContainer)
                    .border(
                        1.dp,
                        ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.4f),
                        ModkeyDesign.Shapes.extraLarge
                    )
                    .width(340.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MainDestination.entries.forEach { dest ->
                        val isSelected = currentDestination == dest
                        val tint = if (isSelected) ModkeyDesign.Colors.primary
                        else ModkeyDesign.Colors.onSurfaceVariant

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(ModkeyDesign.Shapes.small)
                                .modkeyPress {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onDestinationClick(dest)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            ModkeyIcon(
                                imageVector = when (dest) {
                                    MainDestination.DASHBOARD -> Icons.Filled.Dashboard
                                    MainDestination.KEYS -> Icons.Filled.VpnKey
                                    MainDestination.USERS -> Icons.Filled.People
                                    MainDestination.CONNECT -> Icons.Filled.BugReport
                                    MainDestination.SETTINGS -> Icons.Filled.Settings
                                },
                                contentDescription = dest.label,
                                tint = tint,
                                modifier = Modifier.size(23.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dest.label,
                                style = ModkeyDesign.Typography.labelSmall().copy(fontSize = 9.sp),
                                color = tint,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
