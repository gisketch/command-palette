package dev.gisketch.commandpalette.action

object PaletteSearch {
    fun matches(query: String, haystacks: Iterable<String>): Boolean {
        if (query.isBlank()) return true
        return haystacks.any { matchRanges(query, it).isNotEmpty() }
    }

    fun matchRanges(query: String, text: String): List<IntRange> {
        val tokens = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (tokens.isEmpty()) return emptyList()

        val exactRangesByToken = tokens.map { token -> exactMatchRanges(text, token) }
        if (exactRangesByToken.all(List<IntRange>::isNotEmpty)) return mergeRanges(exactRangesByToken.flatten())

        val initialsRanges = initialsMatchRanges(query, text)
        if (initialsRanges.isNotEmpty()) return initialsRanges

        return emptyList()
    }

    private fun exactMatchRanges(text: String, token: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        val lowerText = text.lowercase()
        val lowerToken = token.lowercase()
        var startIndex = lowerText.indexOf(lowerToken)
        while (startIndex >= 0) {
            ranges.add(startIndex until startIndex + token.length)
            startIndex = lowerText.indexOf(lowerToken, startIndex + token.length)
        }
        return ranges
    }

    private fun initialsMatchRanges(query: String, text: String): List<IntRange> {
        val normalizedQuery = query.filter(Char::isLetterOrDigit).lowercase()
        if (normalizedQuery.isEmpty()) return emptyList()

        val words = wordStarts(text)
        if (words.isEmpty() || normalizedQuery.length > words.size) return emptyList()

        val initials = buildString(words.size) { words.forEach { append(text[it].lowercaseChar()) } }
        if (!initials.startsWith(normalizedQuery)) return emptyList()

        return words.take(normalizedQuery.length).map { index -> index..index }
    }

    private fun wordStarts(text: String): List<Int> {
        val starts = mutableListOf<Int>()
        var previous: Char? = null
        text.forEachIndexed { index, char ->
            if (!char.isLetterOrDigit()) {
                previous = char
                return@forEachIndexed
            }

            val previousChar = previous
            val isWordStart = previousChar == null
                || !previousChar.isLetterOrDigit()
                || (previousChar.isLowerCase() && char.isUpperCase())
            if (isWordStart) starts.add(index)
            previous = char
        }
        return starts
    }

    private fun mergeRanges(ranges: List<IntRange>): List<IntRange> = ranges.sortedBy { it.first }
        .fold(mutableListOf<IntRange>()) { merged, range ->
            val last = merged.lastOrNull()
            if (last == null || range.first > last.last + 1) {
                merged.add(range)
            } else {
                merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
            }
            merged
        }
}