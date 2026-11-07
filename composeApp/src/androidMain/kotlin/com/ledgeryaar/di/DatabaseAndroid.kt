package com.ledgeryaar.di

import android.content.Context
import androidx.room.Room
import com.ledgeryaar.data.db.LedgerDatabase

actual fun createDatabase(context: Any): LedgerDatabase {
    val ctx = context as Context
    return Room.databaseBuilder(
        ctx,
        LedgerDatabase::class.java,
        "ledgeryaar.db"
    ).build()
}
