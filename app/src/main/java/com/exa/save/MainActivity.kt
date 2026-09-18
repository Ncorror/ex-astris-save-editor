package com.exa.save

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private val REQ_OPEN = 1
    private val REQ_SAVE_AS = 2

    /** Category keys are language neutral; display names come from string resources. */
    private val CAT_ORDER = listOf("currency", "consumable", "material", "entropite", "other")

    private var uri: Uri? = null
    private var save: SaveFile? = null
    private val names = HashMap<Int, String>()
    private val cats = HashMap<Int, String>()

    private lateinit var header: TextView
    private lateinit var list: ListView
    private var rows: List<Int> = emptyList()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        loadCatalog()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(24, 24, 24, 24)

        header = TextView(this)
        header.text = getString(R.string.title) + "\n\n" + getString(R.string.no_file)
        header.setTypeface(Typeface.MONOSPACE)
        root.addView(header)

        val bar1 = LinearLayout(this)
        bar1.orientation = LinearLayout.HORIZONTAL
        bar1.addView(button(R.string.open) { pickFile() })
        bar1.addView(button(R.string.save) { saveBack() })
        bar1.addView(button(R.string.save_as) { saveAs() })
        root.addView(bar1)

        val bar2 = LinearLayout(this)
        bar2.orientation = LinearLayout.HORIZONTAL
        bar2.addView(button(R.string.materials_10k) { bulk("material", 10000) })
        bar2.addView(button(R.string.consumables_10k) { bulk("consumable", 10000) })
        bar2.addView(button(R.string.add_by_id) { addDialog() })
        root.addView(bar2)

        list = ListView(this)
        list.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        )
        list.setOnItemClickListener { _, _, pos, _ -> editDialog(rows[pos]) }
        root.addView(list)

        setContentView(root)
    }

    private fun button(textRes: Int, onClick: () -> Unit): Button {
        val b = Button(this)
        b.setText(textRes)
        b.textSize = 12f
        b.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        b.setOnClickListener { onClick() }
        return b
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    // ---------------- catalog ----------------

    /** Russian device language picks `name`, everything else picks `name_en`. */
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
        } catch (e: Exception) {
            // the catalog is optional, items just stay unnamed
        }
    }

    private fun nameOf(id: Int): String {
        val n = names[id]
        return if (n.isNullOrEmpty()) getString(R.string.unnamed) else n
    }

    /** Falls back to id ranges when the catalog has no entry. */
    private fun catOf(id: Int): String {
        cats[id]?.let { if (it.isNotEmpty()) return it }
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

    // ---------------- file ----------------

    private fun pickFile() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        startActivityForResult(i, REQ_OPEN)
    }

    private fun saveAs() {
        if (save == null) return toast(getString(R.string.open_first))
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        i.putExtra(Intent.EXTRA_TITLE, "SaveFile0.save")
        startActivityForResult(i, REQ_SAVE_AS)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res != RESULT_OK || data?.data == null) return
        val u = data.data!!
        if (req == REQ_OPEN) openFile(u) else if (req == REQ_SAVE_AS) writeTo(u)
    }

    private fun openFile(u: Uri) {
        try {
            val bytes = contentResolver.openInputStream(u)!!.use { ins ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(1 shl 16)
                while (true) {
                    val n = ins.read(buf)
                    if (n <= 0) break
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            }
            save = SaveFile(bytes)
            uri = u
            backupOriginal(bytes)
            refresh()
        } catch (e: Exception) {
            toast(getString(R.string.cannot_open, e.message ?: ""))
        }
    }

    /** Copy of the original file into the app sandbox, in case a rollback is needed. */
    private fun backupOriginal(bytes: ByteArray) {
        try {
            val dir = getExternalFilesDir(null) ?: return
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            java.io.File(dir, "SaveFile0.$stamp.bak").writeBytes(bytes)
        } catch (e: Exception) {
        }
    }

    private fun saveBack() {
        val u = uri ?: return toast(getString(R.string.open_first))
        writeTo(u)
    }

    private fun writeTo(u: Uri) {
        val sf = save ?: return toast(getString(R.string.open_first))
        try {
            val data = sf.build()
            contentResolver.openOutputStream(u, "wt")!!.use { it.write(data) }
            toast(getString(R.string.written, data.size))
        } catch (e: Exception) {
            toast(getString(R.string.cannot_write, e.message ?: ""))
        }
    }

    // ---------------- list ----------------

    private fun refresh() {
        val sf = save ?: return
        val items = sf.items()
        rows = items.keys.sortedWith(compareBy({ CAT_ORDER.indexOf(catOf(it)) }, { it }))
        val lines = rows.map { id ->
            String.format("%-11s %9d  %s", id.toString(), items[id] ?: 0, nameOf(id))
        }
        list.adapter = object : ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1, lines
        ) {
            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                val v = super.getView(pos, cv, parent) as TextView
                v.setTypeface(Typeface.MONOSPACE)
                v.textSize = 13f
                v.setTextColor(Color.DKGRAY)
                return v
            }
        }
        val when_ = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
            .format(Date(sf.savedAt * 1000))
        header.text = getString(R.string.title) + "\n\n" +
                getString(R.string.header_info, when_, sf.mods.size, items.size)
    }

    // ---------------- editing ----------------

    private fun editDialog(id: Int) {
        val sf = save ?: return
        val cur = sf.items()[id] ?: return
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(cur.toString())
        input.gravity = Gravity.CENTER
        AlertDialog.Builder(this)
            .setTitle("$id — ${nameOf(id)}  [${catLabel(catOf(id))}]")
            .setMessage(getString(R.string.currently, cur))
            .setView(input)
            .setPositiveButton(R.string.apply) { _, _ ->
                val v = input.text.toString().toIntOrNull() ?: return@setPositiveButton
                sf.setItems { i, _ -> if (i == id) v else null }
                refresh()
            }
            .setNeutralButton(R.string.delete) { _, _ ->
                sf.removeItems(setOf(id))
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun bulk(catKey: String, value: Int) {
        val sf = save ?: return toast(getString(R.string.open_first))
        val n = sf.setItems { id, _ ->
            if (catOf(id) == catKey && id !in 10000..10001) value else null
        }
        refresh()
        toast(getString(R.string.entries_changed, n))
    }

    private fun addDialog() {
        val sf = save ?: return toast(getString(R.string.open_first))
        val input = EditText(this)
        input.hint = getString(R.string.add_hint)
        AlertDialog.Builder(this)
            .setTitle(R.string.add_title)
            .setMessage(R.string.add_message)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val plan = HashMap<Int, Int>()
                for (tok in input.text.toString().trim().split(Regex("\\s+"))) {
                    if (tok.isEmpty()) continue
                    val parts = tok.split("=")
                    val id = parts[0].toIntOrNull() ?: continue
                    plan[id] = parts.getOrNull(1)?.toIntOrNull() ?: 500
                }
                if (plan.isEmpty()) return@setPositiveButton
                val added = sf.addItems(plan)
                refresh()
                toast(getString(R.string.added, added.size))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
