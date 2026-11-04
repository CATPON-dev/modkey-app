package com.modkey.app.ui.users

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.core.model.UserItem
import com.modkey.app.ui.components.EmptyState
import com.modkey.app.ui.components.LoadingBlock
import com.modkey.app.ui.components.ModkeyFab
import com.modkey.app.ui.components.ModkeyFilterChip
import com.modkey.app.ui.components.ModkeyDialog
import com.modkey.app.ui.components.ModkeyIconButton
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.connectedItemShape
import com.modkey.app.ui.theme.modkeyPress
import kotlinx.coroutines.launch

private enum class UserFilter(val label: String) {
    ALL("Все"),
    RESELLERS("Реселлеры"),
    ADMINS("Админы"),
    BLOCKED("Заблокированные")
}

@Composable
fun UsersScreen(
    account: SavedAccount
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(UserFilter.ALL) }
    var usersList by remember { mutableStateOf<List<UserItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showCreateUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserItem?>(null) }

    fun loadUsers() {
        isLoading = true
        scope.launch {
            try {
                val levelParam = when (selectedFilter) {
                    UserFilter.RESELLERS -> 3
                    UserFilter.ADMINS -> 2
                    else -> null
                }
                val statusParam = when (selectedFilter) {
                    UserFilter.BLOCKED -> 0
                    else -> null
                }
                usersList = apiClient.getUsers(
                    apiKey = account.apiKey,
                    query = searchQuery.ifBlank { null },
                    level = levelParam,
                    status = statusParam
                )
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(account.id, searchQuery, selectedFilter) {
        loadUsers()
    }

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
            SectionHeader(
                title = "Реселлеры и клиенты (${usersList.size})",
                subtitle = "Управление балансом, ролями, блокировкой и сессиями",
                actionLabel = "Обновить",
                onActionClick = { loadUsers() }
            )

            // Search Bar
            ModkeyOutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Поиск по логину реселлера...",
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

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UserFilter.entries.forEach { filter ->
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
                LoadingBlock(message = "Загрузка клиентских аккаунтов...")
            } else if (usersList.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.People,
                    title = "Нет пользователей",
                    message = "Нажмите кнопку «+» внизу справа, чтобы создать аккаунт реселлера или админа."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(usersList) { index, user ->
                        val isActive = user.status == 1

                        Surface(
                            shape = connectedItemShape(index, usersList.size),
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clipCircleShape()
                                            .background(
                                                if (isActive) ModkeyDesign.Colors.primaryContainer
                                                else ModkeyDesign.Colors.errorContainer,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = if (isActive) ModkeyDesign.Colors.primary else ModkeyDesign.Colors.error,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.username,
                                                style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                                            )
                                            if (!user.fullname.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${user.fullname})",
                                                    style = ModkeyDesign.Typography.bodySmall(),
                                                    color = ModkeyDesign.Colors.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            StatusBadge(
                                                text = if (user.level == 3) "Реселлер" else "Админ",
                                                color = if (user.level == 3) ModkeyDesign.Colors.warning else ModkeyDesign.Colors.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            StatusBadge(
                                                text = if (isActive) "Активен" else "Заблокирован",
                                                color = if (isActive) ModkeyDesign.Colors.success else ModkeyDesign.Colors.error
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Баланс: ${user.saldo} ₽",
                                                style = ModkeyDesign.Typography.bodySmall().copy(fontWeight = FontWeight.ExtraBold),
                                                color = ModkeyDesign.Colors.primary
                                            )
                                        }
                                    }
                                }

                                if (!user.expirationDate.isNullOrBlank() || !user.telegram.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = buildString {
                                            if (!user.expirationDate.isNullOrBlank()) append("До: ${user.expirationDate}")
                                            if (!user.telegram.isNullOrBlank()) {
                                                if (isNotEmpty()) append(" • ")
                                                append("TG: @${user.telegram.removePrefix("@")}")
                                            }
                                        },
                                        style = ModkeyDesign.Typography.labelSmall(),
                                        color = ModkeyDesign.Colors.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Action Bar: Balance/Edit, Block/Unblock, Kick Sessions, Delete
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Edit Balance & Info
                                    ModkeyIconButton(
                                        onClick = { editingUser = user },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.AccountBalanceWallet,
                                            contentDescription = "Баланс и настройки",
                                            tint = ModkeyDesign.Colors.primary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Block / Unblock
                                    ModkeyIconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            scope.launch {
                                                try {
                                                    if (isActive) {
                                                        apiClient.blockUser(account.apiKey, user.id)
                                                        Toast.makeText(context, "${user.username} заблокирован", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        apiClient.unblockUser(account.apiKey, user.id)
                                                        Toast.makeText(context, "${user.username} разблокирован", Toast.LENGTH_SHORT).show()
                                                    }
                                                    loadUsers()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = if (isActive) Icons.Filled.Block else Icons.Filled.CheckCircle,
                                            contentDescription = "Блок/Разблок",
                                            tint = if (isActive) ModkeyDesign.Colors.warning else ModkeyDesign.Colors.success,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Kick sessions button
                                    ModkeyIconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            scope.launch {
                                                try {
                                                    apiClient.kickUserSessions(account.apiKey, user.id)
                                                    Toast.makeText(context, "Сессии сброшены", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        size = 34.dp,
                                        iconSize = 17.dp
                                    ) {
                                        ModkeyIcon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "Сброс сессий",
                                            tint = ModkeyDesign.Colors.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    // Delete User
                                    ModkeyIconButton(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                            scope.launch {
                                                try {
                                                    apiClient.deleteUser(account.apiKey, user.id)
                                                    Toast.makeText(context, "Аккаунт ${user.username} удалён", Toast.LENGTH_SHORT).show()
                                                    loadUsers()
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

        // Floating Create User button
        if (!showCreateUserDialog) {
            ModkeyFab(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showCreateUserDialog = true
                },
                icon = Icons.Filled.PersonAdd,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 96.dp)
            )
        }
    }

    // Create User Dialog (create-user)
    if (showCreateUserDialog) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var roleLevel by remember { mutableIntStateOf(3) } // 3 Reseller, 2 Admin
        var saldoInput by remember { mutableStateOf("0") }
        var expDaysInput by remember { mutableStateOf("30") }
        var fullnameInput by remember { mutableStateOf("") }

        ModkeyDialog(
            title = "Создать аккаунт реселлера",
            onDismiss = { showCreateUserDialog = false },
            confirmLabel = "Создать",
            onConfirm = {
                scope.launch {
                    try {
                        apiClient.createUser(
                            apiKey = account.apiKey,
                            username = username.trim(),
                            password = password,
                            level = roleLevel,
                            saldo = saldoInput.toIntOrNull() ?: 0,
                            expirationDays = expDaysInput.toIntOrNull() ?: 30,
                            fullname = fullnameInput.trim()
                        )
                        showCreateUserDialog = false
                        Toast.makeText(context, "Реселлер $username создан!", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Ошибка создания аккаунта", Toast.LENGTH_SHORT).show()
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModkeyFilterChip(
                        selected = roleLevel == 3,
                        onClick = { roleLevel = 3 },
                        label = "Реселлер"
                    )
                    ModkeyFilterChip(
                        selected = roleLevel == 2,
                        onClick = { roleLevel = 2 },
                        label = "Админ"
                    )
                }
                ModkeyOutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Логин (4-25 лат. символов)"
                )
                ModkeyOutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Пароль (от 8 символов)"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModkeyOutlinedTextField(
                        value = saldoInput,
                        onValueChange = { saldoInput = it },
                        label = "Баланс (₽)",
                        modifier = Modifier.weight(1f)
                    )
                    ModkeyOutlinedTextField(
                        value = expDaysInput,
                        onValueChange = { expDaysInput = it },
                        label = "Срок (дней)",
                        modifier = Modifier.weight(1f)
                    )
                }
                ModkeyOutlinedTextField(
                    value = fullnameInput,
                    onValueChange = { fullnameInput = it },
                    label = "Имя / Заметка (опционально)"
                )
            }
        }
    }

    // Edit User Dialog (edit-user)
    if (editingUser != null) {
        val target = editingUser!!
        var saldoInput by remember(target) { mutableStateOf(target.saldo.toString()) }
        var roleLevel by remember(target) { mutableIntStateOf(target.level) }
        var fullnameInput by remember(target) { mutableStateOf(target.fullname.orEmpty()) }
        var telegramInput by remember(target) { mutableStateOf(target.telegram.orEmpty()) }

        ModkeyDialog(
            title = "Управление: ${target.username}",
            onDismiss = { editingUser = null },
            confirmLabel = "Сохранить",
            onConfirm = {
                scope.launch {
                    try {
                        apiClient.editUser(
                            apiKey = account.apiKey,
                            userId = target.id,
                            saldo = saldoInput.toIntOrNull() ?: target.saldo,
                            level = roleLevel,
                            fullname = fullnameInput.trim(),
                            telegram = telegramInput.trim().removePrefix("@")
                        )
                        editingUser = null
                        Toast.makeText(context, "Данные ${target.username} обновлены!", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Ошибка обновления", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModkeyFilterChip(
                        selected = roleLevel == 3,
                        onClick = { roleLevel = 3 },
                        label = "Реселлер"
                    )
                    ModkeyFilterChip(
                        selected = roleLevel == 2,
                        onClick = { roleLevel = 2 },
                        label = "Админ"
                    )
                }

                ModkeyOutlinedTextField(
                    value = saldoInput,
                    onValueChange = { saldoInput = it },
                    label = "Баланс реселлера (₽)"
                )

                // Quick +Balance presets
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(100, 500, 1000, 5000).forEach { bonus ->
                        ModkeyFilterChip(
                            selected = false,
                            onClick = {
                                val cur = saldoInput.toIntOrNull() ?: 0
                                saldoInput = (cur + bonus).toString()
                            },
                            label = "+$bonus"
                        )
                    }
                }

                ModkeyOutlinedTextField(
                    value = fullnameInput,
                    onValueChange = { fullnameInput = it },
                    label = "Имя / Заметка"
                )

                ModkeyOutlinedTextField(
                    value = telegramInput,
                    onValueChange = { telegramInput = it },
                    label = "Telegram (@username)"
                )
            }
        }
    }
}

private fun Modifier.clipCircleShape(): Modifier = this.clip(CircleShape)
