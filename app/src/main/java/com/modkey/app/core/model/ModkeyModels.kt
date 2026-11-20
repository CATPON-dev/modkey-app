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

@Serializable
data class DeviceBanItem(
    val id: Int = 0,
    @SerialName("soft_id") val softId: Int = 0,
    val serial: String = "",
    val hwid: String? = null,
    val reason: String? = null,
    val key: String? = null,
    @SerialName("user_key") val userKey: String? = null,
    @SerialName("banned_by") val bannedBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null
) {
    val deviceSerial: String get() = serial.ifBlank { hwid.orEmpty() }
}

@Serializable
data class GameProfileItem(
    val id: Int = 0,
    val game: String = "",
    val name: String? = null,
    @SerialName("key_format") val keyFormat: String? = null,
    @SerialName("max_devices") val maxDevices: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CipherKeysInfo(
    @SerialName("public_key") val publicKey: String? = null,
    @SerialName("private_key") val privateKey: String? = null,
    @SerialName("cipher_key") val cipherKey: String? = null,
    @SerialName("rotated_at") val rotatedAt: String? = null
)

@Serializable
data class ConnectConfig(
    @SerialName("soft_id") val softId: Int = 0,
    val enabled: Boolean = true,
    @SerialName("test_connect_enabled") val testConnectEnabled: Boolean = false,
    @SerialName("connect_uuid") val connectUuid: String? = null,
    @SerialName("connect_secret") val connectSecret: String? = null,
    @SerialName("source_template") val sourceTemplate: String? = null,
    @SerialName("require_signature") val requireSignature: Boolean = false,
    @SerialName("check_hwid") val checkHwid: Boolean = true,
    @SerialName("maintenance") val maintenance: Boolean = false,
    @SerialName("maintenance_message") val maintenanceMessage: String? = null,
    @SerialName("page_title") val pageTitle: String? = null,
    @SerialName("page_description") val pageDescription: String? = null,
    @SerialName("page_enabled") val pageEnabled: Boolean = true,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
    val messages: Map<String, String> = emptyMap(),
    @SerialName("game_profiles") val gameProfiles: List<GameProfileItem> = emptyList(),
    @SerialName("cipher_keys") val cipherKeys: CipherKeysInfo? = null,
    @SerialName("public_key") val publicKey: String? = null
)

@Serializable
data class OnlineSystemConfig(
    @SerialName("soft_id") val softId: Int = 0,
    val enabled: Boolean = false,
    @SerialName("heartbeat_interval") val heartbeatInterval: Int = 30,
    @SerialName("timeout_seconds") val timeoutSeconds: Int = 90,
    @SerialName("online_count") val onlineCount: Int = 0,
    @SerialName("max_online") val maxOnline: Int = 0,
    @SerialName("show_count") val showCount: Boolean = false,
    val message: String? = null
)

@Serializable
data class OnlineFileItem(
    val id: Int = 0,
    @SerialName("soft_id") val softId: Int = 0,
    val name: String = "",
    val filename: String = "",
    val size: Long = 0,
    @SerialName("mime_type") val mimeType: String? = null,
    val url: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
    val sha256: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val displayName: String get() = name.ifBlank { filename.ifBlank { "file_$id" } }
}

@Serializable
data class ReferralCodeItem(
    val id: Int = 0,
    val code: String = "",
    val discount: Int = 0,
    @SerialName("bonus_days") val bonusDays: Int = 0,
    @SerialName("max_uses") val maxUses: Int = 0,
    @SerialName("uses_count") val usesCount: Int = 0,
    @SerialName("used_count") val usedCount: Int = 0,
    val active: Boolean = true,
    @SerialName("soft_id") val softId: Int? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val totalUsed: Int get() = if (usesCount > 0) usesCount else usedCount
}

@Serializable
data class AuditLogItem(
    val id: Int = 0,
    val action: String = "",
    val method: String? = null,
    val actor: String? = null,
    val username: String? = null,
    val ip: String? = null,
    val details: String? = null,
    val message: String? = null,
    @SerialName("soft_id") val softId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val titleText: String get() = action.ifBlank { method ?: "event" }
    val actorText: String get() = actor ?: username ?: "system"
}

@Serializable
data class ConnectLoginItem(
    val id: Int = 0,
    val key: String = "",
    @SerialName("user_key") val userKey: String? = null,
    val serial: String? = null,
    val hwid: String? = null,
    val ip: String? = null,
    val country: String? = null,
    val game: String? = null,
    val status: String? = null,
    val ok: Boolean? = null,
    val message: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val keyText: String get() = key.ifBlank { userKey.orEmpty() }
    val deviceText: String get() = serial ?: hwid.orEmpty()
}

@Serializable
data class ClientCountryStat(
    val country: String = "",
    @SerialName("country_code") val countryCode: String? = null,
    val count: Int = 0,
    val total: Int = 0,
    val active: Int? = null,
    val percent: Double? = null
) {
    val clientsCount: Int get() = if (count > 0) count else total
}
