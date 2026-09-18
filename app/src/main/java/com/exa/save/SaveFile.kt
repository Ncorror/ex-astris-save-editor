package com.exa.save

import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Ex Astris local .save format.
 *
 *   [module 0][module 1]...[SaveFileTail][30-byte trailer]
 *
 * Every module is compressed as a SEPARATE zstd frame. SaveFileTail stores `length` and
 * `fileOffset` per module, and both refer to COMPRESSED bytes.
 *
 * Trailer: u64 timestamp | u64 tail length | u32 (=4) | u8 (=1) | u8 compression flag | u64 dataLen
 * Invariant: dataLen + 30 == file size.
 */
class SaveFile(raw: ByteArray) {

    companion object {
        const val TRAILER = 30
        private const val NAMED_INT = 23

        /** Field-name bytes for ItemId, Odin NamedUInt entry. */
        val ITEM_ID = byteArrayOf(0x19, 0x01, 0x06, 0, 0, 0) + utf16("ItemId")
        val NUM = byteArrayOf(0x19, 0x01, 0x03, 0, 0, 0) + utf16("Num")

        fun utf16(s: String): ByteArray {
            val out = ByteArray(s.length * 2)
            for (i in s.indices) {
                out[i * 2] = (s[i].code and 0xFF).toByte()
                out[i * 2 + 1] = (s[i].code shr 8).toByte()
            }
            return out
        }

        fun le32(b: ByteArray, p: Int): Int =
            (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8) or
                    ((b[p + 2].toInt() and 0xFF) shl 16) or ((b[p + 3].toInt() and 0xFF) shl 24)

        fun le64(b: ByteArray, p: Int): Long {
            var v = 0L
            for (i in 7 downTo 0) v = (v shl 8) or (b[p + i].toLong() and 0xFF)
            return v
        }

        fun putLe32(b: ByteArray, p: Int, v: Int) {
            b[p] = (v and 0xFF).toByte()
            b[p + 1] = ((v shr 8) and 0xFF).toByte()
            b[p + 2] = ((v shr 16) and 0xFF).toByte()
            b[p + 3] = ((v shr 24) and 0xFF).toByte()
        }

        fun putLe64(b: ByteArray, p: Int, v: Long) {
            for (i in 0..7) b[p + i] = ((v shr (8 * i)) and 0xFF).toByte()
        }

        fun indexOf(hay: ByteArray, needle: ByteArray, from: Int = 0): Int {
            outer@ for (i in from..hay.size - needle.size) {
                for (j in needle.indices) if (hay[i + j] != needle[j]) continue@outer
                return i
            }
            return -1
        }

        fun lastIndexOf(hay: ByteArray, needle: ByteArray, before: Int): Int {
            outer@ for (i in minOf(before, hay.size - needle.size) downTo 0) {
                for (j in needle.indices) if (hay[i + j] != needle[j]) continue@outer
                return i
            }
            return -1
        }
    }

    val trailer: ByteArray = raw.copyOfRange(raw.size - TRAILER, raw.size)

    /** 1 = modules are zstd-compressed, 0 = stored raw. The game accepts both. */
    val compressedSource: Boolean get() = trailer[21].toInt() != 0
    val tail: ByteArray
    private val frames = ArrayList<ByteArray>()
    val mods = ArrayList<ByteArray>()
    private val lenPos = ArrayList<Int>()
    private val offPos = ArrayList<Int>()
    private val dirty = HashSet<Int>()

    /** Save timestamp, unix seconds. */
    val savedAt: Long get() = le64(trailer, 0)

    init {
        val tailLen = le64(trailer, 8).toInt()
        val dataLen = le64(trailer, 22).toInt()
        require(dataLen + TRAILER == raw.size) { "trailer does not match file size" }
        val body = raw.copyOfRange(0, raw.size - TRAILER)
        val comp = body.copyOfRange(0, body.size - tailLen)
        tail = body.copyOfRange(body.size - tailLen, body.size)

        collectFieldPositions(tail, "length", lenPos)
        collectFieldPositions(tail, "fileOffset", offPos)
        require(lenPos.size == offPos.size && lenPos.isNotEmpty()) { "broken SaveFileTail" }

        val compressed = compressedSource
        for (i in lenPos.indices) {
            val ln = le32(tail, lenPos[i])
            val off = le32(tail, offPos[i])
            val frame = comp.copyOfRange(off, off + ln)
            frames.add(frame)
            mods.add(if (compressed) unzstd(frame) else frame)
        }
    }

    private fun collectFieldPositions(buf: ByteArray, name: String, out: MutableList<Int>) {
        val key = byteArrayOf(NAMED_INT.toByte(), 0x01) +
                byteArrayOf(name.length.toByte(), 0, 0, 0) + utf16(name)
        var i = 0
        while (true) {
            val j = indexOf(buf, key, i)
            if (j < 0) return
            i = j + 1
            out.add(j + key.size)
        }
    }

    private fun unzstd(src: ByteArray): ByteArray {
        ZstdInputStream(ByteArrayInputStream(src)).use { zin ->
            val out = ByteArrayOutputStream(src.size * 4)
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = zin.read(buf)
                if (n <= 0) break
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }

    // ---------------- рюкзак ----------------

    private fun backpackIndex(): Int {
        val hits = mods.indices.filter { indexOf(mods[it], ITEM_ID) >= 0 }
        require(hits.size == 1) { "expected exactly one backpack module, found ${hits.size}" }
        return hits[0]
    }

    /** One dictionary entry: bounds inside the module, item id, count. */
    data class Entry(val a: Int, val b: Int, val id: Int, val num: Int)

    /**
     * Exact walk over the pair array. Scanning for boundary bytes does not work here:
     * 0x07 and 0x04 0x2e also occur inside the values themselves.
     */
    fun entries(): List<Entry> {
        val m = mods[backpackIndex()]
        val first = indexOf(m, ITEM_ID)
        val e0 = lastIndexOf(m, byteArrayOf(0x04, 0x2e), first)
        val start = e0 - 9
        require(start >= 0 && m[start].toInt() == 6) { "array header not found" }
        val cnt = le64(m, start + 1).toInt()
        val out = ArrayList<Entry>(cnt)
        var p = e0
        repeat(cnt) {
            val a = p
            require(m[p].toInt() == 0x04 && m[p + 1].toInt() == 0x2e) { "element does not start with 04 2e" }
            p += 12
            val key = le32(m, p); p += 14
            val t = m[p].toInt() and 0xFF
            p += when (t) {
                0x30 -> 5
                0x2f -> 10 + le32(m, p + 6) * 2
                else -> throw IllegalStateException("unknown type entry")
            }
            p += 26
            val num = le32(m, p + 12)
            p += 16
            require(m[p].toInt() == 0x05 && m[p + 1].toInt() == 0x05) { "element not terminated by 05 05" }
            p += 2
            out.add(Entry(a, p, key, num))
        }
        require(m[p].toInt() == 7) { "array not terminated" }
        return out
    }

    fun items(): LinkedHashMap<Int, Int> {
        val map = LinkedHashMap<Int, Int>()
        for (e in entries()) map[e.id] = e.num
        return map
    }

    /** In-place count edit: module length stays the same. */
    fun setItems(rule: (Int, Int) -> Int?): Int {
        val idx = backpackIndex()
        val m = mods[idx].copyOf()
        var changed = 0
        for (e in entries()) {
            val want = rule(e.id, e.num) ?: continue
            if (want == e.num) continue
            putLe32(m, e.b - 6, want)
            changed++
        }
        if (changed > 0) {
            mods[idx] = m
            dirty.add(idx)
        }
        return changed
    }

    /** Insert entries: clone a 75-byte element, patch key, node id, item id and count. */
    fun addItems(newItems: Map<Int, Int>): List<Int> {
        val idx = backpackIndex()
        val m = mods[idx]
        val have = items()
        val els = entries()
        val template = els.firstOrNull { it.b - it.a == 75 }
            ?: throw IllegalStateException("no template element found")
        val tpl = m.copyOfRange(template.a, template.b)
        var nid = maxNodeId(m) + 1
        val blocks = ByteArrayOutputStream()
        val added = ArrayList<Int>()
        for ((id, num) in newItems.toSortedMap()) {
            if (have.containsKey(id)) continue
            val e = tpl.copyOf()
            putLe32(e, 12, id)
            putLe32(e, 31, nid)
            putLe32(e, 53, id)
            putLe32(e, 69, num)
            blocks.write(e)
            added.add(id)
            nid++
        }
        if (added.isEmpty()) return added
        val first = indexOf(m, ITEM_ID)
        val e0 = lastIndexOf(m, byteArrayOf(0x04, 0x2e), first)
        val start = e0 - 9
        val cnt = le64(m, start + 1).toInt()
        val end = els.last().b
        val out = ByteArrayOutputStream(m.size + blocks.size())
        out.write(m, 0, start + 1)
        val c = ByteArray(8); putLe64(c, 0, (cnt + added.size).toLong()); out.write(c)
        out.write(m, start + 9, end - (start + 9))
        out.write(blocks.toByteArray())
        out.write(m, end, m.size - end)
        mods[idx] = out.toByteArray()
        dirty.add(idx)
        return added
    }

    /** Remove entries. The first element is never touched: it declares the type. */
    fun removeItems(ids: Set<Int>): List<Int> {
        val idx = backpackIndex()
        val m = mods[idx]
        val els = entries()
        val removed = ArrayList<Int>()
        val keep = ByteArrayOutputStream()
        for (e in els) {
            if (e.id in ids && e.b - e.a == 75) {
                removed.add(e.id)
            } else {
                keep.write(m, e.a, e.b - e.a)
            }
        }
        if (removed.isEmpty()) return removed
        val start = els[0].a - 9
        val cnt = le64(m, start + 1).toInt()
        val end = els.last().b
        val out = ByteArrayOutputStream(m.size)
        out.write(m, 0, start + 1)
        val c = ByteArray(8); putLe64(c, 0, (cnt - removed.size).toLong()); out.write(c)
        out.write(keep.toByteArray())
        out.write(m, end, m.size - end)
        mods[idx] = out.toByteArray()
        dirty.add(idx)
        return removed
    }

    private fun maxNodeId(m: ByteArray): Int {
        var best = 0
        var p = 0
        val pat = byteArrayOf(0x30, 0x03, 0, 0, 0)
        while (true) {
            val j = indexOf(m, pat, p)
            if (j < 0) return best
            p = j + 9
            best = maxOf(best, le32(m, j + 5))
        }
    }

    // ---------------- сборка ----------------

    /**
     * Only the modified module is recompressed, every other frame is copied byte for byte.
     * Tail offsets and trailer dataLen are recalculated.
     */
    fun build(level: Int = 3, compress: Boolean? = null): ByteArray {
        // The trailer flag covers the whole file, so raw and compressed modules cannot be mixed.
        val outCompressed = compress ?: compressedSource
        val body = ByteArrayOutputStream()
        val positions = ArrayList<Pair<Int, Int>>(mods.size)
        for (i in mods.indices) {
            val frame = when {
                !outCompressed -> mods[i]
                compressedSource && i !in dirty -> frames[i]
                else -> Zstd.compress(mods[i], level)
            }
            positions.add(Pair(body.size(), frame.size))
            body.write(frame)
        }
        val newTail = tail.copyOf()
        for (i in lenPos.indices) {
            putLe32(newTail, lenPos[i], positions[i].second)
            putLe32(newTail, offPos[i], positions[i].first)
        }
        val newTrailer = trailer.copyOf()
        newTrailer[21] = if (outCompressed) 1 else 0
        putLe64(newTrailer, 22, (body.size() + newTail.size).toLong())
        val out = ByteArrayOutputStream(body.size() + newTail.size + TRAILER)
        out.write(body.toByteArray())
        out.write(newTail)
        out.write(newTrailer)
        return out.toByteArray()
    }
}
