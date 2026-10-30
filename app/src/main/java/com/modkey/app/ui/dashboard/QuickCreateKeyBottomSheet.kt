package com.modkey.app.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.CreatedKeysResponse
import com.modkey.app.core.model.KeyTemplateItem
import com.modkey.app.core.model.SoftwareItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickCreateKeyBottomSheet(
    apiKey: String,
    software: SoftwareItem,
    onDismiss: () -> Unit,
    onKeyCreated: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val durationPresets = listOf(
        "12 часов" to "-12",
        "1 день" to "1",
        "3 дня" to "3",
        "7 дней" to "7",
        "30 дней" to "30",
        "Навсегда" to "lifetime"
    )
    val countPresets = listOf(1, 5, 10, 25, 50)
    val devicePresets = listOf(1, 2, 3, 5, 10)

    var selectedDurationIndex by remember { mutableIntStateOf(3) } // default 7 days
    var selectedKeyType by remember { mutableStateOf("APK") }
    var maxDevices by remember { mutableIntStateOf(1) }
    var keyCount by remember { mutableIntStateOf(1) }
    var isSingleUse by remember { mutableStateOf(false) }
    var gameName by remember { mutableStateOf(software.name.lowercase().take(20)) }

    var templates by remember { mutableStateOf<List<KeyTemplateItem>>(emptyList()) }
    var showCreateTemplateDialog by remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf("") }

    var isCreating by remember { mutableStateOf(false) }
    var createdResult by remember { mutableStateOf<CreatedKeysResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadTemplates() {
        scope.launch {
            try {
                templates = apiClient.getKeyTemplates(apiKey, software.id)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(software.id) {
        loadTemplates()
    }

    fun generateWithParams(
        game: String,
        durationVal: String,
        count: Int,
        devices: Int,
        type: String,
        single: Boolean
    ) {
        isCreating = true
        errorMessage = null
        scope.launch {
            try {
                val result = apiClient.createKeys(
                    apiKey = apiKey,
                    softId = software.id,
                    game = game.ifBlank { "game" },
                    duration = durationVal,
                    count = count,
                    maxDevices = devices,
                    keyType = type,
                    single = single
                )
                createdResult = result
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onKeyCreated()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка создания ключа"
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } finally {
                isCreating = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.ElectricBolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Быстрая выдача ключей",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Софт: ${software.name} (#${software.id})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
            }

            if (createdResult == null) {
                // 1. Saved Key Templates (1-Click Presets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Шаблоны в 1 клик",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showCreateTemplateDialog = true }) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сохранить шаблон")
                    }
                }

                if (templates.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        templates.forEach { tpl ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    val durStr = if (tpl.duration >= 25000) "lifetime" else tpl.duration.toString()
                                    generateWithParams(
                                        game = tpl.game,
                                        durationVal = durStr,
                                        count = 1,
                                        devices = tpl.maxDevices,
                                        type = tpl.keyType ?: "APK",
                                        single = tpl.isSingle
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.ElectricBolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${tpl.name} (${tpl.duration}д • ${tpl.maxDevices} устр.)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    apiClient.deleteKeyTemplate(apiKey, tpl.id)
                                                    loadTemplates()
                                                    Toast.makeText(context, "Шаблон удалён", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Удалить шаблон",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Нажмите «Сохранить шаблон», чтобы создавать ключи в одно касание",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2. Preset Duration
                Text(
                    text = "Длительность лицензии",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durationPresets.forEachIndexed { index, (label, _) ->
                        FilterChip(
                            selected = selectedDurationIndex == index,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                selectedDurationIndex = index
                            },
                            label = { Text(label) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // 3. Key Count & Max Devices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Количество ключей",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            countPresets.forEach { cnt ->
                                FilterChip(
                                    selected = keyCount == cnt,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        keyCount = cnt
                                    },
                                    label = { Text("$cnt шт") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Лимит устройств (HWID слотов на ключ)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        devicePresets.forEach { dev ->
                            FilterChip(
                                selected = maxDevices == dev,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    maxDevices = dev
                                },
                                label = { Text("$dev устр.") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // 4. Key Type (APK or INJECTOR) & Single-use
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("APK", "INJECTOR").forEach { type ->
                            FilterChip(
                                selected = selectedKeyType == type,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    selectedKeyType = type
                                },
                                label = { Text(type) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isSingleUse,
                            onCheckedChange = { isSingleUse = it }
                        )
                        Text(
                            text = "Одноразовый",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // 5. Game name field
                OutlinedTextField(
                    value = gameName,
                    onValueChange = { gameName = it },
                    label = { Text("Идентификатор игры / Game ID") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        val durationVal = durationPresets[selectedDurationIndex].second
                        generateWithParams(
                            game = gameName,
                            durationVal = durationVal,
                            count = keyCount,
                            devices = maxDevices,
                            type = selectedKeyType,
                            single = isSingleUse
                        )
                    },
                    enabled = !isCreating,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Генерация...")
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (keyCount > 1) "Сгенерировать ключи ($keyCount шт.)" else "Сгенерировать ключ")
                    }
                }
            } else {
                // Success View
                val allKeys = createdResult?.items?.map { it.key }.orEmpty()
                val joinedKeys = allKeys.joinToString("\n")
                val durationLabel = durationPresets[selectedDurationIndex].first

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (allKeys.size > 1) "Создано ключей: ${allKeys.size} шт." else "Ключ успешно создан!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = joinedKeys,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                        Text(
                            text = "Срок: $durationLabel • Устройств: $maxDevices • Тип: $selectedKeyType • Игра: $gameName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Copy & Share buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Modkey License", joinedKeys)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(
                                context,
                                if (allKeys.size > 1) "Все ключи скопированы!" else "Ключ скопирован!",
                                Toast.LENGTH_SHORT
                            ).show()
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (allKeys.size > 1) "Копировать все" else "Скопировать")
                    }

                    Button(
                        onClick = {
                            val shareText = if (allKeys.size == 1) {
                                "Ваш лицензионный ключ для ${software.name}:\n🔑 ${allKeys.first()}\n⏳ Длительность: $durationLabel\n📱 Устройств: $maxDevices\nПриятного пользования!"
                            } else {
                                "Лицензионные ключи ${software.name} (${allKeys.size} шт., $durationLabel):\n\n$joinedKeys"
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Отправить клиенту (Telegram / др.)"))
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Поделиться")
                    }
                }

                TextButton(
                    onClick = { createdResult = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Создать ещё ключи")
                }
            }
        }
    }

    if (showCreateTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTemplateDialog = false },
            title = { Text("Сохранить шаблон генерации") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Шаблон сохранит выбранную игру ($gameName), длительность (${durationPresets[selectedDurationIndex].first}), лимит устройств ($maxDevices) и тип ($selectedKeyType).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = newTemplateName,
                        onValueChange = { newTemplateName = it },
                        label = { Text("Название шаблона") },
                        placeholder = { Text("Например: Недельный VIP") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rawDur = durationPresets[selectedDurationIndex].second
                        val durInt = if (rawDur == "lifetime") 25000 else (rawDur.toIntOrNull()?.coerceAtLeast(1) ?: 7)
                        scope.launch {
                            try {
                                apiClient.createKeyTemplate(
                                    apiKey = apiKey,
                                    softId = software.id,
                                    name = newTemplateName.ifBlank { "Шаблон ${durInt}д" },
                                    game = gameName.ifBlank { "game" },
                                    duration = durInt,
                                    keyType = selectedKeyType,
                                    maxDevices = maxDevices,
                                    isSingle = isSingleUse
                                )
                                showCreateTemplateDialog = false
                                newTemplateName = ""
                                loadTemplates()
                                Toast.makeText(context, "Шаблон сохранён!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Ошибка сохранения шаблона", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTemplateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
