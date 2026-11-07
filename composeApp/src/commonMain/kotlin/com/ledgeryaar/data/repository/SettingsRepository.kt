package com.ledgeryaar.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ledgeryaar.domain.model.CustomCategory
import com.ledgeryaar.domain.model.TxType
import com.ledgeryaar.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** واحد پولی که کنار همه‌ی مبلغ‌ها نمایش داده می‌شود */
enum class CurrencyOption(val fa: String, val en: String) {
    TOMAN("تومان", "Toman"),
    RIAL("ریال", "Rial"),
    YUAN("یوان", "Yuan"),
    USD("دلار", "USD"),
    EUR("یورو", "EUR"),
    AZN("منات", "AZN")
}

/** تم ظاهری اپ */
enum class AppTheme { LIGHT, DARK, FOREST }

data class AppSettings(
    val language: AppLanguage = AppLanguage.FA,
    val darkTheme: Boolean = false,
    val currency: CurrencyOption = CurrencyOption.TOMAN,
    /** ضریب پیش‌فرض ورود مبلغ: 1 / 1,000 / 1,000,000 / 1,000,000,000 */
    val inputUnit: Long = 1L,
    val customCategories: List<CustomCategory> = emptyList(),
    val theme: AppTheme = AppTheme.LIGHT,
    /** خواندن خودکار پیامک‌ها و نوتیف‌های بانکی */
    val autoImport: Boolean = true,
    /** اگر واحد در متن پیامک نیامده بود، ریال فرض شود (بیشتر بانک‌های ایران) */
    val bankDefaultRial: Boolean = true,
    /** ایمیلی که پشتیبان‌ها به آن فرستاده می‌شود */
    val backupEmail: String = "",
    /** آخرین زمان پشتیبان‌گیری (میلی‌ثانیه) */
    val lastBackupAt: Long = 0,
    /** کلمه → کلید دسته؛ از اصلاح‌های خودت یاد گرفته می‌شود */
    val learnedRules: Map<String, String> = emptyMap()
)

private val KEY_LANGUAGE = stringPreferencesKey("language")
private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
private val KEY_CURRENCY = stringPreferencesKey("currency")
private val KEY_INPUT_UNIT = longPreferencesKey("input_unit")
private val KEY_CATEGORIES = stringPreferencesKey("custom_categories")
private val KEY_NEXT_CATEGORY_ID = longPreferencesKey("next_category_id")
private val KEY_THEME = stringPreferencesKey("theme")
private val KEY_AUTO_IMPORT = booleanPreferencesKey("auto_import")
private val KEY_BANK_RIAL = booleanPreferencesKey("bank_default_rial")
private val KEY_BACKUP_EMAIL = stringPreferencesKey("backup_email")
private val KEY_LAST_BACKUP = longPreferencesKey("last_backup_at")
private val KEY_RULES = stringPreferencesKey("learned_rules")

private fun parseRules(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    return raw.split("\n").mapNotNull { line ->
        val i = line.lastIndexOf('|')
        if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
    }.toMap()
}

private fun serializeRules(map: Map<String, String>): String =
    map.entries.joinToString("\n") { "${it.key.replace("|", " ").replace("\n", " ")}|${it.value}" }

private const val FIELD_SEP = "|"
private const val LINE_SEP = "\n"

private fun parseCategories(raw: String?): List<CustomCategory> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(LINE_SEP).mapNotNull { line ->
        val parts = line.split(FIELD_SEP, limit = 3)
        if (parts.size < 3) return@mapNotNull null
        val id = parts[0].toLongOrNull() ?: return@mapNotNull null
        val type = runCatching { TxType.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
        CustomCategory(id, parts[2], type)
    }
}

private fun serializeCategories(list: List<CustomCategory>): String =
    list.joinToString(LINE_SEP) { "${it.id}$FIELD_SEP${it.type.name}$FIELD_SEP${it.name}" }

private fun cleanName(name: String): String =
    name.replace(FIELD_SEP, " ").replace(LINE_SEP, " ").trim()

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            language = prefs[KEY_LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.FA,
            darkTheme = prefs[KEY_DARK_THEME] ?: false,
            currency = prefs[KEY_CURRENCY]?.let { runCatching { CurrencyOption.valueOf(it) }.getOrNull() } ?: CurrencyOption.TOMAN,
            inputUnit = prefs[KEY_INPUT_UNIT] ?: 1L,
            customCategories = parseCategories(prefs[KEY_CATEGORIES]),
            theme = prefs[KEY_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                ?: if (prefs[KEY_DARK_THEME] == true) AppTheme.DARK else AppTheme.LIGHT,
            autoImport = prefs[KEY_AUTO_IMPORT] ?: true,
            bankDefaultRial = prefs[KEY_BANK_RIAL] ?: true,
            backupEmail = prefs[KEY_BACKUP_EMAIL] ?: "",
            lastBackupAt = prefs[KEY_LAST_BACKUP] ?: 0L,
            learnedRules = parseRules(prefs[KEY_RULES])
        ).let { it.copy(darkTheme = it.theme != AppTheme.LIGHT) }
    }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit {
            it[KEY_THEME] = theme.name
            it[KEY_DARK_THEME] = theme != AppTheme.LIGHT
        }
    }

    suspend fun setAutoImport(enabled: Boolean) { dataStore.edit { it[KEY_AUTO_IMPORT] = enabled } }
    suspend fun setBankDefaultRial(rial: Boolean) { dataStore.edit { it[KEY_BANK_RIAL] = rial } }
    suspend fun setBackupEmail(email: String) { dataStore.edit { it[KEY_BACKUP_EMAIL] = email.trim() } }
    suspend fun setLastBackupAt(time: Long) { dataStore.edit { it[KEY_LAST_BACKUP] = time } }

    /** یادگرفتن: دفعهٔ بعد هر پیامکی که این کلمه را داشت، همین دسته پیشنهاد شود */
    suspend fun learnRule(keyword: String, categoryKey: String) {
        if (keyword.isBlank()) return
        dataStore.edit { prefs ->
            val map = parseRules(prefs[KEY_RULES]).toMutableMap()
            map[keyword] = categoryKey
            prefs[KEY_RULES] = serializeRules(map)
        }
    }

    suspend fun forgetRule(keyword: String) {
        dataStore.edit { prefs ->
            prefs[KEY_RULES] = serializeRules(parseRules(prefs[KEY_RULES]) - keyword)
        }
    }

    /** برای بازیابی پشتیبان */
    suspend fun restoreRaw(categoriesRaw: String, nextCategoryId: Long, rulesRaw: String) {
        dataStore.edit {
            it[KEY_CATEGORIES] = categoriesRaw
            it[KEY_NEXT_CATEGORY_ID] = nextCategoryId
            it[KEY_RULES] = rulesRaw
        }
    }

    suspend fun exportRaw(): Triple<String, Long, String> {
        val prefs = dataStore.data.first()
        return Triple(prefs[KEY_CATEGORIES] ?: "", prefs[KEY_NEXT_CATEGORY_ID] ?: 1L, prefs[KEY_RULES] ?: "")
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[KEY_LANGUAGE] = language.name }
    }

    suspend fun setDarkTheme(dark: Boolean) = setTheme(if (dark) AppTheme.DARK else AppTheme.LIGHT)

    suspend fun setCurrency(currency: CurrencyOption) {
        dataStore.edit { it[KEY_CURRENCY] = currency.name }
    }

    suspend fun setInputUnit(unit: Long) {
        dataStore.edit { it[KEY_INPUT_UNIT] = unit }
    }

    suspend fun addCategory(name: String, type: TxType) {
        val clean = cleanName(name)
        if (clean.isEmpty()) return
        dataStore.edit { prefs ->
            val list = parseCategories(prefs[KEY_CATEGORIES])
            val nextId = prefs[KEY_NEXT_CATEGORY_ID] ?: ((list.maxOfOrNull { it.id } ?: 0L) + 1L)
            val duplicate = list.any { it.type == type && it.name == clean }
            if (!duplicate) {
                prefs[KEY_CATEGORIES] = serializeCategories(list + CustomCategory(nextId, clean, type))
                prefs[KEY_NEXT_CATEGORY_ID] = nextId + 1L
            }
        }
    }

    suspend fun renameCategory(id: Long, name: String) {
        val clean = cleanName(name)
        if (clean.isEmpty()) return
        dataStore.edit { prefs ->
            val list = parseCategories(prefs[KEY_CATEGORIES])
            prefs[KEY_CATEGORIES] = serializeCategories(list.map { if (it.id == id) it.copy(name = clean) else it })
        }
    }

    suspend fun deleteCategory(id: Long) {
        dataStore.edit { prefs ->
            val list = parseCategories(prefs[KEY_CATEGORIES])
            prefs[KEY_CATEGORIES] = serializeCategories(list.filter { it.id != id })
        }
    }
}

// نکته: ساخت SettingsRepository به‌صورت جدا در androidMain/iosMain انجام می‌شود
// (چون امضای موردنیاز هر پلتفرم فرق دارد: اندروید Context می‌خواهد، iOS نه)
