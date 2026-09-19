package com.exa.save

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.exa.save.databinding.ItemAddCatalogBinding

class AddItemCatalogAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<AddItemCatalogAdapter.Holder>() {

    data class Row(
        val id: Int,
        val name: String,
        val alternateName: String,
        val category: String,
        val description: String,
        val protectedFromBulk: Boolean
    )

    private val rows = ArrayList<Row>()

    init {
        setHasStableIds(true)
    }

    fun submit(newRows: List<Row>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long = rows[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemAddCatalogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(rows[position])
    }

    override fun getItemCount(): Int = rows.size

    inner class Holder(
        private val binding: ItemAddCatalogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: Row) {
            val context = binding.root.context
            binding.itemName.text = row.name
            binding.itemAlias.text = row.alternateName
            binding.itemAlias.visibility = if (row.alternateName.isBlank()) View.GONE else View.VISIBLE
            binding.itemMeta.text = context.getString(R.string.item_meta, row.id, row.category)
            binding.itemDescription.text = row.description
            binding.itemProtectionIcon.visibility = if (row.protectedFromBulk) View.VISIBLE else View.GONE

            val iconRes = context.resources.getIdentifier(
                "item_${row.id}",
                "drawable",
                context.packageName
            )
            binding.itemIcon.setImageResource(
                if (iconRes != 0) iconRes else R.drawable.ic_item_placeholder
            )

            binding.root.setOnClickListener { onClick(row.id) }
        }
    }
}
