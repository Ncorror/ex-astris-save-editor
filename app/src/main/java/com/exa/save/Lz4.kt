package com.exa.save

/**
 * Raw LZ4 block codec.
 *
 * Ex Astris bundles store data blocks as "LZ4Lit": a normal LZ4 block stream whose token
 * nibbles are swapped (high nibble = match length, low nibble = literal length). Pass
 * [lit] = true for that variant.
 */
object Lz4 {
    private const val MIN_MATCH = 4
    private const val LAST_LITERALS = 5
    private const val MF_LIMIT = 12
    private const val MAX_DISTANCE = 0xFFFF
    private const val HASH_BITS = 16

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

    fun compress(src: ByteArray, lit: Boolean = false): ByteArray {
        val out = java.io.ByteArrayOutputStream(src.size / 2 + 16)
        val table = IntArray(1 shl HASH_BITS) { -1 }
        val matchLimit = src.size - LAST_LITERALS
        val searchLimit = src.size - MF_LIMIT
        var anchor = 0
        var i = 0
        while (i < searchLimit) {
            val h = hash(src, i)
            val candidate = table[h]
            table[h] = i
            if (candidate < 0 || i - candidate > MAX_DISTANCE || !same4(src, candidate, i)) {
                i++
                continue
            }
            var length = MIN_MATCH
            while (i + length < matchLimit && src[candidate + length] == src[i + length]) length++
            writeSequence(out, src, anchor, i - anchor, i - candidate, length - MIN_MATCH, lit)
            i += length
            anchor = i
        }
        writeLastLiterals(out, src, anchor, src.size - anchor, lit)
        return out.toByteArray()
    }

    private fun hash(src: ByteArray, at: Int): Int =
        (read32(src, at) * -1640531535) ushr (32 - HASH_BITS)

    private fun read32(src: ByteArray, at: Int): Int =
        (src[at].toInt() and 0xFF) or ((src[at + 1].toInt() and 0xFF) shl 8) or
            ((src[at + 2].toInt() and 0xFF) shl 16) or ((src[at + 3].toInt() and 0xFF) shl 24)

    private fun same4(src: ByteArray, a: Int, b: Int) = read32(src, a) == read32(src, b)

    private fun token(literals: Int, match: Int, lit: Boolean): Int {
        val l = minOf(literals, 15)
        val m = minOf(match, 15)
        return if (lit) (m shl 4) or l else (l shl 4) or m
    }

    private fun writeLength(out: java.io.ByteArrayOutputStream, length: Int) {
        if (length < 15) return
        var rest = length - 15
        while (rest >= 255) {
            out.write(255)
            rest -= 255
        }
        out.write(rest)
    }

    private fun writeSequence(
        out: java.io.ByteArrayOutputStream, src: ByteArray, from: Int, literals: Int,
        offset: Int, match: Int, lit: Boolean
    ) {
        out.write(token(literals, match, lit))
        writeLength(out, literals)
        out.write(src, from, literals)
        out.write(offset and 0xFF)
        out.write(offset ushr 8)
        writeLength(out, match)
    }

    private fun writeLastLiterals(
        out: java.io.ByteArrayOutputStream, src: ByteArray, from: Int, literals: Int, lit: Boolean
    ) {
        out.write(token(literals, 0, lit))
        writeLength(out, literals)
        out.write(src, from, literals)
    }
}
