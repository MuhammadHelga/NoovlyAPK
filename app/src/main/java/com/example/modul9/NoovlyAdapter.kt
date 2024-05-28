package com.example.modul9

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NoteAdapter(
    private val context: Context,
    private val noovlyList: MutableList<Noovly>,
    private val dataChangeListener: DataChangeListener? = null

) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val outTittle: TextView = itemView.findViewById(R.id.showJudul)
        val delNotes: ImageButton = itemView.findViewById(R.id.btn_delete)
        val avatars: ImageView = itemView.findViewById(R.id.ava)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return NoteViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val currentNote = noovlyList[position]
        holder.itemView.setOnClickListener {
            val intent = Intent(context, viewNovels::class.java)
            intent.putExtra("id", currentNote.id)
            intent.putExtra("title", currentNote.title)
            intent.putExtra("penulis", currentNote.penulis)
            intent.putExtra("sinopsis", currentNote.sinopsis)
            intent.putExtra("avatar", currentNote.avatar)
            context.startActivity(intent)
        }

        Glide.with(context).load(currentNote.avatar).into(holder.avatars)

        holder.delNotes.setOnClickListener {
            val itemPosition = holder.adapterPosition
            if (itemPosition != RecyclerView.NO_POSITION) {
                val noteToDelete = noovlyList[position]
                CoroutineScope(Dispatchers.IO).launch {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val userId = currentUser!!.uid
                    val databaseRef = FirebaseDatabase.getInstance("https://prakpam-ef343-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("Noovly")
                    databaseRef.child(noteToDelete.id!!).removeValue().await()

                    val storageRef = FirebaseStorage.getInstance().reference.child("images/${noteToDelete.id}.jpeg")
                    storageRef.delete().await()
                    withContext(Dispatchers.Main) {
                        dataChangeListener?.onDataChange()
                        notifyItemRangeChanged(position, noovlyList.size)
                    }
                }
            }
        }
        holder.outTittle.text = currentNote.title
    }


    override fun getItemCount(): Int {
        return noovlyList.size
    }
}

interface DataChangeListener {
    fun onDataChange()
}

