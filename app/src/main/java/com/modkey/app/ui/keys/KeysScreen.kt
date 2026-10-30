package com.modkey.app.ui.keys

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.dashboard.QuickCreateKeyBottomSheet
import com.modkey.app.ui.theme.connectedItemShape
import kotlinx.coroutines.launch

private enum class KeyFilter(val label: String) {
    ALL("Все"),
    ACTIVE("Активные"),
    PAUSED("На паузе"),
    BLOCKED("Блок"),
    APK("APK"),
    INJECTOR("INJECTOR")
}

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            val selectedSoft = softwareList.find { it.id == selectedSoftId }
            if (selectedSoft != null) {
                FloatingActionButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showCreateSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Создать ключ")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Top Header + Bulk Cleanup & Export buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ключи доступа ($totalKeysCount)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Управление, фильтры, экспорт и HWID",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = { showBulkCleanupDialog = true },
                        label = { Text("Очистка") },
                        leadingIcon = {
                            Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    AssistChip(
                        onClick = { showExportDialog = true },
                        label = { Text("Экспорт") },
                        leadingIcon = {
                            Icon(Icons.Filled.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск по ключу или игре...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeyFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            selectedFilter = filter
                        },
                        label = { Text(filter.label) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                        Surface(
                            shape = connectedItemShape(index, keysList.size),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusBadge(
                                        text = if (keyItem.isBlocked) "Блок" else if (keyItem.isPaused) "Пауза" else "Активен",
                                        color = if (keyItem.isBlocked) Color(0xFFE53935) else if (keyItem.isPaused) Color(0xFFFFB300) else Color(0xFF4CAF50)
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = keyItem.keyType,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (!keyItem.expiredDate.isNullOrBlank()) {
                                    Text(
                                        text = "Истекает: ${keyItem.expiredDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Reset HWID
                                    IconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            val currentSoftId = selectedSoftId ?: return@IconButton
                                            scope.launch {
                                                try {
                                                    apiClient.resetKeyHwid(account.apiKey, currentSoftId, keyItem.key)
                                                    Toast.makeText(context, "HWID сброшен!", Toast.LENGTH_SHORT).show()
                                                    loadKeys()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.RestartAlt, contentDescription = "Сброс HWID", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    // Toggle pause/resume
                                    IconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            val currentSoftId = selectedSoftId ?: return@IconButton
                                            scope.launch {
                                                try {
                                                    apiClient.editKey(account.apiKey, currentSoftId, keyItem.key, active = keyItem.isPaused)
                                                    loadKeys()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (keyItem.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                            contentDescription = "Пауза/Пуск",
                                            tint = if (keyItem.isPaused) Color(0xFF4CAF50) else Color(0xFFFFB300)
                                        )
                                    }

                                    // Block / Unblock
                                    IconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            val currentSoftId = selectedSoftId ?: return@IconButton
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
                                        }
                                    ) {
                                        Icon(
                                            if (keyItem.isBlocked) Icons.Filled.CheckCircle else Icons.Filled.Block,
                                            contentDescription = "Блок/Разблок",
                                            tint = if (keyItem.isBlocked) Color(0xFF4CAF50) else Color(0xFFE53935)
                                        )
                                    }

                                    // Edit / Rename Key
                                    IconButton(
                                        onClick = { editingKeyItem = keyItem }
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                                    }

                                    // Copy Key
                                    IconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Modkey", keyItem.key))
                                            Toast.makeText(context, "Ключ скопирован", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Скопировать")
                                    }

                                    // Share Key
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "🔑 Лицензионный ключ: ${keyItem.key}\n📱 Устройств: ${keyItem.maxDevices}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Отправить ключ"))
                                        }
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = "Отправить")
                                    }

                                     Delete key
                                    IconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            val currentSoftId = selectedSoftId ?: return@IconButton
                                            scope.launch {
                                                try {
                                                    apiClient.deleteKey(account.apiKey, currentSoftId, keyItem.key)
                                                    Toast.makeText(context, "Ключ удален", Toast.LENGTH_SHORT).show()
                                                    loadKeys()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

     Create Key Sheet
    val selectedSoft = softwareList.find { it.id == selectedSoftId }
    if (showCreateSheet && selectedSoft != null) {
        QuickCreateKeyBottomSheet(
            apiKey = account.apiKey,
            software = selectedSoft,
            onDismiss = { showCreateSheet = false },
            onKeyCreated = { loadKeys() }
        )
    }

     Edit & Rename Key Dialog
    if (editingKeyItem != null && selectedSoftId != null) {
        val target = editingKeyItem!!
        var newKeyName by remember(target) { mutableStateOf(target.key) }
        var newMaxDevices by remember(target) { mutableStateOf(target.maxDevices.toString()) }
        var newHwidLimit by remember(target) { mutableStateOf(target.hwidResetLimit.toString()) }

        AlertDialog(
            onDismissRequest = { editingKeyItem = null },
            title = { Text("Редактировать ключ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newKeyName,
                        onValueChange = { newKeyName = it },
                        label = { Text("Строка ключа (Переименование)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = newMaxDevices,
                        onValueChange = { newMaxDevices = it },
                        label = { Text("Макс. устройств (HWID слотов)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = newHwidLimit,
                        onValueChange = { newHwidLimit = it },
                        label = { Text("Лимит сбросов HWID (-1 = безлимит)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
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
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingKeyItem = null }) {
                    Text("Отмена")
                }
            }
        )
    }

     Bulk Cleanup Dialog (delete-keys)
    if (showBulkCleanupDialog && selectedSoftId != null) {
        val cleanupKinds = listOf(
            "expired" to "Истекшие ключи (Expired)",
            "unused" to "Неактивированные (Unused)",
            "blocked" to "Заблокированные (Blocked)",
            "inactive" to "На паузе (Paused)"
        )
        var selectedKind by remember { mutableStateOf("expired") }

        AlertDialog(
            onDismissRequest = { showBulkCleanupDialog = false },
            title = { Text("Массовая очистка ключей") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Выберите категорию ключей для безвозвратного удаления из базы:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    cleanupKinds.forEach { (kindCode, kindLabel) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedKind == kindCode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            onClick = { selectedKind = kindCode },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = kindLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedKind == kindCode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedKind == kindCode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить выбранные")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkCleanupDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

     Export Keys Dialog (export-keys)
    if (showExportDialog && selectedSoftId != null) {
        var selectedFormat by remember { mutableStateOf("txt") }
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Экспорт базы ключей") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Выгрузите до 10 000 ключей в удобном формате для отправки или бэкапа:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("txt", "csv", "json").forEach { fmt ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(fmt.uppercase()) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
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
                    Text("Экспортировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
