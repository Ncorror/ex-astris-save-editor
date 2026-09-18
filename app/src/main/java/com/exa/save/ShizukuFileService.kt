package com.exa.save

import android.content.Context
import android.os.ParcelFileDescriptor
import android.system.Os
import androidx.annotation.Keep
import java.io.File

/**
 * Runs inside Shizuku/Sui with shell or root identity.
 * The service is deliberately restricted to Ex Astris' own Android/data directory.
 */
@Keep
class ShizukuFileService() : IShizukuFileService.Stub() {

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : this()

    override fun destroy() {
        System.exit(0)
    }

    override fun getRemoteUid(): Int = Os.getuid()

    override fun findSaveFiles(): Array<String> {
        val candidates = ArrayList<File>()
        for (root in gameFileRoots()) {
            collectSaveFiles(root, 0, candidates)
        }

        return candidates
            .distinctBy { safeCanonical(it) }
            .sortedWith(
                compareByDescending<File> { it.name.equals(TARGET_SAVE, ignoreCase = true) }
                    .thenByDescending { it.lastModified() }
            )
            .take(MAX_RESULTS)
            .map { safeCanonical(it) }
            .toTypedArray()
    }

    override fun openRead(path: String): ParcelFileDescriptor {
        val file = checkedGameFile(path)
        require(file.isFile) { "File does not exist: $path" }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun openWrite(path: String): ParcelFileDescriptor {
        val file = checkedGameFile(path)
        require(file.isFile) { "File does not exist: $path" }
        return ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE
        )
    }

    override fun exists(path: String): Boolean = try {
        checkedGameFile(path).exists()
    } catch (_: Throwable) {
        false
    }

    private fun checkedGameFile(path: String): File {
        val file = File(path)
        val canonical = safeCanonical(file)
        val allowed = gameDataRoots().any { root ->
            canonical == root || canonical.startsWith(root + File.separator)
        }
        require(allowed) { "Path is outside Ex Astris Android/data" }
        return File(canonical)
    }

    private fun gameDataRoots(): List<String> {
        val roots = LinkedHashSet<String>()

        val emulated = File("/storage/emulated")
        emulated.listFiles()?.forEach { userDir ->
            if (userDir.isDirectory && userDir.name.all(Char::isDigit)) {
                roots += safeCanonical(File(userDir, "Android/data/$GAME_PACKAGE"))
            }
        }

        // Common primary-user alias. canonicalPath normally resolves it to /storage/emulated/0.
        roots += safeCanonical(File("/sdcard/Android/data/$GAME_PACKAGE"))
        roots += safeCanonical(File("/storage/emulated/0/Android/data/$GAME_PACKAGE"))
        return roots.toList()
    }

    private fun gameFileRoots(): List<File> = gameDataRoots()
        .map { File(it, "files") }
        .filter { it.isDirectory }

    private fun collectSaveFiles(dir: File, depth: Int, out: MutableList<File>) {
        if (depth > MAX_DEPTH || out.size >= MAX_SCAN_RESULTS) return
        val children = try {
            dir.listFiles()
        } catch (_: Throwable) {
            null
        } ?: return

        for (child in children) {
            if (out.size >= MAX_SCAN_RESULTS) return
            if (child.isDirectory) {
                collectSaveFiles(child, depth + 1, out)
            } else {
                val name = child.name
                if (name.equals(TARGET_SAVE, ignoreCase = true) || name.endsWith(".save", ignoreCase = true)) {
                    out += child
                }
            }
        }
    }

    private fun safeCanonical(file: File): String = try {
        file.canonicalPath
    } catch (_: Throwable) {
        file.absolutePath
    }

    companion object {
        private const val GAME_PACKAGE = "com.gryphline.exastris.gp"
        private const val TARGET_SAVE = "SaveFile0.save"
        private const val MAX_DEPTH = 7
        private const val MAX_RESULTS = 16
        private const val MAX_SCAN_RESULTS = 64
    }
}
