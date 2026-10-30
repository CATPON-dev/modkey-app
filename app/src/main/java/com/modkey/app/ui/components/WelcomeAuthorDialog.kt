package com.modkey.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.modkey.app.R
import kotlin.math.cos
import kotlin.math.sin

private data class SubtleSparkle(
    val angleDeg: Float,
    val speed: Float,
    val radiusPx: Float,
    val phase: Float,
    val colorIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeAuthorDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "sparkles")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animProgress"
    )

    val breathingGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingGlow"
    )

    // 18 neat, tiny, delicate sparkles around the avatar border
    val sparkles = remember {
        List(18) { i ->
            SubtleSparkle(
                angleDeg = i * (360f / 18f) + ((i * 13) % 9 - 4f),
                speed = 0.75f + ((i * 3) % 4) * 0.1f,
                radiusPx = 1.2f + ((i % 3) * 0.4f), // 1.2dp to 2.0dp — tiny, sharp and elegant!
                phase = (i * 0.23f) % 1f,
                colorIndex = i % 3
            )
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar with delicate subtle sparkles
                Box(
                    modifier = Modifier.size(136.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(136.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val avatarRadius = 48.dp.toPx()
                        val maxFlightDist = 18.dp.toPx()

                        // Soft subtle ambient glow right behind the avatar border
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.22f * breathingGlow),
                                    primaryColor.copy(alpha = 0.06f * breathingGlow),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = avatarRadius + 14.dp.toPx()
                            ),
                            radius = avatarRadius + 14.dp.toPx(),
                            center = center
                        )

                        // Draw fine, delicate micro-sparkles
                        sparkles.forEach { s ->
                            val progress = (animProgress * s.speed + s.phase) % 1f
                            val currentDist = avatarRadius + 2.dp.toPx() + (progress * maxFlightDist)
                            val rad = Math.toRadians(s.angleDeg.toDouble())
                            val x = center.x + (currentDist * cos(rad)).toFloat()
                            val y = center.y + (currentDist * sin(rad)).toFloat()

                            // Smooth alpha envelope: fade in softly, fade out gently
                            val alpha = when {
                                progress < 0.25f -> progress / 0.25f
                                progress > 0.7f -> (1f - progress) / 0.3f
                                else -> 1f
                            }.coerceIn(0f, 1f)

                            val sparkleColor = when (s.colorIndex) {
                                0 -> primaryColor
                                1 -> tertiaryColor
                                else -> Color(0xFFE0E0FF)
                            }

                            // Tiny micro particle
                            drawCircle(
                                color = sparkleColor.copy(alpha = alpha * 0.85f),
                                radius = s.radiusPx.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }

                    // Avatar Surface with delicate gradient border
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
                                        tertiaryColor,
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
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Разработчик Modkey App",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Message Text without the rocket emoji
                Text(
                    text = "Привет! Это приложение было написано мной специально для пользователей панели Modkey, чтобы сделать работу с софтом, лицензиями и реселлерами проще прямо с телефона.\n\nЕсли приложение понравилось и полезно в работе — вы всегда можете написать мне и сказать спасибо в лс!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: Telegram PM (clean multi-line text layout) + Start using
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val telegramIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/lonxzsy"))
                            try {
                                context.startActivity(telegramIntent)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Сказать спасибо в лс",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Telegram: @lonxzsy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Начать пользоваться",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
