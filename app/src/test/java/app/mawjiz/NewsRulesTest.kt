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
    fun questionAndPressRoundupDrop() {
        assertNull(classify("ماذا يعني التصعيد في اليمن؟", "", "بي بي سي", enabled))
        assertNull(classify("تصدرت اهتمامات الصحف الجزائرية الشأن السياسي اليوم في عددها", "", "واس", enabled))
    }
}
