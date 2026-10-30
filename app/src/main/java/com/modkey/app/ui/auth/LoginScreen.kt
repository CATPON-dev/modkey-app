package com.modkey.app.ui.auth

import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.core.network.ModkeyWebAuth
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.theme.connectedItemShape
import com.modkey.app.ui.theme.expressiveClick
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
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
            .background(MaterialTheme.colorScheme.background)
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
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Modkey Platform",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Управление софтом, лицензиями и клиентами",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .expressiveClick {
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
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.label,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ключ: ••••••••${account.apiKey.takeLast(6)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        accountManager.removeAccount(account.id)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Удалить",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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

                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            validationError = null
                        },
                        text = { Text("API Ключ") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            validationError = null
                        },
                        text = { Text("Логин / Пароль") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            validationError = null
                        },
                        text = { Text("Telegram / Web") }
                    )
                }
            }

            if (selectedTab == 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            OutlinedTextField(
                                value = accountLabelInput,
                                onValueChange = { accountLabelInput = it },
                                label = { Text("Название профиля (метка)") },
                                placeholder = { Text("Например: Основной софт") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    validationError = null
                                },
                                label = { Text("API Ключ") },
                                placeholder = { Text("Вставьте ключ из mod.wtf/admin/api") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                            if (!clip.isNullOrBlank()) {
                                                apiKeyInput = clip.trim()
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.ContentPaste, contentDescription = "Вставить")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            AnimatedVisibility(visible = validationError != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = validationError.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            Button(
                                onClick = { validateApiKeyAndProceed(apiKeyInput, accountLabelInput) },
                                enabled = !isValidating && apiKeyInput.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .expressiveClick(enabled = !isValidating && apiKeyInput.isNotBlank()) {
                                        validateApiKeyAndProceed(apiKeyInput, accountLabelInput)
                                    }
                            ) {
                                if (isValidating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Проверка ключа...")
                                } else {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Войти и сохранить")
                                }
                            }

                            Text(
                                text = "💡 API-ключ берется из mod.wtf/admin/api. Telegram должен быть привязан к аккаунту.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (selectedTab == 1) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Приложение автоматически авторизуется на сайте и подтянет ваш API ключ без необходимости открывать браузер.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = {
                                    usernameInput = it
                                    validationError = null
                                },
                                label = { Text("Имя пользователя (Username)") },
                                placeholder = { Text("Ваш логин на mod.wtf") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = {
                                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    validationError = null
                                },
                                label = { Text("Пароль") },
                                placeholder = { Text("Ваш пароль") },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = {
                                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = if (isPasswordVisible) "Скрыть" else "Показать"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            AnimatedVisibility(visible = validationError != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = validationError.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            Button(
                                onClick = { loginWithUsernamePassword() },
                                enabled = !isValidating && usernameInput.isNotBlank() && passwordInput.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .expressiveClick(enabled = !isValidating && usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        loginWithUsernamePassword()
                                    }
                            ) {
                                if (isValidating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Авторизация...")
                                } else {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
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
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Войдите через официальный виджет Telegram или откройте сайт mod.wtf в Chrome. Приложение автоматически извлечет ваш API ключ.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { showWebLoginSheet = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .expressiveClick { showWebLoginSheet = true }
                            ) {
                                Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
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
