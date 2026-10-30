package com.modkey.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SavedAccount(
    val id: String,
    val label: String,
    val apiKey: String,
    val username: String? = null,
    val telegram: String? = null,
    val lastActive: Long = System.currentTimeMillis()
)

@Serializable
data class AccountMeInfo(
    val id: Int = 0,
    val username: String = "",
    val balance: String = "0.00",
    val status: String = "1",
    val level: String = "1",
    @SerialName("telegram_id") val telegramId: String? = null
)

@Serializable
data class OverviewStats(
    val scope: String? = null,
    @SerialName("softs_count") val softsCount: Int = 0,
    @SerialName("keys_total") val keysTotal: Int = 0,
    @SerialName("keys_active") val keysActive: Int = 0,
    @SerialName("custom_domains") val customDomains: Int = 0,
    @SerialName("users_count") val usersCount: Int = 0
)

@Serializable
data class SoftStatsResponse(
    val soft: SoftBrief? = null,
    val keys: SoftKeyCounts? = null,
    @SerialName("custom_domains_count") val customDomainsCount: Int = 0
)

@Serializable
data class SoftBrief(
    val id: Int,
    val name: String
)

@Serializable
data class SoftKeyCounts(
    val total: Int = 0,
    val active: Int = 0,
    val paused: Int = 0,
    val blocked: Int = 0,
    val expired: Int = 0
)

@Serializable
data class SoftwareItem(
    val id: Int,
    val name: String,
    val status: Int = 1,
    @SerialName("status_reason") val statusReason: String? = null,
    @SerialName("key_format") val keyFormat: String? = null,
    val uplink: String? = null,
    val price: Map<String, String>? = null,
    @SerialName("hwid_reset_limit") val hwidResetLimit: Int = 0,
    @SerialName("template_auto_generate") val templateAutoGenerate: Boolean = false,
    @SerialName("test_connect_enabled") val testConnectEnabled: Boolean = false,
    @SerialName("connect_uuid") val connectUuid: String? = null
)

@Serializable
data class KeyItem(
    val id: Int = 0,
    val key: String,
    val game: String? = null,
    @SerialName("soft_id") val softId: Int = 0,
    val duration: String? = null,
    @SerialName("expired_date") val expiredDate: String? = null,
    @SerialName("max_devices") val maxDevices: Int = 1,
    @SerialName("devices_used") val devicesUsed: Int = 0,
    val status: Int = 1, // 1 normal, 0 blocked
    val active: Int = 1, // 1 active, 0 paused
    val single: Boolean = false,
    @SerialName("hwid_reset_limit") val hwidResetLimit: Int = 0,
    @SerialName("hwid_resets_used") val hwidResetsUsed: Int = 0,
    val registrator: String? = null,
    @SerialName("key_type") val keyType: String = "APK",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val isBlocked: Boolean get() = status == 0
    val isPaused: Boolean get() = active == 0
    val isOnline: Boolean get() = status == 1 && active == 1
}

@Serializable
data class KeysResponse(
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
    val items: List<KeyItem> = emptyList()
)

@Serializable
data class CreatedKeysResponse(
    val count: Int = 0,
    val items: List<KeyItem> = emptyList()
)

@Serializable
data class KeyTemplateItem(
    val id: Int,
    val name: String,
    val game: String,
    val duration: Int,
    @SerialName("key_type") val keyType: String? = "APK",
    @SerialName("max_devices") val maxDevices: Int = 1,
    @SerialName("is_single") val isSingle: Boolean = false,
    @SerialName("use_count") val useCount: Int = 0
)

@Serializable
data class UserItem(
    val id: Int,
    val username: String,
    val level: Int = 2, // 2 Admin, 3 Reseller
    @SerialName("level_label") val levelLabel: String? = null,
    val status: Int = 1,
    val saldo: Int = 0,
    @SerialName("expiration_date") val expirationDate: String? = null,
    val email: String? = null,
    val telegram: String? = null,
    val fullname: String? = null
)

@Serializable
data class DomainItem(
    val id: Int,
    @SerialName("soft_id") val softId: Int = 0,
    val domain: String,
    val active: Boolean = true,
    val provisioned: Boolean = true,
    val endpoint: String = "main",
    @SerialName("public_url") val publicUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class ApiResponseWrapper<T>(
    val ok: Boolean? = null,
    val error: String? = null,
    val message: String? = null,
    val data: T? = null
)
