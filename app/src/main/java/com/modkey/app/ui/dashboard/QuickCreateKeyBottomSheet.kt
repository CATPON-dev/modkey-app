package com.modkey.app.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Share
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
import com.modkey.app.ui.components.CircularLoader
import com.modkey.app.ui.components.ModkeyBottomSheet
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyCheckbox
import com.modkey.app.ui.components.ModkeyDialog
import com.modkey.app.ui.components.ModkeyFilterChip
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.ModkeyIconButton
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.ModkeyTextButton
import com.modkey.app.ui.components.SheetHeader
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.modkeyPress
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
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
                title = "Быстрая выдача ключей",
                subtitle = "Софт: ${software.name} (#${software.id})",
                onClose = onDismiss,
                leading = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ModkeyDesign.Colors.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ModkeyIcon(
                            imageVector = Icons.Filled.ElectricBolt,
                            contentDescription = null,
                            tint = ModkeyDesign.Colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            if (createdResult == null) {
                // 1. Saved Key Templates (1-Click Presets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Шаблоны в 1 клик",
                        style = ModkeyDesign.Typography.labelLarge(),
                        color = ModkeyDesign.Colors.onSurface
                    )
                    ModkeyTextButton(
                        onClick = { showCreateTemplateDialog = true },
                        label = "Сохранить шаблон",
                        color = ModkeyDesign.Colors.primary
                    )
                }

                if (templates.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        templates.forEach { tpl ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ModkeyDesign.Colors.surfaceContainerHighest,
                                border = BorderStroke(1.dp, ModkeyDesign.Colors.primary.copy(alpha = 0.4f)),
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
                                    ModkeyIcon(
                                        imageVector = Icons.Filled.ElectricBolt,
                                        contentDescription = null,
                                        tint = ModkeyDesign.Colors.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${tpl.name} (${tpl.duration}д • ${tpl.maxDevices} устр.)",
                                        style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.SemiBold),
                                        color = ModkeyDesign.Colors.onSurface
                                    )
                                    ModkeyIconButton(
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
                                        size = 26.dp,
                                        iconSize = 14.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Удалить шаблон",
                                            tint = ModkeyDesign.Colors.onSurfaceVariant,
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
                        style = ModkeyDesign.Typography.bodySmall(),
                        color = ModkeyDesign.Colors.onSurfaceVariant
                    )
                }

                // 2. Preset Duration
                Text(
                    text = "Длительность лицензии",
                    style = ModkeyDesign.Typography.labelLarge(),
                    color = ModkeyDesign.Colors.onSurface
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durationPresets.forEachIndexed { index, (label, _) ->
                        ModkeyFilterChip(
                            selected = selectedDurationIndex == index,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                selectedDurationIndex = index
                            },
                            label = label
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
                            style = ModkeyDesign.Typography.labelLarge(),
                            color = ModkeyDesign.Colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            countPresets.forEach { cnt ->
                                ModkeyFilterChip(
                                    selected = keyCount == cnt,
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        keyCount = cnt
                                    },
                                    label = "$cnt шт"
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Лимит устройств (HWID слотов на ключ)",
                        style = ModkeyDesign.Typography.labelLarge(),
                        color = ModkeyDesign.Colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        devicePresets.forEach { dev ->
                            ModkeyFilterChip(
                                selected = maxDevices == dev,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    maxDevices = dev
                                },
                                label = "$dev устр."
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
                            ModkeyFilterChip(
                                selected = selectedKeyType == type,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    selectedKeyType = type
                                },
                                label = type
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.modkeyPress { isSingleUse = !isSingleUse }
                    ) {
                        ModkeyCheckbox(
                            checked = isSingleUse,
                            onCheckedChange = { isSingleUse = it },
                            enabled = false // handled by row press to avoid double toggling
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Одноразовый",
                            style = ModkeyDesign.Typography.bodySmall()
                        )
                    }
                }

                // 5. Game name field
                ModkeyOutlinedTextField(
                    value = gameName,
                    onValueChange = { gameName = it },
                    label = "Идентификатор игры / Game ID",
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = ModkeyDesign.Typography.bodySmall(),
                        color = ModkeyDesign.Colors.error
                    )
                }

                ModkeyButton(
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
                    minHeight = 52.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCreating) {
                        CircularLoader(
                            modifier = Modifier.size(20.dp),
                            color = ModkeyDesign.Colors.onPrimary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Генерация...",
                            style = ModkeyDesign.Typography.labelLarge(),
                            color = ModkeyDesign.Colors.onPrimary
                        )
                    } else {
                        ModkeyIcon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = ModkeyDesign.Colors.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (keyCount > 1) "Сгенерировать ключи ($keyCount шт.)" else "Сгенерировать ключ",
                            style = ModkeyDesign.Typography.labelLarge(),
                            color = ModkeyDesign.Colors.onPrimary
                        )
                    }
                }
            } else {
                // Success View
                val allKeys = createdResult?.items?.map { it.key }.orEmpty()
                val joinedKeys = allKeys.joinToString("\n")
                val durationLabel = durationPresets[selectedDurationIndex].first

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ModkeyDesign.Colors.surfaceContainerHighest,
                    border = BorderStroke(1.5.dp, ModkeyDesign.Colors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (allKeys.size > 1) "Создано ключей: ${allKeys.size} шт." else "Ключ успешно создан!",
                            style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.Bold),
                            color = ModkeyDesign.Colors.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ModkeyDesign.Colors.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = joinedKeys,
                                style = ModkeyDesign.Typography.bodyMedium().copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ModkeyDesign.Colors.primary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                        Text(
                            text = "Срок: $durationLabel • Устройств: $maxDevices • Тип: $selectedKeyType • Игра: $gameName",
                            style = ModkeyDesign.Typography.bodySmall(),
                            color = ModkeyDesign.Colors.onSurfaceVariant
                        )
                    }
                }

                // Copy & Share buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModkeyButton(
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
                        variant = com.modkey.app.ui.components.ButtonVariant.Secondary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        ModkeyIcon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = ModkeyDesign.Colors.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (allKeys.size > 1) "Копировать все" else "Скопировать",
                            style = ModkeyDesign.Typography.labelLarge(),
                            color = ModkeyDesign.Colors.onSurface
                        )
                    }

                    ModkeyButton(
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
                        ModkeyIcon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = ModkeyDesign.Colors.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Поделиться",
                            style = ModkeyDesign.Typography.labelLarge(),
                            color = ModkeyDesign.Colors.onPrimary
                        )
                    }
                }

                ModkeyTextButton(
                    onClick = { createdResult = null },
                    label = "Создать ещё ключи",
                    color = ModkeyDesign.Colors.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showCreateTemplateDialog) {
        ModkeyDialog(
            title = "Сохранить шаблон генерации",
            onDismiss = { showCreateTemplateDialog = false },
            confirmLabel = "Сохранить",
            onConfirm = {
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Шаблон сохранит выбранную игру ($gameName), длительность (${durationPresets[selectedDurationIndex].first}), лимит устройств ($maxDevices) и тип ($selectedKeyType).",
                    style = ModkeyDesign.Typography.bodySmall(),
                    color = ModkeyDesign.Colors.onSurfaceVariant
                )
                ModkeyOutlinedTextField(
                    value = newTemplateName,
                    onValueChange = { newTemplateName = it },
                    label = "Название шаблона",
                    placeholder = "Например: Недельный VIP"
                )
            }
        }
    }
}
