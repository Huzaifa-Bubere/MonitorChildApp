package com.expapps.monitorchildapp.auth

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.expapps.monitorchildapp.MainActivity
import com.expapps.monitorchildapp.databinding.ActivityRegisterBinding
import com.expapps.monitorchildapp.models.User
import com.expapps.monitorchildapp.FirebaseSource
import com.expapps.monitorchildapp.Utils
import com.expapps.monitorchildapp.Utils.openActivity
import com.expapps.monitorchildapp.Utils.showToast
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var firebaseSource: FirebaseSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        setRegisterClick()
        binding.newUserBtn.setOnClickListener {
            openActivity(LoginActivity::class.java, finishPrev = true)
        }
    }

    private fun init() {
        firebaseSource = FirebaseSource()
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setTitle("CMS")
        progressDialog.setMessage("Logging in...")
    }

    private fun setRegisterClick() {
        binding.registerBtn.setOnClickListener {
            val email = binding.emailEt.editText?.text.toString().trim()
            val password = binding.passwordEt.editText?.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEt.editText?.text.toString().trim()

            if (!Utils.checkEmptyOrNullString(email, password, confirmPassword)) {
                showToast("Error: Please fill proper data")
                return@setOnClickListener
            }

            if (!Utils.checkPasswordLength(password)) {
                showToast("Error: Password should be more than 6 letters")
                return@setOnClickListener
            }

            if (!Utils.isAllStringsEqual(password, confirmPassword)) {
                showToast("Error: Password must be same")
                return@setOnClickListener
            }

            val user = User(
                email = email,
                password = password
            )

            checkIfUserExist(user)
        }
    }


    private fun register(user: User) {
        firebaseAuth.createUserWithEmailAndPassword(user.email ?: "", user.password ?: "")
            .addOnSuccessListener {
                dismissProgress()
                user.userId = it.user?.uid
                user.mcode = Utils.getMCodeFromUserId(user.userId ?: "")
                addUser(user)
            }
            .addOnFailureListener {
                dismissProgress()
                showToast("Registration error")
            }
    }

    private fun checkIfUserExist(user: User) {
        showProgress()
        firebaseAuth.fetchSignInMethodsForEmail(user.email ?: "")
            .addOnSuccessListener {
                if (!it.signInMethods.isNullOrEmpty()) {
                    dismissProgress()
                    showToast("Error: User already exist")
                } else {
                    register(user)
                }
            }
    }

    private fun addUser(user: User) {
        showProgress()
        firebaseSource.addUser(user).observe(this) {
            if (it == true) {
                showToast("User Registered")
                openActivity(MainActivity::class.java, finishPrev = true)
            } else {
                showToast("Unknown error occurred")
            }
        }
    }

    private fun showProgress() {
        progressDialog.show()
    }

    private fun dismissProgress() {
        progressDialog.dismiss()
    }

}