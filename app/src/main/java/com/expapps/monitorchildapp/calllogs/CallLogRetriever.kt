package com.expapps.monitorchildapp.calllogs// CallLogRetriever.kt
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.util.Log
import com.expapps.monitorchildapp.models.CallLogEntry

class CallLogRetriever(private val context: Context) {

    fun retrieveCallLogs(): List<CallLogEntry> {
        val contentResolver: ContentResolver = context.contentResolver

        // Specify the columns you want to retrieve
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        // Query the CallLog content provider
        val cursor: Cursor? = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"  // Sort by date in descending order (recent first)
        )

        // Process the cursor and convert call logs to CallLogEntry objects
        val callLogs = mutableListOf<CallLogEntry>()
        cursor?.use {
            val idIndex = it.getColumnIndex(CallLog.Calls._ID)
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

            while (it.moveToNext()) {
                val callId = it.getString(idIndex)
                val phoneNumber = it.getString(numberIndex)
                val callType = it.getInt(typeIndex)
                val callDate = it.getLong(dateIndex)
                val callDuration = it.getLong(durationIndex)

                val callLogEntry = CallLogEntry(callId, phoneNumber, callType, callDate, callDuration)
                callLogs.add(callLogEntry)
            }
        }

        // Log the retrieved call logs for debugging
        Log.d("CallLogRetriever", "Retrieved call logs: $callLogs")

        // Return the list of call logs
        return callLogs
    }
}
