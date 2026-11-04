package com.modkey.app.ui.keys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.KeyItem
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.core.model.SoftwareItem
import com.modkey.app.ui.components.EmptyState
import com.modkey.app.ui.components.LoadingBlock
import com.modkey.app.ui.components.ModkeyFab
import com.modkey.app.ui.components.ModkeyFilterChip
import com.modkey.app.ui.components.ModkeyDialog
import com.modkey.app.ui.components.ModkeyIconButton
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.components.StatCard
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.SurfaceChip
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.dashboard.QuickCreateKeyBottomSheet
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.connectedItemShape
import com.modkey.app.ui.theme.modkeyPress
import kotlinx.coroutines.launch

private enum class KeyFilter(val label: String) {
    ALL("Все"),
    ACTIVE("Активные"),
    PAUSED("На паузе"),
    BLOCKED("Блок"),
    APK("APK"),
    INJECTOR("INJECTOR")
}

@Composable
fun KeysScreen(
    account: SavedAccount,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(KeyFilter.ALL) }
    var softwareList by remember { mutableStateOf<List<SoftwareItem>>(emptyList()) }
    var selectedSoftId by remember { mutableStateOf<Int?>(null) }
    var keysList by remember { mutableStateOf<List<KeyItem>>(emptyList()) }
    var totalKeysCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    var showCreateSheet by remember { mutableStateOf(false) }
    var showBulkCleanupDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var editingKeyItem by remember { mutableStateOf<KeyItem?>(null) }

    fun loadKeys() {
        isLoading = true
        scope.launch {
            try {
                if (softwareList.isEmpty()) {
                    val softs = apiClient.getSofts(account.apiKey)
                    softwareList = softs
                    if (selectedSoftId == null) selectedSoftId = softs.firstOrNull()?.id
                }

                val currentSoftId = selectedSoftId ?: return@launch
                val statusParam = when (selectedFilter) {
                    KeyFilter.BLOCKED -> 0
                    KeyFilter.ACTIVE -> 1
                    else -> null
                }
                val activeParam = when (selectedFilter) {
                    KeyFilter.ACTIVE -> 1
                    KeyFilter.PAUSED -> 0
                    else -> null
                }
                val typeParam = when (selectedFilter) {
                    KeyFilter.APK -> "APK"
                    KeyFilter.INJECTOR -> "INJECTOR"
                    else -> null
                }

                val resp = apiClient.getKeys(
                    apiKey = account.apiKey,
                    softId = currentSoftId,
                    query = searchQuery.ifBlank { null },
                    status = statusParam,
                    active = activeParam,
                    keyType = typeParam,
                    limit = 150
                )
                keysList = resp.items
                totalKeysCount = resp.total
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedSoftId, searchQuery, selectedFilter) {
        loadKeys()
    }

    val selectedSoft = softwareList.find { it.id == selectedSoftId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ModkeyDesign.Colors.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Header + Bulk Cleanup & Export buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ключи доступа ($totalKeysCount)",
                        style = ModkeyDesign.Typography.titleLarge().copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "Управление, фильтры, экспорт и HWID",
                        style = ModkeyDesign.Typography.bodySmall(),
                        color = ModkeyDesign.Colors.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SurfaceChip(
                        onClick = { showBulkCleanupDialog = true },
                        shape = ModkeyDesign.Shapes.small,
                        color = ModkeyDesign.Colors.surfaceContainerHigh
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            ModkeyIcon(
                                imageVector = Icons.Filled.CleaningServices,
                                contentDescription = null,
                                tint = ModkeyDesign.Colors.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Очистка",
                                style = ModkeyDesign.Typography.labelSmall().copy(fontWeight = FontWeight.Bold),
                                color = ModkeyDesign.Colors.onSurface
                            )
                        }
                    }
                    SurfaceChip(
                        onClick = { showExportDialog = true },
                        shape = ModkeyDesign.Shapes.small,
                        color = ModkeyDesign.Colors.surfaceContainerHigh
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            ModkeyIcon(
                                imageVector = Icons.Filled.IosShare,
                                contentDescription = null,
                                tint = ModkeyDesign.Colors.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Экспорт",
                                style = ModkeyDesign.Typography.labelSmall().copy(fontWeight = FontWeight.Bold),
                                color = ModkeyDesign.Colors.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            ModkeyOutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Поиск по ключу или игре...",
                leadingIcon = {
                    ModkeyIcon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = ModkeyDesign.Colors.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        ModkeyIconButton(
                            onClick = { searchQuery = "" },
                            size = 26.dp,
                            iconSize = 15.dp
                        ) {
                            ModkeyIcon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Очистить",
                                tint = ModkeyDesign.Colors.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeyFilter.entries.forEach { filter ->
                    ModkeyFilterChip(
                        selected = selectedFilter == filter,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            selectedFilter = filter
                        },
                        label = filter.label
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                LoadingBlock(message = "Загрузка списка ключей...")
            } else if (keysList.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.VpnKey,
                    title = "Ключи не найдены",
                    message = if (searchQuery.isNotBlank() || selectedFilter != KeyFilter.ALL)
                        "По выбранному фильтру ничего не найдено"
                    else
                        "Список ключей пуст. Нажмите «+» для создания."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(keysList) { index, keyItem ->
                        val statusColor = when {
                            keyItem.isBlocked -> ModkeyDesign.Colors.error
                            keyItem.isPaused -> ModkeyDesign.Colors.warning
                            else -> ModkeyDesign.Colors.success
                        }

                        Surface(
                            shape = connectedItemShape(index, keysList.size),
                            color = ModkeyDesign.Colors.surfaceContainerHigh,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = keyItem.key,
                                        style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold),
                                        color = ModkeyDesign.Colors.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusBadge(
                                        text = if (keyItem.isBlocked) "Блок" else if (keyItem.isPaused) "Пауза" else "Активен",
                                        color = statusColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "HWID: ${keyItem.devicesUsed}/${keyItem.maxDevices} • Сбросов: ${keyItem.hwidResetsUsed}/${keyItem.hwidResetLimit} • Срок: ${keyItem.duration ?: "∞"}д",
                                        style = ModkeyDesign.Typography.bodySmall(),
                                        color = ModkeyDesign.Colors.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = keyItem.keyType,
                                        style = ModkeyDesign.Typography.labelSmall(),
                                        color = ModkeyDesign.Colors.onSurface
                                    )
                                }

                                if (!keyItem.expiredDate.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Истекает: ${keyItem.expiredDate}",
                                        style = ModkeyDesign.Typography.labelSmall(),
                                        color = ModkeyDesign.Colors.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Reset HWID
                                    ModkeyIconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            val currentSoftId = selectedSoftId ?: return@ModkeyIconButton
                                            scope.launch {
                                                try {
                                                    apiClient.resetKeyHwid(account.apiKey, currentSoftId, keyItem.key)
                                                    Toast.makeText(context, "HWID сброшен!", Toast.LENGTH_SHORT).show()
                                                    loadKeys()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.RestartAlt,
                                            contentDescription = "Сброс HWID",
                                            tint = ModkeyDesign.Colors.primary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Toggle pause/resume
                                    ModkeyIconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            val currentSoftId = selectedSoftId ?: return@ModkeyIconButton
                                            scope.launch {
                                                try {
                                                    apiClient.editKey(account.apiKey, currentSoftId, keyItem.key, active = keyItem.isPaused)
                                                    loadKeys()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = if (keyItem.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                            contentDescription = "Пауза/Пуск",
                                            tint = if (keyItem.isPaused) ModkeyDesign.Colors.success else ModkeyDesign.Colors.warning,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Block / Unblock
                                    ModkeyIconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            val currentSoftId = selectedSoftId ?: return@ModkeyIconButton
                                            scope.launch {
                                                try {
                                                    apiClient.editKey(account.apiKey, currentSoftId, keyItem.key, status = keyItem.isBlocked)
                                                    Toast.makeText(
                                                        context,
                                                        if (keyItem.isBlocked) "Ключ разблокирован" else "Ключ заблокирован",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    loadKeys()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = if (keyItem.isBlocked) Icons.Filled.CheckCircle else Icons.Filled.Block,
                                            contentDescription = "Блок/Разблок",
                                            tint = if (keyItem.isBlocked) ModkeyDesign.Colors.success else ModkeyDesign.Colors.error,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Edit / Rename Key
                                    ModkeyIconButton(
                                        onClick = { editingKeyItem = keyItem },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Редактировать",
                                            tint = ModkeyDesign.Colors.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Copy Key
                                    ModkeyIconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Modkey", keyItem.key))
                                            Toast.makeText(context, "Ключ скопирован", Toast.LENGTH_SHORT).show()
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Скопировать",
                                            tint = ModkeyDesign.Colors.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Share Key
                                    ModkeyIconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "🔑 Лицензионный ключ: ${keyItem.key}\n📱 Устройств: ${keyItem.maxDevices}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Отправить ключ"))
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.Share,
                                            contentDescription = "Отправить",
                                            tint = ModkeyDesign.Colors.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Delete key
                                    ModkeyIconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            val currentSoftId = selectedSoftId ?: return@ModkeyIconButton
                                            scope.launch {
                                                try {
                                                    apiClient.deleteKey(account.apiKey, currentSoftId, keyItem.key)
                                                    Toast.makeText(context, "Ключ удален", Toast.LENGTH_SHORT).show()
                                                    loadKeys()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Удалить",
                                            tint = ModkeyDesign.Colors.error,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }
        }

        // Floating Create Key button
        if (selectedSoft != null && !showCreateSheet) {
            ModkeyFab(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showCreateSheet = true
                },
                icon = Icons.Filled.Add,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 96.dp)
            )
        }
    }

    // Create Key Sheet
    if (showCreateSheet && selectedSoft != null) {
        QuickCreateKeyBottomSheet(
            apiKey = account.apiKey,
            software = selectedSoft,
            onDismiss = { showCreateSheet = false },
            onKeyCreated = { loadKeys() }
        )
    }

    // Edit & Rename Key Dialog
    if (editingKeyItem != null && selectedSoftId != null) {
        val target = editingKeyItem!!
        var newKeyName by remember(target) { mutableStateOf(target.key) }
        var newMaxDevices by remember(target) { mutableStateOf(target.maxDevices.toString()) }
        var newHwidLimit by remember(target) { mutableStateOf(target.hwidResetLimit.toString()) }

        ModkeyDialog(
            title = "Редактировать ключ",
            onDismiss = { editingKeyItem = null },
            confirmLabel = "Сохранить",
            onConfirm = {
                scope.launch {
                    try {
                        val currentKey = target.key
                        val trimmedNewKey = newKeyName.trim()
                        var effectiveKey = currentKey
                        if (trimmedNewKey.isNotBlank() && trimmedNewKey != currentKey) {
                            apiClient.renameUserKey(account.apiKey, currentKey, trimmedNewKey)
                            effectiveKey = trimmedNewKey
                        }
                        apiClient.editKey(
                            apiKey = account.apiKey,
                            softId = selectedSoftId!!,
                            key = effectiveKey,
                            maxDevices = newMaxDevices.toIntOrNull() ?: target.maxDevices,
                            hwidResetLimit = newHwidLimit.toIntOrNull() ?: target.hwidResetLimit
                        )
                        editingKeyItem = null
                        Toast.makeText(context, "Ключ обновлён!", Toast.LENGTH_SHORT).show()
                        loadKeys()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ModkeyOutlinedTextField(
                    value = newKeyName,
                    onValueChange = { newKeyName = it },
                    label = "Строка ключа (Переименование)"
                )
                ModkeyOutlinedTextField(
                    value = newMaxDevices,
                    onValueChange = { newMaxDevices = it },
                    label = "Макс. устройств (HWID слотов)"
                )
                ModkeyOutlinedTextField(
                    value = newHwidLimit,
                    onValueChange = { newHwidLimit = it },
                    label = "Лимит сбросов HWID (-1 = безлимит)"
                )
            }
        }
    }

    // Bulk Cleanup Dialog (delete-keys)
    if (showBulkCleanupDialog && selectedSoftId != null) {
        val cleanupKinds = listOf(
            "expired" to "Истекшие ключи (Expired)",
            "unused" to "Неактивированные (Unused)",
            "blocked" to "Заблокированные (Blocked)",
            "inactive" to "На паузе (Paused)"
        )
        var selectedKind by remember { mutableStateOf("expired") }

        ModkeyDialog(
            title = "Массовая очистка ключей",
            onDismiss = { showBulkCleanupDialog = false },
            confirmLabel = "Удалить выбранные",
            confirmContainerColor = ModkeyDesign.Colors.error,
            confirmContentColor = ModkeyDesign.Colors.onError,
            onConfirm = {
                scope.launch {
                    try {
                        val deletedCount = apiClient.deleteKeysBulk(account.apiKey, selectedSoftId!!, selectedKind)
                        showBulkCleanupDialog = false
                        Toast.makeText(context, "Удалено ключей: $deletedCount", Toast.LENGTH_LONG).show()
                        loadKeys()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Ошибка очистки", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Выберите категорию ключей для безвозвратного удаления из базы:",
                    style = ModkeyDesign.Typography.bodySmall(),
                    color = ModkeyDesign.Colors.onSurfaceVariant
                )
                cleanupKinds.forEach { (kindCode, kindLabel) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedKind == kindCode) ModkeyDesign.Colors.primaryContainer
                        else ModkeyDesign.Colors.surfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedKind == kindCode) ModkeyDesign.Colors.primary.copy(alpha = 0.5f)
                            else ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .modkeyPress { selectedKind = kindCode }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = kindLabel,
                                style = ModkeyDesign.Typography.bodyMedium().copy(
                                    fontWeight = if (selectedKind == kindCode) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (selectedKind == kindCode) ModkeyDesign.Colors.primary
                                else ModkeyDesign.Colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Export Keys Dialog (export-keys)
    if (showExportDialog && selectedSoftId != null) {
        var selectedFormat by remember { mutableStateOf("txt") }

        ModkeyDialog(
            title = "Экспорт базы ключей",
            onDismiss = { showExportDialog = false },
            confirmLabel = "Экспортировать",
            onConfirm = {
                scope.launch {
                    try {
                        val exportedText = apiClient.exportKeys(account.apiKey, selectedSoftId!!, selectedFormat)
                        showExportDialog = false
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "modkey_export.$selectedFormat")
                            putExtra(Intent.EXTRA_TEXT, exportedText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Экспорт ключей (${selectedFormat.uppercase()})"))
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Ошибка экспорта", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Выгрузите до 10 000 ключей в удобном формате для отправки или бэкапа:",
                    style = ModkeyDesign.Typography.bodySmall(),
                    color = ModkeyDesign.Colors.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("txt", "csv", "json").forEach { fmt ->
                        ModkeyFilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { selectedFormat = fmt },
                            label = fmt.uppercase()
                        )
                    }
                }
            }
        }
    }
}
