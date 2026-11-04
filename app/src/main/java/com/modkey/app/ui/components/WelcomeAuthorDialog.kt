package com.modkey.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.modkey.app.R
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.modkeyPress

@Composable
fun WelcomeAuthorDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val primaryColor = ModkeyDesign.Colors.primary
    val primaryContainerColor = ModkeyDesign.Colors.primaryContainer

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val slideProgress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            slideProgress.animateTo(
                1f,
                spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.75f * slideProgress.value }
                    .background(Color(0xFF050508))
                    .modkeyPress(onClick = onDismiss)
            )

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = ModkeyDesign.Colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, ModkeyDesign.Colors.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        val eased = slideProgress.value
                        scaleX = 0.85f + 0.15f * eased
                        scaleY = 0.85f + 0.15f * eased
                        alpha = eased
                    }
            ) {
                CloseButton(onDismiss = onDismiss)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar with subtle glow
                    Box(
                        modifier = Modifier.size(136.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(136.dp)) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val avatarRadius = 48.dp.toPx()

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.22f),
                                        primaryColor.copy(alpha = 0.06f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = avatarRadius + 14.dp.toPx()
                                ),
                                radius = avatarRadius + 14.dp.toPx(),
                                center = center
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = primaryContainerColor,
                            modifier = Modifier
                                .size(96.dp)
                                .border(
                                    width = 2.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            primaryColor,
                                            primaryColor.copy(alpha = 0.4f),
                                            ModkeyDesign.Colors.onPrimary,
                                            primaryColor
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.author_avatar),
                                contentDescription = "lonxzsy",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(CircleShape)
                            )
                        }
                    }

                    // Author Header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "lonxzsy",
                            style = ModkeyDesign.Typography.headlineLarge().copy(fontWeight = FontWeight.ExtraBold),
                            color = ModkeyDesign.Colors.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ModkeyDesign.Colors.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "Разработчик Modkey App",
                                style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.SemiBold),
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Message Text
                    Text(
                        text = "Привет! Это приложение было написано мной специально для пользователей панели Modkey, чтобы сделать работу с софтом, лицензиями и реселлерами проще прямо с телефона.\n\nЕсли приложение понравилось и полезно в работе — вы всегда можете написать мне и сказать спасибо в лс!",
                        style = ModkeyDesign.Typography.bodyMedium(),
                        textAlign = TextAlign.Center,
                        color = ModkeyDesign.Colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Actions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ModkeyButton(
                            onClick = {
                                val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/lonxzsy"))
                                try {
                                    context.startActivity(telegramIntent)
                                } catch (_: Exception) {}
                            },
                            variant = ButtonVariant.Primary,
                            shape = RoundedCornerShape(18.dp),
                            minHeight = 58.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ModkeyIcon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = ModkeyDesign.Colors.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "Сказать спасибо в лс",
                                    style = ModkeyDesign.Typography.labelLarge().copy(fontWeight = FontWeight.Bold),
                                    color = ModkeyDesign.Colors.onPrimary
                                )
                                Text(
                                    text = "Telegram: @lonxzsy",
                                    style = ModkeyDesign.Typography.labelSmall(),
                                    color = Color(0xFF003B47)
                                )
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                        }

                        ModkeyButton(
                            onClick = onDismiss,
                            variant = ButtonVariant.Outline,
                            shape = RoundedCornerShape(18.dp),
                            minHeight = 52.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ModkeyIcon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = ModkeyDesign.Colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Начать пользоваться",
                                style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold),
                                color = ModkeyDesign.Colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloseButton(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, end = 14.dp, start = 14.dp)
    ) {
        ModkeyIconButton(
            onClick = onDismiss,
            size = 32.dp,
            iconSize = 18.dp,
            containerColor = ModkeyDesign.Colors.surfaceContainer,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            ModkeyIcon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Закрыть",
                tint = ModkeyDesign.Colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
