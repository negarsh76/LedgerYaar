package com.ledgeryaar

import com.ledgeryaar.domain.autoimport.BankMessageParser
import com.ledgeryaar.domain.autoimport.MessageUnit
import com.ledgeryaar.domain.model.TxType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BankMessageParserTest {
    @Test fun melliTransfer() {
        val p = BankMessageParser.parse("بانک ملی ایران\nانتقال:-1,000,000\nحساب:0101234567001\nمانده:25,430,000\n0815-12:30")!!
        assertEquals(TxType.EXPENSE, p.type); assertEquals(1_000_000.0, p.amount); assertEquals(25_430_000.0, p.balance)
    }
    @Test fun mellatWithMaskedCard() {
        val p = BankMessageParser.parse("بانک ملت\nحساب 6037****1234\nبرداشت 2,500,000\nمانده 10,000,000")!!
        assertEquals(TxType.EXPENSE, p.type); assertEquals(2_500_000.0, p.amount)
    }
    @Test fun samanDeposit() {
        val p = BankMessageParser.parse("*بانک سامان*\nواریز: 15,000,000+\nمانده: 40,250,000")!!
        assertEquals(TxType.INCOME, p.type); assertEquals(15_000_000.0, p.amount)
    }
    @Test fun bluPersianDigits() {
        val p = BankMessageParser.parse("بلو\n۲۵۰,۰۰۰ ریال از حساب شما برداشت شد\nبابت: اسنپ\nموجودی: ۳,۵۰۰,۰۰۰ ریال")!!
        assertEquals(TxType.EXPENSE, p.type); assertEquals(250_000.0, p.amount)
        assertEquals(MessageUnit.RIAL, p.unit); assertEquals("اسنپ", p.merchant)
    }
    @Test fun otpAndAdsIgnored() {
        assertNull(BankMessageParser.parse("رمز پویا شما 123456 است. مبلغ 500,000"))
        assertNull(BankMessageParser.parse("تخفیف ویژه! تا 50% در دیجی کالا"))
    }
}
