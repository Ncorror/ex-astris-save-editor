package com.exa.save

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SaveFileTest {
    private fun field(name: String, value: Int): ByteArray {
        val bytes = byteArrayOf(23, 1, name.length.toByte(), 0, 0, 0) + SaveFile.utf16(name)
        return bytes + ByteArray(4).also { SaveFile.putLe32(it, 0, value) }
    }

    private fun rawSave(vararg modules: ByteArray): ByteArray {
        val data = modules.fold(ByteArray(0)) { a, b -> a + b }
        var offset = 0
        val tail = modules.fold(ByteArray(0)) { a, module ->
            val entry = field("length", module.size) + field("fileOffset", offset)
            offset += module.size
            a + entry
        }
        val trailer = ByteArray(SaveFile.TRAILER)
        SaveFile.putLe64(trailer, 8, tail.size.toLong())
        trailer[20] = 1
        trailer[21] = 0 // raw modules, so no zstd native dependency in this test
        SaveFile.putLe64(trailer, 22, (data.size + tail.size).toLong())
        return data + tail + trailer
    }

    @Test fun validModulesRoundTripWithoutChangingBytes() {
        val source = rawSave(byteArrayOf(1, 2, 3), byteArrayOf(4, 5))
        val parsed = SaveFile(source)
        assertEquals(2, parsed.mods.size)
        assertArrayEquals(source, parsed.build())
    }

    @Test fun truncatedTrailerAndOversizedTailAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { SaveFile(ByteArray(29)) }
        val bad = rawSave(byteArrayOf(1))
        SaveFile.putLe64(bad, bad.size - SaveFile.TRAILER + 8, Long.MAX_VALUE)
        assertThrows(IllegalArgumentException::class.java) { SaveFile(bad) }
    }

    @Test fun overlappingOrEscapingModuleRangesAreRejected() {
        val source = rawSave(byteArrayOf(1, 2), byteArrayOf(3))
        val tailStart = source.size - SaveFile.TRAILER - 2 * (field("length", 0).size + field("fileOffset", 0).size)
        val offsetField = field("length", 0).size + field("fileOffset", 0).size + field("length", 0).size
        SaveFile.putLe32(source, tailStart + offsetField + field("fileOffset", 0).size - 4, 1)
        assertThrows(IllegalArgumentException::class.java) { SaveFile(source) }

        val outside = rawSave(byteArrayOf(1, 2))
        val firstLengthValue = outside.size - SaveFile.TRAILER -
            (field("length", 0).size + field("fileOffset", 0).size) + field("length", 0).size - 4
        SaveFile.putLe32(outside, firstLengthValue, 100)
        assertThrows(IllegalArgumentException::class.java) { SaveFile(outside) }
    }

    @Test fun invalidCompressionFlagIsRejected() {
        val source = rawSave(byteArrayOf(1))
        source[source.size - SaveFile.TRAILER + 21] = 2
        assertThrows(IllegalArgumentException::class.java) { SaveFile(source) }
    }

    @Test fun excessiveModuleCountIsRejected() {
        val modules = Array(257) { byteArrayOf(1) }
        assertThrows(IllegalArgumentException::class.java) { SaveFile(rawSave(*modules)) }
    }
}
