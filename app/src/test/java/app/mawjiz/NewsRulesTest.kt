package app.mawjiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRulesTest {
    private val enabled = Catalog.defaultEnabled()

    @Test
    fun newspaperNameIsNotSaudi() {
        val result = classify(
            "نشرت صحيفة الرياض أن إيران قصفت موقعاً في سوريا وأعلنت وقف الضرب فوراً",
            "",
            "الرياض",
            enabled,
        )
        assertTrue(result != null)
        assertFalse(result!!.second.contains("saudi"))
        assertEquals("iran", result.second.first())
    }

    @Test
    fun announcedInRiyadhStaysSaudi() {
        val result = classify(
            "أعلنت الرياض رفع إنتاج النفط بعد قرار أوبك وتثبيت سعر البرميل",
            "",
            "رويترز",
            enabled,
        )
        assertEquals(listOf("saudi", "oil"), result?.second)
    }

    @Test
    fun entityCodesDisappearAndRealNumbersStay() {
        val text = arabicProse("ارتفع السعر إلى 12.5 دولار &#8220;بعد الاجتماع&#8221; بنسبة 2%")
        assertTrue(text.contains("12.5"))
        assertTrue(text.contains("2%"))
        assertFalse(text.contains("8220"))
    }

    @Test
    fun fontColorDoesNotLeakSixes() {
        val raw = "\u0026lt;font color=\u0026quot;#6f6f6f\u0026quot;\u0026gt;الجزيرة\u0026lt;/font\u0026gt; قتلى وجرحى جراء انفجار عبوة في نينوى"
        val text = arabicProse(raw)
        assertFalse(text.contains("6"))
        assertTrue(text.contains("نينوى"))
    }

    @Test
    fun repeatedHeadlineCollapses() {
        val line = "قتلى وجرحى جراء انفجار عبوة من مخلفات داعش في نينوى"
        val text = readable(line, "$line. $line")
        assertEquals(1, text.split("نينوى").size - 1)
    }

    @Test
    fun actorImpliesDeskWithoutCountryName() {
        val fund = classify("أعلن صندوق الاستثمارات شراء حصة جديدة بعد اجتماع مجلس الإدارة اليوم", "", "رويترز", enabled)
        assertEquals("saudi", fund?.second?.first())
        val whiteHouse = classify("فرض البيت الأبيض عقوبات جديدة على شركات الطاقة بعد قرار الكونغرس اليوم", "", "رويترز", enabled)
        assertTrue(whiteHouse!!.second.contains("america"))
        val houthis = classify("استهدف الحوثيون سفينة قرب باب المندب بصاروخ وأصابها أضرار", "", "رويترز", enabled)
        assertTrue(houthis!!.second.contains("yemen"))
    }

    @Test
    fun oilWithAmericaStaysWorld() {
        val desks = matchDesks("أعلن ترامب رفع إنتاج النفط بعد اجتماع البيت الأبيض اليوم", enabled, "رويترز")
        assertEquals("world", scopeOf(primaryDesk(desks).orEmpty()))
        val local = Story("a", "أعلن صندوق الاستثمارات شراء حصة بعد اجتماع المجلس اليوم", listOf("saudi"), 1L, emptyList())
        assertTrue(storyInScope(local, setOf("local")))
        assertFalse(storyInScope(local, setOf("world")))
    }

    @Test
    fun warRanksAboveSoft() {
        val now = 1_700_000_000_000L
        val war = Story("a", "قصفت المقاتلات موقعاً في غزة وسقط قتلى في الغارات المتواصلة", listOf("gaza"), now - 3_600_000, emptyList())
        val soft = Story("b", "أعلن البنك تثبيت الفائدة بعد اجتماع قصير في العاصمة اليوم", listOf("economy"), now - 60_000, emptyList())
        assertTrue(rankScore(war, now, emptyMap()) > rankScore(soft, now, emptyMap()))
    }

    @Test
    fun questionAndPressRoundupDrop() {
        assertNull(classify("ماذا يعني التصعيد في اليمن؟", "", "بي بي سي", enabled))
        assertNull(classify("تصدرت اهتمامات الصحف الجزائرية الشأن السياسي اليوم في عددها", "", "واس", enabled))
    }
}
