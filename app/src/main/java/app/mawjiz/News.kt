package app.mawjiz

import java.security.MessageDigest

data class Desk(val id: String, val label: String, val color: Long, val words: List<String>)
data class DeskGroup(val title: String, val ids: List<String>)
data class Outlet(val id: String, val label: String, val group: String, val query: String, val feed: String? = null)
data class Source(val outlet: String, val url: String)
data class Story(
    val id: String,
    val text: String,
    val desks: List<String>,
    val at: Long,
    val sources: List<Source>,
    val detail: String = "",
    val saved: Boolean = false,
)

object Catalog {
    val desks = listOf(
        Desk("gaza", "غزة", 0xFFE15B64, listOf("غزة", "حماس", "خان يونس", "رفح", "جباليا", "الجهاد الإسلامي")),
        Desk("yemen", "اليمن", 0xFFD98A3A, listOf("يمن", "حوث", "صنعاء", "أنصار الله", "مأرب", "الحديدة")),
        Desk("iran", "إيران", 0xFFC46B4A, listOf("إيران", "هرمز", "طهران", "الحرس الثوري", "خامنئي", "نطنز", "فوردو")),
        Desk("lebanon", "لبنان", 0xFF6FA8A0, listOf("لبنان", "حزب الله", "بيروت", "الضاحية")),
        Desk("syria", "سوريا", 0xFFC4A15A, listOf("سوريا", "دمشق", "إدلب", "حلب")),
        Desk("iraq", "العراق", 0xFF8C7E6A, listOf("العراق", "بغداد", "نينوى", "الموصل", "أربيل")),
        Desk("redsea", "البحر الأحمر", 0xFF3E8E9A, listOf("البحر الأحمر", "باب المندب", "المندب")),
        Desk("saudi", "السعودية", 0xFF3E9A62, listOf("السعود", "أرامكو", "جدة", "مكة", "المملكة", "صندوق الاستثمارات", "نيوم", "ولي العهد", "الحرمين")),
        Desk("gulf", "الخليج", 0xFF4C8D7A, listOf("خليج", "الإمارات", "أبوظبي", "دبي", "قطر", "الدوحة", "الكويت", "البحرين", "عمان", "مسقط")),
        Desk("oil", "النفط", 0xFFC9842A, listOf("نفط", "أوبك", "برميل", "غاز")),
        Desk("politics", "سياسة", 0xFF8E9AA8, emptyList()),
        Desk("economy", "اقتصاد", 0xFFB08A4A, emptyList()),
        Desk("europe", "أوروبا", 0xFF6E8CC4, listOf("أوروبا", "أوروبي", "الاتحاد الأوروبي", "أوكرانيا", "زيلينسكي", "كييف", "فرنسا", "ماكرون", "ألمانيا", "بريطانيا", "لندن", "باريس", "الناتو")),
        Desk("america", "أمريكا", 0xFF5B7FD6, listOf("أمريكا", "أمريكي", "الولايات المتحدة", "واشنطن", "البيت الأبيض", "البنتاغون", "الكونغرس", "ترامب")),
        Desk("china", "الصين", 0xFFC45B5B, listOf("الصين", "صيني", "بكين", "تايوان")),
        Desk("russia", "روسيا", 0xFF8A6BB5, listOf("روسيا", "روسي", "موسكو", "بوتين", "الكرملين")),
        Desk("sports", "رياضة", 0xFF4F9D6E, emptyList()),
        Desk("tech", "تقنية", 0xFF5C8FBF, emptyList()),
        Desk("fun", "ترفيه", 0xFFB56B8A, emptyList()),
    )
    val groups = listOf(
        DeskGroup("محلي", listOf("saudi", "oil")),
        DeskGroup("إقليمي", listOf("gaza", "yemen", "iran", "lebanon", "syria", "iraq", "redsea", "gulf")),
        DeskGroup("عالمي", listOf("europe", "america", "china", "russia", "politics", "economy", "sports", "tech", "fun")),
    )
    val scopes = listOf("local" to "محلي", "region" to "إقليمي", "world" to "عالمي")
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
        Outlet("alriyadh", "الرياض", "local", "صحيفة الرياض"),
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

private val marks = setOf('.', '،', '؛', ':', '؟', '!', '«', '»', '(', ')', '-', '–', '—', '%', '٪')
private val alwaysDrop = listOf("اهتمامات الصحف", "تصدرت اهتمام", "جولة الصحافة", "عناوين الصحف", "مانشيت", "خطايا", "اخطاء الغرب", "افتتاحيه", "اللعب بالنار", "مقال راي", "دوار الحركة", "غثيان", "مشاهير", "ماذا يعني").map(::normalize)
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
private val cueWords = listOf("جريدة", "صحيفة", "وكالة", "نشرت", "نقلت", "ذكرت", "أفادت", "حسب", "وفق")
private val bareAgencies = listOf(
    "رويترز", "فرانس برس", "أسوشيتد برس", "بي بي سي", "عكاظ", "سبق", "الأناضول",
    "فرانس 24", "دويتشه فيله", "يورونيوز", "إندبندنت عربية", "سكاي نيوز عربية",
    "القدس العربي", "عرب نيوز", "وكالة الأنباء السعودية",
)
private val riyadhCityForms = listOf(
    "أعلنت الرياض", "أعلن الرياض", "قررت الرياض", "قرر الرياض", "أصدرت الرياض", "أصدر الرياض",
    "في الرياض", "من الرياض", "إلى الرياض", "بمدينة الرياض",
).map(::normalize)

fun stripMasthead(input: String, outlet: String = ""): String {
    var text = arabicProse(input)
    val names = Catalog.outlets.flatMap { listOf(it.label, it.query) }.plus(outlet).filter { it.isNotBlank() }.distinct()
    val phrases = mutableListOf<String>()
    for (name in names) {
        for (cue in cueWords) phrases += "$cue $name"
        if (' ' in name && name != "الشرق الأوسط") phrases += name
    }
    phrases += bareAgencies
    for (phrase in phrases.distinct().sortedByDescending { it.length }) {
        if (phrase.length < 3 || phrase == "الرياض") continue
        text = text.replace(phrase, " ")
    }
    return arabicProse(text)
}

private fun riyadhIsCity(norm: String): Boolean {
    if (norm.contains("نادي الرياض")) return false
    return riyadhCityForms.any { norm.contains(it) }
}

fun decodeFeed(input: String): String {
    var text = input
    repeat(2) {
        text = text
            .replace("\u0026nbsp;", " ", ignoreCase = true)
            .replace("\u0026quot;", " ", ignoreCase = true)
            .replace("\u0026apos;", " ", ignoreCase = true)
            .replace("\u0026lt;", "<", ignoreCase = true)
            .replace("\u0026gt;", ">", ignoreCase = true)
            .replace("\u0026amp;", "\u0026", ignoreCase = true)
        text = text.replace(Regex("&#x([0-9a-fA-F]+);")) { mark ->
            val code = mark.groupValues[1].toIntOrNull(16) ?: return@replace " "
            entityChar(code)
        }
        text = text.replace(Regex("&#(\\d+);")) { mark ->
            val code = mark.groupValues[1].toIntOrNull() ?: return@replace " "
            entityChar(code)
        }
    }
    return text.replace(Regex("<[^>]+>"), " ").replace(Regex("#[0-9A-Fa-f]{3,8}"), " ")
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
        .trim('.', '،', '؛', ':', '(', ')', '-', '–', '—')
        .let { dropStrayDigits(it) }
        .let { dedupeSentences(it) }
}

private fun dropStrayDigits(input: String): String {
    val singles = Regex("(?<![0-9.])(\\d)(?![0-9.%٪])").findAll(input).map { it.groupValues[1] }.toList()
    val repeated = singles.groupingBy { it }.eachCount().filterValues { it >= 2 }.keys
    if (repeated.isEmpty()) return input
    val pattern = repeated.joinToString("|") { Regex.escape(it) }
    return input.replace(Regex("(?<![0-9.])($pattern)(?![0-9.%٪])"), " ").replace(Regex("\\s+"), " ").trim()
}

private fun dedupeSentences(input: String): String {
    val kept = mutableListOf<String>()
    for (part in input.split(Regex("(?<=[.؟!])\\s+"))) {
        val bit = part.trim().trim('.', '،')
        if (bit.length < 8) continue
        if (kept.any { sameWords(it, bit) }) continue
        kept += bit
    }
    return if (kept.isEmpty()) input.trim() else kept.joinToString(". ")
}

private fun sameWords(left: String, right: String): Boolean {
    val a = tokens(left)
    val b = tokens(right)
    if (a.isEmpty() || b.isEmpty()) return false
    val shared = a.intersect(b).size
    return shared.toFloat() / minOf(a.size, b.size) >= 0.6f
}

fun readable(title: String, body: String): String {
    val head = arabicProse(title)
    val extra = arabicProse(body)
        .split(Regex("(?<=[.؟!])\\s+"))
        .map { it.trim() }
        .firstOrNull { sentence ->
            sentence.length in 28..180 && !sentence.contains('؟') && !sameWords(head, sentence)
        }
        .orEmpty()
    return arabicProse(if (extra.isBlank() || extra == head) head else "$head. $extra")
}

fun isNewsworthy(input: String): Boolean {
    val shown = input.trim()
    if (shown.contains('؟') || shown.contains('?')) return false
    val text = normalize(shown)
    if (text.length < 28) return false
    if (text.startsWith("هل ") || text.startsWith("ماذا") || text.startsWith("لماذا") || text.startsWith("كيف ")) return false
    if (alwaysDrop.any { text.contains(it) }) return false
    val hardHit = hard.any { text.contains(it) } || events.any { text.contains(it) }
    if (softDrop.any { text.contains(it) } && !hardHit) return false
    if (!hardHit) return false
    val words = text.split(" ").filter { it.length >= 2 }
    return words.size >= 6
}

fun scopeOf(id: String): String = when (id) {
    "saudi", "oil" -> "local"
    "europe", "america", "china", "russia", "politics", "economy", "sports", "tech", "fun" -> "world"
    else -> "region"
}

fun storyInScope(story: Story, scopes: Set<String>): Boolean {
    if (scopes.isEmpty()) return true
    return story.desks.any { scopeOf(it) in scopes }
}

fun matchDesks(text: String, enabled: Map<String, Boolean>, outlet: String = ""): List<String> {
    val norm = normalize(stripMasthead(text, outlet))
    for ((id, words) in softTopics) {
        if (words.none { norm.contains(normalize(it)) }) continue
        if (enabled[id] != true) return emptyList()
        return listOf(id)
    }
    val hits = mutableListOf<Pair<Int, String>>()
    var blocked = false
    for (desk in Catalog.desks) {
        val index = desk.words.map { norm.indexOf(normalize(it)) }.filter { it >= 0 }.minOrNull() ?: continue
        if (enabled[desk.id] == true) hits += index to desk.id else blocked = true
    }
    if (riyadhIsCity(norm)) {
        if (enabled["saudi"] == true) hits += norm.indexOf("الرياض") to "saudi" else blocked = true
    }
    val places = hits.sortedBy { it.first }.map { it.second }.distinct()
    if (places.isNotEmpty()) return places.take(2)
    if (blocked) return emptyList()
    if (enabled["economy"] == true && economyWords.any { norm.contains(it) }) return listOf("economy")
    if (enabled["politics"] == true && politicsWords.any { norm.contains(it) }) return listOf("politics")
    return emptyList()
}

fun detailFor(title: String, body: String, outlet: String, card: String): String {
    val parts = arabicProse(body)
        .split(Regex("(?<=[.؟!])\\s+"))
        .map { it.trim() }
        .filter { sentence -> sentence.length >= 24 && !sentence.contains('؟') && !sameWords(card, sentence) }
        .take(3)
    if (parts.isEmpty()) return ""
    return stripMasthead(parts.joinToString(". "), outlet)
}

private val heavyNews = listOf("قصف", "قتل", "حرب", "غاره", "صاروخ", "عقوبات", "اوبك", "نفط", "انفجار", "غزو", "احتل", "اغتيال", "اشتباك").map(::normalize)
private val wireNames = listOf("رويترز", "بي بي سي", "واس", "الجزيرة", "فرانس برس", "أسوشيتد برس")

fun rankScore(story: Story, now: Long, taste: Map<String, Int>): Int {
    val hours = ((now - story.at).coerceAtLeast(0L) / 3_600_000L).toInt()
    val fresh = (48 - hours).coerceIn(0, 48)
    val penalty = if (hours > 48) 220 else 0
    val norm = normalize(story.text)
    val weight = if (heavyNews.any { norm.contains(it) }) 180 else 40
    val wire = if (story.sources.any { source -> wireNames.any { source.outlet.contains(it) } }) 15 else 0
    val liked = story.desks.sumOf { (taste[it] ?: 0).coerceAtMost(8) } * 4
    return weight + fresh + wire + liked - penalty
}

fun classify(title: String, body: String, outlet: String, enabled: Map<String, Boolean>): Pair<String, List<String>>? {
    val text = stripMasthead(readable(title, body), outlet)
    if (!isNewsworthy(text)) return null
    val desks = matchDesks(text, enabled, outlet)
    if (desks.isEmpty()) return null
    return text to desks
}

fun retag(story: Story, enabled: Map<String, Boolean>): Story? {
    val outlet = story.sources.firstOrNull()?.outlet.orEmpty()
    val text = stripMasthead(story.text, outlet)
    if (!isNewsworthy(text)) return null
    val desks = matchDesks(text, enabled, outlet)
    if (desks.isEmpty()) return null
    return story.copy(text = text, desks = desks)
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
    if (left.desks.firstOrNull() != null && right.desks.firstOrNull() != null && left.desks.first() != right.desks.first()) return false
    val shared = tokens(left.text).intersect(tokens(right.text)).size
    return shared >= 4
}

fun mergeStories(existing: List<Story>, incoming: List<Story>, now: Long): List<Story> {
    val added = incoming.filter { item -> existing.none { sameStory(it, item) } }.sortedByDescending { it.at }
    val cutoff = now - 14L * 24 * 60 * 60 * 1000
    return (added + existing).filter { it.at >= cutoff }.take(180)
}
