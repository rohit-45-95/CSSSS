package com.craftstudio.launcher.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.resource.ResourceItem

class ManageResourceAdapter(
    private var items: List<ResourceItem>,
    private val onToggle: (ResourceItem, Boolean) -> Unit,
    private val onDelete: (ResourceItem) -> Unit
) : RecyclerView.Adapter<ManageResourceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvSize: TextView = view.findViewById(R.id.tv_size)
        val switchEnable: SwitchCompat = view.findViewById(R.id.switch_enable)
        val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_resource, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.displayName
        holder.tvSize.text = "${item.sizeKb} KB"
        holder.switchEnable.setOnCheckedChangeListener(null)
        holder.switchEnable.isChecked = item.isEnabled

        holder.switchEnable.setOnCheckedChangeListener { _, isChecked ->
            onToggle(item, isChecked)
        }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<ResourceItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
