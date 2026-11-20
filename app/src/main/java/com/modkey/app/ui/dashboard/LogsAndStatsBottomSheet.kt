package com.modkey.app.ui.dashboard

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.AuditLogItem
import com.modkey.app.core.model.ClientCountryStat
import com.modkey.app.core.model.ConnectLoginItem
import com.modkey.app.core.model.SoftwareItem
import com.modkey.app.ui.components.EmptyState
import com.modkey.app.ui.components.LoadingBlock
import com.modkey.app.ui.components.ModkeyBottomSheet
import com.modkey.app.ui.components.ModkeyFilterChip
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.SheetHeader
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.connectedItemShape
import kotlinx.coroutines.launch

@Composable
fun LogsAndStatsBottomSheet(
    apiKey: String,
    software: SoftwareItem?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Logs, 1: Logins, 2: Countries
    var isLoading by remember { mutableStateOf(true) }

    var logs by remember { mutableStateOf<List<AuditLogItem>>(emptyList()) }
    var logins by remember { mutableStateOf<List<ConnectLoginItem>>(emptyList()) }
    var countries by remember { mutableStateOf<List<ClientCountryStat>>(emptyList()) }
    var keyFilter by remember { mutableStateOf("") }

    fun loadCurrentTab() {
        isLoading = true
        scope.launch {
            try {
                when (selectedTab) {
                    0 -> logs = apiClient.getLogs(apiKey = apiKey, softId = software?.id)
                    1 -> {
                        val softId = software?.id
                        if (softId != null) {
                            logins = apiClient.getConnectLogins(
                                apiKey = apiKey,
                                softId = softId,
                                key = keyFilter.ifBlank { null }
                            )
                        } else {
                            logins = emptyList()
                        }
                    }
                    2 -> countries = apiClient.getClients(apiKey = apiKey, softId = software?.id)
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTab, software?.id, keyFilter) {
        loadCurrentTab()
    }

    ModkeyBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(
                title = "Журнал и статистика",
                subtitle = software?.let { "Проект: ${it.name}" } ?: "Общая статистика",
                onClose = onDismiss
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModkeyFilterChip(
                    selected = selectedTab == 0,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        selectedTab = 0
                    },
                    label = "Журнал",
                    leadingIcon = Icons.Filled.History
                )
                ModkeyFilterChip(
                    selected = selectedTab == 1,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        selectedTab = 1
                    },
                    label = "Входы по ключам",
                    leadingIcon = Icons.Filled.Login
                )
                ModkeyFilterChip(
                    selected = selectedTab == 2,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        selectedTab = 2
                    },
                    label = "Страны",
                    leadingIcon = Icons.Filled.Public
                )
            }

            if (selectedTab == 1) {
                ModkeyOutlinedTextField(
                    value = keyFilter,
                    onValueChange = { keyFilter = it },
                    placeholder = "Фильтр по ключу (необязательно)",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isLoading) {
                LoadingBlock(message = "Загрузка...")
            } else {
                when (selectedTab) {
                    0 -> {
                        if (logs.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.History,
                                title = "Журнал пуст",
                                message = "События не найдены"
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                logs.forEachIndexed { index, item ->
                                    Surface(
                                        shape = connectedItemShape(index, logs.size),
                                        color = ModkeyDesign.Colors.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.titleText,
                                                    style = ModkeyDesign.Typography.bodyMedium().copy(fontWeight = FontWeight.Bold)
                                                )
                                                StatusBadge(
                                                    text = item.actorText,
                                                    color = ModkeyDesign.Colors.primary
                                                )
                                            }
                                            val desc = item.details ?: item.message
                                            if (!desc.isNullOrBlank()) {
                                                Text(
                                                    text = desc,
                                                    style = ModkeyDesign.Typography.bodySmall(),
                                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = item.ip ?: "",
                                                    style = ModkeyDesign.Typography.labelSmall(),
                                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                                )
                                                Text(
                                                    text = item.createdAt ?: "",
                                                    style = ModkeyDesign.Typography.labelSmall(),
                                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (logins.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.Login,
                                title = "Входов не найдено",
                                message = if (software == null) "Выберите софт" else "История входов по ключам пуста"
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                logins.forEachIndexed { index, item ->
                                    val isOk = item.ok ?: (item.status != "error" && item.status != "failed")
                                    Surface(
                                        shape = connectedItemShape(index, logins.size),
                                        color = ModkeyDesign.Colors.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.keyText.ifBlank { "—" },
                                                    style = ModkeyDesign.Typography.bodyMedium().copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                StatusBadge(
                                                    text = item.status ?: if (isOk) "OK" else "FAIL",
                                                    color = if (isOk) ModkeyDesign.Colors.success else ModkeyDesign.Colors.error
                                                )
                                            }
                                            if (item.deviceText.isNotBlank()) {
                                                Text(
                                                    text = "HWID: ${item.deviceText}",
                                                    style = ModkeyDesign.Typography.bodySmall(),
                                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = listOfNotNull(item.country, item.ip).joinToString(" • "),
                                                    style = ModkeyDesign.Typography.labelSmall(),
                                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                                )
                                                Text(
                                                    text = item.createdAt ?: "",
                                                    style = ModkeyDesign.Typography.labelSmall(),
                                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        if (countries.isEmpty()) {
                            EmptyState(
                                icon = Icons.Filled.Public,
                                title = "Нет данных по странам",
                                message = "Статистика по клиентам пока отсутствует"
                            )
                        } else {
                            val maxCount = countries.maxOfOrNull { it.clientsCount }.takeIf { (it ?: 0) > 0 } ?: 1
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                countries.forEachIndexed { index, stat ->
                                    val fraction = (stat.clientsCount.toFloat() / maxCount.toFloat()).coerceIn(0.05f, 1f)
                                    Surface(
                                        shape = connectedItemShape(index, countries.size),
                                        color = ModkeyDesign.Colors.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = buildString {
                                                        if (!stat.countryCode.isNullOrBlank()) append("${stat.countryCode} • ")
                                                        append(stat.country.ifBlank { "Unknown" })
                                                    },
                                                    style = ModkeyDesign.Typography.bodyMedium().copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = buildString {
                                                        append("${stat.clientsCount}")
                                                        if (stat.percent != null) append(" (${String.format("%.1f", stat.percent)}%)")
                                                    },
                                                    style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.Bold),
                                                    color = ModkeyDesign.Colors.primary
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(ModkeyDesign.Colors.surfaceContainerHighest)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(fraction)
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp))
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
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
