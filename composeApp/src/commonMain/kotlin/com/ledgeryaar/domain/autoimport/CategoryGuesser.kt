package com.ledgeryaar.domain.autoimport

import com.ledgeryaar.domain.model.CategoryId
import com.ledgeryaar.domain.model.TxType

/**
 * حدس دسته از روی متن پیامک / توضیح.
 * اول قانون‌هایی که خودت با اصلاح دسته‌ها یاد داده‌ای (learned) بررسی می‌شوند،
 * بعد فهرست کلمه‌های آماده.
 */
object CategoryGuesser {

    private val BUILTIN: List<Pair<List<String>, CategoryId>> = listOf(
        listOf("اسنپ فود", "اسنپفود", "snappfood", "رستوران", "کافه", "فست فود", "پیتزا", "کافی شاپ", "ریحون", "دلینو") to CategoryId.RESTAURANT,
        listOf("سوپر مارکت", "سوپرمارکت", "نانوایی", "میوه", "قصابی", "پروتئین", "بقالی", "اسنپ مارکت", "اسنپ اکسپرس") to CategoryId.FOOD,
        listOf("اسنپ", "تپسی", "تپ سی", "مترو", "بنزین", "جایگاه سوخت", "سوخت", "پارکینگ", "عوارض", "تاکسی", "بلیط", "علی بابا", "snapp", "tapsi", "maxim", "ماکسیم") to CategoryId.TRANSPORT,
        listOf("قبض", "برق", "آب و فاضلاب", "گاز", "همراه اول", "ایرانسل", "رایتل", "مخابرات", "اینترنت", "شارژ", "شاتل", "مبین نت", "اجاره") to CategoryId.BILLS,
        listOf("داروخانه", "بیمارستان", "درمانگاه", "پزشک", "دندان", "آزمایشگاه", "کلینیک", "بیمه") to CategoryId.HEALTH,
        listOf("دیجی کالا", "دیجیکالا", "digikala", "ترب", "باسلام", "پوشاک", "فروشگاه", "هایپر", "افق کوروش", "شهروند", "جانبو", "هایپراستار") to CategoryId.SHOPPING,
        listOf("سینما", "فیلیمو", "نماوا", "تیوال", "کنسرت", "بازی", "اسپاتیفای", "netflix", "steam") to CategoryId.ENTERTAINMENT,
        listOf("کتاب", "دانشگاه", "آموزشگاه", "شهریه", "کلاس", "دوره", "فرادرس", "مکتب خونه") to CategoryId.EDUCATION,
        listOf("حقوق", "مزد", "salary") to CategoryId.SALARY,
        listOf("پاداش", "عیدی", "bonus") to CategoryId.BONUS,
        listOf("پروژه", "قرارداد") to CategoryId.PROJECT,
    )

    /** کلید ساده‌شده برای یادگیری: حروف کوچک، بدون فاصله‌ی اضافه */
    fun keyOf(text: String): String =
        BankMessageParser.normalize(text).lowercase().replace(Regex("""\s+"""), " ").trim()

    /**
     * @param learned قانون‌های یادگرفته: کلمه → کلید دسته
     * @param validKeys کلید دسته‌های معتبر برای این نوع تراکنش (تا مثلاً «حقوق» برای هزینه انتخاب نشود)
     */
    fun guess(text: String, type: TxType, learned: Map<String, String>, validKeys: List<String>): String? {
        val t = keyOf(text)
        if (t.isBlank()) return null
        // طولانی‌ترین کلمهٔ یادگرفته‌ای که داخل متن هست
        learned.entries
            .filter { it.key.isNotBlank() && t.contains(it.key) && it.value in validKeys }
            .maxByOrNull { it.key.length }
            ?.let { return it.value }
        for ((words, cat) in BUILTIN) {
            if (cat.name !in validKeys) continue
            if (words.any { t.contains(keyOf(it)) }) return cat.name
        }
        return null
    }

    fun fallback(type: TxType): String =
        if (type == TxType.INCOME) CategoryId.OTHER_INCOME.name else CategoryId.OTHER_EXPENSE.name
}
