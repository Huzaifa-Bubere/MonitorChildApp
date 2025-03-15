package com.expapps.monitorchildapp.contacts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ContactBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // Handle the broadcast event, for example, by updating UI or performing some action
        // TODO : Upload the added contact
    }

    companion object {
        const val ACTION_CONTACT_ADDED = "com.yourapp.action.CONTACT_ADDED"

        fun getContactAddedIntent(): Intent {
            return Intent(ACTION_CONTACT_ADDED)
        }
    }
}