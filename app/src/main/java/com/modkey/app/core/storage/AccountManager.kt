package com.modkey.app.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.modkey.app.core.model.SavedAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "modkey_accounts_prefs")

class AccountManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    companion object {
        private val KEY_ACCOUNTS_JSON = stringPreferencesKey("saved_accounts_list")
        private val KEY_ACTIVE_ACCOUNT_ID = stringPreferencesKey("active_account_id")
        private val KEY_FIRST_LAUNCH_SHOWN = androidx.datastore.preferences.core.booleanPreferencesKey("first_launch_author_shown")
    }

    val isFirstLaunchAuthorShownFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIRST_LAUNCH_SHOWN] ?: false
    }

    suspend fun setFirstLaunchAuthorShown() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH_SHOWN] = true
        }
    }

    val accountsFlow: Flow<List<SavedAccount>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_ACCOUNTS_JSON] ?: return@map emptyList()
        try {
            json.decodeFromString<List<SavedAccount>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    val activeAccountFlow: Flow<SavedAccount?> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_ACCOUNTS_JSON] ?: return@map null
        val activeId = prefs[KEY_ACTIVE_ACCOUNT_ID]
        try {
            val list = json.decodeFromString<List<SavedAccount>>(raw)
            if (activeId != null) {
                list.find { it.id == activeId } ?: list.firstOrNull()
            } else {
                list.firstOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveOrUpdateAccount(account: SavedAccount) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_ACCOUNTS_JSON]
            val currentList = try {
                if (raw != null) json.decodeFromString<List<SavedAccount>>(raw) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            val updatedList = currentList.filter { it.id != account.id && it.apiKey != account.apiKey } + account
            prefs[KEY_ACCOUNTS_JSON] = json.encodeToString(updatedList)
            prefs[KEY_ACTIVE_ACCOUNT_ID] = account.id
        }
    }

    suspend fun selectAccount(accountId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_ACCOUNT_ID] = accountId
        }
    }

    suspend fun removeAccount(accountId: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_ACCOUNTS_JSON] ?: return@edit
            try {
                val list = json.decodeFromString<List<SavedAccount>>(raw)
                val filtered = list.filter { it.id != accountId }
                prefs[KEY_ACCOUNTS_JSON] = json.encodeToString(filtered)

                if (prefs[KEY_ACTIVE_ACCOUNT_ID] == accountId) {
                    prefs[KEY_ACTIVE_ACCOUNT_ID] = filtered.firstOrNull()?.id ?: ""
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun getActiveAccount(): SavedAccount? {
        return activeAccountFlow.first()
    }
}
