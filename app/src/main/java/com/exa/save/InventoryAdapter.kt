package com.exa.save

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.exa.save.databinding.ItemInventoryBinding

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

    fun submit(newRows: List<Row>) {
        rows.clear()
        rows.addAll(newRows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemInventoryBinding.inflate(
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
        private val binding: ItemInventoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: Row) {
            binding.itemName.text = row.name
            binding.itemMeta.text = binding.root.context.getString(
                R.string.item_meta,
                row.id,
                row.category
            )
            binding.itemCount.text = row.count.toString()
            binding.root.setOnClickListener { onClick(row.id) }
        }
    }
}
