package com.ledgeryaar.di

import com.ledgeryaar.data.db.LedgerDatabase

expect fun createDatabase(context: Any): LedgerDatabase
