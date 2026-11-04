package com.modkey.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.modkey.app.ui.auth.LoginScreen
import com.modkey.app.ui.connect.ConnectDebuggerScreen
import com.modkey.app.ui.dashboard.AccountSwitcherBottomSheet
import com.modkey.app.ui.dashboard.DashboardScreen
import com.modkey.app.ui.keys.KeysScreen
import com.modkey.app.ui.settings.SettingsScreen
import com.modkey.app.ui.theme.ModkeyTheme
import com.modkey.app.ui.theme.ThemeMode
import com.modkey.app.ui.theme.isDarkTheme
import com.modkey.app.ui.users.UsersScreen
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyIconButton
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.components.WelcomeAuthorDialog
import com.modkey.app.ui.components.BottomScaffold

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val activeAccount by viewModel.activeAccount.collectAsState()
            val allAccounts by viewModel.accounts.collectAsState()
            val currentDestination by viewModel.currentDestination.collectAsState()
            val showWelcomeDialog by viewModel.showWelcomeDialog.collectAsState()

            val dark = isDarkTheme(themeMode)
            val view = LocalView.current

            if (!view.isInEditMode) {
                androidx.compose.runtime.SideEffect {
                    val window = (view.context as ComponentActivity).window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
            }

            ModkeyTheme(themeMode = themeMode) {
                var forceShowLogin by remember { mutableStateOf(false) }
                var showAccountSheet by remember { mutableStateOf(false) }

                if (showWelcomeDialog) {
                    WelcomeAuthorDialog(
                        onDismiss = { viewModel.dismissFirstLaunchAuthorCard() }
                    )
                }

                if (activeAccount == null || forceShowLogin) {
                    LoginScreen(
                        savedAccounts = allAccounts,
                        onLoginSuccess = { account ->
                            forceShowLogin = false
                            viewModel.switchAccount(account)
                        }
                    )
                } else {
                    val account = activeAccount!!

                    BottomScaffold(
                        currentDestination = currentDestination,
                        onDestinationClick = { dest -> viewModel.selectDestination(dest) },
                        modifier = Modifier.fillMaxSize()
                    ) { _ ->
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AnimatedContent(
                                targetState = currentDestination,
                                transitionSpec = {
                                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                            scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                                        .togetherWith(
                                            fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                                                    scaleOut(targetScale = 0.98f)
                                        )
                                },
                                label = "screenTransition"
                            ) { dest ->
                                when (dest) {
                                    MainDestination.DASHBOARD -> DashboardScreen(
                                        account = account,
                                        allAccounts = allAccounts,
                                        onSwitchAccount = { viewModel.switchAccount(it) },
                                        onAddNewAccount = { forceShowLogin = true },
                                        onLogout = { viewModel.logout() },
                                        onNavigateToKeys = { viewModel.selectDestination(MainDestination.KEYS) }
                                    )
                                    MainDestination.KEYS -> KeysScreen(
                                        account = account,
                                        onNavigateBack = { viewModel.selectDestination(MainDestination.DASHBOARD) }
                                    )
                                    MainDestination.USERS -> UsersScreen(
                                        account = account
                                    )
                                    MainDestination.CONNECT -> ConnectDebuggerScreen(
                                        account = account
                                    )
                                    MainDestination.SETTINGS -> SettingsScreen(
                                        currentAccount = account,
                                        allAccounts = allAccounts,
                                        themeMode = themeMode,
                                        onThemeModeChange = { viewModel.setThemeMode(it) },
                                        onManageAccounts = { showAccountSheet = true },
                                        onLogout = { viewModel.logout() }
                                    )
                                }
                            }
                        }
                    }

                    if (showAccountSheet) {
                        AccountSwitcherBottomSheet(
                            accounts = allAccounts,
                            activeAccountId = account.id,
                            onAccountSelected = {
                                showAccountSheet = false
                                viewModel.switchAccount(it)
                            },
                            onAddNewAccount = {
                                showAccountSheet = false
                                forceShowLogin = true
                            },
                            onLogoutCurrent = {
                                showAccountSheet = false
                                viewModel.logout()
                            },
                            onDismiss = { showAccountSheet = false }
                        )
                    }
                }
            }
        }
    }
}
