package com.expapps.monitorchildapp

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.FragmentActivity

object Utils {
    fun checkEmptyOrNullString(vararg strings: String?): Boolean {
        if (strings.contains(null) || strings.contains("null") || strings.contains("")) {
            return false
        }
        return true
    }

    fun checkPasswordLength(str: String, length: Int = 6): Boolean {
        if (str.length > length) {
            return true
        }
        return false
    }

    fun isAllStringsEqual(vararg strings: String?): Boolean {
        var str1 = ""
        if (strings.isNotEmpty()) {
            str1 = strings[0] ?: ""
        }
        strings.forEach {
            if (str1 != it) {
                return false
            }
        }
        return true
    }

    fun getMCodeFromUserId(userId: String): String {
        if (checkEmptyOrNullString(userId)) {
            val uidLen = userId.length
            if (uidLen > 6) {
                return userId.substring(uidLen - 6, uidLen)
            }
        }
        return ""
    }

    fun getEmailFromEmailId(str: String): String {
        val emailId = str.split("@")
        if (emailId.isNotEmpty()) {
            return emailId[0]
        }
        return ""
    }

    fun Context?.showToast(message: String?, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, length).show()
    }

    fun FragmentActivity?.openActivity(clazz: Class<*>, finishPrev: Boolean = false) {
        val intent = Intent(this, clazz)
        this?.startActivity(intent)
        if (finishPrev) {
            this?.finish()
        }
    }
}