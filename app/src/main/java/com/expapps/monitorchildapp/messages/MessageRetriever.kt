package com.expapps.monitorchildapp.messages// MessageRetriever.kt
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.expapps.monitorchildapp.models.Message

class MessageRetriever(private val context: Context) {

    fun retrieveMessages(): List<Message> {
        val uri: Uri = Uri.parse("content://sms")
        val contentResolver: ContentResolver = context.contentResolver

        // Specify the columns you want to retrieve
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        // Query the SMS content provider
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        // Process the cursor and convert messages to Message objects
        val messages = mutableListOf<Message>()
        cursor?.use {
            val idIndex = it.getColumnIndex(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val messageId = it.getString(idIndex)
                val sender = it.getString(addressIndex)
                val body = it.getString(bodyIndex)
                val timestamp = it.getLong(dateIndex)

                val message = Message(messageId, sender, body, timestamp)
                messages.add(message)
            }
        }

        // Return the list of messages
        return messages
    }
}
