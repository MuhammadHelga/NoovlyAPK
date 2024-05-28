package com.example.modul9

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var etEmail: EditText? = null
    private var etPass: EditText? = null
    private var btnMasuk: Button? = null
    private var btnDaftar: Button? = null
    private var mAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etEmail = findViewById<View>(R.id.et_email) as EditText
        etPass = findViewById<View>(R.id.et_pass) as EditText
        btnMasuk = findViewById<View>(R.id.btn_masuk) as Button
        btnDaftar = findViewById<View>(R.id.btn_daftar) as Button
        mAuth = FirebaseAuth.getInstance()
        btnMasuk!!.setOnClickListener(this)
        btnDaftar!!.setOnClickListener(this)
    }
    override fun onStart() {
        super.onStart()
        val currentUser = mAuth!!.currentUser
        updateUI(currentUser)
    }
    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_masuk -> login(etEmail!!.text.toString(),
                etPass!!.text.toString())
            R.id.btn_daftar -> signUp(etEmail!!.text.toString(),
                etPass!!.text.toString())
        }
    }
    fun signUp(email: String?, password: String?) {
        if (!validateForm()) {
            return
        }
        mAuth!!.createUserWithEmailAndPassword(email!!, password!!)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
// Sign in success, update UI with the signed-in user's information

                    Log.d(ContentValues.TAG, "createUserWithEmail:success")
                    val user = mAuth!!.currentUser
                    val userId = user!!.uid

                    val database = FirebaseDatabase.getInstance("https://prakpam-ef343-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    val userRef = database.getReference("users").child(userId)
                    val userData = mapOf(
                        "email" to email,
                        "uid" to userId
                    )

                    userRef.setValue(userData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@MainActivity, "User registered successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to register user", Toast.LENGTH_SHORT).show()
                        }
                    }
                    updateUI(user)
//                    Toast.makeText(this@MainActivity, user.toString(), Toast.LENGTH_SHORT).show()
                } else {
// If sign in fails, display a message to the user.
                    Log.w(ContentValues.TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this@MainActivity, task.exception.toString(), Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }
    fun login(email: String?, password: String?) {
        if (!validateForm()) {
            return
        }
        mAuth!!.signInWithEmailAndPassword(email!!, password!!)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
// Sign in success, update UI with the signed-in user's information
                    Log.d(ContentValues.TAG,
                        "signInWithEmail:success")
                    val user = mAuth!!.currentUser
                    Toast.makeText(this@MainActivity,

                        user.toString(),
                        Toast.LENGTH_SHORT).show()
                    updateUI(user)
                } else {
// If sign in fails, display a message to the user.
                    Log.w(ContentValues.TAG,
                        "signInWithEmail:failure", task.exception)
                    Toast.makeText(this@MainActivity,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }
    private fun validateForm(): Boolean {
        var result = true
        if (TextUtils.isEmpty(etEmail!!.text.toString())) {
            etEmail!!.error = "Required"
            result = false
        } else {
            etEmail!!.error = null
        }
        if (TextUtils.isEmpty(etPass!!.text.toString())) {

            etPass!!.error = "Required"
            result = false
        } else {
            etPass!!.error = null
        }
        return result
    }
    fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this@MainActivity, HomePage::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(this@MainActivity, "Log In First",
                Toast.LENGTH_SHORT).show()
        }
    }
}