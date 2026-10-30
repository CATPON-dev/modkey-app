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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modkey.app.ModkeyApp
import com.modkey.app.core.model.SavedAccount
import com.modkey.app.ui.components.SectionHeader
import com.modkey.app.ui.components.StatusBadge
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(
                    title = "Connect Тестер & Код",
                    subtitle = "Проверка лицензий, подписи MD5 и генерация кода для софта"
                )

                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Тестер Connect") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Генератор кода") }
                    )
                }
            }

            // HWID Device Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Android HWID текущего телефона",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("HWID", androidId))
                                    Toast.makeText(context, "HWID скопирован", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Копировать", modifier = Modifier.size(18.dp))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = androidId,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Text(
                            text = "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // Connect Params Card
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !useTestEndpoint,
                                    onClick = { useTestEndpoint = false },
                                    label = { Text("Main Connect") }
                                )
                                FilterChip(
                                    selected = useTestEndpoint,
                                    onClick = { useTestEndpoint = true },
                                    label = { Text("Test Connect") }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = softIdInput,
                                    onValueChange = { softIdInput = it },
                                    label = { Text("Soft ID") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = gameInput,
                                    onValueChange = { gameInput = it },
                                    label = { Text("Game") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1.5f)
                                )
                            }

                            OutlinedTextField(
                                value = userKeyInput,
                                onValueChange = { userKeyInput = it },
                                label = { Text("User Key (Лицензионный ключ)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = serialInput,
                                onValueChange = { serialInput = it },
                                label = { Text("Serial (HWID)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = connectSecretInput,
                                onValueChange = { connectSecretInput = it },
                                label = { Text("CONNECT_SECRET (опционально для проверки MD5)") },
                                placeholder = { Text("Из Connect Settings > Request > Secret") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { runConnectTest() },
                                enabled = !isTesting,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Отправка запроса...")
                                } else {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Проверить ключ через Connect")
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
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (statusSuccess) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = if (statusSuccess) Color(0xFF4CAF50) else Color(0xFFE53935)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (statusSuccess) "Лицензия подтверждена!" else "Отказ валидации",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    StatusBadge(
                                        text = "HTTP $httpCode",
                                        color = if (httpCode == 200) Color(0xFF4CAF50) else Color(0xFFE53935)
                                    )
                                }

                                if (tokenVerified != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.VerifiedUser,
                                            contentDescription = null,
                                            tint = if (tokenVerified == true) Color(0xFF4CAF50) else Color(0xFFE53935),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (tokenVerified == true)
                                                "Подпись MD5 (token) валидна: $expectedTokenHex"
                                            else
                                                "Внимание: токен не совпал с ожидаемым MD5 ($expectedTokenHex)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (tokenVerified == true) Color(0xFF4CAF50) else Color(0xFFE53935)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = responseResult.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
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
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Готовый код интеграции Connect",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Эндпоинт: $endpointUrl",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    FilterChip(
                                        selected = selectedLang == lang,
                                        onClick = { selectedLang = lang },
                                        label = { Text(lang.label) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = generatedCode,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.padding(14.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("Modkey Code", generatedCode))
                                        Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Скопировать")
                                }

                                Button(
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
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Отправить")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
