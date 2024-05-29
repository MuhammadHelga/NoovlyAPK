package com.example.modul9

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

class viewNovels : AppCompatActivity() {

    private var noteId: String? = null
    private lateinit var notedb: DatabaseReference
    private lateinit var viewTitle: TextView
    private lateinit var viewPenulis: TextView
    private lateinit var viewSyn: TextView
    private  lateinit var viewAva: ImageView
    private lateinit var updateNote: Button
//    private lateinit var linearUpdate: LinearLayout
    private lateinit var scroll_update: ScrollView
    private lateinit var upTitle: EditText
    private lateinit var upDesc: EditText
    private lateinit var upSyn: EditText
    private lateinit var upAva: ImageView
    private lateinit var btnUpdateNote: Button
    private lateinit var btnCloseUpdate: Button
    private var imageUri: Uri? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_notes)
        val bundle: Bundle? = intent.extras

        viewTitle = findViewById(R.id.vTittle)
        viewPenulis = findViewById(R.id.vDesc)
        viewSyn = findViewById(R.id.vSyn)
        viewAva = findViewById(R.id.vAva)
        updateNote = findViewById(R.id.btn_update)
        val keluar: Button = findViewById(R.id.btn_ext)
//        linearUpdate = findViewById(R.id.lin_upt)
        scroll_update = findViewById(R.id.scroll_update)
        upTitle = findViewById(R.id.upt_tittle)
        upDesc = findViewById(R.id.upt_desc)
        upSyn = findViewById(R.id.upt_syn)
        upAva = findViewById(R.id.upt_Ava)
        btnUpdateNote = findViewById(R.id.btUp)
        btnCloseUpdate =  findViewById(R.id.upCancel)

        val title = bundle!!.getString("title")
        val desc = bundle.getString("penulis")
        val syn = bundle.getString("sinopsis")
        val avatar = bundle.getString("avatar")
        noteId = bundle.getString("id")

        viewTitle.text = title
        viewPenulis.text = desc
        viewSyn.text = syn
        Glide.with(this).load(avatar).into(viewAva)

        notedb = FirebaseDatabase.getInstance("https://prakpam-ef343-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("Noovly").child(noteId!!)

        updateNote.setOnClickListener {
//            linearUpdate.visibility = View.VISIBLE
            scroll_update.visibility = View.VISIBLE
            viewTitle.visibility = View.GONE
            viewPenulis.visibility = View.GONE
            upTitle.setText(viewTitle.text)
            upDesc.setText(viewPenulis.text)
            upSyn.setText(viewSyn.text)
            Glide.with(this).load(avatar).into(upAva)
        }

        btnCloseUpdate.setOnClickListener {
//            linearUpdate.visibility = View.GONE
            scroll_update.visibility = View.GONE
            viewTitle.visibility = View.VISIBLE
            viewPenulis.visibility = View.VISIBLE
        }

        btnUpdateNote.setOnClickListener {
            updateNote()
        }

        upAva.setOnClickListener {
            selectimage()
        }

        keluar.setOnClickListener {
            finish()
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
            imageUri = data.data
            try {
                val inputStream = contentResolver.openInputStream(imageUri!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                upAva.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        if (requestCode == 10 && resultCode == RESULT_OK) {
            val extras = data?.extras
            val bitmap = extras?.get("data") as Bitmap
            imageUri = getImageUriFromBitmap(bitmap)
            upAva.setImageBitmap(bitmap)
        }
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    private fun updateNote() {
        val updatedTitle = upTitle.text.toString()
        val updatedDesc = upDesc.text.toString()
        val updateSyn = upSyn.text.toString()

        if (updatedTitle.isEmpty() || updatedDesc.isEmpty()) {
            Toast.makeText(this, "Judul dan Deskripsi tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val note = mapOf(
                "title" to updatedTitle,
                "penulis" to updatedDesc,
                "sinopsis" to updateSyn
            )
            notedb.updateChildren(note).await()
            imageUri?.let {
                val imageUrl = uploadImage(noteId!!)
                notedb.child("avatar").setValue(imageUrl).await()
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(this@viewNovels, "Note updated successfully", Toast.LENGTH_SHORT).show()
//                linearUpdate.visibility = View.GONE
                scroll_update.visibility = View.GONE
                viewTitle.visibility = View.VISIBLE
                viewPenulis.visibility = View.VISIBLE
                viewTitle.text = updatedTitle
                viewPenulis.text = updatedDesc
                viewSyn.text = updateSyn
                imageUri?.let {
                    Glide.with(this@viewNovels).load(it).into(viewAva)
                }
            }
        }
    }

    private suspend fun uploadImage(noteId: String): String {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("images/$noteId.jpeg")
        val bitmap = (upAva.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val uploadTask = storageRef.putBytes(data).await()
        return uploadTask.storage.downloadUrl.await().toString()
    }
}