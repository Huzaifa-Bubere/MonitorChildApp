package com.expapps.monitorchildapp

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.expapps.monitorchildapp.Utils.openActivity
import com.expapps.monitorchildapp.Utils.showToast
import com.expapps.monitorchildapp.auth.RegisterActivity
import com.expapps.monitorchildapp.databinding.ActivityMainBinding
import com.expapps.monitorchildapp.models.RegisteredTokens
import com.expapps.monitorchildapp.service.CollectorService
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseSource: FirebaseSource
    private lateinit var firebaseAuth: FirebaseAuth
    private var mCode = ""
    private var registeredTokens = ArrayList<RegisteredTokens>()
    val permissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.READ_CALL_LOG,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        // Add more permissions as needed
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { p ->

            val allPermissionsGranted = p.all { it.value }

            if (allPermissionsGranted) {
                // Permission granted, proceed with your logic
                startService()
                binding.permissionErrorTv.visibility = View.GONE
            } else {
                binding.permissionErrorTv.visibility = View.VISIBLE
                // Permission denied, handle accordingly (e.g., show a message or disable functionality)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        binding.copyTextLayout.setOnClickListener {
            copyToClipboard()
        }
        binding.shareMCodeLayout.setOnClickListener {
            shareMCode()
        }
        binding.notifyParentBtn.setOnClickListener {
            notifyParents()
        }
    }

    private fun notifyParents() {
        val bearerToken = "NjQwZGQ5YzItNTY4OS00NTVlLTgxZTYtOGZkYjhlNjBkYTc1"
        val appId = "b0b06515-ee7d-49a3-aa26-22b89344f74a"
        val map = HashMap<String, Any>()
        val tokens = ArrayList<String>()
        val subs = ArrayList<String>()
        registeredTokens.forEach {
            tokens.add(it.token ?: "")
            subs.add(it.subId ?: "")
        }

        map["app_id"] = appId
        map["include_subscription_ids"] = subs
//        map["include_aliases"] =
//            hashMapOf("external_id" to subs.first())

        val content = "Your child is in emergency"

        val heading = "Child emergency alert"

        map["contents"] = hashMapOf("en" to content)
        map["headings"] = hashMapOf("en" to heading)
        NetworkController.sendNotification(map, bearerToken)
    }

    override fun onStart() {
        super.onStart()
        checkLoggedInStatus()
    }

    private fun init() {
        firebaseSource = FirebaseSource()
        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun getMCode() {
        val email = Utils.getEmailFromEmailId(firebaseAuth.currentUser?.email ?: "")
        firebaseSource.getMCodeByEmail(email).observe(this) {
            if (!it.isNullOrBlank()) {
                val codeInCaps = it.toUpperCase(Locale.ROOT)
                mCode = codeInCaps
                if (codeInCaps.length == 6) {
                    binding.mCodeView.text1.text = codeInCaps[0].toString()
                    binding.mCodeView.text2.text = codeInCaps[1].toString()
                    binding.mCodeView.text3.text = codeInCaps[2].toString()
                    binding.mCodeView.text4.text = codeInCaps[3].toString()
                    binding.mCodeView.text5.text = codeInCaps[4].toString()
                    binding.mCodeView.text6.text = codeInCaps[5].toString()
                }
            }
        }
    }

    private fun copyToClipboard() {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", mCode)
        clipboard.setPrimaryClip(clip)
        showToast("Copied to clipboard")
    }

    private fun shareMCode() {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            val shareBody = "Your Child monitoring system MCode is: $mCode"
            intent.type = "text/plain"
            intent.putExtra(
                Intent.EXTRA_SUBJECT, ""
            )
            intent.putExtra(Intent.EXTRA_TEXT, shareBody)
            startActivity(Intent.createChooser(intent, "Share MCode to"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchData() {
        getMCode()
        runService()
        getRegisteredTokens()
    }

    private fun getRegisteredTokens() {
        firebaseSource.getRegisteredTokensForCurrentUser(firebaseAuth.currentUser?.uid ?: "")
            .observe(this) {
                if (!it.isNullOrEmpty()) {
                    registeredTokens = it
                }
            }
    }

    private fun checkLoggedInStatus() {
        if(!firebaseSource.isLoggedIn()) {
            openActivity(RegisterActivity::class.java, finishPrev = true)
        } else {
            fetchData()
        }
    }

    private fun startService() {
        try {
            if (!isServiceRunning(this, CollectorService::class.java)) {
                val myServiceIntent = Intent(this, CollectorService::class.java)
                ContextCompat.startForegroundService(this, myServiceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        activityManager?.let {
            // Get a list of running services
            for (service in it.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    // The service is running
                    return true
                }
            }
        }

        // The service is not running
        return false
    }

    private fun runService() {
        if (checkPermission()) {
            // Permission is already granted, proceed with your logic
            startService()
        } else {
            // Permission has not been granted, request it
            requestPermission()
        }
    }
    private fun checkPermission(): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(permissions)
    }
}