package com.exa.save

import android.os.ParcelFileDescriptor
import android.system.Os
import java.io.File

/**
 * File API shared by Shizuku and root backends.
 *
 * Even when the process has UID 0, access is deliberately restricted to
 * Ex Astris' own Android/data directory. This keeps the editor from becoming
 * a general-purpose privileged file manager.
 */
open class PrivilegedFileBinder : IShizukuFileService.Stub() {

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
            .sortedWith(compareBy<File>(
                { saveSortGroup(it.name) },
                { saveSortIndex(it.name) },
                { it.parentFile?.parentFile?.name.orEmpty() },
                { it.name.lowercase() }
            ))
            .take(MAX_RESULTS)
            .map { safeCanonical(it) }
            .toTypedArray()
    }

    /** Only the two known, downloaded character maps; never scan or mutate arbitrary bundles. */
    override fun findSkinMapFiles(): Array<String> = gameFileRoots()
        .flatMap { root ->
            listOf(
                File(root, "Download/ab/assets/res/prefabs/characters/hime/skin_hero01_hime_map.prefab.ab"),
                File(root, "Download/ab/assets/res/prefabs/characters/msblack/skin_hero02_msblack_map.prefab.ab")
            )
        }
        .filter { it.isFile }
        .map { safeCanonical(it) }
        .distinct()
        .toTypedArray()

    private fun saveSortGroup(name: String): Int = when {
        name.equals(TARGET_SAVE, ignoreCase = true) -> 0
        name.startsWith("AutoSaveFile", ignoreCase = true) -> 1
        else -> 2
    }

    private fun saveSortIndex(name: String): Int {
        val match = Regex("(?i)AutoSaveFile(\\d+)\\.save").matchEntire(name)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
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

    override fun openAtomicWrite(path: String): ParcelFileDescriptor {
        val file = checkedGameFile(path)
        require(file.isFile) { "File does not exist: $path" }
        val temp = atomicTemp(file)
        if (temp.exists() && !temp.delete()) {
            throw IllegalStateException("Cannot remove stale temporary file")
        }
        return ParcelFileDescriptor.open(
            temp,
            ParcelFileDescriptor.MODE_CREATE or
                ParcelFileDescriptor.MODE_WRITE_ONLY or
                ParcelFileDescriptor.MODE_TRUNCATE
        )
    }

    override fun commitAtomicWrite(path: String) {
        val file = checkedGameFile(path)
        val temp = atomicTemp(file)
        require(temp.isFile) { "Temporary save is missing" }

        // Best-effort metadata preservation. Root can restore owner/mode; Shizuku
        // shell may not be allowed to chown, so failures are intentionally ignored.
        try {
            val stat = Os.stat(file.absolutePath)
            try { Os.chown(temp.absolutePath, stat.st_uid, stat.st_gid) } catch (_: Throwable) {}
            try { Os.chmod(temp.absolutePath, stat.st_mode and 0x1FF) } catch (_: Throwable) {}
        } catch (_: Throwable) {
        }

        Os.rename(temp.absolutePath, file.absolutePath)
    }

    override fun abortAtomicWrite(path: String) {
        val file = checkedGameFile(path)
        val temp = atomicTemp(file)
        if (temp.exists()) temp.delete()
    }

    override fun exists(path: String): Boolean = try {
        checkedGameFile(path).exists()
    } catch (_: Throwable) {
        false
    }

    private fun atomicTemp(file: File): File = File(file.parentFile, ".${file.name}.exa.tmp")

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
