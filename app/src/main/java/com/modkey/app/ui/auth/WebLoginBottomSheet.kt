package com.modkey.app.ui.auth

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.modkey.app.ModkeyApp
import com.modkey.app.core.network.ModkeyWebAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

private class ModkeyJsBridge(
    private val mainHandler: Handler,
    private val onCandidatesReceived: (List<String>, Boolean) -> Unit,
    private val onDirectCopyReceived: (String) -> Unit
) {
    private val json = Json { ignoreUnknownKeys = true }

    @JavascriptInterface
    fun onCandidatesJson(candidatesJson: String, isManualTrigger: Boolean) {
        try {
            val list = json.decodeFromString<List<String>>(candidatesJson)
            mainHandler.post {
                onCandidatesReceived(list, isManualTrigger)
            }
        } catch (e: Exception) {
            Log.e("WebLoginBottomSheet", "Failed to decode JS candidates: ${e.message}")
        }
    }

    @JavascriptInterface
    fun onCopiedText(text: String) {
        val trimmed = text.trim()
        if (trimmed.length in 16..100 && !trimmed.contains(" ") && !trimmed.startsWith("http")) {
            mainHandler.post {
                onDirectCopyReceived(trimmed)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebLoginBottomSheet(
    onDismiss: () -> Unit,
    onApiKeyExtracted: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val apiClient = ModkeyApp.instance.apiClient

    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("https://mod.wtf/login") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isUserLoggedIn by remember { mutableStateOf(false) }
    var manualKeyInput by remember { mutableStateOf("") }
    var isExtractingKey by remember { mutableStateOf(false) }
    var hasCompletedExtraction by remember { mutableStateOf(false) }
    var autoRedirectedToApi by remember { mutableStateOf(false) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun handleCandidates(candidates: List<String>, isManualTrigger: Boolean) {
        if (hasCompletedExtraction) return
        if (candidates.isEmpty()) {
            if (isManualTrigger) {
                isExtractingKey = false
                if (!currentUrl.contains("/admin/api")) {
                    webViewRef?.loadUrl("https://mod.wtf/admin/api")
                    Toast.makeText(context, "Открываем раздел API...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ключ на странице не найден. Создайте или скопируйте ключ ниже.", Toast.LENGTH_LONG).show()
                }
            }
            return
        }

        isExtractingKey = true
        scope.launch {
            try {
                var verifiedKey: String? = null
                for (candidate in candidates) {
                    try {
                        apiClient.getOverviewStats(candidate)
                        verifiedKey = candidate
                        break
                    } catch (e: Exception) {
                        Log.w("WebLoginBottomSheet", "Candidate ${candidate.take(6)}... failed check: ${e.message}")
                    }
                }

                val finalKey = verifiedKey ?: candidates.firstOrNull { it.endsWith("_MDK") }
                if (!finalKey.isNullOrBlank()) {
                    hasCompletedExtraction = true
                    manualKeyInput = finalKey
                    Toast.makeText(context, "API Ключ успешно получен!", Toast.LENGTH_SHORT).show()
                    onApiKeyExtracted(finalKey)
                } else if (isManualTrigger) {
                    val top = candidates.first()
                    manualKeyInput = top
                    Toast.makeText(
                        context,
                        "Найден кандидат ключа, проверьте поле ввода или скопируйте ключ со страницы",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                isExtractingKey = false
            }
        }
    }

    fun scanWebViewForApiKey(view: WebView?, isManualTrigger: Boolean) {
        if (view == null) return
        view.evaluateJavascript(
            """
            (function() {
                function isPlausibleKey(val, csrfTokens) {
                    if (!val || typeof val !== 'string') return false;
                    var s = val.trim();
                    if (s.length < 16 || s.length > 100) return false;
                    if (csrfTokens.indexOf(s) !== -1) return false;
                    if (!/^[A-Za-z0-9_-]+$/.test(s)) return false;
                    var lower = s.toLowerCase();
                    if (lower.startsWith('http') ||
                        lower.includes('your_api_key') ||
                        lower.includes('example') ||
                        lower.includes('placeholder') ||
                        lower.includes('csrf') ||
                        lower.includes('bootstrap')) {
                        return false;
                    }
                    return true;
                }

                var csrfTokens = [];
                var csrfMeta = document.querySelector('meta[name="csrf-token"]');
                if (csrfMeta && csrfMeta.content) csrfTokens.push(csrfMeta.content.trim());
                var tokenInputs = document.querySelectorAll('input[name="_token"], input[type="hidden"]');
                for (var t = 0; t < tokenInputs.length; t++) {
                    if (tokenInputs[t].value) csrfTokens.push(tokenInputs[t].value.trim());
                }

                var candidates = [];
                function addCandidate(val) {
                    if (!val || typeof val !== 'string') return;
                    var s = val.trim();
                    if (isPlausibleKey(s, csrfTokens) && candidates.indexOf(s) === -1) {
                        candidates.push(s);
                    }
                }

                var visibleInputs = document.querySelectorAll('input:not([type="hidden"]):not([name="_token"])');
                for (var i = 0; i < visibleInputs.length; i++) {
                    var v = (visibleInputs[i].value || '').trim();
                    if (v.endsWith('_MDK')) addCandidate(v);
                }

                var copyTargets = document.querySelectorAll('[data-copy-target], [data-toggle-password]');
                for (var j = 0; j < copyTargets.length; j++) {
                    var targetId = copyTargets[j].getAttribute('data-copy-target') || copyTargets[j].getAttribute('data-toggle-password');
                    if (targetId) {
                        var targetEl = document.getElementById(targetId);
                        if (targetEl) {
                            addCandidate(targetEl.value || targetEl.textContent || targetEl.innerText);
                        }
                    }
                }

                var bodyHtml = document.body ? (document.body.innerHTML || '') : '';
                var mdkRegex = /\b([A-Za-z0-9_-]{10,80}_MDK)\b/g;
                var match;
                while ((match = mdkRegex.exec(bodyHtml)) !== null) {
                    addCandidate(match[1]);
                }

                for (var k = 0; k < visibleInputs.length; k++) {
                    var inp = visibleInputs[k];
                    if (inp.type === 'submit' || inp.type === 'button' || inp.type === 'search' || inp.type === 'checkbox') continue;
                    if (inp.readOnly || inp.hasAttribute('readonly') || inp.type === 'password' ||
                        (inp.id && /api|key|token/i.test(inp.id)) ||
                        (inp.name && /api|key|token/i.test(inp.name))) {
                        addCandidate(inp.value);
                    }
                }

                var specialEls = document.querySelectorAll('.copy-value, #api_key, #apiKey, #api-key, .api-key, [data-key]');
                for (var m = 0; m < specialEls.length; m++) {
                    var el = specialEls[m];
                    addCandidate(el.value || el.getAttribute('data-key') || el.textContent || el.innerText);
                }

                if (window.ModkeyBridge && window.ModkeyBridge.onCandidatesJson) {
                    window.ModkeyBridge.onCandidatesJson(JSON.stringify(candidates), $isManualTrigger);
                }
                return JSON.stringify(candidates);
            })();
            """.trimIndent(),
            null
        )
    }

    fun tryExtractApiKeyFromAdminApi() {
        val view = webViewRef
        if (currentUrl.contains("/admin/api") && view != null) {
            isExtractingKey = true
            scanWebViewForApiKey(view, isManualTrigger = true)
            return
        }

        isExtractingKey = true
        scope.launch {
            try {
                CookieManager.getInstance().flush()
                val cookies = CookieManager.getInstance().getCookie("https://mod.wtf") ?: ""
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://mod.wtf/admin/api")
                    .addHeader("Cookie", cookies)
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    )
                    .build()

                val html = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { it.body?.string().orEmpty() }
                }

                val candidates = ModkeyWebAuth.extractApiKeyCandidatesFromHtml(html)
                if (candidates.isNotEmpty()) {
                    handleCandidates(candidates, isManualTrigger = true)
                } else {
                    isExtractingKey = false
                    webViewRef?.loadUrl("https://mod.wtf/admin/api")
                    Toast.makeText(context, "Перешли в раздел API. Скопируйте ключ или создайте его", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                isExtractingKey = false
                webViewRef?.loadUrl("https://mod.wtf/admin/api")
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
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Вход на mod.wtf",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isUserLoggedIn) "Вы вошли в систему — получаем API ключ" else "Telegram / Passkey / Логин",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUserLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        context.startActivity(browserIntent)
                    }
                ) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = "Открыть в Chrome")
                }

                IconButton(onClick = { webViewRef?.reload() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
                }
            }

            if (isLoading || isExtractingKey) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUserLoggedIn) "Сессия активна" else "Войдите на сайте",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Button(
                        onClick = { tryExtractApiKeyFromAdminApi() },
                        enabled = !isExtractingKey,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isExtractingKey) "Проверка..." else "Извлечь ключ API")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualKeyInput,
                    onValueChange = { manualKeyInput = it },
                    placeholder = { Text("Или вставьте скопированный ключ...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!clip.isNullOrBlank()) manualKeyInput = clip.trim()
                            }
                        ) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "Вставить")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (manualKeyInput.isNotBlank()) {
                            onApiKeyExtracted(manualKeyInput.trim())
                        }
                    },
                    enabled = manualKeyInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Готово")
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                javaScriptCanOpenWindowsAutomatically = true
                                setSupportMultipleWindows(false)
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                cacheMode = WebSettings.LOAD_DEFAULT
                                userAgentString =
                                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                            }

                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            addJavascriptInterface(
                                ModkeyJsBridge(
                                    mainHandler = mainHandler,
                                    onCandidatesReceived = { list, isManual ->
                                        handleCandidates(list, isManual)
                                    },
                                    onDirectCopyReceived = { copiedKey ->
                                        handleCandidates(listOf(copiedKey), isManualTrigger = true)
                                    }
                                ),
                                "ModkeyBridge"
                            )

                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    currentUrl = url.orEmpty()
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            window.open = function(url) {
                                                if (url) {
                                                    window.location.assign(url);
                                                }
                                                return null;
                                            };
                                            if (navigator.clipboard && navigator.clipboard.writeText) {
                                                var origWriteText = navigator.clipboard.writeText.bind(navigator.clipboard);
                                                navigator.clipboard.writeText = function(text) {
                                                    if (window.ModkeyBridge && window.ModkeyBridge.onCopiedText && text) {
                                                        window.ModkeyBridge.onCopiedText(String(text));
                                                    }
                                                    return origWriteText(text);
                                                };
                                            }
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    val loadedUrl = url.orEmpty()
                                    currentUrl = loadedUrl
                                    CookieManager.getInstance().flush()
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            if (window.panelCopy && !window.__mdkHookedCopy) {
                                                var origPanelCopy = window.panelCopy;
                                                window.panelCopy = async function(val) {
                                                    if (window.ModkeyBridge && window.ModkeyBridge.onCopiedText && val) {
                                                        window.ModkeyBridge.onCopiedText(String(val));
                                                    }
                                                    return origPanelCopy(val);
                                                };
                                                window.__mdkHookedCopy = true;
                                            }
                                        })();
                                        """.trimIndent(),
                                        null
                                    )

                                    val isModWtf = loadedUrl.startsWith("https://mod.wtf")
                                    val isAuthPage = loadedUrl.contains("/login") ||
                                        loadedUrl.contains("/register") ||
                                        loadedUrl.contains("/auth/") ||
                                        loadedUrl.contains("/policy")

                                    if (isModWtf && !isAuthPage) {
                                        isUserLoggedIn = true
                                        if (loadedUrl.contains("/admin/api")) {
                                            scanWebViewForApiKey(view, isManualTrigger = false)
                                        } else if (!autoRedirectedToApi) {
                                            autoRedirectedToApi = true
                                            view?.loadUrl("https://mod.wtf/admin/api")
                                        }
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString().orEmpty()
                                    if (url.startsWith("tg:") || url.startsWith("intent:")) {
                                        try {
                                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                            ctx.startActivity(intent)
                                            return true
                                        } catch (_: Exception) {}
                                    }

                                    return false
                                }
                            }
                            webViewRef = this
                            loadUrl(currentUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
