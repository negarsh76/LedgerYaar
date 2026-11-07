package com.ledgeryaar.di

import com.ledgeryaar.data.db.LedgerDatabase
import com.ledgeryaar.data.repository.LedgerRepository
import com.ledgeryaar.data.repository.LedgerRepositoryImpl
import com.ledgeryaar.domain.period.PeriodHolder
import com.ledgeryaar.domain.usecase.LedgerActions
import com.ledgeryaar.domain.usecase.ObserveLedgerDataUseCase
import com.ledgeryaar.ui.LedgerViewModel
import com.ledgeryaar.ui.screens.settings.SettingsViewModel
import com.ledgeryaar.data.backup.BackupService
import com.ledgeryaar.domain.autoimport.AutoImporter
import com.ledgeryaar.ui.screens.review.ReviewViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dataModule = module {
    single<LedgerDatabase> { createDatabase(get()) }
    single<LedgerRepository> { LedgerRepositoryImpl(get()) }
    single { AutoImporter(get(), get()) }
    single { BackupService(get(), get()) }
}

val domainModule = module {
    factory { ObserveLedgerDataUseCase(get()) }
    factory { LedgerActions(get(), get()) }
    // ماه/سال انتخاب‌شده، مشترک بین همه‌ی صفحه‌ها
    single { PeriodHolder() }
}

val viewModelModule = module {
    viewModelOf(::LedgerViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ReviewViewModel)
}

val appModules = listOf(dataModule, domainModule, viewModelModule)
