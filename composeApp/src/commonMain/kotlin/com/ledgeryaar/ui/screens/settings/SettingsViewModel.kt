package com.ledgeryaar.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledgeryaar.data.backup.BackupService
import com.ledgeryaar.data.repository.AppSettings
import com.ledgeryaar.data.repository.AppTheme
import com.ledgeryaar.data.repository.CurrencyOption
import com.ledgeryaar.data.repository.SettingsRepository
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.i18n.AppLanguage
import com.ledgeryaar.platform.PermissionState
import com.ledgeryaar.platform.PlatformBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val platform: PlatformBridge,
    private val backup: BackupService
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    val permissions: StateFlow<PermissionState> = platform.permissions

    /** پیام کوتاه نتیجهٔ پشتیبان/بازیابی برای نمایش زیر دکمه‌ها */
    val backupMessage = MutableStateFlow<String?>(null)

    fun setLanguage(language: AppLanguage) { viewModelScope.launch { repo.setLanguage(language) } }
    fun setDarkTheme(dark: Boolean) { viewModelScope.launch { repo.setDarkTheme(dark) } }
    fun setTheme(theme: AppTheme) { viewModelScope.launch { repo.setTheme(theme) } }
    fun setCurrency(currency: CurrencyOption) { viewModelScope.launch { repo.setCurrency(currency) } }
    fun setInputUnit(unit: Long) { viewModelScope.launch { repo.setInputUnit(unit) } }
    fun addCategory(name: String, type: TxType) { viewModelScope.launch { repo.addCategory(name, type) } }
    fun renameCategory(id: Long, name: String) { viewModelScope.launch { repo.renameCategory(id, name) } }
    fun deleteCategory(id: Long) { viewModelScope.launch { repo.deleteCategory(id) } }

    fun setAutoImport(enabled: Boolean) { viewModelScope.launch { repo.setAutoImport(enabled) } }
    fun setBankDefaultRial(rial: Boolean) { viewModelScope.launch { repo.setBankDefaultRial(rial) } }
    fun forgetRule(keyword: String) { viewModelScope.launch { repo.forgetRule(keyword) } }
    fun setBackupEmail(email: String) { viewModelScope.launch { repo.setBackupEmail(email) } }

    fun refreshPermissions() = platform.refreshPermissions()
    fun requestSms() = platform.requestSmsPermission()
    fun openNotificationAccess() = platform.openNotificationListenerSettings()
    fun requestPostNotifications() = platform.requestPostNotifications()

    fun sendBackup(email: String, subject: String, body: String) {
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val content = backup.export(now)
            repo.setBackupEmail(email)
            platform.sendBackupByEmail(email, "ledgeryaar-backup-$now.json", content, subject, body)
            repo.setLastBackupAt(now)
        }
    }

    fun restoreFromFile(ok: (Int) -> String, failed: String) {
        platform.pickBackupFile { content ->
            if (content == null) return@pickBackupFile
            viewModelScope.launch {
                backupMessage.value = runCatching { backup.restore(content) }
                    .fold(onSuccess = { ok(it) }, onFailure = { failed })
            }
        }
    }
}
