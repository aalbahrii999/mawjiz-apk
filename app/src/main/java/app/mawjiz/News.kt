package app.mawjiz

import java.security.MessageDigest

data class Desk(val id: String, val label: String, val color: Long, val words: List<String>)
data class DeskGroup(val title: String, val ids: List<String>)
data class Outlet(val id: String, val label: String, val group: String, val query: String, val feed: String? = null)
data class Source(val outlet: String, val url: String)
data class Story(val id: String, val text: String, val desks: List<String>, val at: Long, val sources: List<Source>)

object Catalog {
    val desks = listOf(
        Desk("gaza", "غزة", 0xFFE15B64, listOf("غزه", "حماس")),
        Desk("yemen", "اليمن", 0xFFD98A3A, listOf("يمن", "حوث", "صنعاء")),
        Desk("iran", "إيران", 0xFFC46B4A, listOf("ايران", "هرمز", "طهران")),
        Desk("lebanon", "لبنان", 0xFF6FA8A0, listOf("لبنان", "حزب الله")),
        Desk("syria", "سوريا", 0xFFC4A15A, listOf("سوريا", "دمشق")),
        Desk("iraq", "العراق", 0xFF8C7E6A, listOf("العراق", "بغداد")),
        Desk("redsea", "البحر الأحمر", 0xFF3E8E9A, listOf("البحر الاحمر", "باب المندب", "المندب")),
        Desk("saudi", "السعودية", 0xFF3E9A62, listOf("السعود", "الرياض", "ارامكو", "جده", "مكه")),
        Desk("gulf", "الخليج", 0xFF4C8D7A, listOf("خليج", "الامارات", "قطر", "الكويت", "البحرين", "عمان")),
        Desk("oil", "النفط", 0xFFC9842A, listOf("نفط", "اوبك", "برميل", "غاز")),
        Desk("politics", "سياسة", 0xFF8E9AA8, emptyList()),
        Desk("economy", "اقتصاد", 0xFFB08A4A, emptyList()),
        Desk("europe", "أوروبا", 0xFF6E8CC4, listOf("اوروبا", "اوروبي", "الاتحاد الاوروبي", "اوكرانيا", "فرنسا", "المانيا", "بريطانيا", "لندن", "باريس")),
        Desk("america", "أمريكا", 0xFF5B7FD6, listOf("امريكا", "امريكي", "الولايات المتحده", "واشنطن")),
        Desk("china", "الصين", 0xFFC45B5B, listOf("الصين", "صيني", "بكين")),
        Desk("russia", "روسيا", 0xFF8A6BB5, listOf("روسيا", "روسي", "موسكو", "بوتين")),
        Desk("sports", "رياضة", 0xFF4F9D6E, emptyList()),
        Desk("tech", "تقنية", 0xFF5C8FBF, emptyList()),
        Desk("fun", "ترفيه", 0xFFB56B8A, emptyList()),
    )
    val groups = listOf(
        DeskGroup("التصنيفات", listOf("gaza", "yemen", "iran", "lebanon", "syria", "iraq", "redsea", "saudi", "gulf", "oil", "politics", "economy")),
        DeskGroup("أخبار عالمية", listOf("europe", "america", "china", "russia")),
        DeskGroup("غيرها", listOf("sports", "tech", "fun")),
    )
    val queries = mapOf(
        "gaza" to "غزة", "yemen" to "اليمن الحوثي", "iran" to "إيران", "lebanon" to "لبنان",
        "syria" to "سوريا", "iraq" to "العراق", "redsea" to "البحر الأحمر", "saudi" to "السعودية",
        "gulf" to "الخليج", "oil" to "أوبك النفط", "politics" to "سياسة الشرق الأوسط", "economy" to "اقتصاد",
        "europe" to "أوروبا أوكرانيا", "america" to "أمريكا واشنطن", "china" to "الصين", "russia" to "روسيا",
    )
    val outletGroups = listOf(
        "global" to "وكالات عالمية",
        "local" to "محلية",
        "arab" to "عربية",
        "foreign" to "أجنبية",
    )
    val outlets = listOf(
        Outlet("reuters", "رويترز", "global", "رويترز"),
        Outlet("afp", "فرانس برس", "global", "فرانس برس"),
        Outlet("ap", "أسوشيتد برس", "global", "أسوشيتد برس"),
        Outlet("bbc", "بي بي سي", "global", "بي بي سي", "https://feeds.bbci.co.uk/arabic/rss.xml"),
        Outlet("spa", "واس", "local", "واس"),
        Outlet("okaz", "عكاظ", "local", "عكاظ"),
        Outlet("sabq", "سبق", "local", "سبق"),
        Outlet("aleqt", "الاقتصادية", "local", "الاقتصادية"),
        Outlet("alriyadh", "الرياض", "local", "جريدة الرياض"),
        Outlet("arabnews", "عرب نيوز", "local", "عرب نيوز"),
        Outlet("aljazeera", "الجزيرة", "arab", "الجزيرة", "https://www.aljazeera.net/aljazeerarss"),
        Outlet("alarabiya", "العربية", "arab", "العربية"),
        Outlet("sky", "سكاي نيوز عربية", "arab", "سكاي نيوز عربية"),
        Outlet("asharq", "الشرق الأوسط", "arab", "الشرق الأوسط"),
        Outlet("anadolu", "الأناضول", "arab", "الأناضول"),
        Outlet("quds", "القدس العربي", "arab", "القدس العربي"),
        Outlet("france24", "فرانس 24", "foreign", "فرانس 24", "https://www.france24.com/ar/rss"),
        Outlet("dw", "دويتشه فيله", "foreign", "دويتشه فيله", "https://rss.dw.com/rdf/rss-ar-all"),
        Outlet("euronews", "يورونيوز", "foreign", "يورونيوز"),
        Outlet("independent", "إندبندنت عربية", "foreign", "إندبندنت عربية"),
    )
    val defaultOrder = desks.map { it.id }
    val defaultSources = listOf("reuters", "bbc", "spa", "aljazeera", "alarabiya", "asharq", "france24", "dw")
    fun desk(id: String) = desks.find { it.id == id }
    fun defaultEnabled() = desks.associate { it.id to (it.id != "sports" && it.id != "tech" && it.id != "fun") }
}

private val marks = setOf('.', '،', '؛', ':', '؟', '!', '«', '»', '"', '\'', '(', ')', '-', '–', '—', '%', '٪')
private val alwaysDrop = listOf("اهتمامات الصحف", "تصدرت اهتمام", "جولة الصحافة", "عناوين الصحف", "مانشيت", "خطايا", "اخطاء الغرب", "افتتاحيه", "اللعب بالنار", "مقال راي", "دوار الحركة", "غثيان", "مشاهير").map(::normalize)
private val softDrop = listOf("يتفقد", "تفقد", "يرعي", "يكرم", "معرض", "مهرجان", "جناح", "صقور", "مبادرة", "تعزيز الوعي").map(::normalize)
private val hard = listOf("قصف", "حرب", "غاره", "صاروخ", "عقوبات", "قتل", "اشتباك", "هدنه", "هرمز", "نفط", "اوبك", "اعتقال", "هجوم", "اتفاق", "انفجار", "سيطر", "استهدف", "اعلن", "اصدر", "فرض", "اغلق", "انسحب", "غزو", "اسقاط", "وقع", "يعلن").map(::normalize)
private val softTopics = listOf(
    "sports" to listOf("دوري", "الهلال", "النصر", "الاهلي", "مباراه", "ملعب", "كاس"),
    "tech" to listOf("ذكاء اصطناعي", "تقنيه", "هاتف"),
    "fun" to listOf("مسلسل", "فيلم", "حفل", "فنان", "اغنيه", "ترفيه", "ممثل", "سينما"),
)

fun normalize(input: String): String = buildString {
    for (ch in input.trim()) {
        when (ch) {
            'أ', 'إ', 'آ', 'ا' -> append('ا')
            'ة' -> append('ه')
            'ى', 'ئ' -> append('ي')
            'ؤ' -> append('و')
            else -> if (ch.code !in 0x064B..0x065F) append(ch)
        }
    }
}.lowercase().replace(Regex("[^\\u0600-\\u06FF0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

private val events = listOf(
    "اعلن", "اعلنت", "قرر", "قررت", "فرض", "اغلق", "قصف", "قتل", "اتفق", "وقع", "اصدر",
    "انسحب", "استهدف", "سيطر", "اعتقل", "هاجم", "انفجر", "ارتفع", "انخفض", "رفض", "وافق",
    "عين", "اقال", "مدد", "اصاب", "دمر", "حظر", "شنت", "اوقف", "استقال", "انتخب",
    "خفض", "رفع", "بلغ", "سجل", "ثبت", "قصفت", "دمرت", "اغتيل", "اجتاحت", "احتلت",
).map(::normalize)

private val politicsWords = listOf("عقوبات", "قمه", "مجلس الامن", "اتفاق", "قرار", "امم متحده").map(::normalize)
private val economyWords = listOf("تضخم", "فائده", "بنك مركزي", "ميزانيه", "ركود", "رسوم").map(::normalize)

fun decodeFeed(input: String): String {
    var text = input.replace(Regex("<[^>]+>"), " ")
    text = text.replace(Regex("&#x([0-9a-fA-F]+);")) { mark ->
        val code = mark.groupValues[1].toIntOrNull(16) ?: return@replace " "
        entityChar(code)
    }
    text = text.replace(Regex("&#(\\d+);")) { mark ->
        val code = mark.groupValues[1].toIntOrNull() ?: return@replace " "
        entityChar(code)
    }
    return text
        .replace("\u0026nbsp;", " ", ignoreCase = true)
        .replace("\u0026amp;", " ", ignoreCase = true)
        .replace("\u0026quot;", " ", ignoreCase = true)
        .replace("\u0026apos;", " ", ignoreCase = true)
        .replace("\u0026lt;", " ", ignoreCase = true)
        .replace("\u0026gt;", " ", ignoreCase = true)
}

private fun entityChar(code: Int): String = when (code) {
    160, 8203, 8239, 8230, 39, 34, 8216, 8217, 8220, 8221 -> " "
    8211, 8212 -> " "
    in 48..57 -> code.toChar().toString()
    else -> if (code in 0x0600..0x06FF) code.toChar().toString() else " "
}

fun arabicProse(input: String): String {
    val stripped = decodeFeed(input)
        .replace(Regex("https?://\\S+|www\\.\\S+"), " ")
        .replace(Regex("[A-Za-z]+"), " ")
    val kept = buildString {
        for (ch in stripped) {
            val digit = ch in '0'..'9' || ch in '\u0660'..'\u0669' || ch in '\u06F0'..'\u06F9'
            val arabic = ch.code in 0x0600..0x06FF || ch.code in 0x0750..0x077F
            if (ch.isWhitespace()) append(' ')
            else if (digit || arabic || ch in marks) append(ch)
        }
    }
    return kept
        .replace(Regex("(?<![0-9])(\\d{3,4})(?![0-9٪%])")) { mark ->
            val value = mark.groupValues[1].toIntOrNull() ?: return@replace " "
            if (value in 1900..2035) mark.value else " "
        }
        .replace(Regex("(\\d+)(\\s+\\1)+"), "$1")
        .replace(Regex("([.،؛:؟!])\\1+"), "$1")
        .replace(Regex("\\s+"), " ")
        .replace(Regex("\\s+([.،؛:؟!])"), "$1")
        .trim()
        .trim('.', '،', '؛', ':', '(', ')', '-', '–', '—', '%', '٪')
}

fun readable(title: String, body: String): String {
    val head = arabicProse(title)
    val extra = arabicProse(body)
        .split(Regex("(?<=[.؟!])\\s+"))
        .map { it.trim() }
        .firstOrNull { sentence ->
            sentence.length in 28..180 &&
                !sentence.contains('؟') &&
                !normalize(sentence).startsWith(normalize(head).take(14))
        }
        .orEmpty()
    return if (extra.isBlank() || extra == head) head else "$head. $extra"
}

fun isNewsworthy(input: String): Boolean {
    val text = normalize(input)
    if (text.length < 28) return false
    if (alwaysDrop.any { text.contains(it) }) return false
    val hardHit = hard.any { text.contains(it) } || events.any { text.contains(it) }
    if (softDrop.any { text.contains(it) } && !hardHit) return false
    if (!hardHit) return false
    val words = text.split(" ").filter { it.length >= 2 }
    return words.size >= 6
}

fun matchDesks(text: String, enabled: Map<String, Boolean>): List<String> {
    val norm = normalize(text)
    for ((id, words) in softTopics) {
        if (words.none { norm.contains(normalize(it)) }) continue
        if (enabled[id] != true) return emptyList()
        val found = mutableListOf(id)
        for (desk in Catalog.desks) {
            if (desk.words.isEmpty() || enabled[desk.id] != true || desk.id == id) continue
            if (desk.words.any { norm.contains(it) }) found.add(desk.id)
        }
        return found.distinct().take(3)
    }
    val found = mutableListOf<String>()
    var saw = false
    for (desk in Catalog.desks) {
        if (desk.words.isEmpty() || desk.words.none { norm.contains(it) }) continue
        saw = true
        if (enabled[desk.id] == true) found.add(desk.id)
    }
    if (found.isNotEmpty()) return found.take(2)
    if (saw) return emptyList()
    if (enabled["economy"] == true && economyWords.any { norm.contains(it) }) return listOf("economy")
    if (enabled["politics"] == true && politicsWords.any { norm.contains(it) }) return listOf("politics")
    return emptyList()
}

fun storyId(url: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
    return digest.take(8).joinToString("") { "%02x".format(it) }
}

private fun tokens(text: String) = normalize(text).split(" ").filter { it.length >= 3 }.toSet()

fun sameStory(left: Story, right: Story): Boolean {
    if (left.id == right.id) return true
    val urls = left.sources.map { it.url }.toSet()
    if (right.sources.any { it.url in urls }) return true
    val shared = tokens(left.text).intersect(tokens(right.text)).size
    return shared >= 3
}

fun mergeStories(existing: List<Story>, incoming: List<Story>, now: Long): List<Story> {
    val added = incoming.filter { item -> existing.none { sameStory(it, item) } }.sortedByDescending { it.at }
    val cutoff = now - 14L * 24 * 60 * 60 * 1000
    return (added + existing).filter { it.at >= cutoff }.take(180)
}
