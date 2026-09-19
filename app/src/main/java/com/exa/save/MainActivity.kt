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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.exa.save.databinding.ActivityMainBinding
import com.exa.save.databinding.BottomSheetAddItemBinding
import com.exa.save.databinding.BottomSheetBulkActionsBinding
import com.exa.save.databinding.BottomSheetEditItemBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val io = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("exa_ui", MODE_PRIVATE) }

    private val requestShizuku = 73

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { selected ->
        if (selected != null) openUri(selected)
    }

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { selected ->
        if (selected != null) writeToUri(selected)
    }

    private val categoryOrder = listOf("currency", "consumable", "material", "entropite", "recipe", "tritris", "quest", "curiosity", "pack", "other")

    private enum class AccessMode { AUTO, ROOT, SHIZUKU, MANUAL }
    private enum class AccessBackend { ROOT, SHIZUKU, MANUAL, NONE }
    private enum class RootProbeState { UNKNOWN, CHECKING, GRANTED, DENIED, ERROR }

    private var uri: Uri? = null
    private var directPath: String? = null
    private var directBackend: AccessBackend = AccessBackend.NONE
    private var save: SaveFile? = null
    private var currentItems: LinkedHashMap<Int, Int> = LinkedHashMap()
    private var baselineItems: Map<Int, Int> = emptyMap()

    private val names = HashMap<Int, String>()
    private val alternateNames = HashMap<Int, String>()
    private val searchTexts = HashMap<Int, String>()
    private val bulkEditableOverrides = HashMap<Int, Boolean>()
    private val cats = HashMap<Int, String>()
    private val descriptions = HashMap<Int, String>()
    private val verifiedItems = HashSet<Int>()
    private var hasUnsavedChanges = false
    private var editGeneration = 0L
    private var baselineSnapshot: ByteArray? = null
    private var undoSnapshot: ByteArray? = null
    private var isSaving = false
    private var suppressAccessModeListener = false

    private var activeCategory: String? = null
    private var searchQuery = ""

    private lateinit var adapter: InventoryAdapter

    @Volatile
    private var remoteService: IShizukuFileService? = null
    private var bindingService = false

    @Volatile
    private var rootService: IShizukuFileService? = null
    private var rootBinding = false
    private var rootProbeState = RootProbeState.UNKNOWN
    private var rootLastError: String? = null
    private var pendingOpenAfterConnect = false

    private val rootIntent by lazy { Intent(this, RootFileService::class.java) }

    private val rootConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val candidate = IShizukuFileService.Stub.asInterface(service)
            val uid = try { candidate?.remoteUid ?: -1 } catch (_: Throwable) { -1 }
            if (candidate == null || uid != 0) {
                rootService = null
                rootBinding = false
                rootProbeState = RootProbeState.ERROR
                rootLastError = getString(R.string.root_wrong_uid, uid)
                updateRootUi()
                updateAccessUi()
                toast(rootLastError ?: getString(R.string.access_unavailable))
                return
            }

            rootService = candidate
            rootBinding = false
            rootProbeState = RootProbeState.GRANTED
            rootLastError = null
            updateRootUi()
            updateAccessUi()
            if (pendingOpenAfterConnect) {
                pendingOpenAfterConnect = false
                openExAstrisSave()
            }
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
            if (pendingOpenAfterConnect) {
                pendingOpenAfterConnect = false
                openExAstrisSave()
            }
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
            } else {
                pendingOpenAfterConnect = false
            }
            updateAccessUi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        setupBackNavigation()

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

        binding.chipAll.setOnClickListener { selectCategory(null, binding.chipAll) }
        binding.chipCurrency.setOnClickListener { selectCategory("currency", binding.chipCurrency) }
        binding.chipConsumable.setOnClickListener { selectCategory("consumable", binding.chipConsumable) }
        binding.chipMaterial.setOnClickListener { selectCategory("material", binding.chipMaterial) }
        binding.chipEntropite.setOnClickListener { selectCategory("entropite", binding.chipEntropite) }
        binding.chipRecipe.setOnClickListener { selectCategory("recipe", binding.chipRecipe) }
        binding.chipTritris.setOnClickListener { selectCategory("tritris", binding.chipTritris) }
        binding.chipQuest.setOnClickListener { selectCategory("quest", binding.chipQuest) }
        binding.chipCuriosity.setOnClickListener { selectCategory("curiosity", binding.chipCuriosity) }
        binding.chipPack.setOnClickListener { selectCategory("pack", binding.chipPack) }
        binding.chipOther.setOnClickListener { selectCategory("other", binding.chipOther) }
    }

    private fun selectCategory(category: String?, chip: View) {
        activeCategory = category
        renderRows()
        binding.categoryScroll.post {
            val centered = chip.left - (binding.categoryScroll.width - chip.width) / 2
            binding.categoryScroll.smoothScrollTo(centered.coerceAtLeast(0), 0)
        }
    }

    private fun setupActions() {
        binding.compactShizukuButton.setOnClickListener { connectSelectedAccess() }
        binding.rootButton.setOnClickListener { requestOrConnectRoot() }
        binding.shizukuButton.setOnClickListener { requestOrConnectShizuku() }
        binding.openGameButton.setOnClickListener {
            resolveUnsavedChanges { openExAstrisSave(forcePicker = save != null) }
        }
        binding.openManualButton.setOnClickListener {
            resolveUnsavedChanges { pickFile() }
        }
        binding.saveButton.setOnClickListener { saveBack() }
        binding.saveNowButton.setOnClickListener { saveBack() }
        binding.undoChangeButton.setOnClickListener { undoLastChange() }
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
            if (!isChecked || suppressAccessModeListener) return@addOnButtonCheckedListener
            val requestedMode = when (checkedId) {
                R.id.accessRootButton -> AccessMode.ROOT
                R.id.accessShizukuButton -> AccessMode.SHIZUKU
                R.id.accessManualButton -> AccessMode.MANUAL
                else -> AccessMode.AUTO
            }
            val previousMode = accessMode()
            if (requestedMode == previousMode) return@addOnButtonCheckedListener

            if (hasUnsavedChanges) {
                suppressAccessModeListener = true
                binding.accessModeGroup.check(accessModeButtonId(previousMode))
                suppressAccessModeListener = false
                resolveUnsavedChanges { applyAccessMode(requestedMode) }
            } else {
                applyAccessMode(requestedMode)
            }
        }

        binding.showIdsSwitch.isChecked = showItemIds()
        binding.largeIconsSwitch.isChecked = largeItemIcons()
        binding.confirmBulkSwitch.isChecked = confirmBulkActions()
        binding.autoBackupSwitch.isChecked = automaticBackup()
        binding.autoSaveSwitch.isChecked = automaticSaveChanges()

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
        binding.autoSaveSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("auto_save_changes", checked).apply()
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
    private fun automaticSaveChanges(): Boolean = prefs.getBoolean("auto_save_changes", false)

    private fun accessModeButtonId(mode: AccessMode): Int = when (mode) {
        AccessMode.AUTO -> R.id.accessAutoButton
        AccessMode.ROOT -> R.id.accessRootButton
        AccessMode.SHIZUKU -> R.id.accessShizukuButton
        AccessMode.MANUAL -> R.id.accessManualButton
    }

    private fun applyAccessMode(mode: AccessMode) {
        prefs.edit().putString("access_mode", mode.name).apply()
        suppressAccessModeListener = true
        binding.accessModeGroup.check(accessModeButtonId(mode))
        suppressAccessModeListener = false
        updateAccessUi()
        when (mode) {
            AccessMode.ROOT -> if (rootService == null) requestOrConnectRoot()
            AccessMode.SHIZUKU -> if (remoteService == null) requestOrConnectShizuku()
            else -> Unit
        }
    }

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
                Shell.getCachedShell()?.isRoot == true -> requestOrConnectRoot(forceRefresh = false)
                shizukuAlive() && hasShizukuPermission() -> bindShizukuFileService()
                else -> requestOrConnectRoot(forceRefresh = true)
            }
        }
    }

    // ---------------- Root / access ----------------

    private fun requestOrConnectRoot(forceRefresh: Boolean = true) {
        if (rootService != null) {
            updateRootUi()
            updateAccessUi()
            return
        }
        if (rootBinding) return

        rootBinding = true
        rootProbeState = RootProbeState.CHECKING
        rootLastError = null
        updateRootUi()
        updateAccessUi()

        val cached = try { Shell.getCachedShell() } catch (_: Throwable) { null }
        if (forceRefresh && cached != null && !cached.isRoot) {
            io.execute {
                val closed = try {
                    cached.waitAndClose(2, TimeUnit.SECONDS)
                } catch (_: Throwable) {
                    false
                }
                if (!closed) {
                    try { cached.close() } catch (_: Throwable) {}
                }
                runOnUiThread { acquireRootShellAndBind() }
            }
        } else {
            acquireRootShellAndBind()
        }
    }

    private fun acquireRootShellAndBind() {
        try {
            Shell.getShell { shell ->
                if (!shell.isRoot) {
                    rootBinding = false
                    rootProbeState = RootProbeState.DENIED
                    rootLastError = null
                    updateRootUi()
                    updateAccessUi()
                    if (accessMode() == AccessMode.AUTO && shizukuAlive()) {
                        requestOrConnectShizuku()
                    } else {
                        pendingOpenAfterConnect = false
                        toast(getString(R.string.root_denied_detail))
                    }
                    return@getShell
                }

                rootProbeState = RootProbeState.GRANTED
                rootLastError = null
                updateRootUi()
                try {
                    RootService.bind(rootIntent, rootConnection)
                    binding.root.postDelayed({
                        if (rootBinding && rootService == null) {
                            rootBinding = false
                            rootProbeState = RootProbeState.ERROR
                            rootLastError = getString(R.string.root_service_timeout)
                            updateRootUi()
                            updateAccessUi()
                            pendingOpenAfterConnect = false
                            toast(rootLastError ?: getString(R.string.access_unavailable))
                        }
                    }, 10_000L)
                } catch (e: Throwable) {
                    rootBinding = false
                    rootProbeState = RootProbeState.ERROR
                    rootLastError = e.message ?: e.javaClass.simpleName
                    updateRootUi()
                    updateAccessUi()
                    pendingOpenAfterConnect = false
                    toast(getString(R.string.root_bind_failed, rootLastError))
                }
            }
        } catch (e: Throwable) {
            rootBinding = false
            rootProbeState = RootProbeState.ERROR
            rootLastError = e.message ?: e.javaClass.simpleName
            updateRootUi()
            updateAccessUi()
            pendingOpenAfterConnect = false
            toast(getString(R.string.root_bind_failed, rootLastError))
        }
    }

    private fun rootGrantState(): Boolean? {
        if (rootService != null) return true
        val cached = try { Shell.getCachedShell() } catch (_: Throwable) { null }
        if (cached != null && cached.isAlive) return cached.isRoot
        return when (rootProbeState) {
            RootProbeState.GRANTED -> true
            RootProbeState.DENIED -> false
            else -> null
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
            rootBinding || rootProbeState == RootProbeState.CHECKING -> {
                binding.rootStatus.setText(R.string.root_connecting)
                binding.rootStatus.setTextColor(color(R.color.exa_warning))
                binding.rootDetail.setText(R.string.root_retrying)
                binding.rootButton.setText(R.string.root_request)
            }
            rootProbeState == RootProbeState.ERROR -> {
                binding.rootStatus.setText(R.string.root_checking)
                binding.rootStatus.setTextColor(color(R.color.exa_warning))
                binding.rootDetail.text = rootLastError ?: getString(R.string.root_description)
                binding.rootButton.setText(R.string.root_reconnect)
            }
            rootGrantState() == true -> {
                binding.rootStatus.setText(R.string.root_checking)
                binding.rootStatus.setTextColor(color(R.color.exa_warning))
                binding.rootDetail.setText(R.string.root_granted)
                binding.rootButton.setText(R.string.root_reconnect)
            }
            rootGrantState() == false -> {
                binding.rootStatus.setText(R.string.root_denied)
                binding.rootStatus.setTextColor(color(R.color.exa_error))
                binding.rootDetail.setText(R.string.root_denied_detail)
                binding.rootButton.setText(R.string.root_reconnect)
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

        binding.rootCard.visibility = if (mode == AccessMode.ROOT) View.VISIBLE else View.GONE
        binding.shizukuCard.visibility = if (mode == AccessMode.SHIZUKU) View.VISIBLE else View.GONE
        binding.accessDiagnosticsCard.visibility = if (mode == AccessMode.AUTO) View.VISIBLE else View.GONE

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
        binding.compactShizukuButton.visibility = if (!ready) View.VISIBLE else View.GONE
        binding.compactShizukuButton.setText(when (mode) {
            AccessMode.MANUAL -> R.string.access_choose_file
            else -> R.string.connect
        })

        val rootStateText = when {
            rootService != null -> getString(R.string.diag_connected)
            rootBinding -> getString(R.string.root_retrying)
            rootGrantState() == true -> getString(R.string.diag_yes)
            rootGrantState() == false -> getString(R.string.diag_no)
            else -> getString(R.string.diag_unknown)
        }
        val shizukuStateText = if (remoteService != null) {
            getString(R.string.diag_connected)
        } else if (shizukuAlive()) {
            getString(R.string.diag_available)
        } else {
            getString(R.string.diag_disconnected)
        }
        binding.accessDiagnostics.text = getString(
            R.string.auto_diag_format,
            rootStateText,
            shizukuStateText,
            statusRes.let { getString(it).removePrefix("● ") }
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
            pendingOpenAfterConnect = false
            toast(getString(R.string.shizuku_missing_message))
            updateShizukuUi()
            return
        }

        try {
            if (Shizuku.isPreV11()) {
                pendingOpenAfterConnect = false
                toast(getString(R.string.shizuku_old))
                return
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindShizukuFileService()
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                pendingOpenAfterConnect = false
                toast(getString(R.string.shizuku_denied))
            } else {
                Shizuku.requestPermission(requestShizuku)
            }
        } catch (e: Throwable) {
            pendingOpenAfterConnect = false
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
                R.string.connect
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
                R.string.grant
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
                R.string.reconnect
            )
        } else {
            applyShizukuState(
                R.string.shizuku_checking,
                R.color.exa_warning,
                R.string.shizuku_description,
                R.string.reconnect
            )
        }
    }

    private fun applyShizukuState(
        statusRes: Int,
        colorRes: Int,
        detailRes: Int,
        buttonRes: Int
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

    private fun cleanCatalogText(raw: String): String {
        if (raw.isBlank()) return ""
        val withoutKeywordTags = raw
            .replace(Regex("\\[keyword=[^\\]]*?text=([^\\]]+)\\]"), "$1")
            .replace(Regex("\\[keyword=[^\\]]+\\]"), "")
        return HtmlCompat.fromHtml(
            withoutKeywordTags,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString().trim()
    }

    private fun loadCatalog() {
        try {
            val text = assets.open("items.json").bufferedReader().use { it.readText() }
            val obj = JSONObject(text)
            val ru = preferRussian()
            for (key in obj.keys()) {
                val id = key.toIntOrNull() ?: continue
                val v = obj.getJSONObject(key)
                val ruName = cleanCatalogText(v.optString("name", ""))
                val enName = cleanCatalogText(v.optString("name_en", ruName))
                names[id] = if (ru) ruName else enName
                alternateNames[id] = (if (ru) enName else ruName).takeIf { it.isNotBlank() && it != names[id] }.orEmpty()
                cats[id] = v.optString("cat", "")
                val ruDesc = cleanCatalogText(v.optString("description", ""))
                val enDesc = cleanCatalogText(v.optString("description_en", ruDesc))
                descriptions[id] = if (ru) ruDesc else enDesc
                // Search intentionally uses names only. Numeric IDs, descriptions and
                // categories remain visible elsewhere but no longer affect search results.
                searchTexts[id] = listOf(ruName, enName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .lowercase(Locale.ROOT)
                if (v.has("bulk_editable")) {
                    bulkEditableOverrides[id] = v.optBoolean("bulk_editable", true)
                }
                if (v.optBoolean("verified", false)) verifiedItems.add(id)
            }
        } catch (_: Exception) {
        }
    }

    private fun nameOf(id: Int): String {
        val name = names[id]
        return if (name.isNullOrBlank()) getString(R.string.unnamed) else name
    }

    private fun alternateNameOf(id: Int): String = alternateNames[id].orEmpty()

    private fun matchesSearch(id: Int): Boolean {
        if (searchQuery.isBlank()) return true
        val haystack = searchTexts[id] ?: nameOf(id).lowercase(Locale.ROOT)
        val terms = searchQuery.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        return terms.all { haystack.contains(it) }
    }

    private fun isBulkEditable(id: Int): Boolean {
        bulkEditableOverrides[id]?.let { return it }
        return when (catOf(id)) {
            "currency", "consumable", "material" -> true
            else -> false
        }
    }

    private fun descriptionOf(id: Int): String = descriptions[id].orEmpty().ifBlank {
        when (catOf(id)) {
            "material" -> if (preferRussian()) "Материал из сохранения." else "Material from the save file."
            "consumable" -> if (preferRussian()) "Расходуемый предмет из сохранения." else "Consumable item from the save file."
            "entropite" -> if (preferRussian()) "Боевой энтропит." else "Combat Entropith."
            "recipe" -> if (preferRussian()) "Рецепт из сохранения." else "Recipe from the save file."
            "tritris" -> if (preferRussian()) "Фрагмент Тритрис из сохранения." else "Tritris piece from the save file."
            "quest" -> if (preferRussian()) "Предмет задания из сохранения." else "Quest item from the save file."
            "curiosity" -> if (preferRussian()) "Коллекционный предмет из сохранения." else "Curiosity item from the save file."
            "pack" -> if (preferRussian()) "Набор из сохранения." else "Pack item from the save file."
            else -> if (preferRussian()) "Предмет из сохранения." else "Item from the save file."
        }
    }

    private fun itemVerified(id: Int): Boolean = id in verifiedItems

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
        "recipe" -> getString(R.string.cat_recipe)
        "tritris" -> getString(R.string.cat_tritris)
        "quest" -> getString(R.string.cat_quest)
        "curiosity" -> getString(R.string.cat_curiosity)
        "pack" -> getString(R.string.cat_pack)
        else -> getString(R.string.cat_other)
    }

    private fun itemIconRes(id: Int): Int {
        val res = resources.getIdentifier("item_$id", "drawable", packageName)
        return if (res != 0) res else R.drawable.ic_item_placeholder
    }

    // ---------------- direct Android/data access ----------------

    private fun openExAstrisSave(forcePicker: Boolean = false) {
        val mode = accessMode()
        if (mode == AccessMode.MANUAL) {
            pickFile()
            return
        }

        val backend = activeBackend()
        if (backend == AccessBackend.NONE) {
            pendingOpenAfterConnect = true
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
                        else -> {
                            if (forcePicker) {
                                showSaveCandidates(paths, backend)
                            } else {
                                val remembered = prefs.getString("last_save_path", null)
                                val rememberedPath = paths.firstOrNull { it == remembered }
                                val mainPath = paths.filter { File(it).name.equals("SaveFile0.save", ignoreCase = true) }
                                    .singleOrNull()
                                when {
                                    rememberedPath != null -> openPrivilegedPath(rememberedPath, backend)
                                    mainPath != null -> openPrivilegedPath(mainPath, backend)
                                    else -> showSaveCandidates(paths, backend)
                                }
                            }
                        }
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
            val base = if (parent.isBlank()) f.name else "$parent / ${f.name}"
            if (path == directPath) "✓ $base" else base
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
                    baselineSnapshot = bytes.copyOf()
                    undoSnapshot = null
                    hasUnsavedChanges = false
                    editGeneration = 0L
                    directPath = path
                    prefs.edit().putString("last_save_path", path).apply()
                    directBackend = backend
                    uri = null
                    refreshInventory()
                    baselineItems = LinkedHashMap(currentItems)
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
        openDocumentLauncher.launch(arrayOf("*/*"))
    }

    private fun saveAs() {
        if (save == null) return toast(getString(R.string.open_first))
        createDocumentLauncher.launch("SaveFile0.save")
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
                    baselineSnapshot = bytes.copyOf()
                    undoSnapshot = null
                    hasUnsavedChanges = false
                    editGeneration = 0L
                    uri = selected
                    directPath = null
                    directBackend = AccessBackend.MANUAL
                    refreshInventory()
                    baselineItems = LinkedHashMap(currentItems)
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
            .setItems(labels) { _, which ->
                resolveUnsavedChanges { loadBackup(backups[which]) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadBackup(file: File) {
        val before = snapshotForUndo()
        io.execute {
            try {
                val parsed = SaveFile(file.readBytes())
                runOnUiThread {
                    save = parsed
                    refreshInventory()
                    markDirty(before)
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

    private fun saveBack(onSuccess: (() -> Unit)? = null) {
        val sf = save ?: return toast(getString(R.string.open_first))
        if (isSaving) return
        val targetPath = directPath
        val documentUri = uri

        if (targetPath == null && documentUri == null) return toast(getString(R.string.open_first))

        isSaving = true
        binding.saveButton.isEnabled = false
        binding.saveNowButton.isEnabled = false
        val generationBeingSaved = editGeneration
        val itemsBeingSaved = LinkedHashMap(currentItems)
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
                    isSaving = false
                    baselineItems = itemsBeingSaved
                    baselineSnapshot = data.copyOf()
                    val fullySaved = editGeneration == generationBeingSaved
                    if (fullySaved) {
                        hasUnsavedChanges = false
                        undoSnapshot = null
                    }
                    updateDirtyUi()
                    toast(if (automaticSaveChanges()) getString(R.string.autosave_written) else getString(R.string.written, data.size))
                    if (fullySaved) onSuccess?.invoke()
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    isSaving = false
                    updateDirtyUi()
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
            binding.bulkButton.isEnabled = false
            return
        }

        val rows = currentItems.entries
            .asSequence()
            .filter { (id, _) -> activeCategory == null || catOf(id) == activeCategory }
            .filter { (id, _) -> matchesSearch(id) }
            .sortedWith(compareBy<Map.Entry<Int, Int>>(
                { categoryOrder.indexOf(catOf(it.key)).let { idx -> if (idx < 0) categoryOrder.size else idx } },
                { it.key }
            ))
            .map { (id, count) ->
                InventoryAdapter.Row(
                    id = id,
                    name = nameOf(id),
                    category = catLabel(catOf(id)),
                    count = count,
                    bulkEditable = isBulkEditable(id)
                )
            }
            .toList()

        adapter.submit(rows, showItemIds(), largeItemIcons())
        binding.summaryText.text = getString(
            R.string.inventory_summary_compact,
            currentItems.size,
            rows.size
        )
        binding.bulkButton.isEnabled = rows.any { it.bulkEditable }
    }

    private fun updateFileUi() {
        val sf = save
        if (sf == null) {
            binding.compactFileTitle.setText(R.string.no_file_short)
            binding.fileTitle.setText(R.string.no_file)
            binding.fileSource.setText(R.string.no_file_hint)
            binding.fileDetails.setText(R.string.empty_summary)
            binding.filePath.setText(R.string.no_file_hint)
            binding.openGameButton.setText(R.string.open_game_save)
            binding.saveButton.isEnabled = false
            binding.saveAsButton.isEnabled = false
            binding.bulkButton.isEnabled = false
            binding.addFab.isEnabled = false
            renderRows()
            baselineItems = emptyMap()
            baselineSnapshot = null
            undoSnapshot = null
            hasUnsavedChanges = false
            isSaving = false
            updateDirtyUi()
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
        binding.openGameButton.setText(if (directPath != null) R.string.switch_save else R.string.open_game_save)
        binding.saveButton.isEnabled = true
        binding.saveAsButton.isEnabled = true
        binding.bulkButton.isEnabled = true
        binding.addFab.isEnabled = true
        renderRows()
        updateDirtyUi()
        updateBackupUi()
    }

    private fun changedItemCount(): Int {
        val ids = HashSet<Int>()
        ids.addAll(baselineItems.keys)
        ids.addAll(currentItems.keys)
        return ids.count { baselineItems[it] != currentItems[it] }
    }

    private fun updateDirtyUi() {
        if (!::binding.isInitialized) return
        val changedCount = changedItemCount()
        binding.dirtyStatus.text = if (hasUnsavedChanges) {
            getString(R.string.unsaved_changes_count, changedCount)
        } else {
            getString(R.string.all_changes_saved)
        }
        binding.dirtyStatus.setTextColor(color(if (hasUnsavedChanges) R.color.exa_warning else R.color.exa_success))

        val label = if (save != null) {
            directPath?.let { File(it).name }
                ?: uri?.lastPathSegment?.substringAfterLast('/')
                ?: "SaveFile0.save"
        } else {
            getString(R.string.no_file_short)
        }

        if (save != null) {
            binding.compactFileTitle.text = if (hasUnsavedChanges) {
                getString(R.string.compact_unsaved_count, label, changedCount)
            } else label
            binding.compactFileTitle.setTextColor(color(if (hasUnsavedChanges) R.color.exa_warning else R.color.exa_text_secondary))
        }

        val showGuard = hasUnsavedChanges && save != null
        binding.saveGuardBar.visibility = if (showGuard) View.VISIBLE else View.GONE
        if (showGuard) {
            binding.saveGuardTitle.text = if (changedCount > 0) {
                getString(R.string.save_guard_compact_count, changedCount)
            } else {
                getString(R.string.save_guard_title)
            }
        }
        binding.undoChangeButton.isEnabled = showGuard && undoSnapshot != null && !isSaving
        binding.saveNowButton.isEnabled = showGuard && !isSaving
        binding.saveButton.isEnabled = save != null && !isSaving

        val badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_save)
        badge.isVisible = showGuard
        if (showGuard) badge.clearNumber()

        updateAccessUi()
    }

    private fun snapshotForUndo(): ByteArray? = try {
        save?.build()
    } catch (_: Throwable) {
        null
    }

    private fun markDirty(before: ByteArray? = null) {
        if (before != null) undoSnapshot = before
        editGeneration += 1
        hasUnsavedChanges = true
        updateDirtyUi()
        if (automaticSaveChanges()) {
            saveBack()
        } else {
            Snackbar.make(binding.root, R.string.changes_pending_notice, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.saveGuardBar)
                .setAction(R.string.save_short) { saveBack() }
                .show()
        }
    }

    private fun undoLastChange() {
        val snapshot = undoSnapshot ?: return
        try {
            save = SaveFile(snapshot)
            undoSnapshot = null
            editGeneration += 1
            refreshInventory()
            hasUnsavedChanges = changedItemCount() > 0
            updateFileUi()
            toast(getString(R.string.undo_done))
        } catch (e: Throwable) {
            toast(getString(R.string.undo_failed, e.message ?: e.javaClass.simpleName))
        }
    }

    private fun discardUnsavedChanges(after: () -> Unit) {
        val snapshot = baselineSnapshot
        if (snapshot == null) {
            hasUnsavedChanges = false
            undoSnapshot = null
            updateDirtyUi()
            after()
            return
        }
        io.execute {
            try {
                val parsed = SaveFile(snapshot)
                runOnUiThread {
                    save = parsed
                    refreshInventory()
                    baselineItems = LinkedHashMap(currentItems)
                    hasUnsavedChanges = false
                    undoSnapshot = null
                    editGeneration += 1
                    updateFileUi()
                    after()
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    toast(getString(R.string.discard_failed, e.message ?: e.javaClass.simpleName))
                }
            }
        }
    }

    private fun resolveUnsavedChanges(after: () -> Unit) {
        if (!hasUnsavedChanges || save == null) {
            after()
            return
        }
        val changedCount = changedItemCount()
        val label = directPath?.let { File(it).name }
            ?: uri?.lastPathSegment?.substringAfterLast('/')
            ?: "SaveFile0.save"
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.unsaved_dialog_title)
            .setMessage(getString(R.string.unsaved_dialog_message, changedCount, label))
            .setPositiveButton(R.string.save_and_continue) { _, _ -> saveBack(onSuccess = after) }
            .setNeutralButton(R.string.continue_without_saving) { _, _ -> discardUnsavedChanges(after) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges) {
                    resolveUnsavedChanges { finish() }
                } else {
                    finish()
                }
            }
        })
    }

    // ---------------- item editing sheets ----------------

    private fun showEditItemSheet(id: Int) {
        val sf = save ?: return
        val current = currentItems[id] ?: return
        val sheet = BottomSheetEditItemBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)

        sheet.itemName.text = nameOf(id)
        val alternate = alternateNameOf(id)
        sheet.itemAlias.text = alternate
        sheet.itemAlias.visibility = if (alternate.isBlank()) View.GONE else View.VISIBLE
        sheet.itemMeta.text = getString(R.string.item_meta, id, catLabel(catOf(id)))
        sheet.itemProtectionCard.visibility = if (isBulkEditable(id)) View.GONE else View.VISIBLE
        sheet.itemDescription.text = descriptionOf(id)
        sheet.itemKnowledgeStatus.setText(if (itemVerified(id)) R.string.item_info_verified else R.string.item_info_unverified)
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
            val before = snapshotForUndo()
            val value = readAmount()
            val changed = sf.setItems { itemId, _ -> if (itemId == id) value else null }
            refreshInventory()
            if (changed > 0) markDirty(before)
            dialog.dismiss()
        }
        sheet.deleteButton.setOnClickListener {
            val before = snapshotForUndo()
            val removed = sf.removeItems(setOf(id))
            refreshInventory()
            if (removed.isNotEmpty()) markDirty(before)
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
            val before = snapshotForUndo()
            val added = sf.addItems(plan)
            refreshInventory()
            if (added.isNotEmpty()) markDirty(before)
            toast(getString(R.string.added, added.size))
            dialog.dismiss()
        }

        dialog.show()
    }

    private enum class BulkOperation { SET, ADD, SUBTRACT }

    private fun showBulkActionsSheet() {
        if (save == null) return toast(getString(R.string.open_first))
        val sheet = BottomSheetBulkActionsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheet.root)

        when (activeCategory) {
            "consumable" -> sheet.categoryGroup.check(R.id.consumablesButton)
            "currency" -> sheet.categoryGroup.check(R.id.currencyButton)
            "material" -> sheet.categoryGroup.check(R.id.materialsButton)
            else -> sheet.categoryGroup.check(R.id.allItemsButton)
        }
        // If the current list is a protected category, keep the sheet scoped to
        // what the user sees so it clearly reports that those items are skipped.
        sheet.visibleOnlyCheck.isChecked = activeCategory != null && activeCategory !in setOf("currency", "consumable", "material")
        sheet.operationGroup.check(R.id.setOperationButton)
        sheet.valueInput.setText(prefs.getInt("last_bulk_value", 10_000).toString())

        fun selectedCategory(): String? = when (sheet.categoryGroup.checkedButtonId) {
            R.id.allItemsButton -> null
            R.id.consumablesButton -> "consumable"
            R.id.currencyButton -> "currency"
            else -> "material"
        }
        fun selectedOperation(): BulkOperation = when (sheet.operationGroup.checkedButtonId) {
            R.id.addOperationButton -> BulkOperation.ADD
            R.id.subtractOperationButton -> BulkOperation.SUBTRACT
            else -> BulkOperation.SET
        }
        fun readValue(): Int {
            val raw = sheet.valueInput.text?.toString()?.replace(" ", "")?.toLongOrNull() ?: 0L
            return raw.coerceIn(0L, 999_999_999L).toInt()
        }
        fun setValue(value: Int) {
            sheet.valueInput.setText(value.toString())
            sheet.valueInput.setSelection(sheet.valueInput.text?.length ?: 0)
        }
        fun candidateIds(): List<Int> {
            val category = selectedCategory()
            val byCategory = currentItems.keys.filter { category == null || catOf(it) == category }
            if (!sheet.visibleOnlyCheck.isChecked) return byCategory
            return byCategory.filter { id ->
                (activeCategory == null || catOf(id) == activeCategory) && matchesSearch(id)
            }
        }
        fun eligibleIds(): Set<Int> = candidateIds().filter(::isBulkEditable).toSet()
        fun updatePreview() {
            val candidates = candidateIds()
            val eligible = candidates.count(::isBulkEditable)
            val protected = candidates.size - eligible
            sheet.previewText.text = if (protected > 0) {
                getString(R.string.bulk_preview_with_protected, eligible, protected)
            } else {
                getString(R.string.bulk_preview, eligible)
            }
            sheet.applyButton.isEnabled = eligible > 0
        }

        sheet.preset100Button.setOnClickListener { setValue(100) }
        sheet.preset999Button.setOnClickListener { setValue(999) }
        sheet.preset10kButton.setOnClickListener { setValue(10_000) }
        sheet.preset100kButton.setOnClickListener { setValue(100_000) }
        sheet.presetMaxButton.setOnClickListener { setValue(999_999) }
        sheet.categoryGroup.addOnButtonCheckedListener { _, _, checked -> if (checked) updatePreview() }
        sheet.visibleOnlyCheck.setOnCheckedChangeListener { _, _ -> updatePreview() }
        updatePreview()

        sheet.applyButton.setOnClickListener {
            val ids = eligibleIds()
            if (ids.isEmpty()) return@setOnClickListener toast(getString(R.string.bulk_nothing))
            val value = readValue()
            prefs.edit().putInt("last_bulk_value", value).apply()
            val operation = selectedOperation()
            val proceed = {
                performBulk(ids, value, operation)
                dialog.dismiss()
            }
            if (!confirmBulkActions()) {
                proceed()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.bulk_confirm_title)
                    .setMessage(getString(
                        when (operation) {
                            BulkOperation.SET -> R.string.bulk_confirm_set
                            BulkOperation.ADD -> R.string.bulk_confirm_add
                            BulkOperation.SUBTRACT -> R.string.bulk_confirm_subtract
                        },
                        ids.size,
                        value
                    ))
                    .setPositiveButton(R.string.apply) { _, _ -> proceed() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
        dialog.show()
    }

    private fun performBulk(ids: Set<Int>, value: Int, operation: BulkOperation) {
        val sf = save ?: return toast(getString(R.string.open_first))
        val before = snapshotForUndo()
        val changed = sf.setItems { id, current ->
            if (id !in ids) null else when (operation) {
                BulkOperation.SET -> value
                BulkOperation.ADD -> (current.toLong() + value.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                BulkOperation.SUBTRACT -> (current.toLong() - value.toLong()).coerceAtLeast(0L).toInt()
            }
        }
        refreshInventory()
        if (changed > 0) markDirty(before)
        toast(getString(R.string.entries_changed, changed))
    }

    private fun color(res: Int): Int = ContextCompat.getColor(this, res)

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
