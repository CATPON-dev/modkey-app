package com.modkey.app.ui.connect

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.modkey.app.core.model.OnlineFileItem
import com.modkey.app.ui.components.CircularLoader
import com.modkey.app.ui.components.EmptyState
import com.modkey.app.ui.components.LoadingBlock
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyDialog
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.ModkeyIconButton
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.connectedItemShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun readFileFromUri(context: Context, uri: Uri): Pair<String, ByteArray>? {
    val resolver = context.contentResolver
    var fileName = "upload.bin"
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) fileName = cursor.getString(idx) ?: fileName
        }
    }
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return fileName to bytes
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
fun ConnectFilesTab(
    apiKey: String,
    softId: Int?
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient

    var files by remember { mutableStateOf<List<OnlineFileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }

    var renamingFile by remember { mutableStateOf<OnlineFileItem?>(null) }
    var replacingFileTarget by remember { mutableStateOf<OnlineFileItem?>(null) }

    fun loadFiles() {
        if (softId == null) {
            isLoading = false
            return
        }
        isLoading = true
        scope.launch {
            try {
                files = apiClient.getFiles(apiKey, softId)
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Ошибка загрузки файлов", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(softId) {
        loadFiles()
    }

    val uploadPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && softId != null) {
            isUploading = true
            scope.launch {
                try {
                    val fileData = withContext(Dispatchers.IO) { readFileFromUri(context, uri) }
                    if (fileData != null) {
                        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        apiClient.uploadFiles(apiKey, softId, fileData.first, fileData.second, mime)
                        Toast.makeText(context, "Файл ${fileData.first} загружен", Toast.LENGTH_SHORT).show()
                        loadFiles()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, e.message ?: "Ошибка загрузки файла", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    val replacePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = replacingFileTarget
        replacingFileTarget = null
        if (uri != null && softId != null && target != null) {
            isUploading = true
            scope.launch {
                try {
                    val fileData = withContext(Dispatchers.IO) { readFileFromUri(context, uri) }
                    if (fileData != null) {
                        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                        apiClient.replaceFile(apiKey, softId, target.id, fileData.first, fileData.second, mime)
                        Toast.makeText(context, "Файл заменён", Toast.LENGTH_SHORT).show()
                        loadFiles()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, e.message ?: "Ошибка замены файла", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                }
            }
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModkeyButton(
            onClick = { uploadPicker.launch(arrayOf("*/*")) },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isUploading) {
                CircularLoader(color = ModkeyDesign.Colors.onPrimary, size = 18.dp)
            } else {
                ModkeyIcon(Icons.Filled.CloudUpload, null, tint = ModkeyDesign.Colors.onPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Загрузить файл (Online Lib)", style = ModkeyDesign.Typography.labelLarge(), color = ModkeyDesign.Colors.onPrimary)
            }
        }

        if (isLoading) {
            LoadingBlock(message = "Загрузка списка файлов...")
        } else if (files.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.InsertDriveFile,
                title = "Файлов нет",
                message = "Загрузите библиотеки или конфиги для Online Lib"
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                files.forEachIndexed { index, file ->
                    Surface(
                        shape = connectedItemShape(index, files.size),
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
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = file.displayName,
                                    style = ModkeyDesign.Typography.bodyMedium().copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = buildString {
                                        append(formatFileSize(file.size))
                                        if (!file.updatedAt.isNullOrBlank()) append(" • ${file.updatedAt}")
                                    },
                                    style = ModkeyDesign.Typography.labelSmall(),
                                    color = ModkeyDesign.Colors.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ModkeyIconButton(
                                    onClick = {
                                        replacingFileTarget = file
                                        replacePicker.launch(arrayOf("*/*"))
                                    }
                                ) {
                                    ModkeyIcon(Icons.Filled.SwapHoriz, "Заменить", tint = ModkeyDesign.Colors.primary, modifier = Modifier.size(18.dp))
                                }
                                ModkeyIconButton(
                                    onClick = { renamingFile = file }
                                ) {
                                    ModkeyIcon(Icons.Filled.DriveFileRenameOutline, "Переименовать", tint = ModkeyDesign.Colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                                ModkeyIconButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        scope.launch {
                                            try {
                                                apiClient.deleteFile(apiKey, softId, file.id)
                                                Toast.makeText(context, "Файл удалён", Toast.LENGTH_SHORT).show()
                                                loadFiles()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, e.message ?: "Ошибка удаления", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    ModkeyIcon(Icons.Filled.Delete, "Удалить", tint = ModkeyDesign.Colors.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (renamingFile != null) {
        val target = renamingFile!!
        var newName by remember(target) { mutableStateOf(target.displayName) }

        ModkeyDialog(
            title = "Переименовать файл",
            onDismiss = { renamingFile = null },
            confirmLabel = "Сохранить",
            onConfirm = {
                if (newName.isBlank()) return@ModkeyDialog
                scope.launch {
                    try {
                        apiClient.renameFile(apiKey, softId, target.id, newName)
                        renamingFile = null
                        Toast.makeText(context, "Файл переименован", Toast.LENGTH_SHORT).show()
                        loadFiles()
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            ModkeyOutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = "Новое имя файла"
            )
        }
    }
}
