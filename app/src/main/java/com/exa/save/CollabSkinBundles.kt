package com.exa.save

import android.content.Context
import java.security.MessageDigest

/** Exact Ex Astris 1.3.0 map bundles supplied with the Stage 127/128 investigation. */
enum class CollabSkin(
    val label: String,
    val fileName: String,
    val stockAsset: String,
    val collabAsset: String,
    val stockSha256: String,
    val collabSha256: String
) {
    HIME(
        "Hime", "skin_hero01_hime_map.prefab.ab",
        "skins/hime_original.ab", "skins/hime_arkknights.ab",
        "8675f6c3a552b02f68188065f72ab851b64daeb47305d4f7ba256610ce3a0fe5",
        "d9d892092c998026ec01331394d5e54f54048d102f7a1263a6f4dc310e862b47"
    ),
    MSBLACK(
        "MsBlack", "skin_hero02_msblack_map.prefab.ab",
        "skins/msblack_original.ab", "skins/msblack_arkknights.ab",
        "3f05d3b6cfa824f1f6779aeb77e47368c4b692d84e5ae3edfa9ec10aca7f2710",
        "c075d849e6b958e184a2c4b5b7993c8aa218857da5e7492eca50979b7f9fce32"
    );

    fun state(hash: String): SkinState = when (hash) {
        stockSha256 -> SkinState.STOCK
        collabSha256 -> SkinState.ARKKNIGHTS
        else -> SkinState.UNKNOWN
    }

    fun bytes(context: Context, enable: Boolean): ByteArray {
        val bytes = context.assets.open(if (enable) collabAsset else stockAsset).use { it.readBytes() }
        val expected = if (enable) collabSha256 else stockSha256
        check(sha256(bytes) == expected) { "Bundled $label asset failed SHA-256 validation" }
        return bytes
    }
}

enum class SkinState { STOCK, ARKKNIGHTS, UNKNOWN, MISSING }

fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
