package com.learning.learningroomdatabase.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.learning.learningroomdatabase.R
import com.learning.learningroomdatabase.data.local.entity.AuditEntity

class AuditAdapter(
    private val onDeleteClicked: (AuditEntity) -> Unit
) : ListAdapter<AuditEntity, AuditAdapter.ViewHolder>(AuditDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClicked)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNama: TextView = itemView.findViewById(R.id.tv_item_nama)
        private val tvLokasiTemuan: TextView = itemView.findViewById(R.id.tv_item_lokasi_temuan)
        private val tvLokasidokumentasi: TextView = itemView.findViewById(R.id.tv_item_lokasi_dokumentasi)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_item_status)
        private val deleteButton: Button = itemView.findViewById(R.id.btn_delete_item)

        private fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        }

        fun bind(audit: AuditEntity, onDeleteClicked: (AuditEntity) -> Unit) {
            tvNama.text = audit.nama_petugas
            tvLokasiTemuan.text = audit.lokasi_temuan
            tvLokasidokumentasi.text = audit.lokasi

            tvStatus.text = itemView.context.getString(R.string.status_format, audit.status_prioritasi)

            deleteButton.setOnClickListener {
                onDeleteClicked(audit)
            }
        }
    }

    class AuditDiffCallback : DiffUtil.ItemCallback<AuditEntity>() {
        override fun areItemsTheSame(oldItem: AuditEntity, newItem: AuditEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AuditEntity, newItem: AuditEntity): Boolean {
            return oldItem == newItem
        }
    }
}
