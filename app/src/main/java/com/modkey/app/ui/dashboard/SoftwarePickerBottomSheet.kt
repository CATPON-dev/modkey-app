package com.modkey.app.ui.dashboard

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.core.model.SoftwareItem
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyBottomSheet
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.SheetHeader
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.connectedItemShape
import com.modkey.app.ui.theme.modkeyPress

@Composable
fun SoftwarePickerBottomSheet(
    softwareList: List<SoftwareItem>,
    selectedSoftwareId: Int?,
    onSoftwareSelected: (SoftwareItem) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current

    ModkeyBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            SheetHeader(
                title = "Выберите софт",
                onClose = onDismiss
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(400.dp)
            ) {
                itemsIndexed(softwareList) { index, soft ->
                    val isSelected = soft.id == selectedSoftwareId
                    Surface(
                        shape = connectedItemShape(index, softwareList.size),
                        color = if (isSelected) ModkeyDesign.Colors.surfaceContainerHighest
                        else ModkeyDesign.Colors.surfaceContainerHigh,
                        border = if (isSelected) BorderStroke(2.dp, ModkeyDesign.Colors.primary)
                        else BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .modkeyPress {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onSoftwareSelected(soft)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isSelected) ModkeyDesign.Colors.primary
                                        else ModkeyDesign.Colors.surfaceContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                ModkeyIcon(
                                    imageVector = Icons.Filled.Computer,
                                    contentDescription = null,
                                    tint = if (isSelected) ModkeyDesign.Colors.onPrimary
                                    else ModkeyDesign.Colors.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = soft.name,
                                    style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold),
                                    color = ModkeyDesign.Colors.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "ID: ${soft.id} • Формат: ${soft.keyFormat ?: "по умолчанию"}",
                                    style = ModkeyDesign.Typography.bodySmall(),
                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                )
                            }
                            StatusBadge(
                                text = if (soft.status == 1) "В сети" else "Офлайн",
                                color = if (soft.status == 1) ModkeyDesign.Colors.success else ModkeyDesign.Colors.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSwitcherBottomSheet(
    accounts: List<SavedAccount>,
    activeAccountId: String?,
    onAccountSelected: (SavedAccount) -> Unit,
    onAddNewAccount: () -> Unit,
    onLogoutCurrent: () -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current

    ModkeyBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetHeader(
                title = "Профили и аккаунты",
                onClose = onDismiss
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(accounts) { index, account ->
                    val isSelected = account.id == activeAccountId
                    Surface(
                        shape = connectedItemShape(index, accounts.size),
                        color = if (isSelected) ModkeyDesign.Colors.surfaceContainerHighest
                        else ModkeyDesign.Colors.surfaceContainerHigh,
                        border = if (isSelected) BorderStroke(2.dp, ModkeyDesign.Colors.primary)
                        else BorderStroke(1.dp, ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .modkeyPress {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onAccountSelected(account)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isSelected) ModkeyDesign.Colors.primary
                                        else ModkeyDesign.Colors.surfaceContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                ModkeyIcon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = if (isSelected) ModkeyDesign.Colors.onPrimary
                                    else ModkeyDesign.Colors.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.label,
                                    style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold),
                                    color = ModkeyDesign.Colors.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Ключ: ••••••••${account.apiKey.takeLast(6)}",
                                    style = ModkeyDesign.Typography.bodySmall(),
                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(ModkeyDesign.Colors.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ModkeyIcon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Активен",
                                        tint = ModkeyDesign.Colors.onPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModkeyButton(
                    onClick = {
                        onDismiss()
                        onAddNewAccount()
                    },
                    variant = com.modkey.app.ui.components.ButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    ModkeyIcon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = ModkeyDesign.Colors.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Добавить",
                        style = ModkeyDesign.Typography.labelLarge(),
                        color = ModkeyDesign.Colors.onSurface
                    )
                }

                ModkeyButton(
                    onClick = {
                        onDismiss()
                        onLogoutCurrent()
                    },
                    variant = com.modkey.app.ui.components.ButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    ModkeyIcon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = ModkeyDesign.Colors.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Выйти",
                        style = ModkeyDesign.Typography.labelLarge(),
                        color = ModkeyDesign.Colors.error
                    )
                }
            }
        }
    }
}
