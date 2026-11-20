package com.modkey.app.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.AccountMeInfo
import com.modkey.app.core.model.ClientCountryStat
import com.modkey.app.core.model.KeyItem
import com.modkey.app.core.model.OverviewStats
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.core.model.SoftStatsResponse
import com.modkey.app.core.model.SoftwareItem
import com.modkey.app.ui.components.ButtonVariant
import com.modkey.app.ui.components.EmptyState
import com.modkey.app.ui.components.LoadingBlock
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyCheckbox
import com.modkey.app.ui.components.ModkeyDialog
import com.modkey.app.ui.components.ModkeyFilterChip
import com.modkey.app.ui.components.ModkeyIconButton
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.ModkeyLogoMark
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.ModkeySwitch
import com.modkey.app.ui.components.QuickActionChip
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.components.StatCard
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.connectedItemShape
import com.modkey.app.ui.theme.modkeyPress
import kotlinx.coroutines.launch

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
    var allSoftKeysForChart by remember { mutableStateOf<List<KeyItem>>(emptyList()) }
    var clientCountries by remember { mutableStateOf<List<ClientCountryStat>>(emptyList()) }
    var meInfo by remember { mutableStateOf<AccountMeInfo?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Bottom Sheets & Dialogs
    var showQuickCreateSheet by remember { mutableStateOf(false) }
    var showSoftPickerSheet by remember { mutableStateOf(false) }
    var showAccountSwitcherSheet by remember { mutableStateOf(false) }
    var showDomainsSheet by remember { mutableStateOf(false) }
    var showBansSheet by remember { mutableStateOf(false) }
    var showLogsStatsSheet by remember { mutableStateOf(false) }
    var showBlockAllDialog by remember { mutableStateOf(false) }
    var showUnblockAllDialog by remember { mutableStateOf(false) }
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
                        limit = 100
                    )
                    allSoftKeysForChart = keysResp.items
                    recentKeys = keysResp.items.take(8)

                    launch {
                        try {
                            clientCountries = apiClient.getClients(account.apiKey, currentSelected.id)
                        } catch (_: Exception) {}
                    }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ModkeyDesign.Colors.background)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dashboard Header Bar (ModKey Panel Workspace Style)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModkeyLogoMark(size = 22.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Workspace",
                            style = ModkeyDesign.Typography.labelMedium(),
                            color = ModkeyDesign.Colors.onSurfaceVariant
                        )
                        Text(
                            text = "  /  ",
                            style = ModkeyDesign.Typography.labelMedium(),
                            color = ModkeyDesign.Colors.onSurfaceDisabled
                        )
                        Text(
                            text = "Dashboard",
                            style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.SemiBold),
                            color = ModkeyDesign.Colors.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Software Selector Pill
                        Surface(
                            shape = ModkeyDesign.Shapes.small,
                            color = ModkeyDesign.Colors.primaryContainer,
                            border = BorderStroke(1.dp, ModkeyDesign.Colors.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.modkeyPress {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showSoftPickerSheet = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ModkeyIcon(
                                    imageVector = Icons.Filled.Computer,
                                    contentDescription = null,
                                    tint = ModkeyDesign.Colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = selectedSoftware?.name ?: "Выбрать софт",
                                    style = ModkeyDesign.Typography.labelSmall().copy(fontWeight = FontWeight.SemiBold),
                                    color = ModkeyDesign.Colors.primary,
                                    maxLines = 1
                                )
                            }
                        }

                        // Refresh Button
                        ModkeyIconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                refreshDashboardData()
                            },
                            size = 32.dp,
                            iconSize = 18.dp
                        ) {
                            ModkeyIcon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Обновить",
                                tint = ModkeyDesign.Colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Greeting + Primary CTAs row (like "Good afternoon, Lonzovich" + Browse keys / + Generate)
                val displayName = meInfo?.username?.ifBlank { account.label } ?: account.label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .modkeyPress {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showAccountSwitcherSheet = true
                            }
                    ) {
                        Text(
                            text = "Привет, $displayName",
                            style = ModkeyDesign.Typography.titleLarge().copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = if (meInfo != null) "${meInfo!!.balance} ₽ • Сменить аккаунт ▾" else "Сменить аккаунт ▾",
                            style = ModkeyDesign.Typography.bodySmall(),
                            color = ModkeyDesign.Colors.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModkeyButton(
                            onClick = onNavigateToKeys,
                            variant = ButtonVariant.Secondary,
                            shape = ModkeyDesign.Shapes.small,
                            minHeight = 38.dp
                        ) {
                            Text(
                                text = "Ключи",
                                style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.SemiBold),
                                color = ModkeyDesign.Colors.onSurface
                            )
                        }
                        ModkeyButton(
                            onClick = { if (selectedSoftware != null) showQuickCreateSheet = true },
                            variant = ButtonVariant.Primary,
                            shape = ModkeyDesign.Shapes.small,
                            minHeight = 38.dp
                        ) {
                            Text(
                                text = "+ Создать",
                                style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.SemiBold),
                                color = ModkeyDesign.Colors.onPrimary
                            )
                        }
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
                        val cardBg = if (isOnline) ModkeyDesign.Colors.primaryContainer.copy(alpha = 0.5f)
                        else ModkeyDesign.Colors.errorContainer.copy(alpha = 0.8f)
                        val iconColor = if (isOnline) ModkeyDesign.Colors.primary else ModkeyDesign.Colors.error
                        val titleColor = if (isOnline) ModkeyDesign.Colors.onSurface else ModkeyDesign.Colors.onErrorContainer

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
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clipCircle()
                                            .background(iconColor.copy(alpha = 0.18f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ModkeyIcon(
                                            imageVector = if (isOnline) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isOnline) "Софт в сети (Online)" else "Техработы (Offline)",
                                            style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold),
                                            color = titleColor
                                        )
                                        Text(
                                            text = if (isOnline) "Клиенты авторизуются штатно" else (currentSoft.statusReason ?: "Сервер на паузе"),
                                            style = ModkeyDesign.Typography.bodySmall(),
                                            color = titleColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                ModkeySwitch(
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
                            containerColor = ModkeyDesign.Colors.primaryContainer,
                            contentColor = ModkeyDesign.Colors.primary,
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
                            label = "Заблок. все",
                            icon = Icons.Filled.Block,
                            onClick = {
                                if (currentSoft != null) showBlockAllDialog = true
                            }
                        )

                        QuickActionChip(
                            label = "Разблок. все",
                            icon = Icons.Filled.LockOpen,
                            onClick = {
                                if (currentSoft != null) showUnblockAllDialog = true
                            }
                        )

                        QuickActionChip(
                            label = "Баны HWID",
                            icon = Icons.Filled.Devices,
                            onClick = {
                                if (currentSoft != null) showBansSheet = true
                            }
                        )

                        QuickActionChip(
                            label = "Журнал и страны",
                            icon = Icons.Filled.History,
                            onClick = {
                                showLogsStatsSheet = true
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
                            badgeColor = ModkeyDesign.Colors.primary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToKeys
                        )

                        StatCard(
                            title = "На паузе",
                            value = "$pausedKeys",
                            icon = Icons.Filled.Pause,
                            subtitle = "заморожено",
                            badgeColor = ModkeyDesign.Colors.warning,
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
                            badgeColor = ModkeyDesign.Colors.error,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToKeys
                        )

                        StatCard(
                            title = "Истекшие",
                            value = "$expiredKeys",
                            icon = Icons.Filled.HourglassBottom,
                            subtitle = "срок завершен",
                            badgeColor = ModkeyDesign.Colors.onSurfaceVariant,
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
                            badgeColor = ModkeyDesign.Colors.success,
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
                            badgeColor = ModkeyDesign.Colors.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3.5 Keys Created (14-day bar chart, ModKey Panel style)
                if (currentSoft != null) {
                    item {
                        val today = remember { java.time.LocalDate.now() }
                        val dayDates = remember(today) { (13 downTo 0).map { today.minusDays(it.toLong()) } }
                        val countsByDay = remember(allSoftKeysForChart, dayDates) {
                            val grouped = allSoftKeysForChart
                                .mapNotNull { it.createdAt?.take(10) }
                                .groupingBy { it }
                                .eachCount()
                            dayDates.map { d ->
                                val iso = d.toString()
                                d to (grouped[iso] ?: 0)
                            }
                        }
                        val maxDayCount = (countsByDay.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                        val busiestDay = countsByDay.maxByOrNull { it.second }

                        Surface(
                            shape = ModkeyDesign.Shapes.medium,
                            color = ModkeyDesign.Colors.surfaceContainer,
                            border = BorderStroke(1.dp, ModkeyDesign.Colors.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Создано ключей (14 дней)",
                                        style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                                    )
                                    val sub = if (busiestDay != null && busiestDay.second > 0) {
                                        val fmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM")
                                        "Пик: ${busiestDay.first.format(fmt)} (${busiestDay.second} кл.)"
                                    } else {
                                        "Нет созданных ключей за последние 14 дней"
                                    }
                                    Text(
                                        text = sub,
                                        style = ModkeyDesign.Typography.bodySmall(),
                                        color = ModkeyDesign.Colors.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(84.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    countsByDay.forEach { (date, count) ->
                                        val ratio = if (count == 0) 0.05f else (count.toFloat() / maxDayCount.toFloat()).coerceIn(0.14f, 1f)
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.7f)
                                                    .height((64 * ratio).dp)
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(
                                                        if (count > 0) ModkeyDesign.Colors.primary
                                                        else ModkeyDesign.Colors.surfaceContainerHighest
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (date.dayOfMonth % 2 == 1 || date == today) "${date.dayOfMonth}" else "",
                                                style = ModkeyDesign.Typography.labelSmall().copy(fontSize = 8.sp),
                                                color = ModkeyDesign.Colors.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3.6 Connect Endpoint Card
                    item {
                        val connectUrl = "https://mod.wtf/${currentSoft.id}/connect"
                        Surface(
                            shape = ModkeyDesign.Shapes.medium,
                            color = ModkeyDesign.Colors.surfaceContainer,
                            border = BorderStroke(1.dp, ModkeyDesign.Colors.outline),
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
                                        text = "Connect",
                                        style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Управление доменами",
                                        style = ModkeyDesign.Typography.labelSmall().copy(fontWeight = FontWeight.SemiBold),
                                        color = ModkeyDesign.Colors.primary,
                                        modifier = Modifier.modkeyPress { showDomainsSheet = true }
                                    )
                                }

                                Surface(
                                    shape = ModkeyDesign.Shapes.small,
                                    color = ModkeyDesign.Colors.background,
                                    border = BorderStroke(1.dp, ModkeyDesign.Colors.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = connectUrl,
                                            style = ModkeyDesign.Typography.bodySmall().copy(fontFamily = FontFamily.Monospace),
                                            color = ModkeyDesign.Colors.onSurface,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = ModkeyDesign.Shapes.extraSmall,
                                            color = ModkeyDesign.Colors.surfaceContainerHigh,
                                            border = BorderStroke(1.dp, ModkeyDesign.Colors.outline),
                                            modifier = Modifier.modkeyPress {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Connect URL", connectUrl))
                                                Toast.makeText(context, "URL скопирован", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                ModkeyIcon(
                                                    imageVector = Icons.Filled.ContentCopy,
                                                    contentDescription = null,
                                                    tint = ModkeyDesign.Colors.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Copy",
                                                    style = ModkeyDesign.Typography.labelSmall(),
                                                    color = ModkeyDesign.Colors.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3.7 Where your clients are (Countries preview card)
                    if (clientCountries.isNotEmpty()) {
                        item {
                            val topCountries = clientCountries.take(4)
                            val maxC = (topCountries.maxOfOrNull { it.clientsCount } ?: 1).coerceAtLeast(1)
                            Surface(
                                shape = ModkeyDesign.Shapes.medium,
                                color = ModkeyDesign.Colors.surfaceContainer,
                                border = BorderStroke(1.dp, ModkeyDesign.Colors.outline),
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
                                            text = "География клиентов",
                                            style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Подробнее",
                                            style = ModkeyDesign.Typography.labelSmall().copy(fontWeight = FontWeight.SemiBold),
                                            color = ModkeyDesign.Colors.primary,
                                            modifier = Modifier.modkeyPress { showLogsStatsSheet = true }
                                        )
                                    }

                                    topCountries.forEach { stat ->
                                        val frac = (stat.clientsCount.toFloat() / maxC.toFloat()).coerceIn(0.06f, 1f)
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = listOfNotNull(stat.countryCode, stat.country.ifBlank { "Unknown" }).joinToString("  "),
                                                    style = ModkeyDesign.Typography.bodySmall().copy(fontWeight = FontWeight.Medium)
                                                )
                                                Text(
                                                    text = "${stat.clientsCount}" + (stat.percent?.let { " (${String.format("%.0f", it)}%)" } ?: ""),
                                                    style = ModkeyDesign.Typography.labelSmall(),
                                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(CircleShape)
                                                    .background(ModkeyDesign.Colors.surfaceContainerHighest)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(frac)
                                                        .height(4.dp)
                                                        .clip(CircleShape)
                                                        .background(ModkeyDesign.Colors.primary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                            color = ModkeyDesign.Colors.surfaceContainerHigh,
                            border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clipCircle()
                                        .background(
                                            if (keyItem.isOnline) ModkeyDesign.Colors.primaryContainer
                                            else ModkeyDesign.Colors.errorContainer,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ModkeyIcon(
                                        imageVector = Icons.Filled.VpnKey,
                                        contentDescription = null,
                                        tint = if (keyItem.isOnline) ModkeyDesign.Colors.primary else ModkeyDesign.Colors.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = keyItem.key,
                                        style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(
                                            text = keyItem.keyType,
                                            color = ModkeyDesign.Colors.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "HWID: ${keyItem.devicesUsed}/${keyItem.maxDevices} • Сбросов: ${keyItem.hwidResetsUsed}/${keyItem.hwidResetLimit}",
                                            style = ModkeyDesign.Typography.bodySmall(),
                                            color = ModkeyDesign.Colors.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Quick Reset HWID button
                                ModkeyIconButton(
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
                                    },
                                    size = 38.dp,
                                    iconSize = 18.dp
                                ) {
                                    ModkeyIcon(
                                        imageVector = Icons.Filled.RestartAlt,
                                        contentDescription = "Сбросить HWID",
                                        tint = ModkeyDesign.Colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Quick Copy
                                ModkeyIconButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Modkey", keyItem.key))
                                        Toast.makeText(context, "Ключ скопирован", Toast.LENGTH_SHORT).show()
                                    },
                                    size = 38.dp,
                                    iconSize = 18.dp
                                ) {
                                    ModkeyIcon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Скопировать",
                                        tint = ModkeyDesign.Colors.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
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

    // Modal: Device Bans
    if (showBansSheet && selectedSoftware != null) {
        DeviceBansBottomSheet(
            apiKey = account.apiKey,
            software = selectedSoftware!!,
            onDismiss = { showBansSheet = false }
        )
    }

    // Modal: Logs & Stats
    if (showLogsStatsSheet) {
        LogsAndStatsBottomSheet(
            apiKey = account.apiKey,
            software = selectedSoftware,
            onDismiss = { showLogsStatsSheet = false }
        )
    }

    // Dialog: Block All Keys
    if (showBlockAllDialog && selectedSoftware != null) {
        val soft = selectedSoftware!!
        ModkeyDialog(
            title = "Заблокировать все ключи?",
            onDismiss = { showBlockAllDialog = false },
            confirmLabel = "Заблокировать",
            confirmContainerColor = ModkeyDesign.Colors.error,
            confirmContentColor = ModkeyDesign.Colors.onError,
            onConfirm = {
                showBlockAllDialog = false
                scope.launch {
                    try {
                        apiClient.blockKeys(account.apiKey, soft.id)
                        Toast.makeText(context, "Все ключи заблокированы", Toast.LENGTH_SHORT).show()
                        refreshDashboardData()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Text(
                text = "Все ключи проекта «${soft.name}» будут переведены в статус заблокированных.",
                style = ModkeyDesign.Typography.bodyMedium(),
                color = ModkeyDesign.Colors.onSurfaceVariant
            )
        }
    }

    // Dialog: Unblock All Keys
    if (showUnblockAllDialog && selectedSoftware != null) {
        val soft = selectedSoftware!!
        ModkeyDialog(
            title = "Разблокировать все ключи?",
            onDismiss = { showUnblockAllDialog = false },
            confirmLabel = "Разблокировать",
            onConfirm = {
                showUnblockAllDialog = false
                scope.launch {
                    try {
                        apiClient.unblockKeys(account.apiKey, soft.id)
                        Toast.makeText(context, "Все ключи разблокированы", Toast.LENGTH_SHORT).show()
                        refreshDashboardData()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Text(
                text = "Блокировка со всех ключей проекта «${soft.name}» будет снята.",
                style = ModkeyDesign.Typography.bodyMedium(),
                color = ModkeyDesign.Colors.onSurfaceVariant
            )
        }
    }

    // Dialog: Software Settings (edit-soft)
    if (showSoftSettingsDialog && selectedSoftware != null) {
        val currentSoft = selectedSoftware!!
        var softNameInput by remember(currentSoft) { mutableStateOf(currentSoft.name) }
        var keyFormatInput by remember(currentSoft) { mutableStateOf(currentSoft.keyFormat ?: "{RANDOM:32}") }
        var hwidLimitInput by remember(currentSoft) { mutableStateOf(currentSoft.hwidResetLimit.toString()) }
        var testConnectChecked by remember(currentSoft) { mutableStateOf(currentSoft.testConnectEnabled) }

        ModkeyDialog(
            title = "Настройки софта #${currentSoft.id}",
            onDismiss = { showSoftSettingsDialog = false },
            confirmLabel = "Сохранить",
            onConfirm = {
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
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModkeyOutlinedTextField(
                    value = softNameInput,
                    onValueChange = { softNameInput = it },
                    label = "Название софта"
                )
                ModkeyOutlinedTextField(
                    value = keyFormatInput,
                    onValueChange = { keyFormatInput = it },
                    label = "Формат ключей (key_format)",
                    placeholder = "{NAME}-{DURATION}-{RANDOM:8}"
                )
                ModkeyOutlinedTextField(
                    value = hwidLimitInput,
                    onValueChange = { hwidLimitInput = it },
                    label = "Лимит сбросов HWID по умолчанию"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.modkeyPress { testConnectChecked = !testConnectChecked }
                ) {
                    ModkeyCheckbox(
                        checked = testConnectChecked,
                        onCheckedChange = { testConnectChecked = it }
                    )
                    Text(
                        text = "Включить Test Connect эндпоинт",
                        style = ModkeyDesign.Typography.bodySmall()
                    )
                }
            }
        }
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
        ModkeyDialog(
            title = "Массовое продление ключей",
            onDismiss = { showExtendKeysDialog = false },
            confirmLabel = "Продлить",
            onConfirm = {
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Добавьте дни компенсации ко всем активированным ключам софта «${selectedSoftware!!.name}».",
                    style = ModkeyDesign.Typography.bodyMedium(),
                    color = ModkeyDesign.Colors.onSurfaceVariant
                )
                ModkeyOutlinedTextField(
                    value = extendDaysInput,
                    onValueChange = { extendDaysInput = it },
                    label = "Количество дней"
                )
            }
        }
    }

    // Dialog: Emergency Status Change
    if (showEmergencyConfirmDialog && selectedSoftware != null) {
        ModkeyDialog(
            title = if (emergencyTargetStatus == 1) "Включить софт (Online)?" else "Включить техработы (Offline)?",
            onDismiss = { showEmergencyConfirmDialog = false },
            confirmLabel = if (emergencyTargetStatus == 1) "Включить" else "Отключить софт",
            confirmContainerColor = if (emergencyTargetStatus == 0) ModkeyDesign.Colors.error else ModkeyDesign.Colors.primary,
            confirmContentColor = ModkeyDesign.Colors.onError,
            onConfirm = {
                scope.launch {
                    try {
                        apiClient.editSoftStatus(
                            apiKey = account.apiKey,
                            softId = selectedSoftware!!.id,
                            status = emergencyTargetStatus,
                            reason = if (emergencyTargetStatus == 0) emergencyReasonInput else null
                        )
                        showEmergencyConfirmDialog = false
                        refreshDashboardData()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (emergencyTargetStatus == 1)
                        "Софт станет доступен для авторизации клиентов."
                    else
                        "Клиенты будут получать сообщение об ошибке при попытке входа в игру.",
                    style = ModkeyDesign.Typography.bodyMedium(),
                    color = ModkeyDesign.Colors.onSurfaceVariant
                )
                if (emergencyTargetStatus == 0) {
                    ModkeyOutlinedTextField(
                        value = emergencyReasonInput,
                        onValueChange = { emergencyReasonInput = it },
                        label = "Причина отключения"
                    )
                }
            }
        }
    }
}

private fun Modifier.clipCircle(): Modifier = this.then(
    Modifier.clip(CircleShape)
)
