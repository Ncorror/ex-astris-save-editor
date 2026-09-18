package com.exa.save

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.exa.save.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val io = Executors.newSingleThreadExecutor()

    private val requestOpen = 1
    private val requestSaveAs = 2
    private val requestShizuku = 73

    private val categoryOrder = listOf("currency", "consumable", "material", "entropite", "other")

    private var uri: Uri? = null
    private var shizukuPath: String? = null
    private var save: SaveFile? = null
    private var currentItems: LinkedHashMap<Int, Int> = LinkedHashMap()

    private val names = HashMap<Int, String>()
    private val cats = HashMap<Int, String>()

    private var activeCategory: String? = null
    private var searchQuery = ""

    private lateinit var adapter: InventoryAdapter

    @Volatile
    private var remoteService: IShizukuFileService? = null
    private var bindingService = false

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(this, ShizukuFileService::class.java))
            .processNameSuffix("exa_files")
            .tag("ex-astris-save-file-service")
            .version(2)
            .daemon(false)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            remoteService = IShizukuFileService.Stub.asInterface(service)
            bindingService = false
            updateShizukuUi()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remoteService = null
            bindingService = false
            updateShizukuUi()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateShizukuUi()
        if (hasShizukuPermission()) bindShizukuFileService()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        remoteService = null
        bindingService = false
        updateShizukuUi()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == requestShizuku) {
            updateShizukuUi()
            if (result == PackageManager.PERMISSION_GRANTED) {
                bindShizukuFileService()
            }
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCatalog()
        setupInventoryList()
        setupFilters()
        setupActions()

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)

        updateShizukuUi()
        if (hasShizukuPermission()) bindShizukuFileService()
        updateFileUi()
    }

    override fun onResume() {
        super.onResume()
        updateShizukuUi()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        try {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, false)
        } catch (_: Throwable) {
        }
        io.shutdownNow()
        super.onDestroy()
    }

    private fun setupInventoryList() {
        adapter = InventoryAdapter { id -> editDialog(id) }
        binding.itemList.layoutManager = LinearLayoutManager(this)
        binding.itemList.adapter = adapter
    }

    private fun setupFilters() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim()?.lowercase(Locale.ROOT).orEmpty()
                renderRows()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.chipAll.setOnClickListener { activeCategory = null; renderRows() }
        binding.chipCurrency.setOnClickListener { activeCategory = "currency"; renderRows() }
        binding.chipConsumable.setOnClickListener { activeCategory = "consumable"; renderRows() }
        binding.chipMaterial.setOnClickListener { activeCategory = "material"; renderRows() }
        binding.chipEntropite.setOnClickListener { activeCategory = "entropite"; renderRows() }
        binding.chipOther.setOnClickListener { activeCategory = "other"; renderRows() }
    }

    private fun setupActions() {
        binding.shizukuButton.setOnClickListener { requestOrConnectShizuku() }
        binding.openGameButton.setOnClickListener { openExAstrisSave() }
        binding.openManualButton.setOnClickListener { pickFile() }
        binding.saveButton.setOnClickListener { saveBack() }
        binding.saveAsButton.setOnClickListener { saveAs() }
        binding.materialsButton.setOnClickListener { bulk("material", 10_000) }
        binding.consumablesButton.setOnClickListener { bulk("consumable", 10_000) }
        binding.addButton.setOnClickListener { addDialog() }
    }

    // ---------------- Shizuku ----------------

    private fun shizukuAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    private fun hasShizukuPermission(): Boolean {
        if (!shizukuAlive()) return false
        return try {
            !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    private fun requestOrConnectShizuku() {
        if (!shizukuAlive()) {
            toast(getString(R.string.shizuku_missing_message))
            updateShizukuUi()
            return
        }

        try {
            if (Shizuku.isPreV11()) {
                toast(getString(R.string.shizuku_old))
                return
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindShizukuFileService()
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                toast(getString(R.string.shizuku_denied))
            } else {
                Shizuku.requestPermission(requestShizuku)
            }
        } catch (e: Throwable) {
            toast(getString(R.string.shizuku_bind_failed, e.message ?: e.javaClass.simpleName))
        }
        updateShizukuUi()
    }

    private fun bindShizukuFileService() {
        if (!hasShizukuPermission() || remoteService != null || bindingService) return
        try {
            bindingService = true
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Throwable) {
            bindingService = false
            toast(getString(R.string.shizuku_bind_failed, e.message ?: e.javaClass.simpleName))
            updateShizukuUi()
        }
    }

    private fun updateShizukuUi() {
        if (!::binding.isInitialized) return

        val status = binding.shizukuStatus
        val detail = binding.shizukuDetail
        val button = binding.shizukuButton

        if (!shizukuAlive()) {
            status.text = getString(R.string.shizuku_not_running)
            status.setTextColor(color(R.color.exa_error))
            detail.text = getString(R.string.shizuku_missing_message)
            button.setText(R.string.connect)
            binding.openGameButton.isEnabled = false
            return
        }

        val granted = try {
            !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

        if (!granted) {
            status.text = getString(R.string.shizuku_permission_needed)
            status.setTextColor(color(R.color.exa_warning))
            detail.text = getString(R.string.shizuku_description)
            button.setText(R.string.grant)
            binding.openGameButton.isEnabled = false
            return
        }

        val service = remoteService
        if (service != null) {
            val uid = try { service.remoteUid } catch (_: Throwable) { -1 }
            status.text = getString(R.string.shizuku_ready)
            status.setTextColor(color(R.color.exa_success))
            detail.text = if (uid == 0) getString(R.string.shizuku_root) else getString(R.string.shizuku_shell)
            button.setText(R.string.reconnect)
            binding.openGameButton.isEnabled = true
        } else {
            status.text = getString(R.string.shizuku_checking)
            status.setTextColor(color(R.color.exa_warning))
            detail.text = getString(R.string.shizuku_description)
            button.setText(R.string.reconnect)
            binding.openGameButton.isEnabled = false
        }
    }

    // ---------------- catalog ----------------

    private fun preferRussian(): Boolean =
        Locale.getDefault().language.equals("ru", ignoreCase = true)

    private fun loadCatalog() {
        try {
            val text = assets.open("items.json").bufferedReader().use { it.readText() }
            val obj = JSONObject(text)
            val ru = preferRussian()
            for (key in obj.keys()) {
                val id = key.toIntOrNull() ?: continue
                val v = obj.getJSONObject(key)
                val ruName = v.optString("name", "")
                val enName = v.optString("name_en", ruName)
                names[id] = if (ru) ruName else enName
                cats[id] = v.optString("cat", "")
            }
        } catch (_: Exception) {
        }
    }

    private fun nameOf(id: Int): String {
        val name = names[id]
        return if (name.isNullOrBlank()) getString(R.string.unnamed) else name
    }

    private fun catOf(id: Int): String {
        cats[id]?.let { if (it.isNotBlank()) return it }
        return when (id) {
            in 10000..10999 -> "currency"
            in 11000..12999 -> "consumable"
            in 800000..899999 -> "consumable"
            in 200000..299999 -> "material"
            in 600000..699999 -> "material"
            in 500000..599999 -> "entropite"
            else -> "other"
        }
    }

    private fun catLabel(key: String): String = when (key) {
        "currency" -> getString(R.string.cat_currency)
        "consumable" -> getString(R.string.cat_consumable)
        "material" -> getString(R.string.cat_material)
        "entropite" -> getString(R.string.cat_entropite)
        else -> getString(R.string.cat_other)
    }

    // ---------------- direct Android/data access ----------------

    private fun openExAstrisSave() {
        val service = remoteService
        if (service == null) {
            requestOrConnectShizuku()
            return
        }

        binding.openGameButton.isEnabled = false
        io.execute {
            try {
                val paths = service.findSaveFiles()
                runOnUiThread {
                    binding.openGameButton.isEnabled = true
                    when {
                        paths.isEmpty() -> toast(getString(R.string.save_not_found))
                        paths.size == 1 -> openShizukuPath(paths[0])
                        else -> showSaveCandidates(paths)
                    }
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    binding.openGameButton.isEnabled = true
                    toast(getString(R.string.cannot_open, e.message ?: e.javaClass.simpleName))
                }
            }
        }
    }

    private fun showSaveCandidates(paths: Array<String>) {
        val labels = paths.map { path ->
            val f = File(path)
            val parent = f.parentFile?.parentFile?.name.orEmpty()
            if (parent.isBlank()) f.name else "$parent / ${f.name}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.save_candidates_title)
            .setItems(labels) { _, which -> openShizukuPath(paths[which]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openShizukuPath(path: String) {
        val service = remoteService ?: return toast(getString(R.string.shizuku_not_running))
        io.execute {
            try {
                if (!service.exists(path)) throw IllegalStateException(getString(R.string.save_path_missing))
                val pfd = service.openRead(path)
                val bytes = ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
                val parsed = SaveFile(bytes)
                backupOriginal(bytes)
                runOnUiThread {
                    save = parsed
                    shizukuPath = path
                    uri = null
                    refreshInventory()
                    updateFileUi()
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    toast(getString(R.string.cannot_open, e.message ?: e.javaClass.simpleName))
                }
            }
        }
    }

    // ---------------- Storage Access Framework fallback ----------------

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, requestOpen)
    }

    private fun saveAs() {
        if (save == null) return toast(getString(R.string.open_first))
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_TITLE, "SaveFile0.save")
        }
        startActivityForResult(intent, requestSaveAs)
    }

    @Deprecated("Deprecated in Android SDK, retained for minSdk-compatible simple SAF flow")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res != RESULT_OK || data?.data == null) return
        val selected = data.data!!
        if (req == requestOpen) openUri(selected) else if (req == requestSaveAs) writeToUri(selected)
    }

    private fun openUri(selected: Uri) {
        io.execute {
            try {
                val bytes = contentResolver.openInputStream(selected)!!.use { input ->
                    val out = ByteArrayOutputStream()
                    input.copyTo(out)
                    out.toByteArray()
                }
                val parsed = SaveFile(bytes)
                backupOriginal(bytes)
                runOnUiThread {
                    save = parsed
                    uri = selected
                    shizukuPath = null
                    refreshInventory()
                    updateFileUi()
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    toast(getString(R.string.cannot_open, e.message ?: e.javaClass.simpleName))
                }
            }
        }
    }

    private fun backupOriginal(bytes: ByteArray) {
        try {
            val dir = getExternalFilesDir("backups") ?: return
            dir.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            File(dir, "SaveFile0.$stamp.bak").writeBytes(bytes)
        } catch (_: Throwable) {
        }
    }

    // ---------------- write ----------------

    private fun saveBack() {
        val sf = save ?: return toast(getString(R.string.open_first))
        val directPath = shizukuPath
        val documentUri = uri

        if (directPath == null && documentUri == null) return toast(getString(R.string.open_first))

        binding.saveButton.isEnabled = false
        io.execute {
            try {
                val data = sf.build()
                if (directPath != null) {
                    val service = remoteService ?: throw IllegalStateException(getString(R.string.shizuku_not_running))
                    if (!service.exists(directPath)) throw IllegalStateException(getString(R.string.save_path_missing))
                    val pfd = service.openWrite(directPath)
                    ParcelFileDescriptor.AutoCloseOutputStream(pfd).use { output ->
                        output.write(data)
                        output.flush()
                        output.fd.sync()
                    }
                } else {
                    contentResolver.openOutputStream(documentUri!!, "wt")!!.use { it.write(data) }
                }
                runOnUiThread {
                    binding.saveButton.isEnabled = true
                    toast(getString(R.string.written, data.size))
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    binding.saveButton.isEnabled = true
                    toast(getString(R.string.cannot_write, e.message ?: e.javaClass.simpleName))
                }
            }
        }
    }

    private fun writeToUri(target: Uri) {
        val sf = save ?: return toast(getString(R.string.open_first))
        io.execute {
            try {
                val data = sf.build()
                contentResolver.openOutputStream(target, "wt")!!.use { it.write(data) }
                runOnUiThread { toast(getString(R.string.written, data.size)) }
            } catch (e: Throwable) {
                runOnUiThread {
                    toast(getString(R.string.cannot_write, e.message ?: e.javaClass.simpleName))
                }
            }
        }
    }

    // ---------------- inventory UI ----------------

    private fun refreshInventory() {
        val sf = save ?: return
        currentItems = sf.items()
        renderRows()
    }

    private fun renderRows() {
        if (!::adapter.isInitialized) return
        val sf = save
        if (sf == null) {
            adapter.submit(emptyList())
            binding.summaryText.setText(R.string.empty_summary)
            return
        }

        val rows = currentItems.entries
            .asSequence()
            .filter { (id, _) -> activeCategory == null || catOf(id) == activeCategory }
            .filter { (id, _) ->
                if (searchQuery.isBlank()) true
                else id.toString().contains(searchQuery) ||
                        nameOf(id).lowercase(Locale.ROOT).contains(searchQuery)
            }
            .sortedWith(compareBy<Map.Entry<Int, Int>>(
                { categoryOrder.indexOf(catOf(it.key)).let { idx -> if (idx < 0) categoryOrder.size else idx } },
                { it.key }
            ))
            .map { (id, count) ->
                InventoryAdapter.Row(
                    id = id,
                    name = nameOf(id),
                    category = catLabel(catOf(id)),
                    count = count
                )
            }
            .toList()

        adapter.submit(rows)
        val saved = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            .format(Date(sf.savedAt * 1000L))
        binding.summaryText.text = getString(
            R.string.inventory_summary,
            currentItems.size,
            rows.size,
            saved
        )
    }

    private fun updateFileUi() {
        val sf = save
        if (sf == null) {
            binding.fileTitle.setText(R.string.no_file)
            binding.filePath.setText(R.string.no_file_hint)
            binding.saveButton.isEnabled = false
            binding.saveAsButton.isEnabled = false
            binding.materialsButton.isEnabled = false
            binding.consumablesButton.isEnabled = false
            binding.addButton.isEnabled = false
            renderRows()
            return
        }

        val label = shizukuPath?.let { File(it).name }
            ?: uri?.lastPathSegment?.substringAfterLast('/')
            ?: "SaveFile0.save"
        binding.fileTitle.text = label
        binding.filePath.text = shizukuPath ?: uri?.toString().orEmpty()
        binding.saveButton.isEnabled = true
        binding.saveAsButton.isEnabled = true
        binding.materialsButton.isEnabled = true
        binding.consumablesButton.isEnabled = true
        binding.addButton.isEnabled = true
        renderRows()
    }

    // ---------------- editing ----------------

    private fun editDialog(id: Int) {
        val sf = save ?: return
        val current = currentItems[id] ?: return
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text.length)
            gravity = Gravity.CENTER
            setTextColor(color(R.color.exa_text))
            setHintTextColor(color(R.color.exa_text_secondary))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("$id — ${nameOf(id)}")
            .setMessage("${catLabel(catOf(id))}\n${getString(R.string.currently, current)}")
            .setView(input)
            .setPositiveButton(R.string.apply) { _, _ ->
                val value = input.text.toString().toIntOrNull() ?: return@setPositiveButton
                sf.setItems { itemId, _ -> if (itemId == id) value else null }
                refreshInventory()
            }
            .setNeutralButton(R.string.delete) { _, _ ->
                sf.removeItems(setOf(id))
                refreshInventory()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun bulk(category: String, value: Int) {
        val sf = save ?: return toast(getString(R.string.open_first))
        val changed = sf.setItems { id, _ ->
            if (catOf(id) == category && id !in 10000..10001) value else null
        }
        refreshInventory()
        toast(getString(R.string.entries_changed, changed))
    }

    private fun addDialog() {
        val sf = save ?: return toast(getString(R.string.open_first))
        val input = EditText(this).apply {
            hint = getString(R.string.add_hint)
            setTextColor(color(R.color.exa_text))
            setHintTextColor(color(R.color.exa_text_secondary))
            setPadding(48, 16, 48, 16)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_title)
            .setMessage(R.string.add_message)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val plan = HashMap<Int, Int>()
                for (token in input.text.toString().trim().split(Regex("\\s+"))) {
                    if (token.isBlank()) continue
                    val parts = token.split("=")
                    val id = parts[0].toIntOrNull() ?: continue
                    plan[id] = parts.getOrNull(1)?.toIntOrNull() ?: 500
                }
                if (plan.isEmpty()) return@setPositiveButton
                val added = sf.addItems(plan)
                refreshInventory()
                toast(getString(R.string.added, added.size))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun color(res: Int): Int = ContextCompat.getColor(this, res)

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
