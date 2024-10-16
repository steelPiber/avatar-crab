// NotificationsAdapter.kt
package com.example.avatar_crab.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import piber.avatar_crab.R

data class NotificationItem(val message: String, val timestamp: String)

class NotificationsAdapter : ListAdapter<NotificationItem, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNotificationMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        private val tvNotificationTimestamp: TextView = itemView.findViewById(R.id.tvNotificationTimestamp)

        fun bind(notification: NotificationItem) {
            tvNotificationMessage.text = notification.message
            tvNotificationTimestamp.text = notification.timestamp
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
    override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
        return oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
        return oldItem == newItem
    }
}
