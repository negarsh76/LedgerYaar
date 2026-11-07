package com.ledgeryaar.platform

import kotlinx.coroutines.flow.StateFlow

/** وضعیت دسترسی‌های گوشی برای ورود خودکار */
data class PermissionState(
    /** آیا این پلتفرم اصلاً پیامک را می‌تواند بخواند (iOS نمی‌تواند) */
    val smsSupported: Boolean = false,
    val smsGranted: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val postNotificationsGranted: Boolean = false
)

/**
 * کارهایی که به سیستم‌عامل وابسته‌اند (پیامک، نوتیف، ایمیل، فایل).
 * پیاده‌سازی اندروید در androidMain است؛ روی iOS بیشترشان در دسترس نیستند.
 */
interface PlatformBridge {
    val permissions: StateFlow<PermissionState>
    fun refreshPermissions()

    fun requestSmsPermission()
    fun openNotificationListenerSettings()
    fun requestPostNotifications()

    /** خواندن پیامک‌های بانکی چند روز گذشته؛ تعداد تراکنش‌های جدید را برمی‌گرداند */
    suspend fun scanSmsInbox(days: Int): Int

    /** باز کردن برنامهٔ ایمیل با فایل پشتیبان پیوست‌شده */
    fun sendBackupByEmail(email: String, fileName: String, content: String, subject: String, body: String)

    /** انتخاب فایل پشتیبان برای بازیابی؛ محتوای فایل به callback داده می‌شود (یا null اگر لغو شد) */
    fun pickBackupFile(onResult: (String?) -> Unit)
}

/** پیاده‌سازی خالی برای پلتفرم‌هایی که این امکانات را ندارند */
class NoopPlatformBridge : PlatformBridge {
    override val permissions: StateFlow<PermissionState> =
        kotlinx.coroutines.flow.MutableStateFlow(PermissionState())
    override fun refreshPermissions() {}
    override fun requestSmsPermission() {}
    override fun openNotificationListenerSettings() {}
    override fun requestPostNotifications() {}
    override suspend fun scanSmsInbox(days: Int): Int = 0
    override fun sendBackupByEmail(email: String, fileName: String, content: String, subject: String, body: String) {}
    override fun pickBackupFile(onResult: (String?) -> Unit) { onResult(null) }
}
