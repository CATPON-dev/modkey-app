package com.modkey.app.core.network

import android.util.Log
import com.modkey.app.core.model.AccountMeInfo
import com.modkey.app.core.model.AuditLogItem
import com.modkey.app.core.model.CipherKeysInfo
import com.modkey.app.core.model.ClientCountryStat
import com.modkey.app.core.model.ConnectConfig
import com.modkey.app.core.model.ConnectLoginItem
import com.modkey.app.core.model.CreatedKeysResponse
import com.modkey.app.core.model.DeviceBanItem
import com.modkey.app.core.model.DomainItem
import com.modkey.app.core.model.GameProfileItem
import com.modkey.app.core.model.KeyItem
import com.modkey.app.core.model.KeyTemplateItem
import com.modkey.app.core.model.KeysResponse
import com.modkey.app.core.model.OnlineFileItem
import com.modkey.app.core.model.OnlineSystemConfig
import com.modkey.app.core.model.OverviewStats
import com.modkey.app.core.model.ReferralCodeItem
import com.modkey.app.core.model.SoftStatsResponse
import com.modkey.app.core.model.SoftwareItem
import com.modkey.app.core.model.UserItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

open class ModkeyApiException(val errorCode: String, message: String) : Exception(message)

class ModkeyDnsVerificationException(
    val recordName: String,
    val recordValue: String,
    message: String
) : ModkeyApiException("dns_txt_required", message)

class ModkeyApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()
    private val endpointV2Url = "https://mod.wtf/api/v2/action"
    private val endpointV1Url = "https://mod.wtf/api/v1/action"

    suspend fun executeAction(apiKey: String, method: String, extraParams: JsonObjectBuilder.() -> Unit = {}): String = withContext(Dispatchers.IO) {
        val bodyObject = buildJsonObject {
            put("api_key", apiKey)
            put("method", method)
            extraParams()
        }

        val requestBody = bodyObject.toString().toRequestBody(mediaTypeJson)
        val request = Request.Builder()
            .url(endpointV2Url)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "Modkey-Android-M3E/1.0")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            Log.d("ModkeyApi", "[$method] HTTP ${response.code}: ${bodyString.take(300)}")

            if (!response.isSuccessful) {
                parseAndThrowApiError(bodyString, response.code)
                throw IOException("HTTP ${response.code}: $bodyString")
            }

            parseAndThrowApiError(bodyString, response.code)

            bodyString
        }
    }

    private fun parseAndThrowApiError(bodyString: String, httpCode: Int) {
        try {
            val element = json.parseToJsonElement(bodyString)
            if (element is JsonObject) {
                val ok = element["ok"]?.jsonPrimitive?.booleanOrNull
                if (ok == false || httpCode >= 400) {
                    val err = element["error"]?.jsonPrimitive?.content ?: "http_error_$httpCode"
                    val msg = element["message"]?.jsonPrimitive?.content ?: "Ошибка запроса ($httpCode)"
                    if (err == "dns_txt_required") {
                        val details = element["details"] as? JsonObject
                        val recName = details?.get("name")?.jsonPrimitive?.content.orEmpty()
                        val recVal = details?.get("value")?.jsonPrimitive?.content.orEmpty()
                        throw ModkeyDnsVerificationException(recName, recVal, msg)
                    }
                    throw ModkeyApiException(err, msg)
                }
            }
        } catch (e: ModkeyApiException) {
            throw e
        } catch (_: Exception) {}
    }

    suspend fun executeV1Action(apiKey: String, method: String, params: Map<String, String> = emptyMap()): JsonObject = withContext(Dispatchers.IO) {
        val formBuilder = FormBody.Builder()
            .add("api_key", apiKey)
            .add("method", method)
        params.forEach { (k, v) -> formBuilder.add(k, v) }

        val request = Request.Builder()
            .url(endpointV1Url)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "Modkey-Android-M3E/1.0")
            .post(formBuilder.build())
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            val elem = json.parseToJsonElement(bodyString).jsonObject
            val status = elem["status"]?.jsonPrimitive?.booleanOrNull ?: false
            if (!status) {
                val msg = elem["message"]?.jsonPrimitive?.content
                    ?: (elem["data"] as? JsonObject)?.get("message")?.jsonPrimitive?.content
                    ?: "Ошибка API v1 ($method)"
                val code = elem["code"]?.jsonPrimitive?.content ?: "${response.code}"
                throw ModkeyApiException(code, msg)
            }
            elem
        }
    }

    private fun unwrapData(rawJson: String): JsonElement {
        val element = json.parseToJsonElement(rawJson)
        return if (element is JsonObject && element.containsKey("data")) {
            element["data"]!!
        } else {
            element
        }
    }

    private inline fun <reified T> decodeListFromResponse(
        rawJson: String,
        vararg fallbackKeys: String
    ): List<T> {
        val element = unwrapData(rawJson)
        if (element is JsonArray) return json.decodeFromJsonElement<List<T>>(element)
        if (element is JsonObject) {
            val keysToTry = listOf("items") + fallbackKeys.toList()
            for (k in keysToTry) {
                val child = element[k]
                if (child is JsonArray) {
                    return json.decodeFromJsonElement<List<T>>(child)
                }
            }
        }
        return emptyList()
    }

    private suspend fun executeMultipartAction(
        apiKey: String,
        method: String,
        softId: Int,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String = "application/octet-stream",
        extraFields: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("api_key", apiKey)
            .addFormDataPart("method", method)
            .addFormDataPart("soft_id", softId.toString())

        extraFields.forEach { (k, v) -> builder.addFormDataPart(k, v) }

        val fileMediaType = mimeType.toMediaType()
        builder.addFormDataPart(
            "file",
            fileName,
            fileBytes.toRequestBody(fileMediaType)
        )

        val request = Request.Builder()
            .url(endpointV2Url)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "Modkey-Android-M3E/1.0")
            .post(builder.build())
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            Log.d("ModkeyApi", "[$method] multipart HTTP ${response.code}: ${bodyString.take(300)}")
            if (!response.isSuccessful) {
                parseAndThrowApiError(bodyString, response.code)
                throw IOException("HTTP ${response.code}: $bodyString")
            }
            parseAndThrowApiError(bodyString, response.code)
            bodyString
        }
    }


    suspend fun getMe(apiKey: String): AccountMeInfo = withContext(Dispatchers.IO) {
        val resp = executeV1Action(apiKey, "get-me")
        val dataObj = resp["data"] ?: throw IOException("Пустой ответ get-me")
        json.decodeFromJsonElement<AccountMeInfo>(dataObj)
    }

    suspend fun renameUserKey(apiKey: String, currentKey: String, newKey: String): Boolean = withContext(Dispatchers.IO) {
        executeV1Action(
            apiKey = apiKey,
            method = "edit-user-key",
            params = mapOf("key" to currentKey, "new_user_key" to newKey)
        )
        true
    }


    suspend fun getOverviewStats(apiKey: String): OverviewStats = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-overview-stats")
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<OverviewStats>(dataElem)
    }

    suspend fun getSofts(apiKey: String, limit: Int = 100, offset: Int = 0): List<SoftwareItem> = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-softs") {
            put("limit", limit)
            put("offset", offset)
        }
        decodeListFromResponse<SoftwareItem>(raw)
    }

    suspend fun getSoft(apiKey: String, softId: Int): SoftwareItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-soft") {
            put("soft_id", softId)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<SoftwareItem>(dataElem)
    }

    suspend fun getSoftStats(apiKey: String, softId: Int): SoftStatsResponse = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-soft-stats") {
            put("soft_id", softId)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<SoftStatsResponse>(dataElem)
    }

    suspend fun editSoftStatus(apiKey: String, softId: Int, status: Int, reason: String? = null): SoftwareItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "edit-soft") {
            put("soft_id", softId)
            put("status", status)
            // Причина техработ meaningful только при отключении (status = 0).
            // При включении отправлять status_reason нельзя — иначе панель не поднимает софт.
            if (status == 0 && !reason.isNullOrBlank()) put("status_reason", reason)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<SoftwareItem>(dataElem)
    }

    suspend fun editSoftSettings(
        apiKey: String,
        softId: Int,
        name: String? = null,
        keyFormat: String? = null,
        hwidResetLimit: Int? = null,
        testConnectEnabled: Boolean? = null,
        templateAutoGenerate: Boolean? = null
    ): SoftwareItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "edit-soft") {
            put("soft_id", softId)
            if (!name.isNullOrBlank()) put("name", name)
            if (!keyFormat.isNullOrBlank()) put("key_format", keyFormat)
            if (hwidResetLimit != null) put("hwid_reset_limit", hwidResetLimit)
            if (testConnectEnabled != null) put("test_connect_enabled", if (testConnectEnabled) 1 else 0)
            if (templateAutoGenerate != null) put("template_auto_generate", if (templateAutoGenerate) 1 else 0)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<SoftwareItem>(dataElem)
    }


    suspend fun getKeys(
        apiKey: String,
        softId: Int,
        query: String? = null,
        status: Int? = null,
        active: Int? = null,
        game: String? = null,
        keyType: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): KeysResponse = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-keys") {
            put("soft_id", softId)
            put("limit", limit)
            put("offset", offset)
            if (!query.isNullOrBlank()) put("q", query)
            if (status != null) put("status", status)
            if (active != null) put("active", active)
            if (!game.isNullOrBlank()) put("game", game)
            if (!keyType.isNullOrBlank()) put("key_type", keyType)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<KeysResponse>(dataElem)
    }

    suspend fun getKey(apiKey: String, softId: Int, key: String): KeyItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-key") {
            put("soft_id", softId)
            put("key", key)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<KeyItem>(dataElem)
    }

    suspend fun createKeys(
        apiKey: String,
        softId: Int,
        game: String,
        duration: String,
        count: Int = 1,
        maxDevices: Int = 1,
        keyType: String = "APK",
        single: Boolean = false,
        keyFormat: String? = null,
        hwidResetLimit: Int? = null
    ): CreatedKeysResponse = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "create-keys") {
            put("soft_id", softId)
            put("game", game)
            val durInt = duration.toIntOrNull()
            if (durInt != null) {
                put("duration", durInt)
            } else {
                put("duration", duration)
            }
            put("count", count)
            put("max_devices", maxDevices)
            put("key_type", keyType)
            put("single", single)
            if (!keyFormat.isNullOrBlank()) put("key_format", keyFormat)
            if (hwidResetLimit != null) put("hwid_reset_limit", hwidResetLimit)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<CreatedKeysResponse>(dataElem)
    }

    suspend fun resetKeyHwid(apiKey: String, softId: Int, key: String): KeyItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "reset-key-hwid") {
            put("soft_id", softId)
            put("key", key)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<KeyItem>(dataElem)
    }

    suspend fun editKey(
        apiKey: String,
        softId: Int,
        key: String,
        duration: Int? = null,
        active: Boolean? = null,
        status: Boolean? = null,
        maxDevices: Int? = null,
        hwidResetLimit: Int? = null,
        keyType: String? = null
    ): KeyItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "edit-key") {
            put("soft_id", softId)
            put("key", key)
            if (duration != null) put("duration", duration)
            if (active != null) put("active", active)
            if (status != null) put("status", status)
            if (maxDevices != null) put("max_devices", maxDevices)
            if (hwidResetLimit != null) put("hwid_reset_limit", hwidResetLimit)
            if (!keyType.isNullOrBlank()) put("key_type", keyType)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<KeyItem>(dataElem)
    }

    suspend fun deleteKey(apiKey: String, softId: Int, key: String): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "delete-key") {
            put("soft_id", softId)
            put("key", key)
        }
        true
    }


    suspend fun pauseKeys(apiKey: String, softId: Int, gameFilter: String? = null): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "pause-keys") {
            put("soft_id", softId)
            if (gameFilter != null) {
                putJsonObject("filter") {
                    put("game", gameFilter)
                }
            }
        }
        true
    }

    suspend fun resumeKeys(apiKey: String, softId: Int, gameFilter: String? = null): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "resume-keys") {
            put("soft_id", softId)
            if (gameFilter != null) {
                putJsonObject("filter") {
                    put("game", gameFilter)
                }
            }
        }
        true
    }

    suspend fun extendKeys(apiKey: String, softId: Int, days: Int, gameFilter: String? = null): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "extend-keys") {
            put("soft_id", softId)
            put("days", days)
            if (gameFilter != null) {
                putJsonObject("filter") {
                    put("game", gameFilter)
                }
            }
        }
        true
    }

    suspend fun resetKeysHwid(apiKey: String, softId: Int, gameFilter: String? = null): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "reset-keys-hwid") {
            put("soft_id", softId)
            if (gameFilter != null) {
                putJsonObject("filter") {
                    put("game", gameFilter)
                }
            }
        }
        true
    }

    /**
     * Bulk delete keys by category: "expired", "blocked", "inactive", "unused", or "all"
     */
    suspend fun deleteKeysBulk(apiKey: String, softId: Int, kind: String): Int = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "delete-keys") {
            put("soft_id", softId)
            put("kind", kind)
            if (kind == "all") {
                put("confirm", "I_KNOW_WHAT_I_AM_DOING")
            }
        }
        val dataElem = unwrapData(raw)
        if (dataElem is JsonObject) {
            dataElem["deleted"]?.jsonPrimitive?.intOrNull ?: 0
        } else {
            0
        }
    }

    /**
     * Export keys in txt, csv or json format.
     */
    suspend fun exportKeys(
        apiKey: String,
        softId: Int,
        format: String = "txt",
        game: String? = null,
        active: Int? = null
    ): String = withContext(Dispatchers.IO) {
        executeAction(apiKey, "export-keys") {
            put("soft_id", softId)
            put("format", format)
            if (!game.isNullOrBlank()) put("game", game)
            if (active != null) put("active", active)
        }
    }


    suspend fun getKeyTemplates(apiKey: String, softId: Int? = null): List<KeyTemplateItem> = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-key-templates") {
            if (softId != null) put("soft_id", softId)
        }
        decodeListFromResponse<KeyTemplateItem>(raw)
    }

    suspend fun createKeyTemplate(
        apiKey: String,
        softId: Int,
        name: String,
        game: String,
        duration: Int,
        keyType: String = "APK",
        maxDevices: Int = 1,
        isSingle: Boolean = false
    ): KeyTemplateItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "create-key-template") {
            put("soft_id", softId)
            put("name", name)
            put("game", game)
            put("duration", duration)
            put("key_type", keyType)
            put("max_devices", maxDevices)
            put("is_single", isSingle)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<KeyTemplateItem>(dataElem)
    }

    suspend fun deleteKeyTemplate(apiKey: String, templateId: Int): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "delete-key-template") {
            put("template_id", templateId)
        }
        true
    }

    suspend fun getUsers(
        apiKey: String,
        query: String? = null,
        level: Int? = null,
        status: Int? = null
    ): List<UserItem> = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-users") {
            if (!query.isNullOrBlank()) put("q", query)
            if (level != null) put("level", level)
            if (status != null) put("status", status)
            put("limit", 100)
        }
        decodeListFromResponse<UserItem>(raw)
    }

    suspend fun createUser(
        apiKey: String,
        username: String,
        password: String,
        level: Int = 3,
        saldo: Int = 0,
        expirationDays: Int = 30,
        fullname: String? = null,
        email: String? = null
    ): UserItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "create-user") {
            put("username", username)
            put("password", password)
            put("level", level)
            put("saldo", saldo)
            put("expiration_days", expirationDays)
            if (!fullname.isNullOrBlank()) put("fullname", fullname)
            if (!email.isNullOrBlank()) put("email", email)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<UserItem>(dataElem)
    }

    suspend fun editUser(
        apiKey: String,
        userId: Int,
        fullname: String? = null,
        email: String? = null,
        telegram: String? = null,
        status: Int? = null,
        saldo: Int? = null,
        level: Int? = null
    ): UserItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "edit-user") {
            put("user_id", userId)
            if (fullname != null) put("fullname", fullname)
            if (email != null) put("email", email)
            if (telegram != null) put("telegram", telegram)
            if (status != null) put("status", status)
            if (saldo != null) put("saldo", saldo)
            if (level != null) put("level", level)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<UserItem>(dataElem)
    }

    suspend fun deleteUser(apiKey: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "delete-user") {
            put("user_id", userId)
        }
        true
    }

    suspend fun blockUser(apiKey: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "block-user") {
            put("user_id", userId)
        }
        true
    }

    suspend fun unblockUser(apiKey: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "unblock-user") {
            put("user_id", userId)
        }
        true
    }

    suspend fun kickUserSessions(apiKey: String, userId: Int): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "kick-user-sessions") {
            put("user_id", userId)
        }
        true
    }

    suspend fun getDomains(apiKey: String, softId: Int, endpoint: String = "main"): List<DomainItem> = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-domains") {
            put("soft_id", softId)
            put("endpoint", endpoint)
        }
        decodeListFromResponse<DomainItem>(raw)
    }

    suspend fun createDomain(apiKey: String, softId: Int, domain: String, endpoint: String = "main"): DomainItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "create-domain") {
            put("soft_id", softId)
            put("domain", domain.lowercase().trim())
            put("endpoint", endpoint)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<DomainItem>(dataElem)
    }

    suspend fun editDomain(apiKey: String, softId: Int, domainId: Int, active: Boolean): DomainItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "edit-domain") {
            put("soft_id", softId)
            put("domain_id", domainId)
            put("active", active)
        }
        val dataElem = unwrapData(raw)
        json.decodeFromJsonElement<DomainItem>(dataElem)
    }

    suspend fun deleteDomain(apiKey: String, softId: Int, domainId: Int): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "delete-domain") {
            put("soft_id", softId)
            put("domain_id", domainId)
        }
        true
    }

    // ── Keys Bulk Block / Unblock ──────────────────────────────────────────────────────

    suspend fun blockKeys(
        apiKey: String,
        softId: Int,
        gameFilter: String? = null,
        keys: List<String>? = null
    ): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "block-keys") {
            put("soft_id", softId)
            if (!keys.isNullOrEmpty()) {
                putJsonArray("keys") { keys.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
            }
            if (!gameFilter.isNullOrBlank()) {
                putJsonObject("filter") { put("game", gameFilter) }
            }
        }
        true
    }

    suspend fun unblockKeys(
        apiKey: String,
        softId: Int,
        gameFilter: String? = null,
        keys: List<String>? = null
    ): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "unblock-keys") {
            put("soft_id", softId)
            if (!keys.isNullOrEmpty()) {
                putJsonArray("keys") { keys.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
            }
            if (!gameFilter.isNullOrBlank()) {
                putJsonObject("filter") { put("game", gameFilter) }
            }
        }
        true
    }

    // ── Device Bans ────────────────────────────────────────────────────────────────────

    suspend fun getBans(
        apiKey: String,
        softId: Int,
        query: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<DeviceBanItem> = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-bans") {
            put("soft_id", softId)
            put("limit", limit)
            put("offset", offset)
            if (!query.isNullOrBlank()) put("q", query)
        }
        decodeListFromResponse<DeviceBanItem>(raw, "bans")
    }

    suspend fun banDevice(
        apiKey: String,
        softId: Int,
        serial: String,
        reason: String? = null
    ): DeviceBanItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "ban-device") {
            put("soft_id", softId)
            put("serial", serial.trim())
            if (!reason.isNullOrBlank()) put("reason", reason.trim())
        }
        val dataElem = unwrapData(raw)
        if (dataElem is JsonObject) {
            json.decodeFromJsonElement<DeviceBanItem>(dataElem)
        } else {
            DeviceBanItem(softId = softId, serial = serial.trim(), reason = reason)
        }
    }

    suspend fun unbanDevice(
        apiKey: String,
        softId: Int,
        banId: Int? = null,
        serial: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "unban-device") {
            put("soft_id", softId)
            if (banId != null && banId > 0) put("ban_id", banId)
            if (!serial.isNullOrBlank()) put("serial", serial.trim())
        }
        true
    }

    private fun JsonObject.str(vararg keys: String): String? {
        for (k in keys) {
            val v = this[k]
            if (v is JsonPrimitive && !v.isString && v.content == "null") continue
            if (v is JsonPrimitive) {
                val c = v.contentOrNull
                if (!c.isNullOrBlank()) return c
            }
        }
        return null
    }

    private fun JsonObject.bool(vararg keys: String, default: Boolean): Boolean {
        for (k in keys) {
            val v = this[k] as? JsonPrimitive ?: continue
            v.booleanOrNull?.let { return it }
            v.intOrNull?.let { return it != 0 }
            val s = v.contentOrNull?.lowercase()
            if (s == "true" || s == "1") return true
            if (s == "false" || s == "0") return false
        }
        return default
    }

    private fun JsonObject.intVal(vararg keys: String, default: Int): Int {
        for (k in keys) {
            val v = this[k] as? JsonPrimitive ?: continue
            v.intOrNull?.let { return it }
            v.contentOrNull?.toIntOrNull()?.let { return it }
        }
        return default
    }

    private fun parseConnectConfig(dataElem: JsonElement, fallbackSoftId: Int): ConnectConfig {
        val root = dataElem as? JsonObject ?: return ConnectConfig(softId = fallbackSoftId)
        val settingsObj = (root["connect"] as? JsonObject) ?: (root["settings"] as? JsonObject) ?: root
        val pageObj = (root["page"] as? JsonObject) ?: (root["connect_page"] as? JsonObject) ?: root

        val messagesMap = mutableMapOf<String, String>()
        val msgElem = root["messages"] ?: settingsObj["messages"]
        if (msgElem is JsonObject) {
            msgElem.forEach { (k, v) ->
                if (v is JsonPrimitive && v.content != "null") {
                    messagesMap[k] = v.contentOrNull.orEmpty()
                }
            }
        }

        val profilesList = mutableListOf<GameProfileItem>()
        val profElem = root["game_profiles"] ?: settingsObj["game_profiles"] ?: root["profiles"]
        if (profElem is JsonArray) {
            profElem.forEach { item ->
                if (item is JsonObject) {
                    profilesList.add(
                        GameProfileItem(
                            id = item.intVal("id", default = 0),
                            game = item.str("game").orEmpty(),
                            name = item.str("name"),
                            keyFormat = item.str("key_format"),
                            maxDevices = item.intVal("max_devices", default = -1).takeIf { it >= 0 },
                            createdAt = item.str("created_at")
                        )
                    )
                }
            }
        }

        val cipherElem = root["cipher_keys"] ?: settingsObj["cipher_keys"]
        val cipherInfo = when (cipherElem) {
            is JsonObject -> CipherKeysInfo(
                publicKey = cipherElem.str("public_key", "cipher_key"),
                privateKey = cipherElem.str("private_key"),
                cipherKey = cipherElem.str("cipher_key", "public_key"),
                rotatedAt = cipherElem.str("rotated_at")
            )
            is JsonPrimitive -> if (cipherElem.content != "null") CipherKeysInfo(publicKey = cipherElem.contentOrNull) else null
            else -> null
        }

        return ConnectConfig(
            softId = settingsObj.intVal("soft_id", "id", default = fallbackSoftId),
            enabled = settingsObj.bool("enabled", "connect_enabled", default = true),
            testConnectEnabled = settingsObj.bool("test_connect_enabled", default = false),
            connectUuid = settingsObj.str("connect_uuid", "uuid"),
            connectSecret = settingsObj.str("connect_secret", "secret"),
            sourceTemplate = settingsObj.str("source_template"),
            requireSignature = settingsObj.bool("require_signature", default = false),
            checkHwid = settingsObj.bool("check_hwid", default = true),
            maintenance = settingsObj.bool("maintenance", default = false),
            maintenanceMessage = settingsObj.str("maintenance_message"),
            pageTitle = pageObj.str("page_title", "title"),
            pageDescription = pageObj.str("page_description", "description"),
            pageEnabled = pageObj.bool("page_enabled", "enabled", default = true),
            accentColor = pageObj.str("accent_color"),
            logoUrl = pageObj.str("logo_url"),
            messages = messagesMap,
            gameProfiles = profilesList,
            cipherKeys = cipherInfo,
            publicKey = settingsObj.str("public_key", "cipher_public_key") ?: cipherInfo?.publicKey
        )
    }

    private fun parseOnlineConfig(dataElem: JsonElement, fallbackSoftId: Int): OnlineSystemConfig {
        val root = dataElem as? JsonObject ?: return OnlineSystemConfig(softId = fallbackSoftId)
        val obj = (root["online"] as? JsonObject) ?: root
        return OnlineSystemConfig(
            softId = obj.intVal("soft_id", default = fallbackSoftId),
            enabled = obj.bool("enabled", "online_enabled", default = false),
            heartbeatInterval = obj.intVal("heartbeat_interval", default = 30),
            timeoutSeconds = obj.intVal("timeout_seconds", default = 90),
            onlineCount = obj.intVal("online_count", "count", default = 0),
            maxOnline = obj.intVal("max_online", default = 0),
            showCount = obj.bool("show_count", default = false),
            message = obj.str("message")
        )
    }

    // ── Connect Settings ───────────────────────────────────────────────────────────────

    suspend fun getConnect(apiKey: String, softId: Int): ConnectConfig = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-connect") {
            put("soft_id", softId)
        }
        val dataElem = unwrapData(raw)
        parseConnectConfig(dataElem, softId)
    }

    suspend fun editConnect(
        apiKey: String,
        softId: Int,
        enabled: Boolean? = null,
        testConnectEnabled: Boolean? = null,
        connectSecret: String? = null,
        sourceTemplate: String? = null,
        requireSignature: Boolean? = null,
        checkHwid: Boolean? = null,
        maintenance: Boolean? = null,
        maintenanceMessage: String? = null
    ): ConnectConfig = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "edit-connect") {
            put("soft_id", softId)
            if (enabled != null) put("enabled", enabled)
            if (testConnectEnabled != null) put("test_connect_enabled", testConnectEnabled)
            if (connectSecret != null) put("connect_secret", connectSecret)
            if (sourceTemplate != null) put("source_template", sourceTemplate)
            if (requireSignature != null) put("require_signature", requireSignature)
            if (checkHwid != null) put("check_hwid", checkHwid)
            if (maintenance != null) put("maintenance", maintenance)
            if (maintenanceMessage != null) put("maintenance_message", maintenanceMessage)
        }
        val dataElem = unwrapData(raw)
        parseConnectConfig(dataElem, softId)
    }

    suspend fun editConnectMessages(
        apiKey: String,
        softId: Int,
        messages: Map<String, String>
    ): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "edit-connect-messages") {
            put("soft_id", softId)
            putJsonObject("messages") {
                messages.forEach { (k, v) -> put(k, v) }
            }
        }
        true
    }

    suspend fun editConnectPage(
        apiKey: String,
        softId: Int,
        pageEnabled: Boolean? = null,
        pageTitle: String? = null,
        pageDescription: String? = null,
        accentColor: String? = null,
        logoUrl: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "edit-connect-page") {
            put("soft_id", softId)
            if (pageEnabled != null) put("page_enabled", pageEnabled)
            if (pageTitle != null) put("page_title", pageTitle)
            if (pageDescription != null) put("page_description", pageDescription)
            if (accentColor != null) put("accent_color", accentColor)
            if (logoUrl != null) put("logo_url", logoUrl)
        }
        true
    }

    suspend fun createGameProfile(
        apiKey: String,
        softId: Int,
        game: String,
        name: String? = null,
        keyFormat: String? = null,
        maxDevices: Int? = null
    ): GameProfileItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "create-game-profile") {
            put("soft_id", softId)
            put("game", game.trim())
            if (!name.isNullOrBlank()) put("name", name.trim())
            if (!keyFormat.isNullOrBlank()) put("key_format", keyFormat.trim())
            if (maxDevices != null) put("max_devices", maxDevices)
        }
        val dataElem = unwrapData(raw)
        if (dataElem is JsonObject) json.decodeFromJsonElement<GameProfileItem>(dataElem) else GameProfileItem(game = game, name = name)
    }

    suspend fun deleteGameProfile(
        apiKey: String,
        softId: Int,
        profileId: Int? = null,
        game: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "delete-game-profile") {
            put("soft_id", softId)
            if (profileId != null && profileId > 0) put("profile_id", profileId)
            if (!game.isNullOrBlank()) put("game", game)
        }
        true
    }

    suspend fun rotateCipherKeys(apiKey: String, softId: Int): CipherKeysInfo = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "rotate-cipher-keys") {
            put("soft_id", softId)
        }
        val dataElem = unwrapData(raw)
        if (dataElem is JsonObject) json.decodeFromJsonElement<CipherKeysInfo>(dataElem) else CipherKeysInfo()
    }

    // ── Online System ──────────────────────────────────────────────────────────────────

    suspend fun getOnline(apiKey: String, softId: Int): OnlineSystemConfig = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-online") {
            put("soft_id", softId)
        }
        val dataElem = unwrapData(raw)
        parseOnlineConfig(dataElem, softId)
    }

    suspend fun editOnline(
        apiKey: String,
        softId: Int,
        enabled: Boolean? = null,
        heartbeatInterval: Int? = null,
        timeoutSeconds: Int? = null,
        maxOnline: Int? = null,
        showCount: Boolean? = null,
        message: String? = null
    ): OnlineSystemConfig = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "edit-online") {
            put("soft_id", softId)
            if (enabled != null) put("enabled", enabled)
            if (heartbeatInterval != null) put("heartbeat_interval", heartbeatInterval)
            if (timeoutSeconds != null) put("timeout_seconds", timeoutSeconds)
            if (maxOnline != null) put("max_online", maxOnline)
            if (showCount != null) put("show_count", showCount)
            if (message != null) put("message", message)
        }
        val dataElem = unwrapData(raw)
        parseOnlineConfig(dataElem, softId)
    }

    // ── Online Lib (Files) ─────────────────────────────────────────────────────────────

    suspend fun getFiles(apiKey: String, softId: Int): List<OnlineFileItem> = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-files") {
            put("soft_id", softId)
        }
        decodeListFromResponse<OnlineFileItem>(raw, "files")
    }

    suspend fun uploadFiles(
        apiKey: String,
        softId: Int,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String = "application/octet-stream"
    ): List<OnlineFileItem> = withContext(Dispatchers.IO) {
        val raw = executeMultipartAction(
            apiKey = apiKey,
            method = "upload-files",
            softId = softId,
            fileName = fileName,
            fileBytes = fileBytes,
            mimeType = mimeType
        )
        val decoded = decodeListFromResponse<OnlineFileItem>(raw, "files")
        if (decoded.isNotEmpty()) decoded else listOf(OnlineFileItem(softId = softId, name = fileName, filename = fileName, size = fileBytes.size.toLong()))
    }

    suspend fun replaceFile(
        apiKey: String,
        softId: Int,
        fileId: Int,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String = "application/octet-stream"
    ): OnlineFileItem = withContext(Dispatchers.IO) {
        val raw = executeMultipartAction(
            apiKey = apiKey,
            method = "replace-file",
            softId = softId,
            fileName = fileName,
            fileBytes = fileBytes,
            mimeType = mimeType,
            extraFields = mapOf("file_id" to fileId.toString())
        )
        val dataElem = unwrapData(raw)
        if (dataElem is JsonObject) {
            json.decodeFromJsonElement<OnlineFileItem>(dataElem)
        } else {
            OnlineFileItem(id = fileId, softId = softId, name = fileName, filename = fileName, size = fileBytes.size.toLong())
        }
    }

    suspend fun renameFile(
        apiKey: String,
        softId: Int,
        fileId: Int,
        newName: String
    ): OnlineFileItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "rename-file") {
            put("soft_id", softId)
            put("file_id", fileId)
            put("name", newName.trim())
            put("new_name", newName.trim())
        }
        val dataElem = unwrapData(raw)
        if (dataElem is JsonObject) {
            json.decodeFromJsonElement<OnlineFileItem>(dataElem)
        } else {
            OnlineFileItem(id = fileId, softId = softId, name = newName.trim(), filename = newName.trim())
        }
    }

    suspend fun deleteFile(apiKey: String, softId: Int, fileId: Int): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "delete-file") {
            put("soft_id", softId)
            put("file_id", fileId)
        }
        true
    }

    // ── Referral Codes ─────────────────────────────────────────────────────────────────

    suspend fun getReferralCodes(apiKey: String, softId: Int? = null): List<ReferralCodeItem> = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-referral-codes") {
            if (softId != null) put("soft_id", softId)
        }
        decodeListFromResponse<ReferralCodeItem>(raw, "codes", "referral_codes")
    }

    suspend fun createReferralCode(
        apiKey: String,
        code: String? = null,
        discount: Int = 0,
        bonusDays: Int = 0,
        maxUses: Int = 0,
        softId: Int? = null
    ): ReferralCodeItem = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "create-referral-code") {
            if (!code.isNullOrBlank()) put("code", code.trim())
            put("discount", discount)
            put("bonus_days", bonusDays)
            if (maxUses > 0) put("max_uses", maxUses)
            if (softId != null) put("soft_id", softId)
        }
        val dataElem = unwrapData(raw)
        if (dataElem is JsonObject) json.decodeFromJsonElement<ReferralCodeItem>(dataElem) else ReferralCodeItem(code = code.orEmpty(), discount = discount, bonusDays = bonusDays, maxUses = maxUses)
    }

    suspend fun deleteReferralCode(apiKey: String, codeId: Int): Boolean = withContext(Dispatchers.IO) {
        executeAction(apiKey, "delete-referral-code") {
            put("code_id", codeId)
        }
        true
    }

    // ── Logs & Statistics ──────────────────────────────────────────────────────────────

    suspend fun getLogs(
        apiKey: String,
        softId: Int? = null,
        action: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<AuditLogItem> = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-logs") {
            if (softId != null) put("soft_id", softId)
            if (!action.isNullOrBlank()) put("action_filter", action)
            put("limit", limit)
            put("offset", offset)
        }
        decodeListFromResponse<AuditLogItem>(raw, "logs")
    }

    suspend fun getConnectLogins(
        apiKey: String,
        softId: Int,
        key: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<ConnectLoginItem> = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-connect-logins") {
            put("soft_id", softId)
            if (!key.isNullOrBlank()) put("key", key.trim())
            put("limit", limit)
            put("offset", offset)
        }
        decodeListFromResponse<ConnectLoginItem>(raw, "logins")
    }

    suspend fun getClients(
        apiKey: String,
        softId: Int? = null
    ): List<ClientCountryStat> = withContext(Dispatchers.IO) {
        val raw = executeAction(apiKey, "get-clients") {
            if (softId != null) put("soft_id", softId)
        }
        decodeListFromResponse<ClientCountryStat>(raw, "countries", "clients")
    }
}
