package com.exa.save

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Random

/** Synthetic bundles that mirror the layout of the real character map files; no game data. */
class CollabSkinPatchTest {
    private val dir = "Assets/Res/Prefabs/Characters/MsBlack/"
    private val scene = "${dir}P_MS_Tpose_hero_02_scene.prefab"
    private val battle = "${dir}P_MS_Tpose_hero_02_battle.prefab"
    private val arkScene = "${dir}P_MS_Tpose_hero_02_ArkKnights_scene.prefab"
    private val arkBattle = "${dir}P_MS_Tpose_hero_02_ArkKnights_battle.prefab"

    private fun ByteArrayOutputStream.int(v: Int) {
        write(v); write(v ushr 8); write(v ushr 16); write(v ushr 24)
    }

    private fun ByteArrayOutputStream.string(s: String) {
        int(s.length)
        write(s.toByteArray())
        repeat((4 - s.length % 4) % 4) { write(0) }
    }

    private fun mesh(name: String, path: String) = ByteArrayOutputStream().apply {
        int(1); int(1); string(name); string(path)
        write(ByteArray(36)); string(path); write(ByteArray(32))
    }.toByteArray()

    private val filler = ByteArray(40_000).also { Random(7).nextBytes(it) }

    /** Filler, then a controller: styleId, scene, ids, battle, ids, count 2, extra scene, extra battle. */
    private fun node(first: String, second: String, third: String, fourth: String): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(filler)
        out.int(0)
        out.write(mesh("P_MS_Tpose_hero_02_scene", first))
        out.int(1); out.int(0)
        out.write(mesh("P_MS_Tpose_hero_02_battle", second))
        out.int(1); out.int(1)
        out.int(2)
        out.write(mesh("P_MS_Tpose_hero_02_scene", third))
        out.write(mesh("P_MS_Tpose_hero_02_battle", fourth))
        out.write("trailing body data".repeat(200).toByteArray())
        return out.toByteArray()
    }

    private fun stockNode() = node(scene, battle, arkScene, arkBattle)

    /** UnityFS v8 with LZ4 block table and two LZ4Lit blocks, like the game's map bundles. */
    private fun bundle(data: ByteArray): ByteArray {
        val split = filler.size
        val blocks = listOf(data.copyOfRange(0, split), data.copyOfRange(split, data.size))
            .map { it.size to Lz4.compress(it, lit = true) }
        val table = ByteArrayOutputStream().apply {
            write(ByteArray(16))
            be32(blocks.size)
            blocks.forEach { (size, raw) -> be32(size); be32(raw.size); write(0); write(5) }
            be32(1)
            write(ByteArray(8)); be32(0); be32(data.size); be32(4)
            write("CAB-test".toByteArray()); write(0)
        }.toByteArray()
        val info = Lz4.compress(table)
        val header = ByteArrayOutputStream().apply {
            write("UnityFS".toByteArray()); write(0)
            be32(8)
            write("5.x.x".toByteArray()); write(0)
            write("2020.3.45f1".toByteArray()); write(0)
            write(ByteArray(8)) // total size, patched below
            be32(info.size); be32(table.size); be32(0x243)
            while (size() % 16 != 0) write(0)
            write(info)
            while (size() % 16 != 0) write(0)
            blocks.forEach { write(it.second) }
        }.toByteArray()
        val size = header.size.toLong()
        val at = 8 + 4 + 6 + 12
        for (i in 0 until 8) header[at + i] = (size ushr (56 - 8 * i)).toByte()
        return header
    }

    private fun ByteArrayOutputStream.be32(v: Int) {
        write(v ushr 24); write(v ushr 16); write(v ushr 8); write(v)
    }

    @Test fun switchesBothMeshesAndBack() {
        val stock = bundle(stockNode())
        assertEquals(SkinState.STOCK, CollabSkin.MSBLACK.state(stock))

        val enabled = CollabSkin.MSBLACK.patch(stock, enable = true)
        assertEquals(SkinState.ARKKNIGHTS, CollabSkin.MSBLACK.state(enabled))
        assertArrayEquals(node(arkScene, arkBattle, scene, battle), UnityBundle.parse(enabled).data)

        val disabled = CollabSkin.MSBLACK.patch(enabled, enable = false)
        assertArrayEquals(stock, disabled)
    }

    @Test fun partiallySwitchedFileIsUnknownButCanBeCompleted() {
        val partial = bundle(node(arkScene, battle, scene, arkBattle))
        assertEquals(SkinState.UNKNOWN, CollabSkin.MSBLACK.state(partial))
        val enabled = CollabSkin.MSBLACK.patch(partial, enable = true)
        assertArrayEquals(node(arkScene, arkBattle, scene, battle), UnityBundle.parse(enabled).data)
    }

    @Test fun otherCharacterAndDamagedFilesAreRejected() {
        val stock = bundle(stockNode())
        assertEquals(SkinState.UNKNOWN, CollabSkin.HIME.state(stock))
        assertThrows(IllegalArgumentException::class.java) { CollabSkin.HIME.patch(stock, enable = true) }
        assertEquals(SkinState.UNKNOWN, CollabSkin.MSBLACK.state(stock.copyOf(stock.size - 1)))
        assertEquals(SkinState.UNKNOWN, CollabSkin.MSBLACK.state(ByteArray(100)))
    }

    @Test fun unchangedBlocksAreKeptByteForByte() {
        val stock = bundle(stockNode())
        val enabled = CollabSkin.MSBLACK.patch(stock, enable = true)
        val firstBlock = Lz4.compress(filler, lit = true)
        val found = (0..enabled.size - firstBlock.size).any { at ->
            firstBlock.indices.all { enabled[at + it] == firstBlock[it] }
        }
        assertEquals(true, found)
    }

    @Test fun lz4RoundTripsInBothTokenLayouts() {
        val random = ByteArray(70_000).also { Random(1).nextBytes(it) }
        val text = "Assets/Res/Prefabs/Characters/".repeat(3000).toByteArray()
        for (input in listOf(ByteArray(0), ByteArray(5) { 1 }, random, text, ByteArray(300_000))) {
            for (lit in listOf(false, true)) {
                assertArrayEquals(input, Lz4.decompress(Lz4.compress(input, lit), input.size, lit))
            }
        }
    }

    @Test fun lz4RejectsDamagedStreams() {
        val packed = Lz4.compress("abcdabcdabcdabcdabcdabcdabcd".toByteArray())
        assertThrows(IllegalArgumentException::class.java) { Lz4.decompress(packed, 27) }
        assertThrows(IllegalArgumentException::class.java) { Lz4.decompress(packed.copyOf(packed.size - 3), 28) }
        assertThrows(IllegalArgumentException::class.java) { Lz4.decompress(byteArrayOf(0x0F, 0x10, 0x00), 20) }
    }
}
