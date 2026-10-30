package com.modkey.app.ui.users

import android.view.HapticFeedbackConstants
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.core.model.UserItem
import com.modkey.app.ui.components.EmptyState
import com.modkey.app.ui.components.LoadingBlock
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.theme.connectedItemShape
import kotlinx.coroutines.launch

private enum class UserFilter(val label: String) {
    ALL("Все"),
    RESELLERS("Реселлеры"),
    ADMINS("Админы"),
    BLOCKED("Заблокированные")
}

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showCreateUserDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "Добавить реселлера")
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
            SectionHeader(
                title = "Реселлеры и клиенты (${usersList.size})",
                subtitle = "Управление балансом, ролями, блокировкой и сессиями",
                actionLabel = "Обновить",
                onActionClick = { loadUsers() }
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск по логину реселлера...") },
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

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UserFilter.entries.forEach { filter ->
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
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.username,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (!user.fullname.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${user.fullname})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            StatusBadge(
                                                text = if (user.level == 3) "Реселлер" else "Админ",
                                                color = if (user.level == 3) Color(0xFFAB47BC) else Color(0xFF03A9F4)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            StatusBadge(
                                                text = if (isActive) "Активен" else "Заблокирован",
                                                color = if (isActive) Color(0xFF4CAF50) else Color(0xFFE53935)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Баланс: ${user.saldo} ₽",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
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
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    IconButton(onClick = { editingUser = user }) {
                                        Icon(
                                            Icons.Filled.AccountBalanceWallet,
                                            contentDescription = "Баланс и настройки",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Block / Unblock
                                    IconButton(
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
                                        }
                                    ) {
                                        Icon(
                                            if (isActive) Icons.Filled.Block else Icons.Filled.CheckCircle,
                                            contentDescription = "Блок/Разблок",
                                            tint = if (isActive) Color(0xFFFFB300) else Color(0xFF4CAF50)
                                        )
                                    }

                                    // Kick sessions button
                                    IconButton(
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
                                        }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "Сброс сессий",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Delete User
                                    IconButton(
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
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Удалить",
                                            tint = MaterialTheme.colorScheme.error
                                        )
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

    // Create User Dialog (create-user)
    if (showCreateUserDialog) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var roleLevel by remember { mutableIntStateOf(3) } // 3 Reseller, 2 Admin
        var saldoInput by remember { mutableStateOf("0") }
        var expDaysInput by remember { mutableStateOf("30") }
        var fullnameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateUserDialog = false },
            title = { Text("Создать аккаунт реселлера") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = roleLevel == 3,
                            onClick = { roleLevel = 3 },
                            label = { Text("Реселлер") }
                        )
                        FilterChip(
                            selected = roleLevel == 2,
                            onClick = { roleLevel = 2 },
                            label = { Text("Админ") }
                        )
                    }
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Логин (4-25 лат. символов)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль (от 8 символов)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = saldoInput,
                            onValueChange = { saldoInput = it },
                            label = { Text("Баланс (₽)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = expDaysInput,
                            onValueChange = { expDaysInput = it },
                            label = { Text("Срок (дней)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = fullnameInput,
                        onValueChange = { fullnameInput = it },
                        label = { Text("Имя / Заметка (опционально)") },
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
                    },
                    enabled = username.length >= 4 && password.length >= 8
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateUserDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Edit User Dialog (edit-user)
    if (editingUser != null) {
        val target = editingUser!!
        var saldoInput by remember(target) { mutableStateOf(target.saldo.toString()) }
        var roleLevel by remember(target) { mutableIntStateOf(target.level) }
        var fullnameInput by remember(target) { mutableStateOf(target.fullname.orEmpty()) }
        var telegramInput by remember(target) { mutableStateOf(target.telegram.orEmpty()) }

        AlertDialog(
            onDismissRequest = { editingUser = null },
            title = { Text("Управление: ${target.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = roleLevel == 3,
                            onClick = { roleLevel = 3 },
                            label = { Text("Реселлер") }
                        )
                        FilterChip(
                            selected = roleLevel == 2,
                            onClick = { roleLevel = 2 },
                            label = { Text("Админ") }
                        )
                    }

                    OutlinedTextField(
                        value = saldoInput,
                        onValueChange = { saldoInput = it },
                        label = { Text("Баланс реселлера (₽)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Quick +Balance presets
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(100, 500, 1000, 5000).forEach { bonus ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cur = saldoInput.toIntOrNull() ?: 0
                                    saldoInput = (cur + bonus).toString()
                                },
                                label = { Text("+$bonus") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fullnameInput,
                        onValueChange = { fullnameInput = it },
                        label = { Text("Имя / Заметка") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = telegramInput,
                        onValueChange = { telegramInput = it },
                        label = { Text("Telegram (@username)") },
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
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}
