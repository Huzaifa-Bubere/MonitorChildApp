package com.expapps.monitorchildapp.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import androidx.core.app.NotificationCompat
import com.expapps.monitorchildapp.FirebaseSource
import com.expapps.monitorchildapp.MainActivity
import com.expapps.monitorchildapp.R
import com.expapps.monitorchildapp.calllogs.CallLogRetriever
import com.expapps.monitorchildapp.contacts.ContactObserver
import com.expapps.monitorchildapp.location.IUserLocationListener
import com.expapps.monitorchildapp.location.UserLocationUtils
import com.expapps.monitorchildapp.messages.MessageRetriever
import com.expapps.monitorchildapp.models.Contact
import com.expapps.monitorchildapp.models.Locations
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timerTask


class CollectorService : Service() {
    private val CHANNEL_ID = "Main_Channel"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var contactObserver: ContactObserver? = null
    var timer: Timer? = null
    var isTimerRunning = false
    private var messageRetriever: MessageRetriever? = null
    private var callLogRetriever: CallLogRetriever? = null

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationCallback: LocationCallback? = null
    private val binder = LocalBinder()
    private var firebaseSource: FirebaseSource? = null

    inner class LocalBinder : Binder() {
        fun getService(): CollectorService = this@CollectorService
    }

    override fun onCreate() {
        super.onCreate()
        setLocationListener()
//        contactObserver = ContactObserver(this, Handler())
//        contentResolver.registerContentObserver(
//            ContactsContract.Contacts.CONTENT_URI,
//            true,
//            contactObserver!!
//        )
    }

    private fun setLocationListener() {
        firebaseSource = FirebaseSource()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // Update interval in milliseconds (10 seconds)
            fastestInterval = 5000 // Fastest update interval in milliseconds (5 seconds)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // Handle location updates
                    val latitude = location.latitude
                    val longitude = location.longitude
                    firebaseSource?.addCurrentLocation(Locations(latitude, longitude))
                    Log.d("LocationUpdateService", "Updated Latitude: $latitude, Longitude: $longitude")
                }
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        messageRetriever = MessageRetriever(this@CollectorService)
        callLogRetriever = CallLogRetriever(this@CollectorService)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Child Monitoring")
            .setContentText("Data collector is running in background")
            .setSmallIcon(R.drawable.ic_hat_magnifier)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        startTimer()
        startLocationUpdates()

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    private fun startLocationUpdates() {
        try {
            if (locationCallback != null && locationRequest != null) {
                fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }
        } catch (securityException: SecurityException) {
            Log.e("LocationUpdateService", "Location permission not granted")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Collector Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun fetchAllContacts() {
        coroutineScope.launch {
            val contacts = getAllContacts()
            Log.d("TAG", "fetchAllContacts: $contacts")
            firebaseSource?.addContacts(contacts)
        }
        coroutineScope.launch {
            val messages = messageRetriever?.retrieveMessages()
            Log.d("TAG", "fetchAllContacts: $messages")
            messages?.let { firebaseSource?.addMessages(it) }
        }
        coroutineScope.launch {
            val callLogs = callLogRetriever?.retrieveCallLogs()
            Log.d("TAG", "fetchAllContacts: $callLogs")
            callLogs?.let { firebaseSource?.addCallLogs(it) }
        }
        CoroutineScope(Dispatchers.Main).launch {
            //setLocationListener()
            startLocationUpdates()
        }
    }

    private fun getAllContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = contentResolver

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        if (cursor != null && cursor.count > 0) {
            val idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val contactId = cursor.getString(idColumnIndex)
                val name = cursor.getString(nameColumnIndex)
                val phoneNumbers = getContactPhoneNumbers(contactId)
                contacts.add(Contact(contactId, name, phoneNumbers))
            }
        }

        cursor?.close()
        return contacts
    }

    private fun getContactPhoneNumbers(contactId: String): List<String> {
        val phoneNumbers = mutableListOf<String>()
        val phoneCursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        if (phoneCursor != null && phoneCursor.count > 0) {
            val phoneNumberColumnIndex =
                phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (phoneCursor.moveToNext()) {
                val phoneNumber = phoneCursor.getString(phoneNumberColumnIndex)
                phoneNumbers.add(phoneNumber)
            }
        }

        phoneCursor?.close()
        return phoneNumbers
    }

    private fun startTimer() {
        if (!isTimerRunning) {
            timer = Timer()
            timer?.schedule(
                timerTask {
                      fetchAllContacts()
                }, 0, 60 * 1000
            )
            isTimerRunning = true
        }
    }

    private fun stopTimer() {
        if (isTimerRunning) {
            timer?.cancel()
            isTimerRunning = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        stopLocationUpdates()
//        contactObserver?.let { contentResolver.unregisterContentObserver(it) }
    }
}