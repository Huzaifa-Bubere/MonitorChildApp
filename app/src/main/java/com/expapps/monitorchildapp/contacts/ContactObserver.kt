package com.expapps.monitorchildapp.contacts

import android.content.Context
import android.database.ContentObserver
import android.os.Handler

class ContactObserver(context: Context, handler: Handler) : ContentObserver(handler) {

    private val applicationContext: Context = context.applicationContext

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)

        // Handle the contact change, for example, by sending a broadcast
        applicationContext.sendBroadcast(ContactBroadcastReceiver.getContactAddedIntent())
    }
}