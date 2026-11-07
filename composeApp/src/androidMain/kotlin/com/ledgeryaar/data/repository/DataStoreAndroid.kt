package com.ledgeryaar.data.repository

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore by preferencesDataStore(name = "ledgeryaar_settings")

fun createSettingsRepository(context: Context): SettingsRepository {
    return SettingsRepository(context.dataStore)
}
