package com.modkey.app.ui.connect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.CipherKeysInfo
import com.modkey.app.core.model.ConnectConfig
import com.modkey.app.ui.components.ButtonVariant
import com.modkey.app.ui.components.CircularLoader
import com.modkey.app.ui.components.LoadingBlock
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyDialog
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.ModkeyIconButton
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.ModkeySwitch
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.connectedItemShape
import kotlinx.coroutines.launch

@Composable
fun ConnectSettingsTab(
    apiKey: String,
    softId: Int?
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf<ConnectConfig?>(null) }

    // Connect settings fields
    var enabled by remember { mutableStateOf(true) }
    var testConnectEnabled by remember { mutableStateOf(false) }
    var connectSecret by remember { mutableStateOf("") }
    var sourceTemplate by remember { mutableStateOf("") }
    var requireSignature by remember { mutableStateOf(false) }
    var checkHwid by remember { mutableStateOf(true) }
    var maintenance by remember { mutableStateOf(false) }
    var maintenanceMsg by remember { mutableStateOf("") }

    // Connect Page fields
    var pageEnabled by remember { mutableStateOf(true) }
    var pageTitle by remember { mutableStateOf("") }
    var pageDescription by remember { mutableStateOf("") }
    var accentColor by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }

    // Messages map
    val messagesMap = remember { mutableStateMapOf<String, String>() }
    var newMsgKey by remember { mutableStateOf("") }
    var newMsgValue by remember { mutableStateOf("") }

    // CipherLink
    var rotatedCipher by remember { mutableStateOf<CipherKeysInfo?>(null) }
    var showRotateConfirm by remember { mutableStateOf(false) }

    // Game profile dialog
    var showAddProfileDialog by remember { mutableStateOf(false) }

    fun loadConnectConfig() {
        if (softId == null) {
            isLoading = false
            return
        }
        isLoading = true
        scope.launch {
            try {
                val res = apiClient.getConnect(apiKey, softId)
                config = res
                enabled = res.enabled
                testConnectEnabled = res.testConnectEnabled
                connectSecret = res.connectSecret.orEmpty()
                sourceTemplate = res.sourceTemplate.orEmpty()
                requireSignature = res.requireSignature
                checkHwid = res.checkHwid
                maintenance = res.maintenance
                maintenanceMsg = res.maintenanceMessage.orEmpty()

                pageEnabled = res.pageEnabled
                pageTitle = res.pageTitle.orEmpty()
                pageDescription = res.pageDescription.orEmpty()
                accentColor = res.accentColor.orEmpty()
                logoUrl = res.logoUrl.orEmpty()

                messagesMap.clear()
                if (res.messages.isNotEmpty()) {
                    messagesMap.putAll(res.messages)
                } else {
                    messagesMap["invalid_key"] = ""
                    messagesMap["expired_key"] = ""
                    messagesMap["blocked_key"] = ""
                    messagesMap["hwid_mismatch"] = ""
                }
                rotatedCipher = res.cipherKeys
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Ошибка загрузки Connect", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(softId) {
        loadConnectConfig()
    }

    if (softId == null) {
        Text(
            text = "Выберите или создайте проект на Дашборде",
            style = ModkeyDesign.Typography.bodyMedium(),
            color = ModkeyDesign.Colors.onSurfaceVariant
        )
        return
    }

    if (isLoading) {
        LoadingBlock(message = "Загрузка настроек Connect...")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Core Connect Settings (edit-connect)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ModkeyDesign.Colors.surfaceContainerHigh,
            border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Основные настройки Connect",
                    style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                )

                SwitchRow("Connect включен", enabled) { enabled = it }
                SwitchRow("Test Connect (/test/connect)", testConnectEnabled) { testConnectEnabled = it }
                SwitchRow("Проверка HWID", checkHwid) { checkHwid = it }
                SwitchRow("Требовать подпись", requireSignature) { requireSignature = it }
                SwitchRow("Режим техработ", maintenance) { maintenance = it }

                if (maintenance) {
                    ModkeyOutlinedTextField(
                        value = maintenanceMsg,
                        onValueChange = { maintenanceMsg = it },
                        label = "Сообщение техработ",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ModkeyOutlinedTextField(
                    value = connectSecret,
                    onValueChange = { connectSecret = it },
                    label = "CONNECT_SECRET",
                    modifier = Modifier.fillMaxWidth()
                )
                ModkeyOutlinedTextField(
                    value = sourceTemplate,
                    onValueChange = { sourceTemplate = it },
                    label = "source_template",
                    modifier = Modifier.fillMaxWidth()
                )

                ModkeyButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        isSaving = true
                        scope.launch {
                            try {
                                apiClient.editConnect(
                                    apiKey = apiKey,
                                    softId = softId,
                                    enabled = enabled,
                                    testConnectEnabled = testConnectEnabled,
                                    connectSecret = connectSecret.ifBlank { null },
                                    sourceTemplate = sourceTemplate.ifBlank { null },
                                    requireSignature = requireSignature,
                                    checkHwid = checkHwid,
                                    maintenance = maintenance,
                                    maintenanceMessage = maintenanceMsg.ifBlank { null }
                                )
                                Toast.makeText(context, "Настройки Connect сохранены", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularLoader(color = ModkeyDesign.Colors.onPrimary, size = 18.dp)
                    } else {
                        ModkeyIcon(Icons.Filled.Save, null, tint = ModkeyDesign.Colors.onPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить настройки", style = ModkeyDesign.Typography.labelLarge(), color = ModkeyDesign.Colors.onPrimary)
                    }
                }
            }
        }

        // 2. CipherLink Keys (rotate-cipher-keys)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ModkeyDesign.Colors.surfaceContainerHigh,
            border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ключи CipherLink",
                        style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                    )
                    StatusBadge(text = "E2E", color = ModkeyDesign.Colors.primary)
                }

                val displayPubKey = rotatedCipher?.publicKey ?: rotatedCipher?.cipherKey ?: config?.publicKey
                if (!displayPubKey.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ModkeyDesign.Colors.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayPubKey,
                                style = ModkeyDesign.Typography.bodySmall().copy(fontFamily = FontFamily.Monospace),
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                            ModkeyIconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("CipherKey", displayPubKey))
                                    Toast.makeText(context, "Ключ скопирован", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                ModkeyIcon(Icons.Filled.ContentCopy, "Копировать", tint = ModkeyDesign.Colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                ModkeyButton(
                    onClick = { showRotateConfirm = true },
                    variant = ButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModkeyIcon(Icons.Filled.Key, null, tint = ModkeyDesign.Colors.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Сгенерировать новые ключи (Rotate)", style = ModkeyDesign.Typography.labelLarge(), color = ModkeyDesign.Colors.onSurface)
                }
            }
        }

        // 3. Game Profiles (create-game-profile, delete-game-profile)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ModkeyDesign.Colors.surfaceContainerHigh,
            border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Профили игр (${config?.gameProfiles?.size ?: 0})",
                        style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                    )
                    ModkeyIconButton(
                        onClick = { showAddProfileDialog = true },
                        containerColor = ModkeyDesign.Colors.primaryContainer
                    ) {
                        ModkeyIcon(Icons.Filled.Add, "Добавить профиль", tint = ModkeyDesign.Colors.primary, modifier = Modifier.size(18.dp))
                    }
                }

                val profiles = config?.gameProfiles.orEmpty()
                if (profiles.isEmpty()) {
                    Text(
                        text = "Профили игр пока не добавлены",
                        style = ModkeyDesign.Typography.bodySmall(),
                        color = ModkeyDesign.Colors.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        profiles.forEachIndexed { index, profile ->
                            Surface(
                                shape = connectedItemShape(index, profiles.size),
                                color = ModkeyDesign.Colors.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        ModkeyIcon(Icons.Filled.SportsEsports, null, tint = ModkeyDesign.Colors.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = profile.name?.ifBlank { profile.game } ?: profile.game,
                                                style = ModkeyDesign.Typography.bodyMedium().copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = buildString {
                                                    append("game: ${profile.game}")
                                                    if (!profile.keyFormat.isNullOrBlank()) append(" • ${profile.keyFormat}")
                                                    if (profile.maxDevices != null) append(" • max: ${profile.maxDevices}")
                                                },
                                                style = ModkeyDesign.Typography.labelSmall(),
                                                color = ModkeyDesign.Colors.onSurfaceVariant
                                            )
                                        }
                                    }
                                    ModkeyIconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    apiClient.deleteGameProfile(
                                                        apiKey = apiKey,
                                                        softId = softId,
                                                        profileId = profile.id.takeIf { it > 0 },
                                                        game = profile.game.takeIf { it.isNotBlank() }
                                                    )
                                                    Toast.makeText(context, "Профиль удалён", Toast.LENGTH_SHORT).show()
                                                    loadConnectConfig()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        ModkeyIcon(Icons.Filled.Delete, "Удалить", tint = ModkeyDesign.Colors.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Connect Messages (edit-connect-messages)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ModkeyDesign.Colors.surfaceContainerHigh,
            border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Тексты ответов Connect",
                    style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                )

                messagesMap.keys.toList().forEach { key ->
                    ModkeyOutlinedTextField(
                        value = messagesMap[key].orEmpty(),
                        onValueChange = { messagesMap[key] = it },
                        label = key,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ModkeyOutlinedTextField(
                        value = newMsgKey,
                        onValueChange = { newMsgKey = it },
                        placeholder = "Ключ",
                        modifier = Modifier.weight(1f)
                    )
                    ModkeyOutlinedTextField(
                        value = newMsgValue,
                        onValueChange = { newMsgValue = it },
                        placeholder = "Текст ответа",
                        modifier = Modifier.weight(1.4f)
                    )
                    ModkeyIconButton(
                        onClick = {
                            if (newMsgKey.isNotBlank()) {
                                messagesMap[newMsgKey.trim()] = newMsgValue
                                newMsgKey = ""
                                newMsgValue = ""
                            }
                        },
                        containerColor = ModkeyDesign.Colors.primaryContainer
                    ) {
                        ModkeyIcon(Icons.Filled.Add, "Добавить", tint = ModkeyDesign.Colors.primary, modifier = Modifier.size(18.dp))
                    }
                }

                ModkeyButton(
                    onClick = {
                        scope.launch {
                            try {
                                apiClient.editConnectMessages(apiKey, softId, messagesMap.toMap())
                                Toast.makeText(context, "Тексты ответов сохранены", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    variant = ButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить тексты ответов", style = ModkeyDesign.Typography.labelLarge(), color = ModkeyDesign.Colors.onSurface)
                }
            }
        }

        // 5. Connect Page (edit-connect-page)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ModkeyDesign.Colors.surfaceContainerHigh,
            border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Страница Connect",
                    style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                )

                SwitchRow("Публичная страница включена", pageEnabled) { pageEnabled = it }

                ModkeyOutlinedTextField(
                    value = pageTitle,
                    onValueChange = { pageTitle = it },
                    label = "Заголовок страницы",
                    modifier = Modifier.fillMaxWidth()
                )
                ModkeyOutlinedTextField(
                    value = pageDescription,
                    onValueChange = { pageDescription = it },
                    label = "Описание",
                    modifier = Modifier.fillMaxWidth()
                )
                ModkeyOutlinedTextField(
                    value = accentColor,
                    onValueChange = { accentColor = it },
                    label = "Цвет акцента (#HEX)",
                    modifier = Modifier.fillMaxWidth()
                )
                ModkeyOutlinedTextField(
                    value = logoUrl,
                    onValueChange = { logoUrl = it },
                    label = "URL логотипа",
                    modifier = Modifier.fillMaxWidth()
                )

                ModkeyButton(
                    onClick = {
                        scope.launch {
                            try {
                                apiClient.editConnectPage(
                                    apiKey = apiKey,
                                    softId = softId,
                                    pageEnabled = pageEnabled,
                                    pageTitle = pageTitle.ifBlank { null },
                                    pageDescription = pageDescription.ifBlank { null },
                                    accentColor = accentColor.ifBlank { null },
                                    logoUrl = logoUrl.ifBlank { null }
                                )
                                Toast.makeText(context, "Страница Connect обновлена", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    variant = ButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить страницу Connect", style = ModkeyDesign.Typography.labelLarge(), color = ModkeyDesign.Colors.onSurface)
                }
            }
        }
    }

    if (showRotateConfirm) {
        ModkeyDialog(
            title = "Ротировать ключи CipherLink?",
            onDismiss = { showRotateConfirm = false },
            confirmLabel = "Ротировать",
            confirmContainerColor = ModkeyDesign.Colors.error,
            confirmContentColor = ModkeyDesign.Colors.onError,
            onConfirm = {
                showRotateConfirm = false
                scope.launch {
                    try {
                        rotatedCipher = apiClient.rotateCipherKeys(apiKey, softId)
                        Toast.makeText(context, "Ключи CipherLink обновлены", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Ошибка ротации", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Text(
                text = "Старые ключи CipherLink перестанут работать. Клиентам потребуется обновить публичный ключ.",
                style = ModkeyDesign.Typography.bodyMedium(),
                color = ModkeyDesign.Colors.onSurfaceVariant
            )
        }
    }

    if (showAddProfileDialog) {
        var gameInput by remember { mutableStateOf("") }
        var nameInput by remember { mutableStateOf("") }
        var keyFormatInput by remember { mutableStateOf("") }
        var maxDevicesInput by remember { mutableStateOf("1") }

        ModkeyDialog(
            title = "Новый профиль игры",
            onDismiss = { showAddProfileDialog = false },
            confirmLabel = "Создать",
            onConfirm = {
                if (gameInput.isBlank()) {
                    Toast.makeText(context, "Укажите код игры (game)", Toast.LENGTH_SHORT).show()
                    return@ModkeyDialog
                }
                scope.launch {
                    try {
                        apiClient.createGameProfile(
                            apiKey = apiKey,
                            softId = softId,
                            game = gameInput,
                            name = nameInput.ifBlank { null },
                            keyFormat = keyFormatInput.ifBlank { null },
                            maxDevices = maxDevicesInput.toIntOrNull()
                        )
                        showAddProfileDialog = false
                        Toast.makeText(context, "Профиль игры создан", Toast.LENGTH_SHORT).show()
                        loadConnectConfig()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Ошибка создания", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ModkeyOutlinedTextField(value = gameInput, onValueChange = { gameInput = it }, label = "Код игры (например: standoff2)")
                ModkeyOutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = "Отображаемое имя")
                ModkeyOutlinedTextField(value = keyFormatInput, onValueChange = { keyFormatInput = it }, label = "Формат ключей (опционально)")
                ModkeyOutlinedTextField(value = maxDevicesInput, onValueChange = { maxDevicesInput = it }, label = "Макс. устройств")
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = ModkeyDesign.Typography.bodyMedium())
        ModkeySwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
