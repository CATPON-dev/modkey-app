package com.modkey.app

import android.app.Application
import com.modkey.app.core.network.ModkeyApiClient
import com.modkey.app.core.storage.AccountManager

class ModkeyApp : Application() {

    lateinit var apiClient: ModkeyApiClient
        private set

    lateinit var accountManager: AccountManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        apiClient = ModkeyApiClient()
        accountManager = AccountManager(this)
    }

    companion object {
        lateinit var instance: ModkeyApp
            private set
    }
}
