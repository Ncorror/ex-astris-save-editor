package com.exa.save

import java.io.ByteArrayOutputStream

/**
 * Raw LZ4 block codec.
 *
 * Ex Astris bundles store data blocks as "LZ4Lit": a normal LZ4 block stream whose token
 * nibbles are swapped (high nibble = match length, low nibble = literal length). Pass
 * [lit] = true for that variant.
 *
 * [compress] is a port of the reference LZ4HC level 12 ("optimal") compressor from lz4 1.9.4,
 * which is what the game's bundles were built with. Keeping the exact algorithm matters: for
 * the same input it produces byte-identical output, so a restored map file is the original
 * file again, not merely an equivalent one.
 */
object Lz4 {
    private const val MIN_MATCH = 4
    private const val LAST_LITERALS = 5
    private const val MF_LIMIT = 12
    private const val RUN_MASK = 15
    private const val ML_MASK = 15
    private const val MAX_DISTANCE = 65535

    fun decompress(src: ByteArray, size: Int, lit: Boolean = false): ByteArray {
        require(size >= 0) { "invalid LZ4 size" }
        val dst = ByteArray(size)
        var s = 0
        var d = 0
        while (s < src.size) {
            val token = src[s++].toInt() and 0xFF
            var literals = if (lit) token and 0x0F else token ushr 4
            var match = if (lit) token ushr 4 else token and 0x0F
            if (literals == 15) {
                while (true) {
                    require(s < src.size) { "truncated LZ4 literal length" }
                    val b = src[s++].toInt() and 0xFF
                    literals += b
                    if (b != 255) break
                }
            }
            require(literals <= src.size - s && literals <= size - d) { "LZ4 literals out of range" }
            System.arraycopy(src, s, dst, d, literals)
            s += literals
            d += literals
            if (s == src.size) break
            require(s + 2 <= src.size) { "truncated LZ4 offset" }
            val offset = (src[s].toInt() and 0xFF) or ((src[s + 1].toInt() and 0xFF) shl 8)
            s += 2
            require(offset in 1..d) { "invalid LZ4 offset" }
            if (match == 15) {
                while (true) {
                    require(s < src.size) { "truncated LZ4 match length" }
                    val b = src[s++].toInt() and 0xFF
                    match += b
                    if (b != 255) break
                }
            }
            match += MIN_MATCH
            require(match <= size - d) { "LZ4 match out of range" }
            // Byte-wise copy: matches may overlap their own output.
            var from = d - offset
            repeat(match) { dst[d++] = dst[from++] }
        }
        require(d == size) { "LZ4 size mismatch" }
        return dst
    }

    fun compress(src: ByteArray, lit: Boolean = false): ByteArray = HcOptimal(src, lit).run()

    /** LZ4HC_compress_optimal with level 12 parameters, single block, no dictionary. */
    private class HcOptimal(private val src: ByteArray, private val lit: Boolean) {
        private companion object {
            const val HASH_LOG = 15
            const val OPT_NUM = 1 shl 12
            const val TRAILING_LITERALS = 3
            const val NB_SEARCHES = 16384
            const val SUFFICIENT_LEN = OPT_NUM - 1
            /** Index of src[0], as in a freshly initialized LZ4HC context. */
            const val PREFIX_IDX = 65536
        }

        private val hashTable = IntArray(1 shl HASH_LOG)
        private val chainTable = IntArray(1 shl 16) // U16 deltas
        private var nextToUpdate = PREFIX_IDX
        private val out = ByteArrayOutputStream(src.size / 2 + 16)

        private val optPrice = IntArray(OPT_NUM + TRAILING_LITERALS)
        private val optOff = IntArray(OPT_NUM + TRAILING_LITERALS)
        private val optMlen = IntArray(OPT_NUM + TRAILING_LITERALS)
        private val optLitlen = IntArray(OPT_NUM + TRAILING_LITERALS)

        private var ip = 0
        private var anchor = 0
        private var matchLen = 0
        private var matchOff = 0

        private fun read32(p: Int) =
            (src[p].toInt() and 0xFF) or ((src[p + 1].toInt() and 0xFF) shl 8) or
                ((src[p + 2].toInt() and 0xFF) shl 16) or ((src[p + 3].toInt() and 0xFF) shl 24)

        private fun read16(p: Int) = (src[p].toInt() and 0xFF) or ((src[p + 1].toInt() and 0xFF) shl 8)

        private fun hash(p: Int) = (read32(p) * -1640531535) ushr (32 - HASH_LOG)

        private fun chain(index: Int) = chainTable[index and 0xFFFF]

        private fun insert(target: Int) {
            var idx = nextToUpdate
            while (idx < target) {
                val h = hash(idx - PREFIX_IDX)
                chainTable[idx and 0xFFFF] = minOf(idx - hashTable[h], MAX_DISTANCE)
                hashTable[h] = idx
                idx++
            }
            nextToUpdate = target
        }

        /** Equal bytes from a and b, stopping at limit (exclusive) for a. */
        private fun count(a: Int, b: Int, limit: Int): Int {
            var n = 0
            while (a + n < limit && src[a + n] == src[b + n]) n++
            return n
        }

        private fun patternByte(pattern: Int, k: Int) = ((pattern ushr ((k and 3) shl 3)) and 0xFF).toByte()

        private fun countPattern(from: Int, end: Int, pattern: Int): Int {
            var n = 0
            while (from + n < end && src[from + n] == patternByte(pattern, n)) n++
            return n
        }

        private fun reverseCountPattern(from: Int, low: Int, pattern: Int): Int {
            var n = 0
            while (from - n - 1 >= low && src[from - n - 1] == patternByte(pattern, -(n + 1))) n++
            return n
        }

        private fun protectDictEnd(matchIndex: Int) = ((PREFIX_IDX - 1 - matchIndex).toLong() and 0xFFFFFFFFL) >= 3

        /** LZ4HC_FindLongerMatch / LZ4HC_InsertAndGetWiderMatch (forward search, pattern analysis, chain swap). */
        private fun findLongerMatch(at: Int, highLimit: Int, minLen: Int): Boolean {
            val ipIndex = at + PREFIX_IDX
            val lowestMatchIndex = if (PREFIX_IDX + MAX_DISTANCE + 1 > ipIndex) PREFIX_IDX else ipIndex - MAX_DISTANCE
            var nbAttempts = NB_SEARCHES
            var matchChainPos = 0
            val pattern = read32(at)
            var repeat = 0 // 0 untested, 1 not, 2 confirmed
            var srcPatternLength = 0
            var longest = minLen
            var bestMatchIndex = 0

            insert(ipIndex)
            var matchIndex = hashTable[hash(at)]

            while (matchIndex >= lowestMatchIndex && nbAttempts > 0) {
                var length = 0
                nbAttempts--
                val matchPtr = matchIndex - PREFIX_IDX
                if (read16(at + longest - 1) == read16(matchPtr + longest - 1)) {
                    if (read32(matchPtr) == pattern) {
                        length = MIN_MATCH + count(at + MIN_MATCH, matchPtr + MIN_MATCH, highLimit)
                        if (length > longest) {
                            longest = length
                            bestMatchIndex = matchIndex
                        }
                    }
                }

                if (length == longest) {
                    if (matchIndex + longest <= ipIndex) {
                        val kTrigger = 4
                        var distanceToNextMatch = 1
                        val end = longest - MIN_MATCH + 1
                        var step: Int
                        var accel = 1 shl kTrigger
                        var pos = 0
                        while (pos < end) {
                            val candidateDist = chain(matchIndex + pos)
                            step = accel++ shr kTrigger
                            if (candidateDist > distanceToNextMatch) {
                                distanceToNextMatch = candidateDist
                                matchChainPos = pos
                                accel = 1 shl kTrigger
                            }
                            pos += step
                        }
                        if (distanceToNextMatch > 1) {
                            if (distanceToNextMatch > matchIndex) break
                            matchIndex -= distanceToNextMatch
                            continue
                        }
                    }
                }

                val distNextMatch = chain(matchIndex)
                if (distNextMatch == 1 && matchChainPos == 0) {
                    val matchCandidateIdx = matchIndex - 1
                    if (repeat == 0) {
                        repeat = if ((pattern and 0xFFFF) == (pattern ushr 16) &&
                            (pattern and 0xFF) == (pattern ushr 24)) {
                            srcPatternLength = countPattern(at + 4, highLimit, pattern) + 4
                            2
                        } else 1
                    }
                    if (repeat == 2 && matchCandidateIdx >= lowestMatchIndex && protectDictEnd(matchCandidateIdx)) {
                        val candidatePtr = matchCandidateIdx - PREFIX_IDX
                        if (read32(candidatePtr) == pattern) {
                            val forwardPatternLength = countPattern(candidatePtr + 4, highLimit, pattern) + 4
                            var backLength = reverseCountPattern(candidatePtr, 0, pattern)
                            backLength = matchCandidateIdx - maxOf(matchCandidateIdx - backLength, lowestMatchIndex)
                            val currentSegmentLength = backLength + forwardPatternLength
                            if (currentSegmentLength >= srcPatternLength && forwardPatternLength <= srcPatternLength) {
                                val newMatchIndex = matchCandidateIdx + forwardPatternLength - srcPatternLength
                                matchIndex = if (protectDictEnd(newMatchIndex)) newMatchIndex else PREFIX_IDX
                            } else {
                                val newMatchIndex = matchCandidateIdx - backLength
                                if (!protectDictEnd(newMatchIndex)) {
                                    matchIndex = PREFIX_IDX
                                } else {
                                    matchIndex = newMatchIndex
                                    val maxML = minOf(currentSegmentLength, srcPatternLength)
                                    if (longest < maxML) {
                                        if (ipIndex - matchIndex > MAX_DISTANCE) break
                                        longest = maxML
                                        bestMatchIndex = matchIndex
                                    }
                                    val distToNextPattern = chain(matchIndex)
                                    if (distToNextPattern > matchIndex) break
                                    matchIndex -= distToNextPattern
                                }
                            }
                            continue
                        }
                    }
                }

                matchIndex -= chain(matchIndex + matchChainPos)
            }

            if (longest <= minLen) return false
            matchLen = longest
            matchOff = ipIndex - bestMatchIndex
            return true
        }

        private fun literalsPrice(litlen: Int): Int {
            var price = litlen
            if (litlen >= RUN_MASK) price += 1 + (litlen - RUN_MASK) / 255
            return price
        }

        private fun sequencePrice(litlen: Int, mlen: Int): Int {
            var price = 1 + 2 + literalsPrice(litlen)
            if (mlen >= ML_MASK + MIN_MATCH) price += 1 + (mlen - (ML_MASK + MIN_MATCH)) / 255
            return price
        }

        private fun token(literals: Int, match: Int): Int {
            val l = minOf(literals, RUN_MASK)
            val m = minOf(match, ML_MASK)
            return if (lit) (m shl 4) or l else (l shl 4) or m
        }

        private fun encodeSequence(matchLength: Int, offset: Int) {
            val literals = ip - anchor
            out.write(token(literals, matchLength - MIN_MATCH))
            if (literals >= RUN_MASK) {
                var len = literals - RUN_MASK
                while (len >= 255) { out.write(255); len -= 255 }
                out.write(len)
            }
            out.write(src, anchor, literals)
            out.write(offset and 0xFF)
            out.write(offset ushr 8)
            var length = matchLength - MIN_MATCH
            if (length >= ML_MASK) {
                length -= ML_MASK
                while (length >= 510) { out.write(255); out.write(255); length -= 510 }
                if (length >= 255) { length -= 255; out.write(255) }
                out.write(length)
            }
            ip += matchLength
            anchor = ip
        }

        private fun setOpt(pos: Int, mlen: Int, off: Int, litlen: Int, price: Int) {
            optMlen[pos] = mlen; optOff[pos] = off; optLitlen[pos] = litlen; optPrice[pos] = price
        }

        fun run(): ByteArray {
            val iend = src.size
            val mflimit = iend - MF_LIMIT
            val matchlimit = iend - LAST_LITERALS

            while (ip <= mflimit) {
                val llen = ip - anchor
                if (!findLongerMatch(ip, matchlimit, MIN_MATCH - 1)) { ip++; continue }
                val firstLen = matchLen
                val firstOff = matchOff

                if (firstLen > SUFFICIENT_LEN) {
                    encodeSequence(firstLen, firstOff)
                    continue
                }

                for (rPos in 0 until MIN_MATCH) setOpt(rPos, 1, 0, llen + rPos, literalsPrice(llen + rPos))
                for (mlen in MIN_MATCH..firstLen) setOpt(mlen, mlen, firstOff, llen, sequencePrice(llen, mlen))
                var lastMatchPos = firstLen
                for (addLit in 1..TRAILING_LITERALS) {
                    setOpt(lastMatchPos + addLit, 1, 0, addLit, optPrice[lastMatchPos] + literalsPrice(addLit))
                }

                var bestMlen: Int
                var bestOff: Int
                var cur = 1
                var immediate = false
                bestMlen = 0; bestOff = 0
                while (cur < lastMatchPos) {
                    val curPtr = ip + cur
                    if (curPtr > mflimit) break
                    if (optPrice[cur + 1] <= optPrice[cur] && optPrice[cur + MIN_MATCH] < optPrice[cur] + 3) {
                        cur++; continue
                    }
                    if (!findLongerMatch(curPtr, matchlimit, MIN_MATCH - 1)) { cur++; continue }
                    val newLen = matchLen
                    val newOff = matchOff

                    if (newLen > SUFFICIENT_LEN || newLen + cur >= OPT_NUM) {
                        bestMlen = newLen
                        bestOff = newOff
                        lastMatchPos = cur + 1
                        immediate = true
                        break
                    }

                    val baseLitlen = optLitlen[cur]
                    for (litlen in 1 until MIN_MATCH) {
                        val price = optPrice[cur] - literalsPrice(baseLitlen) + literalsPrice(baseLitlen + litlen)
                        val pos = cur + litlen
                        if (price < optPrice[pos]) setOpt(pos, 1, 0, baseLitlen + litlen, price)
                    }

                    for (ml in MIN_MATCH..newLen) {
                        val pos = cur + ml
                        val ll: Int
                        val price: Int
                        if (optMlen[cur] == 1) {
                            ll = optLitlen[cur]
                            price = (if (cur > ll) optPrice[cur - ll] else 0) + sequencePrice(ll, ml)
                        } else {
                            ll = 0
                            price = optPrice[cur] + sequencePrice(0, ml)
                        }
                        if (pos > lastMatchPos + TRAILING_LITERALS || price <= optPrice[pos]) {
                            if (ml == newLen && lastMatchPos < pos) lastMatchPos = pos
                            setOpt(pos, ml, newOff, ll, price)
                        }
                    }
                    for (addLit in 1..TRAILING_LITERALS) {
                        setOpt(lastMatchPos + addLit, 1, 0, addLit, optPrice[lastMatchPos] + literalsPrice(addLit))
                    }
                    cur++
                }

                if (!immediate) {
                    bestMlen = optMlen[lastMatchPos]
                    bestOff = optOff[lastMatchPos]
                    cur = lastMatchPos - bestMlen
                }

                // Reverse traversal: turn the chosen path into forward links.
                var candidatePos = cur
                var selectedLength = bestMlen
                var selectedOffset = bestOff
                while (true) {
                    val nextLength = optMlen[candidatePos]
                    val nextOffset = optOff[candidatePos]
                    optMlen[candidatePos] = selectedLength
                    optOff[candidatePos] = selectedOffset
                    selectedLength = nextLength
                    selectedOffset = nextOffset
                    if (nextLength > candidatePos) break
                    candidatePos -= nextLength
                }

                var rPos = 0
                while (rPos < lastMatchPos) {
                    val ml = optMlen[rPos]
                    val offset = optOff[rPos]
                    if (ml == 1) { ip++; rPos++; continue }
                    rPos += ml
                    encodeSequence(ml, offset)
                }
            }

            val lastRun = iend - anchor
            out.write(token(lastRun, 0))
            if (lastRun >= RUN_MASK) {
                var acc = lastRun - RUN_MASK
                while (acc >= 255) { out.write(255); acc -= 255 }
                out.write(acc)
            }
            out.write(src, anchor, lastRun)
            return out.toByteArray()
        }
    }
}
