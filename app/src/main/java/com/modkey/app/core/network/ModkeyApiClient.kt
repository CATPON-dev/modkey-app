package com.modkey.app.core.network

import android.util.Log
import com.modkey.app.core.model.AccountMeInfo
import com.modkey.app.core.model.CreatedKeysResponse
import com.modkey.app.core.model.DomainItem
import com.modkey.app.core.model.KeyItem
import com.modkey.app.core.model.KeyTemplateItem
import com.modkey.app.core.model.KeysResponse
import com.modkey.app.core.model.OverviewStats
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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
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

    private inline fun <reified T> decodeListFromResponse(rawJson: String): List<T> {
        val element = unwrapData(rawJson)
        return when {
            element is JsonArray -> json.decodeFromJsonElement<List<T>>(element)
            element is JsonObject && element.containsKey("items") -> json.decodeFromJsonElement<List<T>>(element["items"]!!)
            else -> emptyList()
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
}
