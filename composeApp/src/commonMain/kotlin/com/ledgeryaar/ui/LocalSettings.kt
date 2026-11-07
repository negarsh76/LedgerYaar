package com.ledgeryaar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.ledgeryaar.domain.model.CustomCategory
import com.ledgeryaar.domain.model.builtinCategory
import com.ledgeryaar.i18n.LocalStrings
import com.ledgeryaar.i18n.t

/** ضریب پیش‌فرض ورود مبلغ (۱ / هزار / میلیون / میلیارد) */
val LocalInputUnit = compositionLocalOf { 1L }

/** دسته‌بندی‌های دلخواه کاربر */
val LocalCustomCategories = compositionLocalOf<List<CustomCategory>> { emptyList() }

/** نام نمایشی یک دسته (پیش‌فرض یا دلخواه) */
@Composable
fun categoryName(key: String): String {
    val strings = LocalStrings.current
    val custom = LocalCustomCategories.current
    builtinCategory(key)?.let { return strings.categoryLabelFor(it) }
    custom.firstOrNull { it.key == key }?.let { return it.name }
    return strings.t("(دستهٔ حذف‌شده)", "(deleted category)")
}
