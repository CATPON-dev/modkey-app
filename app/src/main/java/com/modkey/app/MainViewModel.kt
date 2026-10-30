package com.modkey.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.core.storage.AccountManager
import com.modkey.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainDestination(val label: String) {
    DASHBOARD("Дашборд"),
    KEYS("Ключи"),
    USERS("Реселлеры"),
    CONNECT("Connect"),
    SETTINGS("Настройки")
}

class MainViewModel : ViewModel() {

    private val accountManager: AccountManager = ModkeyApp.instance.accountManager

    val accounts: StateFlow<List<SavedAccount>> = accountManager.accountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAccount: StateFlow<SavedAccount?> = accountManager.activeAccountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isFirstLaunchAuthorDismissedLocal = MutableStateFlow(false)
    val isFirstLaunchAuthorShown: StateFlow<Boolean> = accountManager.isFirstLaunchAuthorShownFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showWelcomeDialog: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        accountManager.isFirstLaunchAuthorShownFlow,
        _isFirstLaunchAuthorDismissedLocal
    ) { hasShownInPrefs, dismissedLocally ->
        !hasShownInPrefs && !dismissedLocally
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun dismissFirstLaunchAuthorCard() {
        _isFirstLaunchAuthorDismissedLocal.value = true
        viewModelScope.launch {
            accountManager.setFirstLaunchAuthorShown()
        }
    }

    private val _currentDestination = MutableStateFlow(MainDestination.DASHBOARD)
    val currentDestination: StateFlow<MainDestination> = _currentDestination.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun selectDestination(destination: MainDestination) {
        _currentDestination.value = destination
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun switchAccount(account: SavedAccount) {
        viewModelScope.launch {
            accountManager.selectAccount(account.id)
            _currentDestination.value = MainDestination.DASHBOARD
        }
    }

    fun logout() {
        viewModelScope.launch {
            val active = activeAccount.value
            if (active != null) {
                accountManager.removeAccount(active.id)
            }
        }
    }
}
