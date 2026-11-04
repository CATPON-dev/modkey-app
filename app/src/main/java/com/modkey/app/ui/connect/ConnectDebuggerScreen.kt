package com.modkey.app.ui.connect

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.ui.components.CircularLoader
import com.modkey.app.ui.components.ModkeyButton
import com.modkey.app.ui.components.ModkeyFilterChip
import com.modkey.app.ui.components.ModkeyIcon
import com.modkey.app.ui.components.ModkeyOutlinedTextField
import com.modkey.app.ui.components.ModkeyIconButton
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.components.StatusBadge
import com.modkey.app.ui.components.Surface
import com.modkey.app.ui.components.Text
import com.modkey.app.ui.theme.ModkeyDesign
import com.modkey.app.ui.theme.NataSans
import com.modkey.app.ui.theme.modkeyPress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest

private enum class CodeLang(val label: String) {
    KOTLIN("Kotlin / Android"),
    CPP("C++ (libcurl)"),
    CSHARP("C# (.NET)"),
    PYTHON("Python"),
    CURL("cURL")
}

private fun md5Hex(input: String): String {
    val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

@Composable
fun ConnectDebuggerScreen(
    account: SavedAccount
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val apiClient = ModkeyApp.instance.apiClient

    @SuppressLint("HardwareIds")
    val androidId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tester, 1: Code Generator
    var selectedLang by remember { mutableStateOf(CodeLang.KOTLIN) }

    var softIdInput by remember { mutableStateOf("618") }
    var gameInput by remember { mutableStateOf("standoff2") }
    var userKeyInput by remember { mutableStateOf("") }
    var serialInput by remember { mutableStateOf(androidId) }
    var connectSecretInput by remember { mutableStateOf("") }
    var useTestEndpoint by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var responseResult by remember { mutableStateOf<String?>(null) }
    var responseStatus by remember { mutableStateOf<Boolean?>(null) }
    var tokenVerified by remember { mutableStateOf<Boolean?>(null) }
    var expectedTokenHex by remember { mutableStateOf<String?>(null) }
    var httpCode by remember { mutableStateOf(200) }

    LaunchedEffect(account.id) {
        try {
            val softs = apiClient.getSofts(account.apiKey)
            val first = softs.firstOrNull()
            if (first != null) {
                softIdInput = first.id.toString()
                gameInput = first.name.lowercase().take(20)
            }
        } catch (_: Exception) {}
    }

    fun runConnectTest() {
        if (softIdInput.isBlank() || userKeyInput.isBlank()) {
            Toast.makeText(context, "Заполните Soft ID и ключ", Toast.LENGTH_SHORT).show()
            return
        }

        isTesting = true
        responseResult = null
        responseStatus = null
        tokenVerified = null
        expectedTokenHex = null

        scope.launch {
            try {
                val softId = softIdInput.trim()
                val game = gameInput.trim()
                val key = userKeyInput.trim()
                val serial = serialInput.trim()
                val secret = connectSecretInput.trim()

                val url = if (useTestEndpoint) "https://mod.wtf/$softId/test/connect" else "https://mod.wtf/$softId/connect"

                val body = FormBody.Builder()
                    .add("game", game)
                    .add("user_key", key)
                    .add("serial", serial)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .post(body)
                    .build()

                val client = OkHttpClient()
                val (code, responseBody) = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { resp ->
                        resp.code to resp.body?.string().orEmpty()
                    }
                }

                httpCode = code
                responseResult = responseBody
                val isOk = responseBody.contains("\"status\":true") || responseBody.contains("\"status\": true")
                responseStatus = isOk

                if (secret.isNotEmpty() && isOk) {
                    val source = if (useTestEndpoint) {
                        "modkey-connect-v2|test|$game-$key-$serial-$secret"
                    } else {
                        "$game-$key-$serial-$secret"
                    }
                    val calcHex = md5Hex(source)
                    expectedTokenHex = calcHex
                    tokenVerified = responseBody.contains("\"$calcHex\"", ignoreCase = true)
                }

                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } catch (e: Exception) {
                responseResult = "Ошибка запроса: ${e.message}"
                responseStatus = false
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } finally {
                isTesting = false
            }
        }
    }

    val endpointUrl = if (useTestEndpoint) {
        "https://mod.wtf/${softIdInput.ifBlank { "77" }}/test/connect"
    } else {
        "https://mod.wtf/${softIdInput.ifBlank { "77" }}/connect"
    }

    val sourceTemplate = if (useTestEndpoint) {
        "modkey-connect-v2|test|\${game}-\${key}-\${serial}-\${secret}"
    } else {
        "\${game}-\${key}-\${serial}-\${secret}"
    }

    val generatedCode = remember(selectedLang, softIdInput, gameInput, useTestEndpoint) {
        val g = gameInput.ifBlank { "pubg" }
        val url = endpointUrl
        val prefix = if (useTestEndpoint) "modkey-connect-v2|test|" else ""
        when (selectedLang) {
            CodeLang.KOTLIN -> """
                // Modkey Connect Client (Kotlin + OkHttp)
                import okhttp3.FormBody
                import okhttp3.OkHttpClient
                import okhttp3.Request
                import org.json.JSONObject
                import java.security.MessageDigest

                fun verifyModkeyLicense(userKey: String, hwid: String): Boolean {
                    val game = "$g"
                    val secret = "YOUR_CONNECT_SECRET"
                    val client = OkHttpClient()

                    val formBody = FormBody.Builder()
                        .add("game", game.trim())
                        .add("user_key", userKey.trim())
                        .add("serial", hwid.trim())
                        .build()

                    val request = Request.Builder()
                        .url("$url")
                        .addHeader("Accept", "application/json")
                        .post(formBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return false
                        val json = JSONObject(response.body?.string().orEmpty())
                        if (!json.optBoolean("status", false)) {
                            println("Denied: " + json.optString("reason"))
                            return false
                        }
                        val data = json.getJSONObject("data")
                        val serverToken = data.getString("token")
                        val rawSource = "${prefix}${"$"}{game.trim()}-${"$"}{userKey.trim()}-${"$"}{hwid.trim()}-${"$"}secret"
                        val expectedToken = MessageDigest.getInstance("MD5")
                            .digest(rawSource.toByteArray())
                            .joinToString("") { "%02x".format(it) }
                        return serverToken.equals(expectedToken, ignoreCase = true)
                    }
                }
            """.trimIndent()

            CodeLang.CPP -> """
                // Modkey Connect Client (C++ / libcurl)
                #include <iostream>
                #include <string>
                #include <curl/curl.h>

                bool VerifyModkeyLicense(const std::string& userKey, const std::string& serial) {
                    const std::string url = "$url";
                    const std::string game = "$g";
                    const std::string secret = "YOUR_CONNECT_SECRET";

                    CURL* curl = curl_easy_init();
                    if (!curl) return false;

                    std::string postFields = "game=" + game + "&user_key=" + userKey + "&serial=" + serial;
                    std::string responseBody;

                    struct curl_slist* headers = nullptr;
                    headers = curl_slist_append(headers, "Accept: application/json");
                    headers = curl_slist_append(headers, "Content-Type: application/x-www-form-urlencoded");

                    curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
                    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
                    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, postFields.c_str());
                    // Verify MD5("${prefix}" + game + "-" + userKey + "-" + serial + "-" + secret) == data.token
                    CURLcode res = curl_easy_perform(curl);
                    curl_slist_free_all(headers);
                    curl_easy_cleanup(curl);
                    return (res == CURLE_OK);
                }
            """.trimIndent()

            CodeLang.CSHARP -> """
                // Modkey Connect Client (C# .NET)
                using System;
                using System.Collections.Generic;
                using System.Net.Http;
                using System.Security.Cryptography;
                using System.Text;
                using System.Text.Json;
                using System.Threading.Tasks;

                public static class ModkeyAuth {
                    private static readonly HttpClient client = new HttpClient();
                    private const string Endpoint = "$url";
                    private const string Game = "$g";
                    private const string Secret = "YOUR_CONNECT_SECRET";

                    public static async Task<bool> ValidateAsync(string userKey, string serial) {
                        var form = new FormUrlEncodedContent(new[] {
                            new KeyValuePair<string, string>("game", Game),
                            new KeyValuePair<string, string>("user_key", userKey.Trim()),
                            new KeyValuePair<string, string>("serial", serial.Trim())
                        });
                        var resp = await client.PostAsync(Endpoint, form);
                        var json = JsonDocument.Parse(await resp.Content.ReadAsStringAsync()).RootElement;
                        if (!json.GetProperty("status").GetBoolean()) return false;

                        string token = json.GetProperty("data").GetProperty("token").GetString();
                        string source = $"${prefix}{Game}-{userKey.Trim()}-{serial.Trim()}-{Secret}";
                        byte[] hash = MD5.HashData(Encoding.UTF8.GetBytes(source));
                        string expected = Convert.ToHexString(hash).ToLowerInvariant();
                        return string.Equals(token, expected, StringComparison.OrdinalIgnoreCase);
                    }
                }
            """.trimIndent()

            CodeLang.PYTHON -> """
                # Modkey Connect Client (Python 3)
                import hashlib
                import requests

                ENDPOINT = "$url"
                GAME = "$g"
                CONNECT_SECRET = "YOUR_CONNECT_SECRET"

                def verify_license(user_key: str, serial: str) -> bool:
                    user_key, serial = user_key.strip(), serial.strip()
                    resp = requests.post(
                        ENDPOINT,
                        data={"game": GAME, "user_key": user_key, "serial": serial},
                        headers={"Accept": "application/json"},
                        timeout=10
                    ).json()

                    if not resp.get("status"):
                        print("License error:", resp.get("reason"))
                        return False

                    server_token = resp["data"]["token"]
                    source = f"${prefix}{GAME}-{user_key}-{serial}-{CONNECT_SECRET}"
                    expected_token = hashlib.md5(source.encode("utf-8")).hexdigest()
                    return server_token.lower() == expected_token.lower()
            """.trimIndent()

            CodeLang.CURL -> """
                curl -sS -X POST '$url' \
                  -H 'Accept: application/json' \
                  --data-urlencode 'game=$g' \
                  --data-urlencode 'user_key=${userKeyInput.ifBlank { "XXXX-XXXX-XXXX-XXXX" }}' \
                  --data-urlencode 'serial=$serialInput'
            """.trimIndent()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ModkeyDesign.Colors.background)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Connect Тестер & Код",
                    subtitle = "Проверка лицензий, подписи MD5 и генерация кода для софта"
                )

                // Segmented tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ModkeyDesign.Colors.surfaceContainerLow, RoundedCornerShape(16.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf("Тестер Connect" to 0, "Генератор кода" to 1).forEach { (label, idx) ->
                        val isSelected = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) ModkeyDesign.Colors.primary else Color.Transparent,
                                    RoundedCornerShape(13.dp)
                                )
                                .modkeyPress {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    selectedTab = idx
                                }
                                .padding(vertical = 9.dp)
                        ) {
                            Text(
                                text = label,
                                style = ModkeyDesign.Typography.labelMedium().copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) ModkeyDesign.Colors.onPrimary else ModkeyDesign.Colors.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // HWID Device Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ModkeyDesign.Colors.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                ModkeyIcon(
                                    imageVector = Icons.Filled.PhoneAndroid,
                                    contentDescription = null,
                                    tint = ModkeyDesign.Colors.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Android HWID текущего телефона",
                                    style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                            ModkeyIconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("HWID", androidId))
                                    Toast.makeText(context, "HWID скопирован", Toast.LENGTH_SHORT).show()
                                },
                                size = 34.dp,
                                iconSize = 17.dp
                            ) {
                                ModkeyIcon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Копировать",
                                    tint = ModkeyDesign.Colors.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ModkeyDesign.Colors.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = androidId,
                                style = ModkeyDesign.Typography.bodyMedium(),
                                fontFamily = FontFamily.Monospace,
                                color = ModkeyDesign.Colors.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Text(
                            text = "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}",
                            style = ModkeyDesign.Typography.bodySmall(),
                            color = ModkeyDesign.Colors.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // Connect Params Card
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ModkeyDesign.Colors.surfaceContainer,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Параметры проверки Connect",
                                style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.Bold)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ModkeyFilterChip(
                                    selected = !useTestEndpoint,
                                    onClick = { useTestEndpoint = false },
                                    label = "Main Connect"
                                )
                                ModkeyFilterChip(
                                    selected = useTestEndpoint,
                                    onClick = { useTestEndpoint = true },
                                    label = "Test Connect"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ModkeyOutlinedTextField(
                                    value = softIdInput,
                                    onValueChange = { softIdInput = it },
                                    label = "Soft ID",
                                    modifier = Modifier.weight(1f)
                                )

                                ModkeyOutlinedTextField(
                                    value = gameInput,
                                    onValueChange = { gameInput = it },
                                    label = "Game",
                                    modifier = Modifier.weight(1.5f)
                                )
                            }

                            ModkeyOutlinedTextField(
                                value = userKeyInput,
                                onValueChange = { userKeyInput = it },
                                label = "User Key (Лицензионный ключ)",
                                modifier = Modifier.fillMaxWidth()
                            )

                            ModkeyOutlinedTextField(
                                value = serialInput,
                                onValueChange = { serialInput = it },
                                label = "Serial (HWID)",
                                modifier = Modifier.fillMaxWidth()
                            )

                            ModkeyOutlinedTextField(
                                value = connectSecretInput,
                                onValueChange = { connectSecretInput = it },
                                label = "CONNECT_SECRET (опционально для проверки MD5)",
                                placeholder = "Из Connect Settings > Request > Secret",
                                modifier = Modifier.fillMaxWidth()
                            )

                            ModkeyButton(
                                onClick = { runConnectTest() },
                                enabled = !isTesting,
                                shape = RoundedCornerShape(16.dp),
                                minHeight = 52.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isTesting) {
                                    CircularLoader(
                                        modifier = Modifier.size(20.dp),
                                        color = ModkeyDesign.Colors.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Отправка запроса...",
                                        style = ModkeyDesign.Typography.labelLarge(),
                                        color = ModkeyDesign.Colors.onPrimary
                                    )
                                } else {
                                    ModkeyIcon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = ModkeyDesign.Colors.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Проверить ключ через Connect",
                                        style = ModkeyDesign.Typography.labelLarge(),
                                        color = ModkeyDesign.Colors.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Results Card
                if (responseResult != null) {
                    item {
                        val statusSuccess = responseStatus == true
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = ModkeyDesign.Colors.surfaceContainerHigh,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                (if (statusSuccess) ModkeyDesign.Colors.success else ModkeyDesign.Colors.error).copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        ModkeyIcon(
                                            imageVector = if (statusSuccess) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = if (statusSuccess) ModkeyDesign.Colors.success else ModkeyDesign.Colors.error,
                                            modifier = Modifier.size(21.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (statusSuccess) "Лицензия подтверждена!" else "Отказ валидации",
                                            style = ModkeyDesign.Typography.titleSmall().copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusBadge(
                                        text = "HTTP $httpCode",
                                        color = if (httpCode == 200) ModkeyDesign.Colors.success else ModkeyDesign.Colors.error
                                    )
                                }

                                if (tokenVerified != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ModkeyIcon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = null,
                                            tint = if (tokenVerified == true) ModkeyDesign.Colors.success else ModkeyDesign.Colors.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (tokenVerified == true)
                                                "Подпись MD5 (token) валидна: $expectedTokenHex"
                                            else
                                                "Внимание: токен не совпал с ожидаемым MD5 ($expectedTokenHex)",
                                            style = ModkeyDesign.Typography.bodySmall().copy(fontWeight = FontWeight.SemiBold),
                                            color = if (tokenVerified == true) ModkeyDesign.Colors.success else ModkeyDesign.Colors.error
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ModkeyDesign.Colors.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = responseResult.orEmpty(),
                                        style = ModkeyDesign.Typography.bodySmall(),
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Client Code Generator
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ModkeyDesign.Colors.surfaceContainer,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            ModkeyDesign.Colors.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ModkeyIcon(
                                    imageVector = Icons.Filled.Code,
                                    contentDescription = null,
                                    tint = ModkeyDesign.Colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Готовый код интеграции Connect",
                                        style = ModkeyDesign.Typography.titleMedium().copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Эндпоинт: $endpointUrl",
                                        style = ModkeyDesign.Typography.bodySmall(),
                                        color = ModkeyDesign.Colors.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CodeLang.entries.forEach { lang ->
                                    ModkeyFilterChip(
                                        selected = selectedLang == lang,
                                        onClick = { selectedLang = lang },
                                        label = lang.label
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ModkeyDesign.Colors.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = generatedCode,
                                    style = ModkeyDesign.Typography.bodySmall(),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ModkeyButton(
                                    onClick = {
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("Modkey Code", generatedCode))
                                        Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                                    },
                                    variant = com.modkey.app.ui.components.ButtonVariant.Secondary,
                                    shape = RoundedCornerShape(14.dp),
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
                                        text = "Скопировать",
                                        style = ModkeyDesign.Typography.labelLarge(),
                                        color = ModkeyDesign.Colors.onSurface
                                    )
                                }

                                ModkeyButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, generatedCode)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Отправить код (${selectedLang.label})"))
                                    },
                                    shape = RoundedCornerShape(14.dp),
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
                                        text = "Отправить",
                                        style = ModkeyDesign.Typography.labelLarge(),
                                        color = ModkeyDesign.Colors.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding().padding(bottom = 104.dp))
            }
        }
    }
}
