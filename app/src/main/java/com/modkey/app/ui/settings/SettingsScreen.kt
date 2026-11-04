package com.modkey.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.AccountMeInfo
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyDialog
import com.modkey.app.ui.components.ModkeyFilterChip
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    currentAccount: SavedAccount,
    allAccounts: List<SavedAccount>,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onManageAccounts: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val apiClient = ModkeyApp.instance.apiClient
    var meInfo by remember { mutableStateOf<AccountMeInfo?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(currentAccount.id) {
        try {
            meInfo = apiClient.getMe(currentAccount.apiKey)
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ModkeyDesign.Colors.background)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Настройки",
                    subtitle = "Параметры приложения и учетная запись"
                )
            }

            // Current Profile Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ModkeyDesign.Colors.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(ModkeyDesign.Colors.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                ModkeyIcon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = ModkeyDesign.Colors.primary,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meInfo?.username?.ifBlank { currentAccount.label } ?: currentAccount.label,
                                    style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = buildString {
                                        append("Ключ: ••••••••${currentAccount.apiKey.takeLast(6)}")
                                        if (meInfo != null) {
                                            append(" • Баланс: ${meInfo!!.balance} ₽")
                                        }
                                        if (!meInfo?.telegramId.isNullOrBlank()) {
                                            append(" • TG ID: ${meInfo!!.telegramId}")
                                        }
                                    },
                                    style = ModkeyDesign.Typography.bodySmall(),
                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ModkeyButton(
                                onClick = onManageAccounts,
                                variant = com.modkey.app.ui.components.ButtonVariant.Secondary,
                                shape = RoundedCornerShape(14.dp),
                                minHeight = 44.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Сменить профиль",
                                    style = ModkeyDesign.Typography.labelLarge(),
                                    color = ModkeyDesign.Colors.onSurface
                                )
                            }

                            ModkeyButton(
                                onClick = { showLogoutConfirm = true },
                                variant = com.modkey.app.ui.components.ButtonVariant.Danger,
                                shape = RoundedCornerShape(14.dp),
                                minHeight = 44.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                ModkeyIcon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = ModkeyDesign.Colors.onError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Выход",
                                    style = ModkeyDesign.Typography.labelLarge(),
                                    color = ModkeyDesign.Colors.onError
                                )
                            }
                        }
                    }
                }
            }

            // Appearance
            item {
                SectionHeader(
                    title = "Оформление",
                    subtitle = "Тема интерфейса приложения"
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ModkeyDesign.Colors.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Цветовая тема",
                            style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                ModkeyFilterChip(
                                    selected = themeMode == mode,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        onThemeModeChange(mode)
                                    },
                                    label = mode.label,
                                    leadingIcon = when (mode) {
                                        ThemeMode.SYSTEM -> Icons.Filled.SettingsBrightness
                                        ThemeMode.LIGHT -> Icons.Filled.LightMode
                                        ThemeMode.DARK -> Icons.Filled.DarkMode
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Links & Resources
            item {
                SectionHeader(
                    title = "Ресурсы платформы",
                    subtitle = "Официальные сайты и документация"
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ModkeyDesign.Colors.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        LinkRow(
                            icon = Icons.Filled.Language,
                            label = "Официальный сайт (mod.wtf)",
                            url = "https://mod.wtf",
                            context = context
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinkRow(
                            icon = Icons.Filled.Info,
                            label = "Разработка и API (dev.mod.wtf)",
                            url = "https://dev.mod.wtf",
                            context = context
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding().padding(bottom = 104.dp))
            }
        }
    }

    // Logout confirmation
    if (showLogoutConfirm) {
        ModkeyDialog(
            title = "Выйти из аккаунта?",
            onDismiss = { showLogoutConfirm = false },
            confirmLabel = "Выйти",
            confirmContainerColor = ModkeyDesign.Colors.error,
            confirmContentColor = ModkeyDesign.Colors.onError,
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            }
        ) {
            Text(
                text = "Активная сессия будет закрыта. Вы всегда сможете войти снова — сохранённые аккаунты доступны для быстрого входа.",
                style = ModkeyDesign.Typography.bodyMedium(),
                color = ModkeyDesign.Colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    url: String,
    context: android.content.Context
) {
    Surface(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        shape = RoundedCornerShape(14.dp),
        color = ModkeyDesign.Colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModkeyIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ModkeyDesign.Colors.primary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = ModkeyDesign.Typography.bodyMedium()
                )
            }
            ModkeyIcon(
                imageVector = Icons.Filled.OpenInBrowser,
                contentDescription = null,
                tint = ModkeyDesign.Colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
