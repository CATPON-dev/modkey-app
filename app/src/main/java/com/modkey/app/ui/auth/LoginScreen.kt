package com.modkey.app.ui.auth

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.core.network.ModkeyWebAuth
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyIconButton
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.components.CircularLoader
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.SurfaceChip
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.connectedItemShape
import com.modkey.app.ui.theme.modkeyPress
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun LoginScreen(
    savedAccounts: List<SavedAccount>,
    onLoginSuccess: (SavedAccount) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient
    val accountManager = ModkeyApp.instance.accountManager

    var selectedTab by remember { mutableIntStateOf(0) } // 0: API Key, 1: Login/Password, 2: Web/Telegram

    var apiKeyInput by remember { mutableStateOf("") }
    var accountLabelInput by remember { mutableStateOf("") }

    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showWebLoginSheet by remember { mutableStateOf(false) }

    fun validateApiKeyAndProceed(key: String, label: String) {
        val trimmedKey = key.trim()
        if (trimmedKey.isEmpty()) {
            validationError = "Введите API ключ"
            return
        }

        isValidating = true
        validationError = null

        scope.launch {
            try {
                val stats = apiClient.getOverviewStats(trimmedKey)
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

                val account = SavedAccount(
                    id = UUID.randomUUID().toString(),
                    label = if (label.isNotBlank()) label.trim() else "Аккаунт (${stats.softsCount} софтов)",
                    apiKey = trimmedKey,
                    lastActive = System.currentTimeMillis()
                )
                accountManager.saveOrUpdateAccount(account)
                onLoginSuccess(account)
            } catch (e: Exception) {
                validationError = e.message ?: "Ошибка авторизации. Проверьте правильность API ключа."
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } finally {
                isValidating = false
            }
        }
    }

    fun loginWithUsernamePassword() {
        if (usernameInput.isBlank() || passwordInput.isBlank()) {
            validationError = "Заполните логин и пароль"
            return
        }

        isValidating = true
        validationError = null

        scope.launch {
            try {
                val extractedKey = ModkeyWebAuth.loginWithCredentials(
                    username = usernameInput.trim(),
                    password = passwordInput
                )
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

                val stats = apiClient.getOverviewStats(extractedKey)
                val account = SavedAccount(
                    id = UUID.randomUUID().toString(),
                    label = usernameInput.trim(),
                    apiKey = extractedKey,
                    username = usernameInput.trim(),
                    lastActive = System.currentTimeMillis()
                )
                accountManager.saveOrUpdateAccount(account)
                onLoginSuccess(account)
            } catch (e: Exception) {
                validationError = e.message ?: "Не удалось войти на mod.wtf. Проверьте логин и пароль."
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } finally {
                isValidating = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ModkeyDesign.Colors.background)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(ModkeyDesign.Colors.primaryContainer, RoundedCornerShape(26.dp))
                            .border(
                                1.dp,
                                ModkeyDesign.Colors.primary.copy(alpha = 0.35f),
                                RoundedCornerShape(26.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ModkeyIcon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = ModkeyDesign.Colors.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Modkey Platform",
                        style = ModkeyDesign.Typography.headlineLarge().copy(fontWeight = FontWeight.ExtraBold),
                        color = ModkeyDesign.Colors.onSurface
                    )
                    Text(
                        text = "Управление софтом, лицензиями и клиентами",
                        style = ModkeyDesign.Typography.bodySmall(),
                        color = ModkeyDesign.Colors.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (savedAccounts.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Сохранённые аккаунты",
                        subtitle = "Быстрый вход в один тап"
                    )
                }
                itemsIndexed(savedAccounts) { index, account ->
                    Surface(
                        shape = connectedItemShape(index, savedAccounts.size),
                        color = ModkeyDesign.Colors.surfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .modkeyPress {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                scope.launch {
                                    accountManager.selectAccount(account.id)
                                    onLoginSuccess(account)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ModkeyDesign.Colors.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    ModkeyIcon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = ModkeyDesign.Colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.label,
                                    style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold),
                                    color = ModkeyDesign.Colors.onSurface
                                )
                                Text(
                                    text = "Ключ: ••••••••${account.apiKey.takeLast(6)}",
                                    style = ModkeyDesign.Typography.bodySmall(),
                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                )
                            }
                            ModkeyIconButton(
                                onClick = {
                                    scope.launch {
                                        accountManager.removeAccount(account.id)
                                    }
                                },
                                size = 36.dp,
                                iconSize = 18.dp,
                                tint = ModkeyDesign.Colors.error.copy(alpha = 0.7f)
                            ) {
                                ModkeyIcon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Удалить",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = if (savedAccounts.isEmpty()) "Авторизация" else "Добавить другой аккаунт",
                    subtitle = "Выберите предпочтительный способ входа"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ModkeyDesign.Colors.surfaceContainerLow, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("API Ключ" to 0, "Логин / Пароль" to 1, "Telegram / Web" to 2)
                    tabs.forEach { (label, idx) ->
                        val isSelected = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    if (isSelected) ModkeyDesign.Colors.primary else Color.Transparent,
                                    RoundedCornerShape(13.dp)
                                )
                                .modkeyPress { selectedTab = idx; validationError = null }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) ModkeyDesign.Colors.onPrimary
                                else ModkeyDesign.Colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ModkeyDesign.Colors.surfaceContainer,
                        border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Вход по API Ключу",
                                style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.Bold)
                            )

                            ModkeyOutlinedTextField(
                                value = accountLabelInput,
                                onValueChange = { accountLabelInput = it },
                                label = "Название профиля (метка)",
                                placeholder = "Например: Основной софт",
                                shape = RoundedCornerShape(14.dp)
                            )

                            ModkeyOutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    validationError = null
                                },
                                label = "API Ключ",
                                placeholder = "Вставьте ключ из mod.wtf/admin/api",
                                shape = RoundedCornerShape(14.dp),
                                trailingIcon = {
                                    ModkeyIconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                            if (!clip.isNullOrBlank()) {
                                                apiKeyInput = clip.trim()
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            }
                                        },
                                        size = 36.dp,
                                        iconSize = 18.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.ContentPaste,
                                            contentDescription = "Вставить",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )

                            if (validationError != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ModkeyDesign.Colors.errorContainer, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ModkeyIcon(
                                        Icons.Filled.Error,
                                        contentDescription = null,
                                        tint = ModkeyDesign.Colors.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = validationError.orEmpty(),
                                        style = ModkeyDesign.Typography.bodySmall(),
                                        color = ModkeyDesign.Colors.onErrorContainer
                                    )
                                }
                            }

                            ModkeyButton(
                                onClick = { validateApiKeyAndProceed(apiKeyInput, accountLabelInput) },
                                enabled = !isValidating && apiKeyInput.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isValidating) {
                                    CircularLoader(
                                        modifier = Modifier.size(20.dp),
                                        color = ModkeyDesign.Colors.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Проверка ключа...")
                                } else {
                                    ModkeyIcon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Войти и сохранить")
                                }
                            }

                            Text(
                                text = "💡 API-ключ берется из mod.wtf/admin/api. Telegram должен быть привязан к аккаунту.",
                                style = ModkeyDesign.Typography.bodySmall(),
                                color = ModkeyDesign.Colors.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (selectedTab == 1) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ModkeyDesign.Colors.surfaceContainer,
                        border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Вход по логину и паролю mod.wtf",
                                style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Приложение автоматически авторизуется на сайте и подтянет ваш API ключ без необходимости открывать браузер.",
                                style = ModkeyDesign.Typography.bodySmall(),
                                color = ModkeyDesign.Colors.onSurfaceVariant
                            )

                            ModkeyOutlinedTextField(
                                value = usernameInput,
                                onValueChange = {
                                    usernameInput = it
                                    validationError = null
                                },
                                label = "Имя пользователя (Username)",
                                placeholder = "Ваш логин на mod.wtf",
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = {
                                    ModkeyIcon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )

                            ModkeyOutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    validationError = null
                                },
                                label = "Пароль",
                                placeholder = "Ваш пароль",
                                shape = RoundedCornerShape(14.dp),
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                leadingIcon = {
                                    ModkeyIcon(
                                        Icons.Filled.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    ModkeyIconButton(
                                        onClick = { isPasswordVisible = !isPasswordVisible },
                                        size = 36.dp,
                                        iconSize = 18.dp
                                    ) {
                                        ModkeyIcon(
                                            if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (isPasswordVisible) "Скрыть" else "Показать",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )

                            if (validationError != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ModkeyDesign.Colors.errorContainer, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ModkeyIcon(
                                        Icons.Filled.Error,
                                        contentDescription = null,
                                        tint = ModkeyDesign.Colors.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = validationError.orEmpty(),
                                        style = ModkeyDesign.Typography.bodySmall(),
                                        color = ModkeyDesign.Colors.onErrorContainer
                                    )
                                }
                            }

                            ModkeyButton(
                                onClick = { loginWithUsernamePassword() },
                                enabled = !isValidating && usernameInput.isNotBlank() && passwordInput.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isValidating) {
                                    CircularLoader(
                                        modifier = Modifier.size(20.dp),
                                        color = ModkeyDesign.Colors.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Авторизация...")
                                } else {
                                    ModkeyIcon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Войти по логину")
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ModkeyDesign.Colors.surfaceContainer,
                        border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Вход через Telegram / Web",
                                style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Войдите через официальный виджет Telegram или откройте сайт mod.wtf в Chrome. Приложение автоматически извлечет ваш API ключ.",
                                style = ModkeyDesign.Typography.bodySmall(),
                                color = ModkeyDesign.Colors.onSurfaceVariant
                            )

                            ModkeyButton(
                                onClick = { showWebLoginSheet = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                ModkeyIcon(
                                    Icons.Filled.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Открыть окно входа mod.wtf")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (showWebLoginSheet) {
        WebLoginBottomSheet(
            onDismiss = { showWebLoginSheet = false },
            onApiKeyExtracted = { extractedKey ->
                showWebLoginSheet = false
                apiKeyInput = extractedKey
                selectedTab = 0
                validateApiKeyAndProceed(extractedKey, "Web-аккаунт")
            }
        )
    }
}
