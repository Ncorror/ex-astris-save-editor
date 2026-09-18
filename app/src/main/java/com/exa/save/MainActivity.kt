package com.exa.save

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.exa.save.databinding.ActivityMainBinding
import com.exa.save.databinding.BottomSheetAddItemBinding
import com.exa.save.databinding.BottomSheetBulkActionsBinding
import com.exa.save.databinding.BottomSheetEditItemBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
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
    private val prefs by lazy { getSharedPreferences("exa_ui", MODE_PRIVATE) }

    private val requestOpen = 1
    private val requestSaveAs = 2
    private val requestShizuku = 73

    private val categoryOrder = listOf("currency", "consumable", "material", "entropite", "other")

    private enum class AccessMode { AUTO, ROOT, SHIZUKU, MANUAL }
    private enum class AccessBackend { ROOT, SHIZUKU, MANUAL, NONE }

    private var uri: Uri? = null
    private var directPath: String? = null
    private var directBackend: AccessBackend = AccessBackend.NONE
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

    @Volatile
    private var rootService: IShizukuFileService? = null
    private var rootBinding = false

    private val rootIntent by lazy { Intent(this, RootFileService::class.java) }

    private val rootConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            rootService = IShizukuFileService.Stub.asInterface(service)
            rootBinding = false
            updateRootUi()
            updateAccessUi()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            rootService = null
            rootBinding = false
            updateRootUi()
            updateAccessUi()
        }
    }

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
            updateAccessUi()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remoteService = null
            bindingService = false
            updateShizukuUi()
            updateAccessUi()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateShizukuUi()
        if (hasShizukuPermission()) bindShizukuFileService()
        updateAccessUi()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        remoteService = null
        bindingService = false
        updateShizukuUi()
        updateAccessUi()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == requestShizuku) {
            updateShizukuUi()
            if (result == PackageManager.PERMISSION_GRANTED) {
                bindShizukuFileService()
            }
            updateAccessUi()
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setContext(applicationContext)
                .setTimeout(15)
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCatalog()
        setupInventoryList()
        setupFilters()
        setupActions()
        setupNavigation()
        setupSettings()

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)

        updateRootUi()
        updateShizukuUi()
        if (hasShizukuPermission()) bindShizukuFileService()
        updateAccessUi()
        updateFileUi()
    }

    override fun onResume() {
        super.onResume()
        updateRootUi()
        updateShizukuUi()
        updateAccessUi()
        updateBackupUi()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        try {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, false)
        } catch (_: Throwable) {
        }
        try {
            RootService.unbind(rootConnection)
        } catch (_: Throwable) {
        }
        io.shutdownNow()
        super.onDestroy()
    }

    private fun setupInventoryList() {
        adapter = InventoryAdapter { id -> showEditItemSheet(id) }
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
        binding.compactShizukuButton.setOnClickListener { connectSelectedAccess() }
        binding.rootButton.setOnClickListener { requestOrConnectRoot() }
        binding.shizukuButton.setOnClickListener { requestOrConnectShizuku() }
        binding.openGameButton.setOnClickListener { openExAstrisSave() }
        binding.openManualButton.setOnClickListener { pickFile() }
        binding.saveButton.setOnClickListener { saveBack() }
        binding.saveAsButton.setOnClickListener { saveAs() }
        binding.addFab.setOnClickListener { showAddItemSheet() }
        binding.bulkButton.setOnClickListener { showBulkActionsSheet() }
        binding.backupNowButton.setOnClickListener { createManualBackup() }
        binding.restoreBackupButton.setOnClickListener { showBackupPicker() }
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inventory -> showPage(Page.INVENTORY)
                R.id.nav_save -> showPage(Page.SAVE)
                R.id.nav_settings -> showPage(Page.SETTINGS)
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_inventory
    }

    private enum class Page { INVENTORY, SAVE, SETTINGS }

    private fun showPage(page: Page): Boolean {
        binding.inventoryPage.visibility = if (page == Page.INVENTORY) View.VISIBLE else View.GONE
        binding.savePage.visibility = if (page == Page.SAVE) View.VISIBLE else View.GONE
        binding.settingsPage.visibility = if (page == Page.SETTINGS) View.VISIBLE else View.GONE
        binding.addFab.visibility = if (page == Page.INVENTORY) View.VISIBLE else View.GONE
        return true
    }

    private fun setupSettings() {
        when (accessMode()) {
            AccessMode.AUTO -> binding.accessModeGroup.check(R.id.accessAutoButton)
            AccessMode.ROOT -> binding.accessModeGroup.check(R.id.accessRootButton)
            AccessMode.SHIZUKU -> binding.accessModeGroup.check(R.id.accessShizukuButton)
            AccessMode.MANUAL -> binding.accessModeGroup.check(R.id.accessManualButton)
        }
        binding.accessModeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.accessRootButton -> AccessMode.ROOT
                R.id.accessShizukuButton -> AccessMode.SHIZUKU
                R.id.accessManualButton -> AccessMode.MANUAL
                else -> AccessMode.AUTO
            }
            prefs.edit().putString("access_mode", mode.name).apply()
            updateAccessUi()
            when (mode) {
                AccessMode.ROOT -> if (rootService == null) requestOrConnectRoot()
                AccessMode.SHIZUKU -> if (remoteService == null) requestOrConnectShizuku()
                else -> Unit
            }
        }

        binding.showIdsSwitch.isChecked = showItemIds()
        binding.largeIconsSwitch.isChecked = largeItemIcons()
        binding.confirmBulkSwitch.isChecked = confirmBulkActions()
        binding.autoBackupSwitch.isChecked = automaticBackup()

        binding.showIdsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("show_ids", checked).apply()
            renderRows()
        }
        binding.largeIconsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("large_icons", checked).apply()
            renderRows()
        }
        binding.confirmBulkSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("confirm_bulk", checked).apply()
        }
        binding.autoBackupSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("auto_backup", checked).apply()
        }

        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (_: Throwable) {
            ""
        }
        binding.versionText.text = getString(R.string.version_label, version)
    }

    private fun showItemIds(): Boolean = prefs.getBoolean("show_ids", true)
    private fun largeItemIcons(): Boolean = prefs.getBoolean("large_icons", false)
    private fun confirmBulkActions(): Boolean = prefs.getBoolean("confirm_bulk", true)
    private fun automaticBackup(): Boolean = prefs.getBoolean("auto_backup", true)

    private fun accessMode(): AccessMode = try {
        AccessMode.valueOf(prefs.getString("access_mode", AccessMode.AUTO.name) ?: AccessMode.AUTO.name)
    } catch (_: Throwable) {
        AccessMode.AUTO
    }

    private fun activeBackend(): AccessBackend = when (accessMode()) {
        AccessMode.ROOT -> if (rootService != null) AccessBackend.ROOT else AccessBackend.NONE
        AccessMode.SHIZUKU -> if (remoteService != null) AccessBackend.SHIZUKU else AccessBackend.NONE
        AccessMode.MANUAL -> AccessBackend.MANUAL
        AccessMode.AUTO -> when {
            rootService != null -> AccessBackend.ROOT
            remoteService != null -> AccessBackend.SHIZUKU
            else -> AccessBackend.NONE
        }
    }

    private fun serviceFor(backend: AccessBackend): IShizukuFileService? = when (backend) {
        AccessBackend.ROOT -> rootService
        AccessBackend.SHIZUKU -> remoteService
        else -> null
    }

    private fun connectSelectedAccess() {
        when (accessMode()) {
            AccessMode.ROOT -> requestOrConnectRoot()
            AccessMode.SHIZUKU -> requestOrConnectShizuku()
            AccessMode.MANUAL -> pickFile()
            AccessMode.AUTO -> when {
                rootService != null || remoteService != null -> updateAccessUi()
                Shell.isAppGrantedRoot() == true -> requestOrConnectRoot()
                shizukuAlive() && hasShizukuPermission() -> bindShizukuFileService()
                else -> requestOrConnectRoot()
            }
        }
    }

    // ---------------- Root / access ----------------

    private fun requestOrConnectRoot() {
        if (rootService != null) {
            updateRootUi()
            updateAccessUi()
            return
        }
        if (rootBinding) return

        rootBinding = true
        updateRootUi()
        updateAccessUi()

        try {
            Shell.getShell { shell ->
                if (!shell.isRoot) {
                    rootBinding = false
                    updateRootUi()
                    updateAccessUi()
                    if (accessMode() == AccessMode.AUTO && shizukuAlive()) {
                        requestOrConnectShizuku()
                    } else {
                        toast(getString(R.string.root_denied_detail))
                    }
                    return@getShell
                }
                try {
                    RootService.bind(rootIntent, rootConnection)
                } catch (e: Throwable) {
                    rootBinding = false
                    updateRootUi()
                    updateAccessUi()
                    toast(getString(R.string.root_bind_failed, e.message ?: e.javaClass.simpleName))
                }
            }
        } catch (e: Throwable) {
            rootBinding = false
            updateRootUi()
            updateAccessUi()
            toast(getString(R.string.root_bind_failed, e.message ?: e.javaClass.simpleName))
        }
    }

    private fun updateRootUi() {
        if (!::binding.isInitialized) return
        val service = rootService
        when {
            service != null -> {
                val uid = try { service.remoteUid } catch (_: Throwable) { 0 }
                binding.rootStatus.setText(R.string.root_ready)
                binding.rootStatus.setTextColor(color(R.color.exa_success))
                binding.rootDetail.text = getString(R.string.root_uid_detail, uid)
                binding.rootButton.setText(R.string.root_reconnect)
            }
            rootBinding -> {
                binding.rootStatus.setText(R.string.root_connecting)
                binding.rootStatus.setTextColor(color(R.color.exa_warning))
                binding.rootDetail.setText(R.string.root_description)
                binding.rootButton.setText(R.string.root_request)
            }
            Shell.isAppGrantedRoot() == true -> {
                binding.rootStatus.setText(R.string.root_checking)
                binding.rootStatus.setTextColor(color(R.color.exa_warning))
                binding.rootDetail.setText(R.string.root_granted)
                binding.rootButton.setText(R.string.root_reconnect)
            }
            Shell.isAppGrantedRoot() == false -> {
                binding.rootStatus.setText(R.string.root_denied)
                binding.rootStatus.setTextColor(color(R.color.exa_error))
                binding.rootDetail.setText(R.string.root_denied_detail)
                binding.rootButton.setText(R.string.root_request)
            }
            else -> {
                binding.rootStatus.setText(R.string.root_checking)
                binding.rootStatus.setTextColor(color(R.color.exa_warning))
                binding.rootDetail.setText(R.string.root_description)
                binding.rootButton.setText(R.string.root_request)
            }
        }
    }

    private fun updateAccessUi() {
        if (!::binding.isInitialized) return
        val mode = accessMode()
        binding.accessModeSummary.setText(when (mode) {
            AccessMode.AUTO -> R.string.access_auto_summary
            AccessMode.ROOT -> R.string.access_root_summary
            AccessMode.SHIZUKU -> R.string.access_shizuku_summary
            AccessMode.MANUAL -> R.string.access_manual_summary
        })

        val backend = activeBackend()
        val statusRes = when (backend) {
            AccessBackend.ROOT -> R.string.access_active_root
            AccessBackend.SHIZUKU -> R.string.access_active_shizuku
            AccessBackend.MANUAL -> R.string.access_active_manual
            AccessBackend.NONE -> R.string.access_unavailable
        }
        val statusColor = when (backend) {
            AccessBackend.ROOT, AccessBackend.SHIZUKU -> R.color.exa_success
            AccessBackend.MANUAL -> R.color.exa_primary
            AccessBackend.NONE -> R.color.exa_warning
        }
        binding.compactShizukuStatus.setText(statusRes)
        binding.compactShizukuStatus.setTextColor(color(statusColor))

        val ready = backend == AccessBackend.ROOT || backend == AccessBackend.SHIZUKU
        binding.openGameButton.isEnabled = true
        binding.compactShizukuButton.visibility = if (ready) View.GONE else View.VISIBLE
        binding.compactShizukuButton.setText(if (mode == AccessMode.MANUAL) R.string.access_choose_file else R.string.connect)

        val rootGrant = when (Shell.isAppGrantedRoot()) {
            true -> getString(R.string.diag_yes)
            false -> getString(R.string.diag_no)
            null -> getString(R.string.diag_unknown)
        }
        val rootSvc = if (rootService != null) getString(R.string.diag_connected) else getString(R.string.diag_disconnected)
        val shizuku = if (remoteService != null) getString(R.string.diag_connected) else getString(R.string.diag_disconnected)
        val androidData = if (rootService != null || remoteService != null) getString(R.string.diag_available) else getString(R.string.diag_unavailable)
        binding.accessDiagnostics.text = getString(
            R.string.diagnostics_format,
            mode.name,
            backend.name,
            rootGrant,
            rootSvc,
            shizuku,
            androidData
        )
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

        if (!shizukuAlive()) {
            applyShizukuState(
                R.string.shizuku_not_running,
                R.color.exa_error,
                R.string.shizuku_missing_message,
                R.string.connect,
                false
            )
            return
        }

        val granted = try {
            !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

        if (!granted) {
            applyShizukuState(
                R.string.shizuku_permission_needed,
                R.color.exa_warning,
                R.string.shizuku_description,
                R.string.grant,
                false
            )
            return
        }

        val service = remoteService
        if (service != null) {
            val uid = try { service.remoteUid } catch (_: Throwable) { -1 }
            val detail = if (uid == 0) R.string.shizuku_root else R.string.shizuku_shell
            applyShizukuState(
                R.string.shizuku_ready,
                R.color.exa_success,
                detail,
                R.string.reconnect,
                true
            )
        } else {
            applyShizukuState(
                R.string.shizuku_checking,
                R.color.exa_warning,
                R.string.shizuku_description,
                R.string.reconnect,
                false
            )
        }
    }

    private fun applyShizukuState(
        statusRes: Int,
        colorRes: Int,
        detailRes: Int,
        buttonRes: Int,
        ready: Boolean
    ) {
        val statusText = getString(statusRes)
        val color = color(colorRes)

        binding.shizukuStatus.text = statusText
        binding.shizukuStatus.setTextColor(color)
        binding.shizukuDetail.setText(detailRes)
        binding.shizukuButton.setText(buttonRes)
        updateAccessUi()
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

    private fun itemIconRes(id: Int): Int {
        val res = resources.getIdentifier("item_$id", "drawable", packageName)
        return if (res != 0) res else R.drawable.ic_item_placeholder
    }

    // ---------------- direct Android/data access ----------------

    private fun openExAstrisSave() {
        val mode = accessMode()
        if (mode == AccessMode.MANUAL) {
            pickFile()
            return
        }

        var backend = activeBackend()
        if (backend == AccessBackend.NONE) {
            connectSelectedAccess()
            return
        }
        if (backend == AccessBackend.MANUAL) {
            pickFile()
            return
        }
        val service = serviceFor(backend) ?: run {
            connectSelectedAccess()
            return
        }

        binding.openGameButton.isEnabled = false
        io.execute {
            try {
                val paths = service.findSaveFiles()
                runOnUiThread {
                    updateRootUi()
                    updateShizukuUi()
                    updateAccessUi()
                    when {
                        paths.isEmpty() -> toast(getString(R.string.save_not_found))
                        paths.size == 1 -> openPrivilegedPath(paths[0], backend)
                        else -> showSaveCandidates(paths, backend)
                    }
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    updateAccessUi()
                    toast(getString(R.string.cannot_open, e.message ?: e.javaClass.simpleName))
                }
            }
        }
    }

    private fun showSaveCandidates(paths: Array<String>, backend: AccessBackend) {
        val labels = paths.map { path ->
            val f = File(path)
            val parent = f.parentFile?.parentFile?.name.orEmpty()
            if (parent.isBlank()) f.name else "$parent / ${f.name}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.save_candidates_title)
            .setItems(labels) { _, which -> openPrivilegedPath(paths[which], backend) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openPrivilegedPath(path: String, backend: AccessBackend) {
        val service = serviceFor(backend) ?: return toast(getString(R.string.access_unavailable))
        io.execute {
            try {
                if (!service.exists(path)) throw IllegalStateException(getString(R.string.save_path_missing))
                val pfd = service.openRead(path)
                val bytes = ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
                val parsed = SaveFile(bytes)
                backupOriginal(bytes)
                runOnUiThread {
                    save = parsed
                    directPath = path
                    directBackend = backend
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
                    directPath = null
                    directBackend = AccessBackend.MANUAL
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

    // ---------------- backups ----------------

    private fun backupDir(): File? = getExternalFilesDir("backups")?.apply { mkdirs() }

    private fun listBackups(): List<File> = backupDir()
        ?.listFiles { file -> file.isFile && file.name.endsWith(".bak") }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()

    private fun writeBackup(bytes: ByteArray): File? {
        val dir = backupDir() ?: return null
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        val file = File(dir, "SaveFile0.$stamp.bak")
        file.writeBytes(bytes)
        return file
    }

    private fun backupOriginal(bytes: ByteArray) {
        if (!automaticBackup()) return
        try {
            writeBackup(bytes)
        } catch (_: Throwable) {
        }
    }

    private fun createManualBackup() {
        val sf = save ?: return toast(getString(R.string.open_first))
        binding.backupNowButton.isEnabled = false
        io.execute {
            try {
                writeBackup(sf.build()) ?: throw IllegalStateException("backup directory unavailable")
                runOnUiThread {
                    binding.backupNowButton.isEnabled = true
                    updateBackupUi()
                    toast(getString(R.string.backup_created))
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    binding.backupNowButton.isEnabled = true
                    toast(getString(R.string.backup_failed, e.message ?: e.javaClass.simpleName))
                }
            }
        }
    }

    private fun updateBackupUi() {
        if (!::binding.isInitialized) return
        val backups = listBackups()
        if (backups.isEmpty()) {
            binding.backupInfo.setText(R.string.backup_none)
            binding.restoreBackupButton.isEnabled = false
        } else {
            val latest = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(Date(backups.first().lastModified()))
            binding.backupInfo.text = getString(R.string.backup_info, backups.size, latest)
            binding.restoreBackupButton.isEnabled = true
        }
        binding.backupNowButton.isEnabled = save != null
    }

    private fun showBackupPicker() {
        val backups = listBackups()
        if (backups.isEmpty()) return

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val labels = backups.map { file ->
            "${dateFormat.format(Date(file.lastModified()))} · ${file.length() / 1024} KB"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_backup)
            .setItems(labels) { _, which -> loadBackup(backups[which]) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadBackup(file: File) {
        io.execute {
            try {
                val parsed = SaveFile(file.readBytes())
                runOnUiThread {
                    save = parsed
                    refreshInventory()
                    updateFileUi()
                    toast(getString(R.string.backup_loaded))
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    toast(getString(R.string.backup_failed, e.message ?: e.javaClass.simpleName))
                }
            }
        }
    }

    // ---------------- write ----------------

    private fun saveBack() {
        val sf = save ?: return toast(getString(R.string.open_first))
        val targetPath = directPath
        val documentUri = uri

        if (targetPath == null && documentUri == null) return toast(getString(R.string.open_first))

        binding.saveButton.isEnabled = false
        io.execute {
            try {
                val data = sf.build()
                if (targetPath != null) {
                    val backend = directBackend
                    val service = serviceFor(backend)
                        ?: throw IllegalStateException(getString(R.string.access_unavailable))
                    if (!service.exists(targetPath)) throw IllegalStateException(getString(R.string.save_path_missing))

                    if (automaticBackup()) {
                        try {
                            val original = service.openRead(targetPath)
                            val bytes = ParcelFileDescriptor.AutoCloseInputStream(original).use { it.readBytes() }
                            writeBackup(bytes)
                        } catch (_: Throwable) {
                        }
                    }

                    var committed = false
                    try {
                        val pfd = service.openAtomicWrite(targetPath)
                        ParcelFileDescriptor.AutoCloseOutputStream(pfd).use { output ->
                            output.write(data)
                            output.flush()
                            output.fd.sync()
                        }
                        service.commitAtomicWrite(targetPath)
                        committed = true
                    } finally {
                        if (!committed) {
                            try { service.abortAtomicWrite(targetPath) } catch (_: Throwable) {}
                        }
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
            adapter.submit(emptyList(), showItemIds(), largeItemIcons())
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

        adapter.submit(rows, showItemIds(), largeItemIcons())
        binding.summaryText.text = getString(
            R.string.inventory_summary_compact,
            currentItems.size,
            rows.size
        )
    }

    private fun updateFileUi() {
        val sf = save
        if (sf == null) {
            binding.compactFileTitle.setText(R.string.no_file_short)
            binding.fileTitle.setText(R.string.no_file)
            binding.fileSource.setText(R.string.no_file_hint)
            binding.fileDetails.setText(R.string.empty_summary)
            binding.filePath.setText(R.string.no_file_hint)
            binding.saveButton.isEnabled = false
            binding.saveAsButton.isEnabled = false
            binding.bulkButton.isEnabled = false
            binding.addFab.isEnabled = false
            renderRows()
            updateBackupUi()
            return
        }

        val label = directPath?.let { File(it).name }
            ?: uri?.lastPathSegment?.substringAfterLast('/')
            ?: "SaveFile0.save"
        val saved = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            .format(Date(sf.savedAt * 1000L))

        binding.compactFileTitle.text = label
        binding.fileTitle.text = label
        binding.fileSource.setText(when {
            directPath == null -> R.string.save_source_manual
            directBackend == AccessBackend.ROOT -> R.string.access_root_file_source
            else -> R.string.save_source_shizuku
        })
        binding.fileDetails.text = getString(R.string.save_details, saved, sf.mods.size, currentItems.size)
        binding.filePath.text = directPath ?: uri?.toString().orEmpty()
        binding.saveButton.isEnabled = true
        binding.saveAsButton.isEnabled = true
        binding.bulkButton.isEnabled = true
        binding.addFab.isEnabled = true
        renderRows()
        updateBackupUi()
    }

    // ---------------- item editing sheets ----------------

    private fun showEditItemSheet(id: Int) {
        val sf = save ?: return
        val current = currentItems[id] ?: return
        val sheet = BottomSheetEditItemBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)

        sheet.itemName.text = nameOf(id)
        sheet.itemMeta.text = getString(R.string.item_meta, id, catLabel(catOf(id)))
        sheet.itemIcon.setImageResource(itemIconRes(id))
        sheet.quantityInput.setText(current.toString())
        sheet.quantityInput.setSelection(sheet.quantityInput.text?.length ?: 0)

        fun readAmount(): Int {
            val raw = sheet.quantityInput.text?.toString()?.toLongOrNull() ?: 0L
            return raw.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        }

        fun setAmount(value: Int) {
            sheet.quantityInput.setText(value.coerceAtLeast(0).toString())
            sheet.quantityInput.setSelection(sheet.quantityInput.text?.length ?: 0)
        }

        sheet.minusButton.setOnClickListener { setAmount((readAmount() - 1).coerceAtLeast(0)) }
        sheet.plusButton.setOnClickListener {
            val value = readAmount()
            setAmount(if (value == Int.MAX_VALUE) value else value + 1)
        }
        sheet.amount100Button.setOnClickListener { setAmount(100) }
        sheet.amount999Button.setOnClickListener { setAmount(999) }
        sheet.amount10kButton.setOnClickListener { setAmount(10_000) }
        sheet.amountMaxButton.setOnClickListener { setAmount(999_999) }

        sheet.applyButton.setOnClickListener {
            val value = readAmount()
            sf.setItems { itemId, _ -> if (itemId == id) value else null }
            refreshInventory()
            dialog.dismiss()
        }
        sheet.deleteButton.setOnClickListener {
            sf.removeItems(setOf(id))
            refreshInventory()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddItemSheet() {
        val sf = save ?: return toast(getString(R.string.open_first))
        val sheet = BottomSheetAddItemBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)

        sheet.addButton.setOnClickListener {
            val plan = HashMap<Int, Int>()
            for (token in sheet.input.text?.toString().orEmpty().trim().split(Regex("\\s+"))) {
                if (token.isBlank()) continue
                val parts = token.split("=")
                val id = parts[0].toIntOrNull() ?: continue
                plan[id] = parts.getOrNull(1)?.toIntOrNull() ?: 500
            }
            if (plan.isEmpty()) return@setOnClickListener
            val added = sf.addItems(plan)
            refreshInventory()
            toast(getString(R.string.added, added.size))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showBulkActionsSheet() {
        if (save == null) return toast(getString(R.string.open_first))
        val sheet = BottomSheetBulkActionsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)

        sheet.materialsButton.setOnClickListener {
            confirmAndBulk("material", 10_000, dialog)
        }
        sheet.consumablesButton.setOnClickListener {
            confirmAndBulk("consumable", 10_000, dialog)
        }
        sheet.currencyButton.setOnClickListener {
            confirmAndBulk("currency", 100_000, dialog)
        }

        dialog.show()
    }

    private fun confirmAndBulk(category: String, value: Int, sheet: BottomSheetDialog) {
        if (!confirmBulkActions()) {
            performBulk(category, value)
            sheet.dismiss()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bulk_confirm_title)
            .setMessage(getString(R.string.bulk_confirm_message, catLabel(category), value))
            .setPositiveButton(R.string.apply) { _, _ ->
                performBulk(category, value)
                sheet.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performBulk(category: String, value: Int) {
        val sf = save ?: return toast(getString(R.string.open_first))
        val changed = sf.setItems { id, _ ->
            if (catOf(id) == category) value else null
        }
        refreshInventory()
        toast(getString(R.string.entries_changed, changed))
    }

    private fun color(res: Int): Int = ContextCompat.getColor(this, res)

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
