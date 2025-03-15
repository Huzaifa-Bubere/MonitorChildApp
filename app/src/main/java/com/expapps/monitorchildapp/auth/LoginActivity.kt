package com.expapps.monitorchildapp.auth

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.expapps.monitorchildapp.MainActivity
import com.expapps.monitorchildapp.databinding.ActivityLoginBinding
import com.expapps.monitorchildapp.models.User
import com.expapps.monitorchildapp.Utils
import com.expapps.monitorchildapp.Utils.openActivity
import com.expapps.monitorchildapp.Utils.showToast
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setLoginClick()
        setNewUserClick()
    }


    private fun init() {
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setTitle("CMS")
        progressDialog.setMessage("Registering your child...")
    }

    private fun setLoginClick() {
        binding.registerBtn.setOnClickListener {
            val email = binding.emailEt.editText?.text.toString().trim()
            val password = binding.passwordEt.editText?.text.toString().trim()

            if (!Utils.checkEmptyOrNullString(email, password)) {
                showToast("Error: Please fill proper data")
                return@setOnClickListener
            }

            if (!Utils.checkPasswordLength(password)) {
                showToast("Error: Password should be more than 6 letters")
            }

            val user = User(
                email = email,
                password = password
            )

            login(user)
        }
    }

    private fun setNewUserClick() {
        binding.newUserBtn.setOnClickListener {
            openActivity(RegisterActivity::class.java, finishPrev = true)
        }
    }

    private fun login(user: User) {
        showProgress()
        firebaseAuth.signInWithEmailAndPassword(user.email ?: "", user.password ?: "")
            .addOnSuccessListener {
                dismissProgress()
                showToast("Login successful")
                openActivity(MainActivity::class.java, finishPrev = true)
            }
            .addOnFailureListener {
                dismissProgress()
                showToast("Login failed, please check credentials")
            }
    }


    private fun showProgress() {
        progressDialog.show()
    }

    private fun dismissProgress() {
        progressDialog.dismiss()
    }

}