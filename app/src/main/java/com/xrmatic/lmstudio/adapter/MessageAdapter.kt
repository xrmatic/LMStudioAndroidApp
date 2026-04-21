package com.xrmatic.lmstudio.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xrmatic.lmstudio.R
import com.xrmatic.lmstudio.model.ChatMessage
import com.xrmatic.lmstudio.model.UiMessage

class MessageAdapter : ListAdapter<UiMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_ASSISTANT = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).role == ChatMessage.ROLE_USER) VIEW_TYPE_USER else VIEW_TYPE_ASSISTANT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val view = inflater.inflate(R.layout.item_message_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_message_assistant, parent, false)
            AssistantViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(item)
            is AssistantViewHolder -> holder.bind(item)
        }
    }

    // ── ViewHolders ────────────────────────────────────────────────────────

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageContent)
        fun bind(message: UiMessage) {
            tvMessage.text = message.content
        }
    }

    class AssistantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageContent)
        fun bind(message: UiMessage) {
            tvMessage.text = message.content
            // Highlight error messages
            tvMessage.setTextColor(
                itemView.context.getColor(
                    if (message.isError) R.color.error_text else R.color.assistant_text
                )
            )
        }
    }

    // ── DiffUtil ───────────────────────────────────────────────────────────

    class DiffCallback : DiffUtil.ItemCallback<UiMessage>() {
        override fun areItemsTheSame(oldItem: UiMessage, newItem: UiMessage) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: UiMessage, newItem: UiMessage) =
            oldItem == newItem
    }
}
