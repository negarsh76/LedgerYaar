package com.ledgeryaar

import androidx.compose.ui.window.ComposeUIViewController
import com.ledgeryaar.data.repository.SettingsRepository
import com.ledgeryaar.data.repository.createSettingsRepository
import com.ledgeryaar.di.appModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController {
        LedgerYaarApp()
    }
}

private var koinStarted = false

fun initKoin() {
    if (koinStarted) return
    koinStarted = true
    startKoin {
        modules(
            appModules + listOf(
                module {
                    single<SettingsRepository> { createSettingsRepository() }
                    // iOS نیاز به Context ندارد — یک مقدار ساختگی برای createDatabase(context: Any) پاس می‌دهیم
                    single<Any> { Unit }
                    // پیامک و نوتیف بانک روی iOS قابل خواندن نیست
                    single<com.ledgeryaar.platform.PlatformBridge> { com.ledgeryaar.platform.NoopPlatformBridge() }
                }
            )
        )
    }
}
