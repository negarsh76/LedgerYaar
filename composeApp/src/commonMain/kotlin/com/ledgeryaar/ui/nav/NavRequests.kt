package com.ledgeryaar.ui.nav

import kotlinx.coroutines.flow.MutableStateFlow

/** درخواست باز کردن یک صفحه از بیرون اپ (مثلاً با لمس نوتیف تراکنش جدید) */
object NavRequests {
    val pendingRoute = MutableStateFlow<String?>(null)
}
