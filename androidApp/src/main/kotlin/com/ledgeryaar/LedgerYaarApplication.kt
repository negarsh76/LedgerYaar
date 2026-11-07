package com.ledgeryaar

import android.app.Application
import com.ledgeryaar.autoimport.DailyReminderReceiver
import com.ledgeryaar.autoimport.TxNotifications
import com.ledgeryaar.data.repository.SettingsRepository
import com.ledgeryaar.data.repository.createSettingsRepository
import com.ledgeryaar.di.appModules
import com.ledgeryaar.platform.AndroidPlatformBridge
import com.ledgeryaar.platform.PlatformBridge
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class LedgerYaarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LedgerYaarApplication)
            modules(
                appModules + listOf(
                    module {
                        // زمینه‌ی اندروید برای createDatabase(context: Any) در dataModule
                        single<Any> { androidContext() }
                        single<SettingsRepository> { createSettingsRepository(androidContext()) }
                        single<PlatformBridge> { AndroidPlatformBridge(androidContext()) }
                    }
                )
            )
        }
        TxNotifications.ensureChannels(this)
        DailyReminderReceiver.schedule(this)
    }
}
