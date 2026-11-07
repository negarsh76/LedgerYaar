package com.ledgeryaar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ledgeryaar.autoimport.TxNotifications
import com.ledgeryaar.platform.AndroidPlatformBridge
import com.ledgeryaar.platform.PlatformBridge
import com.ledgeryaar.ui.nav.NavRequests
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val bridge: PlatformBridge by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (bridge as? AndroidPlatformBridge)?.let {
            it.attach(this)
            it.requestInitialPermissionsOnce()
        }
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            LedgerYaarApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // بعد از برگشتن از تنظیمات گوشی، وضعیت دسترسی‌ها تازه شود
        bridge.refreshPermissions()
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(TxNotifications.EXTRA_OPEN_ROUTE)?.let {
            NavRequests.pendingRoute.value = it
            intent.removeExtra(TxNotifications.EXTRA_OPEN_ROUTE)
        }
    }
}
