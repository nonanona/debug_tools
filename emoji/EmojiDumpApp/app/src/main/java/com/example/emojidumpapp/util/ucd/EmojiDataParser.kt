package com.example.emojidumpapp.util.ucd

import android.content.res.AssetManager
import java.io.InputStream
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.util.*

data class EmojiData(
    val cp: List<Int>,
    val props: Set<String>,
    val generation: String
) {
    val str: String

    init {
        val s = StringBuilder()
        cp.forEach {
            s.append(Character.toChars(it))
        }
        str = s.toString()
    }

    internal companion object {
        class EmojiDataComparator : Comparator<EmojiData> {
            override fun compare(left: EmojiData?, right: EmojiData?): Int {
                val l = left?.cp?: emptyList()
                val r = right?.cp?: emptyList()

                val cmpMax = Math.min(l.size, r.size)

                for (i in 0..cmpMax-1) {
                    if (l[i] < r[i]) {
                        return -1
                    } else if (l[i] > r[i]){
                        return 1
                    } else {
                        // keep comparing next value
                    }
                }

                if (l.size < r.size) {
                    return -1
                } else if (l.size == r.size) {
                    // Exactly the same length and contains all the same value. return 0
                    return 0
                } else {
                    return 1
                }
            }

        }
    }


    override fun toString(): String {
        return "CodePoints: ${cp.joinToString(separator = " "){ String.format("U+%04X", it) }}, " +
                "Generation: ${generation}, " +
                "Properties: ${props.joinToString()}"
    }
}

data class Range(
    val start: Int,  // inclusive
    val end: Int // exclusive
) {
    fun forEach(action: (Int) -> Unit) {
        for (i in start..end - 1) {
            action(i)
        }
    }
}

private val SEGMENT_SEPARATOR = "# ================================================\n"

private val TOTAL_ELEMENTS_PREFIX = "# Total elements:"

data class Entry(val cp: List<Int>, val generation: String)

data class SegmentData(val prop: String, val result: Set<Entry>)

private fun parseRange(str: String) : Range = if (str.contains("..")) {
    val tokens = str.split("..")
    Range(tokens[0].toInt(16), tokens[1].toInt(16) + 1 /* to be exclusive */)
} else {
    val num = str.toInt(16)
    Range(num, num + 1 /* to be exclusive */)
}


// Parser for emoji-data.txt
private class EmojiDataParser {
    internal companion object {
        private data class LineData(val range: Range, val prop: String, val generation: String)

        fun parseLine(line: String) : LineData? {
            // The line look like
            // 1F3FB..1F3FF  ; Emoji_Component      #  8.0  [5] (🏻..🏿)    light skin tone..dark skin tone
            val tokens = line.split(";|#".toRegex())
            val range = parseRange(tokens[0].trim())
            val prop = tokens[1].trim()
            val generation = tokens[2].substringBefore('[').trim()

            return LineData(range, prop, generation)
        }

        fun parseSegment(str: String) : SegmentData? {
            val result = mutableSetOf<Entry>()
            var prop: String? = null

            str.split("\n").forEach {
                if (it.isEmpty()) {
                    // Skip the empty line
                } else if (it[0] == '#') {  // comment line
                    if (it.startsWith(TOTAL_ELEMENTS_PREFIX)) {
                        val localProp = prop
                        if (localProp == null) {
                            return null  // Looks like Header.
                        }
                        val total = it.substring(TOTAL_ELEMENTS_PREFIX.length).trim().toInt()
                        if (result.size != total) {
                            throw RuntimeException("The parsed elements didn't match with the expected number." +
                                    " Expected: $total, Actual ${result.size}")
                        }
                        return SegmentData(localProp, result)
                    }
                } else {
                    parseLine(it)?.let {
                        it.range.forEach { cp -> result.add(Entry(listOf(cp), it.generation)) }
                        if (prop == null) {
                            prop = it.prop
                        } else if (prop != it.prop) {
                            throw RuntimeException("This segment has multiple properties. Unsupported")
                        }
                    }
                }
            }
            return null  // Empty segment
        }
    }
}

// Parser for emoji-sequences.txt
private class EmojiSequencesParser {
    internal companion object {
        private data class LineData(val cps: List<List<Int>>, val prop: String, val generation: String)

        fun parseLine(line: String) : LineData? {
            // The line look like
            // 25FD..25FE    ; Basic_Emoji              ; white medium-small square                                      #  3.2  [2] (◽..◾)
            // or
            // 0023 FE0F 20E3; Emoji_Keycap_Sequence    ; keycap: \x{23}                                                 #  3.0  [1] (#️⃣)
            val tokens = line.split(";|#".toRegex())
            val prop = tokens[1].trim()
            val generation = tokens[3].substringBefore('[').trim()

            val cpStr = tokens[0].trim()

            val cps = mutableListOf<List<Int>>()
            if (cpStr.contains("..")) {
                // Range expressioon
                parseRange(cpStr).forEach {
                    cps.add(listOf(it))
                }
            } else {
                // sequence expression

                cps.add(cpStr.split(" ").map{ it.toInt(16) })
            }

            return LineData(cps, prop, generation)
        }

        fun parseSegment(str: String) : SegmentData? {
            val result = mutableSetOf<Entry>()
            var prop: String? = null

            str.split("\n").forEach {
                if (it.isEmpty()) {
                    // Skip the empty line
                } else if (it[0] == '#') {  // comment line
                    if (it.startsWith(TOTAL_ELEMENTS_PREFIX)) {
                        val localProp = prop
                        if (localProp == null) {
                            return null  // Looks like Header.
                        }
                        val total = it.substring(TOTAL_ELEMENTS_PREFIX.length).trim().toInt()
                        if (result.size != total) {
                            throw RuntimeException("The parsed elements didn't match with the expected number." +
                                    " Expected: $total, Actual ${result.size}")
                        }
                        return SegmentData(localProp, result)
                    }
                } else {
                    EmojiSequencesParser.parseLine(it)?.let {
                        it.cps.forEach { cp -> result.add(Entry(cp, it.generation)) }
                        if (prop == null) {
                            prop = it.prop
                        } else if (prop != it.prop) {
                            throw RuntimeException("This segment has multiple properties. Unsupported")
                        }
                    }
                }
            }
            return null  // Empty segment
        }
    }
}

/**
 * parser for emoji-data.txt
 */
class UnicodeEmojiDataParser {
    internal companion object {
        private fun mergeSements(list: List<SegmentData>) : List<EmojiData> {
            // two-path merging.
            // 1. Collect all code pooints.
            // 2. Merge the final emoji data
            var codePoints = mutableSetOf<Entry>()
            list.forEach{
                codePoints.addAll(it.result)
            }

            val result = mutableListOf<EmojiData>()

            for (entry in codePoints) {
                var prop = mutableSetOf<String>()
                list.forEach {
                    if (it.result.contains(entry)) {
                        prop.add(it.prop)
                    }
                }
                result.add(EmojiData(entry.cp, prop, entry.generation))
            }

            result.sortWith(EmojiData.Companion.EmojiDataComparator())
            return result
        }

        val FILE_PARSER = mapOf(
            "emoji/13.1/emoji-data.txt" to { it : String -> EmojiDataParser.parseSegment(it) },
            "emoji/13.1/emoji-sequences.txt" to { it : String -> EmojiSequencesParser.parseSegment(it) },
            "emoji/13.1/emoji-zwj-sequences.txt" to { it : String -> EmojiSequencesParser.parseSegment(it) }
        )

        fun parse(asset: AssetManager) : List<EmojiData> {
            val tmp = mutableListOf<SegmentData>()

            FILE_PARSER.forEach{ file, parser ->
                asset.open(file).use {
                    it.bufferedReader().use {
                        it.readText().split(SEGMENT_SEPARATOR).forEach {
                            parser(it)?.let { tmp.add(it) }
                        }
                    }
                }
            }

            return mergeSements(tmp)
        }
    }
}