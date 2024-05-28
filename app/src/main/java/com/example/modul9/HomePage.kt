package com.example.modul9

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream
import java.io.IOException

class HomePage : AppCompatActivity(), View.OnClickListener {

    private var tvEmail: TextView? = null
    private lateinit var btnKeluar: ImageButton
    private lateinit var addNovel: ImageButton
    private var mAuth: FirebaseAuth? = null
    private var etTitle: EditText? = null
    private var etDesc: EditText? = null
    private var etSyn: EditText? = null
    private lateinit  var ava: ImageView
    private lateinit  var darkness: ImageView
    private lateinit  var popup: LinearLayout
    private lateinit var btnSubmit: Button
    private lateinit var btnBatal: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var notedb: DatabaseReference
    private var noovlyList: MutableList<Noovly> = mutableListOf()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insert_note)

        tvEmail = findViewById(R.id.tv_email)
        btnKeluar = findViewById(R.id.btn_keluar)
        addNovel = findViewById(R.id.add_novel)
        darkness = findViewById(R.id.darkness)
        popup = findViewById(R.id.popup)
        mAuth = FirebaseAuth.getInstance()
        btnKeluar.setOnClickListener(this)
        etTitle = findViewById(R.id.add_judul)
        etDesc = findViewById(R.id.add_penulis)
        etSyn = findViewById(R.id.add_sinopsis)
        btnSubmit = findViewById(R.id.btn_submit)
        btnBatal = findViewById(R.id.btn_batal)
        ava = findViewById(R.id.add_cover)
        btnSubmit.setOnClickListener(this)

        recyclerView = findViewById(R.id.rv)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        noteAdapter = NoteAdapter(this, noovlyList)
        recyclerView.adapter = noteAdapter

        addNovel.setOnClickListener{
            popup.visibility = View.VISIBLE
            darkness.visibility = View.VISIBLE
        }

        btnBatal.setOnClickListener {
            popup.visibility = View.GONE
            darkness.visibility = View.GONE
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser!!.uid
        notedb = FirebaseDatabase.getInstance("https://prakpam-ef343-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("Noovly")

        notedb.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                noovlyList.clear()
                for (noteSnapshot in snapshot.children) {
                    val noovly = noteSnapshot.getValue(Noovly::class.java)
                    noovly?.id = noteSnapshot.key
                    if (noovly != null) {
                        noovlyList.add(noovly)
                    }
                }
                noteAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomePage, "Failed to load notes.", Toast.LENGTH_SHORT).show()
            }
        })

        ava.setOnClickListener {
            selectimage()
        }
    }

    private fun selectimage() {
        val items = arrayOf<CharSequence>("Take Photo", "Choose from Library", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setIcon(R.mipmap.ic_launcher)
        builder.setItems(items) { dialog, item ->
            when {
                items[item] == "Take Photo" -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, 10)
                }
                items[item] == "Choose from Library" -> {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.type = "image/*"
                    startActivityForResult(Intent.createChooser(intent, "Select Image"), 20)
                }
                items[item] == "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 20 && resultCode == RESULT_OK && data != null) {
            val path: Uri? = data.data
            val thread = Thread {
                try {
                    val inputStream = contentResolver.openInputStream(path!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    ava.post {
                        ava.setImageBitmap(bitmap)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            thread.start()
        }

        if (requestCode == 10 && resultCode == RESULT_OK) {
            val extras = data?.extras
            val thread = Thread {
                val bitmap = extras?.get("data") as Bitmap
                ava.post {
                    ava.setImageBitmap(bitmap)
                }
            }
            thread.start()
        }
    }

    private fun uploadImg(noteId: String) {
        ava.isDrawingCacheEnabled = true
        ava.buildDrawingCache()
        val bitmap = (ava.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val storage = Firebase.storage
        val storageRef = storage.reference.child("images/$noteId.jpeg")
        val uploadTask = storageRef.putBytes(data)
        uploadTask.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUrl = task.result.toString()
                    saveNoteData(noteId, downloadUrl)
                } else {
                    Toast.makeText(this, "Failed to get download URL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth!!.currentUser
        if (currentUser != null) {
            tvEmail!!.text = currentUser.email
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_keluar -> logOut()
            R.id.btn_submit -> submitData()
        }
    }

    private fun saveNoteData(noteId: String, imageUrl: String) {
        val title = etTitle?.text.toString()
        val desc = etDesc?.text.toString()
        val noovly = Noovly(
            id = noteId,
            title = title,
            description = desc,
            avatar = imageUrl
        )
        notedb.child(noteId).setValue(noovly).addOnSuccessListener {
            Toast.makeText(this@HomePage, "Note added", Toast.LENGTH_SHORT).show()
            etTitle?.text?.clear()
            etDesc?.text?.clear()
            etSyn?.text?.clear()
            ava.setImageResource(R.drawable.add_cover)
        }.addOnFailureListener {
            Toast.makeText(this@HomePage, "Failed to add note", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitData() {
        if (!validateForm()) {
            return
        }
        val noteId = notedb.push().key ?: return
        uploadImg(noteId)
    }

    private fun validateForm(): Boolean {
        var result = true
        if (TextUtils.isEmpty(etTitle?.text.toString())) {
            etTitle?.error = "Required"
            result = false
        } else {
            etTitle?.error = null
        }
        if (TextUtils.isEmpty(etDesc?.text.toString())) {
            etDesc?.error = "Required"
            result = false
        } else {
            etDesc?.error = null
        }
        if (TextUtils.isEmpty(etSyn?.text.toString())) {
            etSyn?.error = "Required"
            result = false
        } else {
            etSyn?.error = null
        }
        return result
    }

    private fun logOut() {
        mAuth!!.signOut()
        val intent = Intent(this@HomePage, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}