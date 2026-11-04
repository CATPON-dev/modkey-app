package com.modkey.app.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.modkey.app.core.model.DomainItem
import com.modkey.app.core.model.SoftwareItem
import com.modkey.app.core.network.ModkeyDnsVerificationException
import com.modkey.app.ui.components.CircularLoader
import com.modkey.app.ui.components.ModkeyBottomSheet
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyFilterChip
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
fun DomainsBottomSheet(
    apiKey: String,
    software: SoftwareItem,
    onDismiss: () -> Unit,
    onDomainsChanged: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient

    var selectedEndpoint by remember { mutableStateOf("main") } // "main" or "test"
    var domains by remember { mutableStateOf<List<DomainItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var newDomainInput by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    var dnsTxtRecordName by remember { mutableStateOf<String?>(null) }
    var dnsTxtRecordValue by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadDomains() {
        isLoading = true
        scope.launch {
            try {
                domains = apiClient.getDomains(apiKey, software.id, selectedEndpoint)
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(software.id, selectedEndpoint) {
        loadDomains()
    }

    ModkeyBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            SheetHeader(
                title = "Зеркала и SSL Домены",
                subtitle = "Софт: ${software.name} • Лимит: до 5 доменов",
                onClose = onDismiss,
                leading = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ModkeyDesign.Colors.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ModkeyIcon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = null,
                            tint = ModkeyDesign.Colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            // Endpoint Switcher
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModkeyFilterChip(
                    selected = selectedEndpoint == "main",
                    onClick = { selectedEndpoint = "main" },
                    label = "Main Connect"
                )
                ModkeyFilterChip(
                    selected = selectedEndpoint == "test",
                    onClick = { selectedEndpoint = "test" },
                    label = "Test Connect"
                )
            }

            // Add New Domain Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ModkeyDesign.Colors.surfaceContainer,
                border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Привязать новый домен (Let's Encrypt SSL)",
                        style = ModkeyDesign.Typography.labelLarge().copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModkeyOutlinedTextField(
                            value = newDomainInput,
                            onValueChange = {
                                newDomainInput = it
                                errorMessage = null
                            },
                            placeholder = "keys.yourdomain.com",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ModkeyButton(
                            onClick = {
                                if (newDomainInput.isBlank()) return@ModkeyButton
                                isAdding = true
                                errorMessage = null
                                dnsTxtRecordName = null
                                dnsTxtRecordValue = null
                                scope.launch {
                                    try {
                                        apiClient.createDomain(
                                            apiKey = apiKey,
                                            softId = software.id,
                                            domain = newDomainInput.trim(),
                                            endpoint = selectedEndpoint
                                        )
                                        newDomainInput = ""
                                        Toast.makeText(context, "Домен успешно привязан и защищен SSL!", Toast.LENGTH_LONG).show()
                                        loadDomains()
                                        onDomainsChanged()
                                    } catch (dnsEx: ModkeyDnsVerificationException) {
                                        dnsTxtRecordName = dnsEx.recordName
                                        dnsTxtRecordValue = dnsEx.recordValue
                                        errorMessage = "Требуется подтверждение владения доменом через DNS TXT запись:"
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Ошибка добавления домена"
                                    } finally {
                                        isAdding = false
                                    }
                                }
                            },
                            enabled = !isAdding && newDomainInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            minHeight = 48.dp
                        ) {
                            if (isAdding) {
                                CircularLoader(
                                    modifier = Modifier.size(18.dp),
                                    color = ModkeyDesign.Colors.onPrimary
                                )
                            } else {
                                ModkeyIcon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = ModkeyDesign.Colors.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Добавить",
                                    style = ModkeyDesign.Typography.labelLarge(),
                                    color = ModkeyDesign.Colors.onPrimary
                                )
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = ModkeyDesign.Typography.bodySmall(),
                            color = ModkeyDesign.Colors.error
                        )
                    }

                    // DNS TXT Verification Helper Card
                    if (!dnsTxtRecordName.isNullOrBlank() && !dnsTxtRecordValue.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ModkeyDesign.Colors.surfaceContainerHighest,
                            border = BorderStroke(1.dp, ModkeyDesign.Colors.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "1. Направьте A-запись домена на IP панели mod.wtf\n2. Добавьте TXT-запись в DNS вашего домена:",
                                    style = ModkeyDesign.Typography.bodySmall().copy(fontWeight = FontWeight.SemiBold)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Имя (Host / Name):",
                                            style = ModkeyDesign.Typography.labelSmall(),
                                            color = ModkeyDesign.Colors.onSurfaceVariant
                                        )
                                        Text(
                                            text = dnsTxtRecordName.orEmpty(),
                                            style = ModkeyDesign.Typography.bodySmall().copy(fontWeight = FontWeight.Bold),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    ModkeyIconButton(
                                        onClick = {
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cb.setPrimaryClip(ClipData.newPlainText("TXT Name", dnsTxtRecordName))
                                            Toast.makeText(context, "Имя TXT скопировано", Toast.LENGTH_SHORT).show()
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = null,
                                            tint = ModkeyDesign.Colors.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Значение (TXT Value):",
                                            style = ModkeyDesign.Typography.labelSmall(),
                                            color = ModkeyDesign.Colors.onSurfaceVariant
                                        )
                                        Text(
                                            text = dnsTxtRecordValue.orEmpty(),
                                            style = ModkeyDesign.Typography.bodySmall().copy(fontWeight = FontWeight.Bold),
                                            fontFamily = FontFamily.Monospace,
                                            color = ModkeyDesign.Colors.primary
                                        )
                                    }
                                    ModkeyIconButton(
                                        onClick = {
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cb.setPrimaryClip(ClipData.newPlainText("TXT Value", dnsTxtRecordValue))
                                            Toast.makeText(context, "Значение TXT скопировано", Toast.LENGTH_SHORT).show()
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = null,
                                            tint = ModkeyDesign.Colors.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Existing Domains List
            Text(
                text = "Подключённые домены (${domains.size}/5)",
                style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularLoader(modifier = Modifier.size(28.dp))
                }
            } else if (domains.isEmpty()) {
                Text(
                    text = "Для этого эндпоинта пока нет привязанных зеркал.",
                    style = ModkeyDesign.Typography.bodySmall(),
                    color = ModkeyDesign.Colors.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    domains.forEachIndexed { idx, dom ->
                        Surface(
                            shape = connectedItemShape(idx, domains.size),
                            color = ModkeyDesign.Colors.surfaceContainerHigh,
                            border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = if (dom.active) ModkeyDesign.Colors.success else ModkeyDesign.Colors.warning,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dom.domain,
                                            style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                    }
                                    StatusBadge(
                                        text = if (dom.active) "Активен" else "Пауза",
                                        color = if (dom.active) ModkeyDesign.Colors.success else ModkeyDesign.Colors.warning
                                    )
                                }

                                val pubUrl = dom.publicUrl ?: "https://${dom.domain}/connect"
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pubUrl,
                                    style = ModkeyDesign.Typography.bodySmall(),
                                    fontFamily = FontFamily.Monospace,
                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    ModkeyIconButton(
                                        onClick = {
                                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cb.setPrimaryClip(ClipData.newPlainText("Connect URL", pubUrl))
                                            Toast.makeText(context, "URL скопирован", Toast.LENGTH_SHORT).show()
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Копировать URL",
                                            tint = ModkeyDesign.Colors.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    ModkeyIconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            scope.launch {
                                                try {
                                                    apiClient.editDomain(apiKey, software.id, dom.id, active = !dom.active)
                                                    loadDomains()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = if (dom.active) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Пауза/Пуск",
                                            tint = if (dom.active) ModkeyDesign.Colors.warning else ModkeyDesign.Colors.success,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    ModkeyIconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            scope.launch {
                                                try {
                                                    apiClient.deleteDomain(apiKey, software.id, dom.id)
                                                    Toast.makeText(context, "Домен удален", Toast.LENGTH_SHORT).show()
                                                    loadDomains()
                                                    onDomainsChanged()
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
                }
            }
        }
    }
}
