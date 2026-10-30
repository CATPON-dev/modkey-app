package com.modkey.app.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ModkeyWebAuthException(message: String) : Exception(message)

object ModkeyWebAuth {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    suspend fun loginWithCredentials(username: String, password: String): String = withContext(Dispatchers.IO) {
        val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val host = url.host
                val list = cookieStore.getOrPut(host) { mutableListOf() }
                cookies.forEach { newCookie ->
                    list.removeAll { it.name == newCookie.name }
                    list.add(newCookie)
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        }

        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val getLoginRequest = Request.Builder()
            .url("https://mod.wtf/login")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        val loginPageHtml = client.newCall(getLoginRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Не удалось загрузить страницу входа (HTTP ${response.code})")
            response.body?.string().orEmpty()
        }

        val csrfToken = extractCsrfToken(loginPageHtml)
        if (csrfToken.isBlank()) {
            throw ModkeyWebAuthException("Не удалось получить CSRF-токен с сайта mod.wtf")
        }

        val formBody = FormBody.Builder()
            .add("_token", csrfToken)
            .add("username", username.trim())
            .add("password", password)
            .build()

        val postLoginRequest = Request.Builder()
            .url("https://mod.wtf/login")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", "https://mod.wtf/login")
            .post(formBody)
            .build()

        val postResponseHtml = client.newCall(postLoginRequest).execute().use { response ->
            val html = response.body?.string().orEmpty()
            val finalUrl = response.request.url.toString()

            if (finalUrl.contains("/login")) {
                val alertMatcher = Pattern.compile("""class=["'][^"']*alert-(?:danger|warning)[^"']*["'][^>]*>(.*?)</div>""", Pattern.DOTALL)
                    .matcher(html)
                if (alertMatcher.find()) {
                    val rawErr = alertMatcher.group(1).replace(Regex("<[^>]*>"), "").trim()
                    if (rawErr.isNotBlank()) {
                        throw ModkeyWebAuthException(rawErr)
                    }
                }
                val swalMatcher = Pattern.compile("""Swal\.fire\(\{[^}]*text:\s*["']([^"']+)["']""", Pattern.DOTALL).matcher(html)
                if (swalMatcher.find()) {
                    throw ModkeyWebAuthException(swalMatcher.group(1))
                }
                throw ModkeyWebAuthException("Неверный логин или пароль")
            }
            html
        }

        // 3. GET /admin/api to extract the API Key
        val getApiRequest = Request.Builder()
            .url("https://mod.wtf/admin/api")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Referer", "https://mod.wtf/")
            .build()

        val apiPageHtml = client.newCall(getApiRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Не удалось открыть раздел API (HTTP ${response.code})")
            response.body?.string().orEmpty()
        }

        val candidates = extractApiKeyCandidatesFromHtml(apiPageHtml)
        if (candidates.isEmpty()) {
            throw ModkeyWebAuthException("Вы успешно вошли, но на аккаунте еще не создан API ключ. Создайте его на https://mod.wtf/admin/api")
        }

        val apiKey = resolveValidApiKey(candidates, com.modkey.app.ModkeyApp.instance.apiClient)
            ?: candidates.first()

        Log.d("ModkeyWebAuth", "Successfully authenticated native user $username, got API key: ${apiKey.take(6)}...")
        apiKey
    }

    suspend fun resolveValidApiKey(candidates: List<String>, apiClient: ModkeyApiClient): String? {
        if (candidates.isEmpty()) return null
        for (candidate in candidates) {
            try {
                apiClient.getOverviewStats(candidate)
                return candidate
            } catch (e: Exception) {
                Log.w("ModkeyWebAuth", "Candidate ${candidate.take(6)}... failed validation: ${e.message}")
            }
        }
        return candidates.firstOrNull { it.endsWith("_MDK") } ?: candidates.firstOrNull()
    }

    private fun extractCsrfToken(html: String): String {
        val metaMatcher = Pattern.compile("""<meta[^>]+name=["']csrf-token["'][^>]+content=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE).matcher(html)
        if (metaMatcher.find()) return metaMatcher.group(1).orEmpty().trim()

        val inputMatcher1 = Pattern.compile("""<input[^>]+name=["']_token["'][^>]+value=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE).matcher(html)
        if (inputMatcher1.find()) return inputMatcher1.group(1).orEmpty().trim()

        val inputMatcher2 = Pattern.compile("""<input[^>]+value=["']([^"']+)["'][^>]+name=["']_token["']""", Pattern.CASE_INSENSITIVE).matcher(html)
        if (inputMatcher2.find()) return inputMatcher2.group(1).orEmpty().trim()

        return ""
    }

    private fun extractAllCsrfTokens(html: String): Set<String> {
        val tokens = mutableSetOf<String>()
        val metaMatcher = Pattern.compile("""<meta[^>]+name=["']csrf-token["'][^>]+content=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE).matcher(html)
        while (metaMatcher.find()) {
            metaMatcher.group(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { tokens.add(it) }
        }
        val inputMatcher1 = Pattern.compile("""<input[^>]+name=["']_token["'][^>]+value=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE).matcher(html)
        while (inputMatcher1.find()) {
            inputMatcher1.group(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { tokens.add(it) }
        }
        val inputMatcher2 = Pattern.compile("""<input[^>]+value=["']([^"']+)["'][^>]+name=["']_token["']""", Pattern.CASE_INSENSITIVE).matcher(html)
        while (inputMatcher2.find()) {
            inputMatcher2.group(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { tokens.add(it) }
        }
        return tokens
    }

    private fun isPlausibleApiKey(candidate: String, csrfTokens: Set<String>): Boolean {
        val trimmed = candidate.trim()
        if (trimmed.length < 16 || trimmed.length > 100) return false
        if (trimmed in csrfTokens) return false
        if (!trimmed.matches(Regex("^[A-Za-z0-9_-]+$"))) return false
        val lower = trimmed.lowercase()
        if (lower.startsWith("http") ||
            lower.contains("your_api_key") ||
            lower.contains("example") ||
            lower.contains("placeholder") ||
            lower.contains("csrf") ||
            lower.contains("bootstrap") ||
            lower.contains("sweetalert")
        ) {
            return false
        }
        return true
    }

    fun extractApiKeyCandidatesFromHtml(html: String): List<String> {
        val csrfTokens = extractAllCsrfTokens(html)
        val candidates = mutableListOf<String>()

        fun addCandidate(raw: String?) {
            val s = raw?.trim().orEmpty()
            if (s.isNotEmpty() && isPlausibleApiKey(s, csrfTokens) && s !in candidates) {
                candidates.add(s)
            }
        }

        val cleanHtml = html
            .replace(Regex("""<input\b[^>]*\btype\s*=\s*["']hidden["'][^>]*>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<input\b[^>]*\bname\s*=\s*["']_token["'][^>]*>""", RegexOption.IGNORE_CASE), "")

        val mdkMatcher = Pattern.compile("""\b([A-Za-z0-9_-]{10,80}_MDK)\b""").matcher(cleanHtml)
        while (mdkMatcher.find()) {
            addCandidate(mdkMatcher.group(1))
        }

        val targetIdMatcher = Pattern.compile("""data-(?:copy-target|toggle-password)=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE).matcher(cleanHtml)
        val targetIds = mutableSetOf<String>()
        while (targetIdMatcher.find()) {
            targetIdMatcher.group(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { targetIds.add(it) }
        }
        for (targetId in targetIds) {
            val quotedId = Pattern.quote(targetId)
            val tagMatcher = Pattern.compile("""<[^>]*\bid=["']$quotedId["'][^>]*>""", Pattern.CASE_INSENSITIVE).matcher(cleanHtml)
            while (tagMatcher.find()) {
                val tag = tagMatcher.group(0).orEmpty()
                val valMatcher = Pattern.compile("""\bvalue=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE).matcher(tag)
                if (valMatcher.find()) {
                    addCandidate(valMatcher.group(1))
                }
            }
            val textMatcher = Pattern.compile("""\bid=["']$quotedId["'][^>]*>([^<]+)<""", Pattern.CASE_INSENSITIVE).matcher(cleanHtml)
            while (textMatcher.find()) {
                addCandidate(textMatcher.group(1))
            }
        }
        val inputTagMatcher = Pattern.compile("""<input\b[^>]*>""", Pattern.CASE_INSENSITIVE).matcher(cleanHtml)
        val fallbackInputValues = mutableListOf<String>()
        while (inputTagMatcher.find()) {
            val tag = inputTagMatcher.group(0).orEmpty()
            if (tag.contains(Regex("""\btype\s*=\s*["'](?:submit|button|checkbox|radio|file|search)["']""", RegexOption.IGNORE_CASE))) {
                continue
            }
            val valMatcher = Pattern.compile("""\bvalue=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE).matcher(tag)
            if (valMatcher.find()) {
                val value = valMatcher.group(1).orEmpty().trim()
                val isPriorityInput = tag.contains(Regex("""\breadonly\b""", RegexOption.IGNORE_CASE)) ||
                    tag.contains(Regex("""\b(?:id|name|class)\s*=\s*["'][^"']*(?:api|key|token)[^"']*["']""", RegexOption.IGNORE_CASE))
                if (isPriorityInput) {
                    addCandidate(value)
                } else {
                    fallbackInputValues.add(value)
                }
            }
        }
        for (v in fallbackInputValues) {
            addCandidate(v)
        }

        val copyValueMatcher = Pattern.compile("""class=["'][^"']*(?:copy-value|api-key|api_key)[^"']*["'][^>]*>([^<]+)<""", Pattern.CASE_INSENSITIVE).matcher(cleanHtml)
        while (copyValueMatcher.find()) {
            addCandidate(copyValueMatcher.group(1))
        }

        return candidates
    }

    fun extractApiKeyFromHtml(html: String): String? {
        return extractApiKeyCandidatesFromHtml(html).firstOrNull()
    }
}
