package com.expapps.monitorchildapp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.expapps.monitorchildapp.models.CallLogEntry
import com.expapps.monitorchildapp.models.Contact
import com.expapps.monitorchildapp.models.Locations
import com.expapps.monitorchildapp.models.Message
import com.expapps.monitorchildapp.models.RegisteredTokens
import com.expapps.monitorchildapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseSource {

    private var firebaseAuth = FirebaseAuth.getInstance()
    private var firebaseDatabase = FirebaseDatabase.getInstance()
    private var databaseReference = firebaseDatabase.reference

    fun getUserList(): LiveData<ArrayList<User>?> {
        val users = MutableLiveData<ArrayList<User>?>()
        databaseReference.child("Users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                val userArray = ArrayList<User>()
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach {
                        val user = it.getValue(User::class.java)
                        user?.let { u ->
                            userArray.add(u)
                        }
                    }
                    users.value = userArray
                }

                override fun onCancelled(error: DatabaseError) {
                    users.value = null
                }
            })
        return users
    }

    fun addUser(user: User): LiveData<Boolean?> {
        val isSuccess = MutableLiveData<Boolean>()
        databaseReference.child("Users")
            .child(user.userId ?: "")
            .setValue(user)
            .addOnSuccessListener {
                addMCode(user.email ?: "", Utils.getMCodeFromUserId(user.userId ?: ""), user.userId ?: "")
                isSuccess.value = true
            }
            .addOnFailureListener {
                isSuccess.value = false
            }
        return isSuccess
    }

    fun addMCode(email: String, code: String, uid: String) {
        databaseReference.child("UsersMCode")
            .updateChildren(
                hashMapOf<String, Any>(
                    Pair(
                        Utils.getEmailFromEmailId(email), uid
                    )
                )
            )
            .addOnSuccessListener {}
            .addOnFailureListener {}
    }

    fun getMCodeByEmail(email: String): LiveData<String?> {
        val isSuccess = MutableLiveData<String>()
        val emailStr = Utils.getEmailFromEmailId(email)
        databaseReference.child("UsersMCode/$emailStr")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val child = snapshot.value.toString()
                        isSuccess.value = Utils.getMCodeFromUserId(child)
                    } else {
                        isSuccess.value = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    isSuccess.value = null
                }
            })
        return isSuccess
    }

    fun addContacts(contact: List<Contact>) {
        databaseReference.child("Users/${firebaseAuth.currentUser?.uid}/data/contacts")
            .setValue(contact)
    }

    fun addCallLogs(callLogEntry: List<CallLogEntry>) {
        databaseReference.child("Users/${firebaseAuth.currentUser?.uid}/data/callLogs")
            .setValue(callLogEntry)
    }
    fun addMessages(messages: List<Message>) {
        databaseReference.child("Users/${firebaseAuth.currentUser?.uid}/data/messages")
            .setValue(messages)
    }
    fun addCurrentLocation(locations: Locations) {
        databaseReference.child("Users/${firebaseAuth.currentUser?.uid}/data/locations")
            .setValue(locations)
    }

    fun getRegisteredTokensForCurrentUser(uid: String): LiveData<ArrayList<RegisteredTokens>?> {
        val _registeredTokens = MutableLiveData<ArrayList<RegisteredTokens>?>()
        databaseReference.child("RegisteredParents/$uid")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val registeredParents = ArrayList<RegisteredTokens>()
                    if (snapshot.exists()) {
                        snapshot.children.forEach {
                            val parent = it.getValue(RegisteredTokens::class.java)
                            if (parent != null) {
                                registeredParents.add(parent)
                            }
                        }
                    }
                    _registeredTokens.value = registeredParents
                }

                override fun onCancelled(error: DatabaseError) {
                    _registeredTokens.value = null
                }
            })
        return _registeredTokens
    }

    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}