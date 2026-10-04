package app.mawjiz

import java.security.MessageDigest

data class Desk(val id: String, val label: String, val color: Long, val words: List<String>)

data class Source(val outlet: String, val url: String)

data class Story(
    val id: String,
    val text: String,
    val desks: List<String>,
    val at: Long,
    val sources: List<Source>,
)

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
        Desk("europe", "أوروبا", 0xFF6E8CC4, listOf("اوروبا", "اوروبي", "الاتحاد الاوروبي", "اوكرانيا", "فرنسا", "المانيا", "بريطانيا", "لندن", "باريس")),
        Desk("america", "أمريكا", 0xFF5B7FD6, listOf("امريكا", "امريكي", "الولايات المتحده", "واشنطن")),
        Desk("china", "الصين", 0xFFC45B5B, listOf("الصين", "صيني", "بكين")),
        Desk("russia", "روسيا", 0xFF8A6BB5, listOf("روسيا", "روسي", "موسكو", "بوتين")),
        Desk("politics", "سياسة", 0xFF8E9AA8, emptyList()),
        Desk("economy", "اقتصاد", 0xFFB08A4A, emptyList()),
        Desk("sports", "رياضة", 0xFF4F9D6E, emptyList()),
        Desk("tech", "تقنية", 0xFF5C8FBF, emptyList()),
        Desk("fun", "ترفيه", 0xFFB56B8A, emptyList()),
    )

    val queries = mapOf(
        "gaza" to "غزة",
        "yemen" to "اليمن الحوثي",
        "iran" to "إيران",
        "lebanon" to "لبنان",
        "syria" to "سوريا",
        "iraq" to "العراق",
        "redsea" to "البحر الأحمر",
        "saudi" to "السعودية",
        "gulf" to "الخليج",
        "oil" to "أوبك النفط",
        "europe" to "أوروبا أوكرانيا",
        "america" to "أمريكا واشنطن",
        "china" to "الصين",
        "russia" to "روسيا بوتين",
        "politics" to "سياسة",
        "economy" to "اقتصاد",
    )

    val direct = listOf(
        "https://feeds.bbci.co.uk/arabic/rss.xml" to "بي بي سي",
        "https://www.aljazeera.net/aljazeerarss" to "الجزيرة",
        "https://www.france24.com/ar/rss" to "فرانس 24",
        "https://rss.dw.com/rdf/rss-ar-all" to "دويتشه فيله",
    )

    fun byId(id: String) = desks.find { it.id == id }

    val defaultOrder = desks.map { it.id }

    fun defaultEnabled() = desks.associate { it.id to (it.id != "sports" && it.id != "tech" && it.id != "fun") }
}

private val soft = listOf(
    "sports" to listOf("دوري", "الهلال", "النصر", "الاهلي", "مباراه", "ملعب", "كاس"),
    "tech" to listOf("ذكاء اصطناعي", "تقنيه", "هاتف"),
    "fun" to listOf("مسلسل", "فيلم", "حفل", "فنان", "اغنيه", "موسيقي", "ترفيه", "ممثل", "سينما"),
)

private val alwaysDrop = listOf(
    "اهتمامات الصحف", "تصدرت اهتمام", "جولة الصحافة", "عناوين الصحف", "مانشيت",
    "خطايا", "اخطاء الغرب", "افتتاحيه", "اللعب بالنار", "مقال راي",
    "دوار الحركة", "غثيان", "دوخه", "صديقها", "صديقه", "مشاهير",
).map(::normalize)

private val softDrop = listOf(
    "يتفقد", "تفقد", "يرعي", "يكرم", "معرض", "مهرجان", "جناح", "صقور", "مبادرة", "تعزيز الوعي", "توعيه",
).map(::normalize)

private val hard = listOf(
    "قصف", "حرب", "غاره", "صاروخ", "عقوبات", "قتل", "اشتباك", "هدنه", "هرمز", "نفط", "اوبك",
    "اعتقال", "هجوم", "اتفاق", "انفجار", "سيطر", "استهدف", "اعلن", "اصدر", "فرض", "اغلق",
    "انسحب", "غزو", "اسقاط", "وقع", "وقعت", "يعلن",
).map(::normalize)

fun normalize(input: String): String {
    val mapped = buildString {
        for (ch in input.trim()) {
            when (ch) {
                'أ', 'إ', 'آ', 'ا' -> append('ا')
                'ة' -> append('ه')
                'ى' -> append('ي')
                'ؤ' -> append('و')
                'ئ' -> append('ي')
                else -> if (ch.code !in 0x064B..0x065F) append(ch)
            }
        }
    }
    return mapped.lowercase()
        .replace(Regex("[^\\u0600-\\u06FF0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun cleanCopy(input: String): String {
    return input
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("https?://\\S+"), " ")
        .replace(Regex("[A-Za-z]{3,}"), " ")
        .replace(Regex("[?؟].*"), " ")
        .replace("الجزيرة نت", " ")
        .replace("بي بي سي", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun isNewsworthy(input: String): Boolean {
    val text = normalize(cleanCopy(input))
    if (text.isBlank()) return false
    val hardHit = hard.any { text.contains(it) }
    if (text.length < if (hardHit) 16 else 24) return false
    if (alwaysDrop.any { text.contains(it) }) return false
    if (softDrop.any { text.contains(it) } && !hardHit) return false
    val words = text.split(" ").filter { it.isNotBlank() }
    if (words.size < 6 && !hardHit) return false
    return true
}

fun matchDesks(text: String, enabled: Map<String, Boolean>): List<String> {
    val norm = normalize(text)
    for ((id, words) in soft) {
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
        if (desk.words.isEmpty()) continue
        if (desk.words.none { norm.contains(it) }) continue
        saw = true
        if (enabled[desk.id] == true) found.add(desk.id)
    }
    if (found.isNotEmpty()) return found.take(3)
    if (saw) return emptyList()
    if (enabled["politics"] == true) return listOf("politics")
    return emptyList()
}

fun storyId(url: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
    return digest.take(8).joinToString("") { "%02x".format(it) }
}

fun tokens(text: String): Set<String> = normalize(text).split(" ").filter { it.length >= 3 }.toSet()

fun sameStory(left: Story, right: Story): Boolean {
    if (left.id == right.id) return true
    val urls = left.sources.map { it.url }.toSet()
    if (right.sources.any { it.url in urls }) return true
    val a = tokens(left.text)
    val b = tokens(right.text)
    val shared = a.intersect(b).size
    val union = (a + b).size
    return shared >= 3 || (shared >= 2 && union > 0 && shared.toFloat() / union >= 0.45f)
}

fun mergeStories(existing: List<Story>, incoming: List<Story>, now: Long): List<Story> {
    val added = incoming.filter { item -> existing.none { sameStory(it, item) } }
        .sortedByDescending { it.at }
    val cutoff = now - 14L * 24 * 60 * 60 * 1000
    return (added + existing).filter { it.at >= cutoff }.take(180)
}
