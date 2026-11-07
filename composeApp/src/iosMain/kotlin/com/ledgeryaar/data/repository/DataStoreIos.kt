package com.ledgeryaar.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun createSettingsRepository(): SettingsRepository {
    val docDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )!!.path!!

    val dataStore = PreferenceDataStoreFactory.createWithPath {
        "$docDir/ledgeryaar_settings.preferences_pb".toPath()
    }
    return SettingsRepository(dataStore)
}
