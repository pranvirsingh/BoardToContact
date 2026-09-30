package com.pranvir.boardtocontact

/**
 * Turns raw board text into contact fields. Pure logic, no Android calls.
 * Tuned for Indian shop boards: 10-digit mobiles, +91, landlines, PIN codes.
 */
object BoardParse {

    data class Result(
        val name: String,
        val phones: List<String>,
        val address: String,
    )

    private val addressHints = listOf(
        "road", "rd", "street", "st", "nagar", "market", "chowk", "gali",
        "avenue", "complex", "plaza", "colony", "sector", "phase", "near",
        "opp", "opposite", "behind", "beside", "floor", "building", "shop",
        "plot", "lane", "marg", "bazaar", "circle", "cross", "main",
    )

    /** Whole-word match, so "st" never fires inside "store". */
    private val hintRegex = Regex(
        "\\b(${addressHints.joinToString("|") { Regex.escape(it) }})\\b",
        RegexOption.IGNORE_CASE,
    )

    /** Candidate digit runs: optional +91/91/0 prefix, then the number. */
    private val phonePattern = Regex("""\+?91[\s-]?[6-9]\d{4}[\s-]?\d{5}""")
    private val shortPattern = Regex("""(?<!\d)(?:0[\s-]?)?[6-9]\d{4}[\s-]?\d{5}(?!\d)""")
    private val pinPattern = Regex("""(?<!\d)\d{6}(?!\d)""")

    /** " +91 98765 43210 " -> "+91 98765 43210". Null when not a mobile. */
    fun normalize(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        val core = when {
            digits.length == 12 && digits.startsWith("91") -> digits.drop(2)
            digits.length == 11 && digits.startsWith("0") -> digits.drop(1)
            digits.length == 10 -> digits
            else -> return null
        }
        if (core[0] !in '6'..'9') return null
        return if (raw.trimStart().startsWith("+")) "+91 ${core.take(5)} ${core.drop(5)}"
        else "${core.take(5)} ${core.drop(5)}"
    }

    fun extractPhones(text: String): List<String> {
        val found = LinkedHashMap<String, String>()
        (phonePattern.findAll(text) + shortPattern.findAll(text)).forEach { match ->
            val pretty = normalize(match.value) ?: return@forEach
            val core = pretty.filter { it.isDigit() }.takeLast(10)
            found.putIfAbsent(core, pretty)
        }
        return found.values.toList()
    }

    /** Lines that look like a street address: hint word and/or PIN code. */
    fun extractAddress(lines: List<String>): String {
        val picked = lines.filter { line ->
            val hasHint = hintRegex.containsMatchIn(line)
            val hasPin = pinPattern.containsMatchIn(line)
            val hasDigits = line.any { it.isDigit() }
            (hasHint && line.length > 8) || (hasPin && line.length > 6) ||
                (hasHint && hasDigits)
        }
        return picked.joinToString(", ")
    }

    /**
     * Shop name: first substantial line that is mostly letters and not an
     * address or phone line. Falls back to the first non-empty line.
     */
    fun guessName(lines: List<String>): String {
        val clean = lines.map { it.trim() }.filter { it.length >= 3 }
        if (clean.isEmpty()) return ""
        for (line in clean) {
            val letters = line.count { it.isLetter() }
            if (letters < 3) continue
            if (phonePattern.containsMatchIn(line) || shortPattern.containsMatchIn(line)) continue
            if (hintRegex.containsMatchIn(line) && line.any { it.isDigit() }) continue
            if (letters * 2 < line.length) continue
            return line
        }
        return clean.first()
    }

    fun parse(fullText: String, lines: List<String>): Result =
        Result(
            name = guessName(lines),
            phones = extractPhones(fullText),
            address = extractAddress(lines),
        )
}
