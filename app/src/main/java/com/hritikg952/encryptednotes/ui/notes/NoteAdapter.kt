package com.hritikg952.encryptednotes.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hritikg952.encryptednotes.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DecryptedNote(
    val id: Long,
    val title: String,
    val bodyPreview: String,
    val updatedAt: Long
)

class NoteAdapter(
    private val onClick: (DecryptedNote) -> Unit,
    private val onLongClick: (DecryptedNote) -> Unit
) : ListAdapter<DecryptedNote, NoteAdapter.ViewHolder>(DIFF) {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: DecryptedNote) {
            binding.tvNoteTitle.text = note.title
            binding.tvNotePreview.text = note.bodyPreview.ifBlank { "—" }
            binding.tvNoteDate.text = dateFormat.format(Date(note.updatedAt))
            binding.root.setOnClickListener { onClick(note) }
            binding.root.setOnLongClickListener { onLongClick(note); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DecryptedNote>() {
            override fun areItemsTheSame(a: DecryptedNote, b: DecryptedNote) = a.id == b.id
            override fun areContentsTheSame(a: DecryptedNote, b: DecryptedNote) = a == b
        }
    }
}
