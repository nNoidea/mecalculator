package com.nnoidea.mecalculator.data

import com.nnoidea.mecalculator.ui.main.HistoryEntry
import com.nnoidea.mecalculator.ui.main.MathToken

/**
 * Compact, deterministic serializer & deserializer for 2D MathToken ASTs and History entries.
 */
object TokenSerialization {
    private const val ENTRY_SEP = '\n'
    private const val ENTRY_PART_SEP = '\t'

    fun serializeTokens(tokens: List<MathToken>): String = buildString {
        for (tok in tokens) {
            when (tok) {
                is MathToken.CharToken -> {
                    append("C:")
                    append(tok.ch)
                }
                is MathToken.FuncToken -> {
                    append("U:")
                        .append(tok.name.length)
                        .append(':')
                        .append(tok.name)
                }
                is MathToken.FractionToken -> {
                    val n = serializeTokens(tok.num)
                    val d = serializeTokens(tok.den)
                    append("F:")
                        .append(n.length)
                        .append(':')
                        .append(d.length)
                        .append(':')
                        .append(n)
                        .append(d)
                }
                is MathToken.PowerToken -> {
                    val e = serializeTokens(tok.exp)
                    append("P:")
                        .append(e.length)
                        .append(':')
                        .append(e)
                }
                is MathToken.SqrtToken -> {
                    val r = serializeTokens(tok.rad)
                    append("S:")
                        .append(r.length)
                        .append(':')
                        .append(r)
                }
            }
        }
    }

    fun deserializeTokens(raw: String): List<MathToken> {
        if (raw.isEmpty()) return emptyList()
        val result = mutableListOf<MathToken>()
        var i = 0
        while (i < raw.length) {
            when {
                raw.startsWith("C:", i) -> {
                    if (i + 2 < raw.length) {
                        result.add(MathToken.CharToken(raw[i + 2]))
                        i += 3
                    } else {
                        break
                    }
                }
                raw.startsWith("U:", i) -> {
                    val colon = raw.indexOf(':', i + 2)
                    if (colon == -1) break
                    val len = raw.substring(i + 2, colon).toIntOrNull() ?: break
                    val start = colon + 1
                    val end = start + len
                    if (end > raw.length) break
                    result.add(MathToken.FuncToken(raw.substring(start, end)))
                    i = end
                }
                raw.startsWith("F:", i) -> {
                    val firstColon = raw.indexOf(':', i + 2)
                    if (firstColon == -1) break
                    val secondColon = raw.indexOf(':', firstColon + 1)
                    if (secondColon == -1) break
                    val nLen = raw.substring(i + 2, firstColon).toIntOrNull() ?: break
                    val dLen = raw.substring(firstColon + 1, secondColon).toIntOrNull() ?: break
                    val nStart = secondColon + 1
                    val dStart = nStart + nLen
                    val end = dStart + dLen
                    if (end > raw.length) break
                    val num = deserializeTokens(raw.substring(nStart, dStart))
                    val den = deserializeTokens(raw.substring(dStart, end))
                    result.add(MathToken.FractionToken(num = num, den = den))
                    i = end
                }
                raw.startsWith("P:", i) -> {
                    val colon = raw.indexOf(':', i + 2)
                    if (colon == -1) break
                    val eLen = raw.substring(i + 2, colon).toIntOrNull() ?: break
                    val eStart = colon + 1
                    val end = eStart + eLen
                    if (end > raw.length) break
                    val exp = deserializeTokens(raw.substring(eStart, end))
                    result.add(MathToken.PowerToken(exp = exp))
                    i = end
                }
                raw.startsWith("S:", i) -> {
                    val colon = raw.indexOf(':', i + 2)
                    if (colon == -1) break
                    val rLen = raw.substring(i + 2, colon).toIntOrNull() ?: break
                    val rStart = colon + 1
                    val end = rStart + rLen
                    if (end > raw.length) break
                    val rad = deserializeTokens(raw.substring(rStart, end))
                    result.add(MathToken.SqrtToken(rad = rad))
                    i = end
                }
                else -> break
            }
        }
        return result
    }

    fun serializeHistory(entries: List<HistoryEntry>): String =
        entries.joinToString(ENTRY_SEP.toString()) { entry ->
            "${serializeTokens(entry.tokens)}$ENTRY_PART_SEP${entry.result}"
        }

    fun deserializeHistory(raw: String): List<HistoryEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split(ENTRY_SEP).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split(ENTRY_PART_SEP)
            if (parts.size != 2) return@mapNotNull null
            val tokens = deserializeTokens(parts[0])
            val result = parts[1]
            if (tokens.isEmpty() || result.isEmpty()) null else HistoryEntry(tokens = tokens, result = result)
        }
    }
}
