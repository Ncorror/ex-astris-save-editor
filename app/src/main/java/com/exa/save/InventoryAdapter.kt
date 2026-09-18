package com.exa.save

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.exa.save.databinding.ItemInventoryBinding
import java.text.NumberFormat

class InventoryAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.Holder>() {

    data class Row(
        val id: Int,
        val name: String,
        val category: String,
        val count: Int
    )

    private val rows = ArrayList<Row>()
    private var showIds = true
    private var largeIcons = false

    init {
        setHasStableIds(true)
    }

    fun submit(newRows: List<Row>, showIds: Boolean, largeIcons: Boolean) {
        rows.clear()
        rows.addAll(newRows)
        this.showIds = showIds
        this.largeIcons = largeIcons
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long = rows[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(rows[position], showIds, largeIcons)
    }

    override fun getItemCount(): Int = rows.size

    inner class Holder(private val binding: ItemInventoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(row: Row, showIds: Boolean, largeIcons: Boolean) {
            val context = binding.root.context
            binding.itemName.text = row.name
            binding.itemMeta.text = if (showIds) {
                context.getString(R.string.item_meta, row.id, row.category)
            } else {
                row.category
            }
            binding.itemCount.text = NumberFormat.getIntegerInstance().format(row.count)

            val iconRes = context.resources.getIdentifier(
                "item_${row.id}",
                "drawable",
                context.packageName
            )
            binding.itemIcon.setImageResource(if (iconRes != 0) iconRes else R.drawable.ic_item_placeholder)

            val sizeDp = if (largeIcons) 64 else 52
            val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
            binding.itemIconContainer.updateLayoutParams {
                width = sizePx
                height = sizePx
            }

            binding.root.setOnClickListener { onClick(row.id) }
        }
    }
}
