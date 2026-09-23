package com.exa.save

import java.security.MessageDigest

/**
 * Arknights collaboration skins, applied to the game's own character map bundle on the device.
 *
 * Each map bundle has one `EntityComp_CharacterMeshCtrl` with the base `_sceneMeshInfo` and
 * `_battleMeshInfo` (used for styleId 0) and `_ExtraMeshInfoList` entries [0] (scene) and [1]
 * (battle) that point at the Arknights prefabs. Switching exchanges the whole serialized
 * SoftLink structures of base and extra entries, moving the fields between them unchanged, so
 * styleId 0 loads the real Arknights prefabs with their dependencies. The operation is its own
 * inverse and keeps the decompressed length. See docs/PATCH_METHOD_HIME_MSBLACK.md.
 */
enum class CollabSkin(val label: String, val fileName: String, private val folder: String) {
    HIME("Hime", "skin_hero01_hime_map.prefab.ab", "Hime"),
    MSBLACK("MsBlack", "skin_hero02_msblack_map.prefab.ab", "MsBlack");

    /** STOCK or ARKKNIGHTS for a recognized map bundle, UNKNOWN for anything else. */
    fun state(bundle: ByteArray): SkinState = try {
        MeshController.find(UnityBundle.parse(bundle).data, folder).state
    } catch (_: IllegalArgumentException) {
        SkinState.UNKNOWN
    } catch (_: IllegalStateException) {
        SkinState.UNKNOWN
    }

    /** Returns [bundle] switched to the Arknights ([enable]) or standard skin. */
    fun patch(bundle: ByteArray, enable: Boolean): ByteArray {
        val container = UnityBundle.parse(bundle)
        var data = container.data
        val target = if (enable) SkinState.ARKKNIGHTS else SkinState.STOCK
        for (pair in MeshPair.entries) {
            val controller = MeshController.find(data, folder)
            if (controller.isArknights(pair) != enable) data = controller.swap(data, pair)
        }
        check(MeshController.find(data, folder).state == target) { "$label map patch failed" }
        val result = container.rebuild(data)
        check(state(result) == target) { "$label map patch failed verification" }
        return result
    }
}

enum class SkinState { STOCK, ARKKNIGHTS, UNKNOWN, MISSING }

private enum class MeshPair { SCENE, BATTLE }

/** One serialized SoftLink mesh entry: two ints, name, path, 36 bytes, path again, 32 bytes. */
private class MeshInfo(val start: Int, val end: Int, val path: String) {
    val arknights get() = path.contains(ARK_MARKER)

    companion object {
        const val ARK_MARKER = "_ArkKnights_"

        fun read(b: ByteArray, at: Int): MeshInfo {
            val name = SerializedReader(b, at + 8)
            name.string()
            val path = name.string()
            name.skip(36)
            val again = name.string()
            name.skip(32)
            require(path == again) { "inconsistent mesh path" }
            return MeshInfo(at, name.position, path)
        }
    }
}

private class SerializedReader(private val b: ByteArray, var position: Int) {
    fun int(): Int {
        require(position >= 0 && position <= b.size - 4) { "truncated data" }
        val v = (b[position].toInt() and 0xFF) or ((b[position + 1].toInt() and 0xFF) shl 8) or
            ((b[position + 2].toInt() and 0xFF) shl 16) or ((b[position + 3].toInt() and 0xFF) shl 24)
        position += 4
        return v
    }

    fun string(): String {
        val length = int()
        require(length in 1..512 && length <= b.size - position) { "invalid string" }
        for (i in position until position + length) {
            require(b[i].toInt() in 0x20..0x7E) { "invalid string" }
        }
        val s = String(b, position, length, Charsets.US_ASCII)
        position += (length + 3) and 3.inv()
        return s
    }

    fun intArray() {
        val count = int()
        require(count in 0..16) { "invalid array" }
        skip(count * 4)
    }

    fun skip(bytes: Int) {
        require(bytes >= 0 && bytes <= b.size - position) { "truncated data" }
        position += bytes
    }
}

/** base scene, style ids, base battle, style ids, count 2, extra scene, extra battle. */
private class MeshController(val scene: MeshInfo, val battle: MeshInfo, val extraScene: MeshInfo,
                             val extraBattle: MeshInfo) {
    val state: SkinState
        get() = when {
            scene.arknights && battle.arknights -> SkinState.ARKKNIGHTS
            !scene.arknights && !battle.arknights -> SkinState.STOCK
            else -> SkinState.UNKNOWN
        }

    fun isArknights(pair: MeshPair) = if (pair == MeshPair.SCENE) scene.arknights else battle.arknights

    /** [base | fields between | extra] becomes [extra | fields between | base]. */
    fun swap(data: ByteArray, pair: MeshPair): ByteArray {
        val (base, extra) = if (pair == MeshPair.SCENE) scene to extraScene else battle to extraBattle
        val out = data.copyOf()
        var at = base.start
        for (part in listOf(extra.start until extra.end, base.end until extra.start, base.start until base.end)) {
            System.arraycopy(data, part.first, out, at, part.last - part.first + 1)
            at += part.last - part.first + 1
        }
        check(at == extra.end) { "mesh swap changed length" }
        return out
    }

    companion object {
        private const val PREFIX = "Assets/Res/Prefabs/Characters/"
        private val PREFIX_BYTES = PREFIX.toByteArray(Charsets.US_ASCII)

        fun find(data: ByteArray, folder: String): MeshController {
            val found = ArrayList<MeshController>(1)
            var at = 0
            while (at <= data.size - 64) {
                if (looksLikeMesh(data, at)) tryRead(data, at, folder)?.let { found.add(it) }
                at += 4
            }
            require(found.size == 1) { "character mesh controller not found" }
            return found.single()
        }

        /** Cheap pre-check (name length, then a character prefab path) before a full parse. */
        private fun looksLikeMesh(data: ByteArray, at: Int): Boolean {
            val nameLength = le32(data, at + 8)
            if (nameLength !in 1..512) return false
            val pathAt = at + 12 + ((nameLength + 3) and 3.inv())
            if (pathAt < 0 || pathAt > data.size - 4 - PREFIX_BYTES.size) return false
            for (i in PREFIX_BYTES.indices) if (data[pathAt + 4 + i] != PREFIX_BYTES[i]) return false
            return true
        }

        private fun le32(b: ByteArray, p: Int) =
            (b[p].toInt() and 0xFF) or ((b[p + 1].toInt() and 0xFF) shl 8) or
                ((b[p + 2].toInt() and 0xFF) shl 16) or ((b[p + 3].toInt() and 0xFF) shl 24)

        private fun tryRead(data: ByteArray, at: Int, folder: String): MeshController? = try {
            val scene = MeshInfo.read(data, at)
            val reader = SerializedReader(data, scene.end)
            reader.intArray()
            val battle = MeshInfo.read(data, reader.position)
            reader.position = battle.end
            reader.intArray()
            require(reader.int() == 2) { "unexpected extra mesh count" }
            val extraScene = MeshInfo.read(data, reader.position)
            val extraBattle = MeshInfo.read(data, extraScene.end)
            MeshController(scene, battle, extraScene, extraBattle).takeIf {
                pairMatches(it.scene, it.extraScene, folder, "_scene.prefab") &&
                    pairMatches(it.battle, it.extraBattle, folder, "_battle.prefab")
            }
        } catch (_: IllegalArgumentException) {
            null
        }

        /** One standard and one Arknights prefab of the same character and kind. */
        private fun pairMatches(a: MeshInfo, b: MeshInfo, folder: String, suffix: String): Boolean {
            if (a.arknights == b.arknights) return false
            val (standard, arknights) = if (a.arknights) b to a else a to b
            return standard.path.startsWith("$PREFIX$folder/") && standard.path.endsWith(suffix) &&
                arknights.path == standard.path.removeSuffix(suffix) + MeshInfo.ARK_MARKER.dropLast(1) + suffix
        }
    }
}

fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
