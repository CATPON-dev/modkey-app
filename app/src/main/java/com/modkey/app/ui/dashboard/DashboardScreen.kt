package com.modkey.app.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.AccountMeInfo
import com.modkey.app.core.model.KeyItem
import com.modkey.app.core.model.OverviewStats
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.core.model.SoftStatsResponse
import com.modkey.app.core.model.SoftwareItem
import com.modkey.app.ui.components.EmptyState
import com.modkey.app.ui.components.LoadingBlock
import com.modkey.app.ui.components.QuickActionChip
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.components.StatCard
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.theme.connectedItemShape
import com.modkey.app.ui.theme.expressiveClick
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    account: SavedAccount,
    allAccounts: List<SavedAccount>,
    onSwitchAccount: (SavedAccount) -> Unit,
    onAddNewAccount: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToKeys: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient

    var isLoading by remember { mutableStateOf(true) }
    var softwareList by remember { mutableStateOf<List<SoftwareItem>>(emptyList()) }
    var selectedSoftware by remember { mutableStateOf<SoftwareItem?>(null) }
    var overviewStats by remember { mutableStateOf<OverviewStats?>(null) }
    var softStats by remember { mutableStateOf<SoftStatsResponse?>(null) }
    var recentKeys by remember { mutableStateOf<List<KeyItem>>(emptyList()) }
    var meInfo by remember { mutableStateOf<AccountMeInfo?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Bottom Sheets & Dialogs
    var showQuickCreateSheet by remember { mutableStateOf(false) }
    var showSoftPickerSheet by remember { mutableStateOf(false) }
    var showAccountSwitcherSheet by remember { mutableStateOf(false) }
    var showDomainsSheet by remember { mutableStateOf(false) }
    var showSoftSettingsDialog by remember { mutableStateOf(false) }
    var showExtendKeysDialog by remember { mutableStateOf(false) }
    var extendDaysInput by remember { mutableStateOf("1") }
    var showEmergencyConfirmDialog by remember { mutableStateOf(false) }
    var emergencyTargetStatus by remember { mutableIntStateOf(0) }
    var emergencyReasonInput by remember { mutableStateOf("Игра обновилась. Идут технические работы.") }

    fun refreshDashboardData() {
        isLoading = true
        errorText = null
        scope.launch {
            try {
                launch {
                    try {
                        meInfo = apiClient.getMe(account.apiKey)
                    } catch (_: Exception) {}
                }
                val stats = apiClient.getOverviewStats(account.apiKey)
                overviewStats = stats

                val softs = apiClient.getSofts(account.apiKey)
                softwareList = softs

                val currentSelected = selectedSoftware?.let { curr -> softs.find { it.id == curr.id } }
                    ?: softs.firstOrNull()
                selectedSoftware = currentSelected

                if (currentSelected != null) {
                    val sStats = apiClient.getSoftStats(account.apiKey, currentSelected.id)
                    softStats = sStats

                    val keysResp = apiClient.getKeys(
                        apiKey = account.apiKey,
                        softId = currentSelected.id,
                        limit = 8
                    )
                    recentKeys = keysResp.items
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка синхронизации данных"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(account.id) {
        refreshDashboardData()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dashboard Header Bar
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile button / Account Switcher
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.expressiveClick {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showAccountSwitcherSheet = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.VpnKey,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = meInfo?.username?.ifBlank { account.label } ?: account.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (meInfo != null) "${meInfo!!.balance} ₽ • Сменить ▾" else "Сменить ▾",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Software Selector Chip
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.expressiveClick {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showSoftPickerSheet = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Computer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedSoftware?.name ?: "Выбрать софт",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Refresh Button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            refreshDashboardData()
                        }
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Обновить",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (isLoading && softwareList.isEmpty()) {
                item {
                    LoadingBlock(message = "Синхронизация с сервером Modkey...")
                }
            } else if (errorText != null && softwareList.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Warning,
                        title = "Ошибка синхронизации",
                        message = errorText.orEmpty()
                    )
                }
            } else {
                val currentSoft = selectedSoftware

                // 1. Emergency Status Card (Maintenance Switcher)
                if (currentSoft != null) {
                    item {
                        val isOnline = currentSoft.status == 1
                        val cardBg = if (isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        val iconColor = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        val titleColor = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = iconColor.copy(alpha = 0.18f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isOnline) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                                contentDescription = null,
                                                tint = iconColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isOnline) "Софт в сети (Online)" else "Техработы (Offline)",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = titleColor
                                        )
                                        Text(
                                            text = if (isOnline) "Клиенты авторизуются штатно" else (currentSoft.statusReason ?: "Сервер на паузе"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = titleColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Switch(
                                    checked = isOnline,
                                    onCheckedChange = { checked ->
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        emergencyTargetStatus = if (checked) 1 else 0
                                        showEmergencyConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                // 2. Quick Action Bar (Horizontal Scroll)
                item {
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionChip(
                            label = "Быстрый ключ",
                            icon = Icons.Filled.ElectricBolt,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = {
                                if (currentSoft != null) showQuickCreateSheet = true
                            }
                        )

                        QuickActionChip(
                            label = "Пауза всех",
                            icon = Icons.Filled.PauseCircle,
                            onClick = {
                                if (currentSoft != null) {
                                    scope.launch {
                                        try {
                                            apiClient.pauseKeys(account.apiKey, currentSoft.id)
                                            Toast.makeText(context, "Ключи поставлены на паузу", Toast.LENGTH_SHORT).show()
                                            refreshDashboardData()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )

                        QuickActionChip(
                            label = "Возобновить все",
                            icon = Icons.Filled.PlayArrow,
                            onClick = {
                                if (currentSoft != null) {
                                    scope.launch {
                                        try {
                                            apiClient.resumeKeys(account.apiKey, currentSoft.id)
                                            Toast.makeText(context, "Ключи возобновлены", Toast.LENGTH_SHORT).show()
                                            refreshDashboardData()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )

                        QuickActionChip(
                            label = "Продлить (+дни)",
                            icon = Icons.Filled.HourglassBottom,
                            onClick = {
                                showExtendKeysDialog = true
                            }
                        )

                        QuickActionChip(
                            label = "Сброс HWID всех",
                            icon = Icons.Filled.RestartAlt,
                            onClick = {
                                if (currentSoft != null) {
                                    scope.launch {
                                        try {
                                            apiClient.resetKeysHwid(account.apiKey, currentSoft.id)
                                            Toast.makeText(context, "HWID всех ключей сброшены", Toast.LENGTH_SHORT).show()
                                            refreshDashboardData()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )

                        QuickActionChip(
                            label = "Зеркала SSL",
                            icon = Icons.Filled.Language,
                            onClick = {
                                if (currentSoft != null) showDomainsSheet = true
                            }
                        )

                        QuickActionChip(
                            label = "Настройки софта",
                            icon = Icons.Filled.Settings,
                            onClick = {
                                if (currentSoft != null) showSoftSettingsDialog = true
                            }
                        )
                    }
                }

                // 3. Overview Statistics
                item {
                    SectionHeader(
                        title = "Статистика софта",
                        subtitle = "Проект: ${currentSoft?.name ?: "Все"}"
                    )
                }

                item {
                    val counts = softStats?.keys
                    val totalKeys = counts?.total ?: overviewStats?.keysTotal ?: 0
                    val activeKeys = counts?.active ?: overviewStats?.keysActive ?: 0
                    val pausedKeys = counts?.paused ?: 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Активные",
                            value = "$activeKeys",
                            icon = Icons.Filled.VpnKey,
                            subtitle = "из $totalKeys всего",
                            badgeColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToKeys
                        )

                        StatCard(
                            title = "На паузе",
                            value = "$pausedKeys",
                            icon = Icons.Filled.Pause,
                            subtitle = "заморожено",
                            badgeColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToKeys
                        )
                    }
                }

                item {
                    val counts = softStats?.keys
                    val blockedKeys = counts?.blocked ?: 0
                    val expiredKeys = counts?.expired ?: 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Заблокированы",
                            value = "$blockedKeys",
                            icon = Icons.Filled.Block,
                            subtitle = "доступ закрыт",
                            badgeColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToKeys
                        )

                        StatCard(
                            title = "Истекшие",
                            value = "$expiredKeys",
                            icon = Icons.Filled.HourglassBottom,
                            subtitle = "срок завершен",
                            badgeColor = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToKeys
                        )
                    }
                }

                // Domains & Users
                item {
                    val domainsCount = softStats?.customDomainsCount ?: overviewStats?.customDomains ?: 0
                    val usersCount = overviewStats?.usersCount ?: 0

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Зеркала и домены",
                            value = "$domainsCount",
                            icon = Icons.Filled.Language,
                            subtitle = "управление SSL",
                            badgeColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (selectedSoftware != null) showDomainsSheet = true
                            }
                        )

                        StatCard(
                            title = "Реселлеры",
                            value = "$usersCount",
                            icon = Icons.Filled.People,
                            subtitle = "клиентских аккаунтов",
                            badgeColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Recent Keys Activity
                item {
                    SectionHeader(
                        title = "Недавние ключи",
                        subtitle = "Быстрый сброс HWID и статус",
                        actionLabel = "Все ключи →",
                        onActionClick = onNavigateToKeys
                    )
                }

                if (recentKeys.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.VpnKey,
                            title = "Нет активных ключей",
                            message = "Нажмите «Быстрый ключ» выше для создания первой лицензии"
                        )
                    }
                } else {
                    itemsIndexed(recentKeys) { index, keyItem ->
                        Surface(
                            shape = connectedItemShape(index, recentKeys.size),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (keyItem.isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.VpnKey,
                                            contentDescription = null,
                                            tint = if (keyItem.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = keyItem.key,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(
                                            text = keyItem.keyType,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "HWID: ${keyItem.devicesUsed}/${keyItem.maxDevices} • Сбросов: ${keyItem.hwidResetsUsed}/${keyItem.hwidResetLimit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Quick Reset HWID button
                                IconButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        if (currentSoft != null) {
                                            scope.launch {
                                                try {
                                                    apiClient.resetKeyHwid(account.apiKey, currentSoft.id, keyItem.key)
                                                    Toast.makeText(context, "HWID сброшен!", Toast.LENGTH_SHORT).show()
                                                    refreshDashboardData()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.RestartAlt,
                                        contentDescription = "Сбросить HWID",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Quick Copy
                                IconButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Modkey", keyItem.key))
                                        Toast.makeText(context, "Ключ скопирован", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.ContentCopy,
                                        contentDescription = "Скопировать",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Modal: Quick Create Key
    if (showQuickCreateSheet && selectedSoftware != null) {
        QuickCreateKeyBottomSheet(
            apiKey = account.apiKey,
            software = selectedSoftware!!,
            onDismiss = { showQuickCreateSheet = false },
            onKeyCreated = { refreshDashboardData() }
        )
    }

    // Modal: Software Picker
    if (showSoftPickerSheet) {
        SoftwarePickerBottomSheet(
            softwareList = softwareList,
            selectedSoftwareId = selectedSoftware?.id,
            onSoftwareSelected = { soft ->
                selectedSoftware = soft
                showSoftPickerSheet = false
                refreshDashboardData()
            },
            onDismiss = { showSoftPickerSheet = false }
        )
    }

    // Modal: Custom Domains & SSL
    if (showDomainsSheet && selectedSoftware != null) {
        DomainsBottomSheet(
            apiKey = account.apiKey,
            software = selectedSoftware!!,
            onDismiss = { showDomainsSheet = false },
            onDomainsChanged = { refreshDashboardData() }
        )
    }

    // Dialog: Software Settings (edit-soft)
    if (showSoftSettingsDialog && selectedSoftware != null) {
        val currentSoft = selectedSoftware!!
        var softNameInput by remember(currentSoft) { mutableStateOf(currentSoft.name) }
        var keyFormatInput by remember(currentSoft) { mutableStateOf(currentSoft.keyFormat ?: "{RANDOM:32}") }
        var hwidLimitInput by remember(currentSoft) { mutableStateOf(currentSoft.hwidResetLimit.toString()) }
        var testConnectChecked by remember(currentSoft) { mutableStateOf(currentSoft.testConnectEnabled) }

        AlertDialog(
            onDismissRequest = { showSoftSettingsDialog = false },
            title = { Text("Настройки софта #${currentSoft.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = softNameInput,
                        onValueChange = { softNameInput = it },
                        label = { Text("Название софта") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = keyFormatInput,
                        onValueChange = { keyFormatInput = it },
                        label = { Text("Формат ключей (key_format)") },
                        placeholder = { Text("{NAME}-{DURATION}-{RANDOM:8}") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = hwidLimitInput,
                        onValueChange = { hwidLimitInput = it },
                        label = { Text("Лимит сбросов HWID по умолчанию") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = testConnectChecked,
                            onCheckedChange = { testConnectChecked = it }
                        )
                        Text(
                            text = "Включить Test Connect эндпоинт",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                apiClient.editSoftSettings(
                                    apiKey = account.apiKey,
                                    softId = currentSoft.id,
                                    name = softNameInput.trim(),
                                    keyFormat = keyFormatInput.trim(),
                                    hwidResetLimit = hwidLimitInput.toIntOrNull() ?: currentSoft.hwidResetLimit,
                                    testConnectEnabled = testConnectChecked
                                )
                                showSoftSettingsDialog = false
                                Toast.makeText(context, "Настройки софта сохранены!", Toast.LENGTH_SHORT).show()
                                refreshDashboardData()
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
                TextButton(onClick = { showSoftSettingsDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Modal: Account Switcher
    if (showAccountSwitcherSheet) {
        AccountSwitcherBottomSheet(
            accounts = allAccounts,
            activeAccountId = account.id,
            onAccountSelected = { acc ->
                showAccountSwitcherSheet = false
                onSwitchAccount(acc)
            },
            onAddNewAccount = onAddNewAccount,
            onLogoutCurrent = onLogout,
            onDismiss = { showAccountSwitcherSheet = false }
        )
    }

    // Dialog: Extend Keys (+Days)
    if (showExtendKeysDialog && selectedSoftware != null) {
        AlertDialog(
            onDismissRequest = { showExtendKeysDialog = false },
            title = { Text("Массовое продление ключей") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Добавьте дни компенсации ко всем активированным ключам софта «${selectedSoftware!!.name}».",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = extendDaysInput,
                        onValueChange = { extendDaysInput = it },
                        label = { Text("Количество дней") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = extendDaysInput.toIntOrNull() ?: 1
                        scope.launch {
                            try {
                                apiClient.extendKeys(account.apiKey, selectedSoftware!!.id, days)
                                Toast.makeText(context, "Ключи продлены на $days дн.", Toast.LENGTH_SHORT).show()
                                showExtendKeysDialog = false
                                refreshDashboardData()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Продлить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExtendKeysDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Emergency Status Change
    if (showEmergencyConfirmDialog && selectedSoftware != null) {
        AlertDialog(
            onDismissRequest = { showEmergencyConfirmDialog = false },
            title = {
                Text(if (emergencyTargetStatus == 1) "Включить софт (Online)?" else "Включить техработы (Offline)?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (emergencyTargetStatus == 1)
                            "Софт станет доступен для авторизации клиентов."
                        else
                            "Клиенты будут получать сообщение об ошибке при попытке входа в игру.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (emergencyTargetStatus == 0) {
                        OutlinedTextField(
                            value = emergencyReasonInput,
                            onValueChange = { emergencyReasonInput = it },
                            label = { Text("Причина отключения") },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                apiClient.editSoftStatus(
                                    apiKey = account.apiKey,
                                    softId = selectedSoftware!!.id,
                                    status = emergencyTargetStatus,
                                    reason = if (emergencyTargetStatus == 0) emergencyReasonInput else "on"
                                )
                                showEmergencyConfirmDialog = false
                                refreshDashboardData()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = if (emergencyTargetStatus == 0) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) {
                    Text(if (emergencyTargetStatus == 1) "Включить" else "Отключить софт")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
