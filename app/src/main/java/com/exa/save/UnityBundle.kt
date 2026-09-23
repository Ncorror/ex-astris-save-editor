package com.exa.save

import java.io.ByteArrayOutputStream

/**
 * Minimal UnityFS (bundle format 6–8) reader/writer for in-place edits that keep the
 * uncompressed data length unchanged. Only blocks whose bytes changed are recompressed;
 * every other byte of the container is carried over as-is.
 */
class UnityBundle private constructor(
    private val source: ByteArray,
    private val sizeFieldAt: Int,
    private val flags: Int,
    private val blocksInfoAt: Int,
    private val blocksInfo: ByteArray,
    private val blocks: List<Block>,
    /** Decompressed contents of all data blocks, concatenated. */
    val data: ByteArray
) {
    private class Block(val uncompressedSize: Int, val raw: ByteArray, val flags: Int)

    companion object {
        private val SIGNATURE = "UnityFS\u0000".toByteArray(Charsets.US_ASCII)
        private const val MAX_BUNDLE_BYTES = 64 * 1024 * 1024
        private const val MAX_DATA_BYTES = 256 * 1024 * 1024
        private const val MAX_BLOCKS = 4096
        private const val BLOCKS_INFO_AT_END = 0x80
        private const val BLOCK_INFO_PADDING = 0x200
        private const val COMPRESSION_MASK = 0x3F
        private const val NONE = 0
        private const val LZ4 = 2
        private const val LZ4HC = 3
        private const val LZ4LIT = 5

        fun parse(bytes: ByteArray): UnityBundle {
            require(bytes.size in 64..MAX_BUNDLE_BYTES) { "invalid bundle size" }
            require(bytes.copyOfRange(0, SIGNATURE.size).contentEquals(SIGNATURE)) { "not a UnityFS bundle" }
            var p = SIGNATURE.size
            val version = be32(bytes, p); p += 4
            require(version in 6..8) { "unsupported UnityFS version $version" }
            p = skipCString(bytes, p)
            p = skipCString(bytes, p)
            val sizeFieldAt = p
            require(p + 20 <= bytes.size) { "truncated bundle header" }
            require(be64(bytes, p) == bytes.size.toLong()) { "bundle size mismatch" }
            p += 8
            val compressedInfoSize = be32(bytes, p); p += 4
            val infoSize = be32(bytes, p); p += 4
            val flags = be32(bytes, p); p += 4
            require(flags and BLOCKS_INFO_AT_END == 0) { "unsupported bundle layout" }
            if (version >= 7) p = align16(p)
            require(compressedInfoSize in 1..bytes.size - p && infoSize in 1..1024 * 1024) {
                "invalid block table size"
            }
            val infoRaw = bytes.copyOfRange(p, p + compressedInfoSize)
            val info = when (flags and COMPRESSION_MASK) {
                NONE -> infoRaw.also { require(it.size == infoSize) { "block table size mismatch" } }
                LZ4, LZ4HC -> Lz4.decompress(infoRaw, infoSize)
                else -> throw IllegalArgumentException("unsupported block table compression")
            }
            var dataAt = p + compressedInfoSize
            if (flags and BLOCK_INFO_PADDING != 0) dataAt = align16(dataAt)

            var q = 16 // uncompressed data hash
            require(q + 4 <= info.size) { "truncated block table" }
            val count = be32(info, q); q += 4
            require(count in 1..MAX_BLOCKS && q + count * 10 <= info.size) { "invalid block count" }
            val blocks = ArrayList<Block>(count)
            val data = ByteArrayOutputStream()
            var at = dataAt
            repeat(count) {
                val size = be32(info, q)
                val compressed = be32(info, q + 4)
                val blockFlags = be16(info, q + 8)
                q += 10
                require(size >= 0 && compressed >= 0 && compressed <= bytes.size - at) { "block out of range" }
                require(data.size() + size.toLong() <= MAX_DATA_BYTES) { "bundle data too large" }
                val raw = bytes.copyOfRange(at, at + compressed)
                at += compressed
                data.write(decode(raw, size, blockFlags))
                blocks.add(Block(size, raw, blockFlags))
            }
            require(at == bytes.size) { "unexpected bytes after bundle data" }
            return UnityBundle(bytes, sizeFieldAt, flags, p, info, blocks, data.toByteArray())
        }

        private fun decode(raw: ByteArray, size: Int, blockFlags: Int): ByteArray =
            when (blockFlags and COMPRESSION_MASK) {
                NONE -> raw.also { require(it.size == size) { "stored block size mismatch" } }
                LZ4, LZ4HC -> Lz4.decompress(raw, size)
                LZ4LIT -> Lz4.decompress(raw, size, lit = true)
                else -> throw IllegalArgumentException("unsupported block compression")
            }

        private fun encode(data: ByteArray, blockFlags: Int): ByteArray =
            when (blockFlags and COMPRESSION_MASK) {
                NONE -> data
                LZ4, LZ4HC -> Lz4.compress(data)
                LZ4LIT -> Lz4.compress(data, lit = true)
                else -> throw IllegalArgumentException("unsupported block compression")
            }

        private fun skipCString(b: ByteArray, from: Int): Int {
            var i = from
            while (i < b.size && i - from < 256) {
                if (b[i].toInt() == 0) return i + 1
                i++
            }
            throw IllegalArgumentException("invalid bundle header string")
        }

        private fun align16(p: Int) = (p + 15) and 15.inv()

        private fun be16(b: ByteArray, p: Int) =
            ((b[p].toInt() and 0xFF) shl 8) or (b[p + 1].toInt() and 0xFF)

        private fun be32(b: ByteArray, p: Int) =
            ((b[p].toInt() and 0xFF) shl 24) or ((b[p + 1].toInt() and 0xFF) shl 16) or
                ((b[p + 2].toInt() and 0xFF) shl 8) or (b[p + 3].toInt() and 0xFF)

        private fun be64(b: ByteArray, p: Int) =
            (be32(b, p).toLong() shl 32) or (be32(b, p + 4).toLong() and 0xFFFFFFFFL)

        private fun putBe32(b: ByteArray, p: Int, v: Int) {
            b[p] = (v ushr 24).toByte(); b[p + 1] = (v ushr 16).toByte()
            b[p + 2] = (v ushr 8).toByte(); b[p + 3] = v.toByte()
        }

        private fun putBe64(b: ByteArray, p: Int, v: Long) {
            putBe32(b, p, (v ushr 32).toInt())
            putBe32(b, p + 4, v.toInt())
        }
    }

    /** Returns a bundle whose decompressed data is [newData]; the length must not change. */
    fun rebuild(newData: ByteArray): ByteArray {
        require(newData.size == data.size) { "bundle data length changed" }
        val raws = ArrayList<ByteArray>(blocks.size)
        val table = blocksInfo.copyOf()
        var offset = 0
        blocks.forEachIndexed { index, block ->
            val slice = newData.copyOfRange(offset, offset + block.uncompressedSize)
            val unchanged = slice.contentEquals(data.copyOfRange(offset, offset + block.uncompressedSize))
            val raw = if (unchanged) block.raw else encode(slice, block.flags)
            putBe32(table, 20 + index * 10 + 4, raw.size)
            raws.add(raw)
            offset += block.uncompressedSize
        }

        val info = when (flags and COMPRESSION_MASK) {
            NONE -> table
            else -> Lz4.compress(table)
        }
        val out = ByteArrayOutputStream(source.size + 64)
        out.write(source, 0, blocksInfoAt)
        out.write(info)
        if (flags and BLOCK_INFO_PADDING != 0) {
            while (out.size() % 16 != 0) out.write(0)
        }
        raws.forEach { out.write(it) }
        val bytes = out.toByteArray()
        putBe64(bytes, sizeFieldAt, bytes.size.toLong())
        putBe32(bytes, sizeFieldAt + 8, info.size)

        // Never hand out a container this reader cannot open back to the requested data.
        check(parse(bytes).data.contentEquals(newData)) { "rebuilt bundle failed verification" }
        return bytes
    }
}
