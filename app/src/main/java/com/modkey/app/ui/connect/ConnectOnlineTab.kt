package com.modkey.app.ui.connect

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.ui.components.CircularLoader
import com.modkey.app.ui.components.LoadingBlock
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.ModkeySwitch
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import kotlinx.coroutines.launch

@Composable
fun ConnectOnlineTab(
    apiKey: String,
    softId: Int?
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var enabled by remember { mutableStateOf(false) }
    var onlineCount by remember { mutableStateOf(0) }
    var heartbeatInterval by remember { mutableStateOf("30") }
    var timeoutSeconds by remember { mutableStateOf("90") }
    var maxOnline by remember { mutableStateOf("0") }
    var showCount by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(softId) {
        if (softId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        try {
            val cfg = apiClient.getOnline(apiKey, softId)
            enabled = cfg.enabled
            onlineCount = cfg.onlineCount
            heartbeatInterval = cfg.heartbeatInterval.toString()
            timeoutSeconds = cfg.timeoutSeconds.toString()
            maxOnline = cfg.maxOnline.toString()
            showCount = cfg.showCount
            message = cfg.message.orEmpty()
        } catch (e: Exception) {
            Toast.makeText(context, e.message ?: "Ошибка загрузки Online System", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
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
        LoadingBlock(message = "Загрузка Online System...")
        return
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ModkeyDesign.Colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Online System",
                    style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                )
                StatusBadge(
                    text = "Онлайн: $onlineCount",
                    color = if (enabled) ModkeyDesign.Colors.success else ModkeyDesign.Colors.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Мониторинг онлайна включен", style = ModkeyDesign.Typography.bodyMedium())
                ModkeySwitch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Показывать счётчик клиентам", style = ModkeyDesign.Typography.bodyMedium())
                ModkeySwitch(checked = showCount, onCheckedChange = { showCount = it })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModkeyOutlinedTextField(
                    value = heartbeatInterval,
                    onValueChange = { heartbeatInterval = it },
                    label = "Heartbeat (сек)",
                    modifier = Modifier.weight(1f)
                )
                ModkeyOutlinedTextField(
                    value = timeoutSeconds,
                    onValueChange = { timeoutSeconds = it },
                    label = "Таймаут (сек)",
                    modifier = Modifier.weight(1f)
                )
            }

            ModkeyOutlinedTextField(
                value = maxOnline,
                onValueChange = { maxOnline = it },
                label = "Лимит онлайна (0 = безлимит)",
                modifier = Modifier.fillMaxWidth()
            )

            ModkeyOutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = "Сообщение при превышении лимита",
                modifier = Modifier.fillMaxWidth()
            )

            ModkeyButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    isSaving = true
                    scope.launch {
                        try {
                            val updated = apiClient.editOnline(
                                apiKey = apiKey,
                                softId = softId,
                                enabled = enabled,
                                heartbeatInterval = heartbeatInterval.toIntOrNull(),
                                timeoutSeconds = timeoutSeconds.toIntOrNull(),
                                maxOnline = maxOnline.toIntOrNull(),
                                showCount = showCount,
                                message = message.ifBlank { null }
                            )
                            onlineCount = updated.onlineCount
                            Toast.makeText(context, "Online System сохранён", Toast.LENGTH_SHORT).show()
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
                    Text("Сохранить Online System", style = ModkeyDesign.Typography.labelLarge(), color = ModkeyDesign.Colors.onPrimary)
                }
            }
        }
    }
}
