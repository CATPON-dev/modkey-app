package com.modkey.app.ui.dashboard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.modkey.app.core.model.DeviceBanItem
import com.modkey.app.core.model.SoftwareItem
import com.modkey.app.ui.components.ButtonVariant
import com.modkey.app.ui.components.CircularLoader
import com.modkey.app.ui.components.EmptyState
import com.modkey.app.ui.components.LoadingBlock
import com.modkey.app.ui.components.ModkeyBottomSheet
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.ModkeyIconButton
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.SheetHeader
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.connectedItemShape
import kotlinx.coroutines.launch

@Composable
fun DeviceBansBottomSheet(
    apiKey: String,
    software: SoftwareItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient

    var bans by remember { mutableStateOf<List<DeviceBanItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    var serialInput by remember { mutableStateOf("") }
    var reasonInput by remember { mutableStateOf("") }
    var isBanning by remember { mutableStateOf(false) }

    fun loadBans() {
        isLoading = true
        scope.launch {
            try {
                bans = apiClient.getBans(
                    apiKey = apiKey,
                    softId = software.id,
                    query = searchQuery.ifBlank { null }
                )
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Ошибка загрузки банов", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(software.id, searchQuery) {
        loadBans()
    }

    ModkeyBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(
                title = "Баны устройств (${bans.size})",
                subtitle = "Проект: ${software.name}",
                onClose = onDismiss
            )

            // Ban new device card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ModkeyDesign.Colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Заблокировать устройство (HWID / Serial)",
                        style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                    )
                    ModkeyOutlinedTextField(
                        value = serialInput,
                        onValueChange = { serialInput = it },
                        placeholder = "Serial / HWID устройства",
                        modifier = Modifier.fillMaxWidth()
                    )
                    ModkeyOutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        placeholder = "Причина (необязательно)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    ModkeyButton(
                        onClick = {
                            if (serialInput.isBlank()) {
                                Toast.makeText(context, "Укажите Serial / HWID", Toast.LENGTH_SHORT).show()
                                return@ModkeyButton
                            }
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            isBanning = true
                            scope.launch {
                                try {
                                    apiClient.banDevice(
                                        apiKey = apiKey,
                                        softId = software.id,
                                        serial = serialInput,
                                        reason = reasonInput.ifBlank { null }
                                    )
                                    serialInput = ""
                                    reasonInput = ""
                                    Toast.makeText(context, "Устройство заблокировано", Toast.LENGTH_SHORT).show()
                                    loadBans()
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Ошибка блокировки", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isBanning = false
                                }
                            }
                        },
                        variant = ButtonVariant.Danger,
                        enabled = !isBanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isBanning) {
                            CircularLoader(color = ModkeyDesign.Colors.onError, size = 18.dp)
                        } else {
                            ModkeyIcon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = null,
                                tint = ModkeyDesign.Colors.onError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Забанить устройство",
                                style = ModkeyDesign.Typography.labelLarge(),
                                color = ModkeyDesign.Colors.onError
                            )
                        }
                    }
                }
            }

            // Search
            ModkeyOutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Поиск по Serial / причине...",
                leadingIcon = {
                    ModkeyIcon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = ModkeyDesign.Colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        ModkeyIconButton(onClick = { searchQuery = "" }, size = 24.dp, iconSize = 14.dp) {
                            ModkeyIcon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Очистить",
                                tint = ModkeyDesign.Colors.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (isLoading) {
                LoadingBlock(message = "Загрузка списка банов...")
            } else if (bans.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Devices,
                    title = "Банов нет",
                    message = "Заблокированные устройства отсутствуют"
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    bans.forEachIndexed { index, ban ->
                        Surface(
                            shape = connectedItemShape(index, bans.size),
                            color = ModkeyDesign.Colors.surfaceContainerHigh,
                            border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = ban.deviceSerial.ifBlank { "ID #${ban.id}" },
                                        style = ModkeyDesign.Typography.bodyMedium().copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                    if (!ban.reason.isNullOrBlank()) {
                                        Text(
                                            text = ban.reason,
                                            style = ModkeyDesign.Typography.bodySmall(),
                                            color = ModkeyDesign.Colors.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        StatusBadge(text = "BAN", color = ModkeyDesign.Colors.error)
                                        if (!ban.createdAt.isNullOrBlank()) {
                                            Text(
                                                text = ban.createdAt,
                                                style = ModkeyDesign.Typography.labelSmall(),
                                                color = ModkeyDesign.Colors.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                ModkeyIconButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        scope.launch {
                                            try {
                                                apiClient.unbanDevice(
                                                    apiKey = apiKey,
                                                    softId = software.id,
                                                    banId = ban.id.takeIf { it > 0 },
                                                    serial = ban.deviceSerial.takeIf { it.isNotBlank() }
                                                )
                                                Toast.makeText(context, "Устройство разбанено", Toast.LENGTH_SHORT).show()
                                                loadBans()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    containerColor = ModkeyDesign.Colors.errorContainer
                                ) {
                                    ModkeyIcon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Разбанить",
                                        tint = ModkeyDesign.Colors.error,
                                        modifier = Modifier.size(18.dp)
                                    )
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
