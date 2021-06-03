package com.example.emojidumpapp.util

import android.util.Log

fun stringToCodePoints(str: String) : List<Int> {
    val out = mutableListOf<Int>()
    var i = 0
    while (i < str.length) {
        val cp = str.codePointAt(i)
        out.add(cp)
        i += Character.charCount(cp)
    }
    return out
}

fun stringToUtf16(str: String) : List<Char>  = str.toCharArray().asList()

fun utf16ToCodePoints(chars: List<Char>) : List<Int> {
    var i = 0;
    val out = mutableListOf<Int>()
    while (i < chars.size) {
        if (Character.isHighSurrogate(chars[i]) && (i + 1) < chars.size
                && Character.isLowSurrogate(chars[i + 1])) {
            out.add(Character.toCodePoint(chars[i], chars[i + 1]))
        } else {
            out.add(chars[i].toInt())
        }
    }
    return out
}

fun utf16ToString(chars: List<Char>) : String = String(chars.toCharArray())

fun codePointsToUtf16(codePoints: List<Int>) : List<Char> {
    val out = mutableListOf<Char>()
    for (cp in codePoints) {
        if (Character.charCount(cp) == 2) {
            out.add(Character.highSurrogate(cp))
            out.add(Character.lowSurrogate(cp))
        } else {
            out.add(cp.toChar())
        }
    }
    return out
}

fun codePointsToString(codePoints: List<Int>) : String = String(codePoints.toIntArray(), 0, codePoints.size)


fun parseCodePoint(str: String) : List<Int> {
    return str.toUpperCase().split("\\s+|U\\+".toRegex())
            .map { it.filter { it.isDigit() || ('A' <= it && it <= 'F') } }
            .filter { it.isNotEmpty() }
            .filter { it.length <= 6 }
            .map { it.toInt(16) }
            .filter { 0 <= it && it <= 0x10FFFF }
}

fun parseUtf16(str: String) : List<Char> {
    return str.toUpperCase().split("\\\\U|\\s+".toRegex())
            .map { it.filter { it.isDigit() || ('A' <= it && it <= 'F') } }
            .filter { it.isNotEmpty() }
            .filter { it.length <= 4 }
            .map { it.toInt(16) }
            .filter { 0 <= it && it <= 0xFFFF }
            .map { it.toChar() }
}

fun toCodePointString(codePoints: List<Int>) : String {
    return codePoints.joinToString(separator = " ") { String.format("U+%04X", it) }
}

fun toUtf16String(utf16: List<Char>) : String {
    return utf16.joinToString(separator = "") { String.format("\\u%04X", it.toInt())}
}