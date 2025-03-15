package com.expapps.monitorchildapp

import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException

object NetworkController {

    fun sendNotification(map: HashMap<String, Any>, bearerToken: String) {

        try {
            // map["include_subscription_ids"] = arrayListOf("f1a910fc-355d-4609-b208-1efa5f0dba30")
            map["target_channel"] = "push"

            val json = Gson().toJson(map)

            val client = OkHttpClient()
            val mediaType = "application/json".toMediaTypeOrNull()
            val body = RequestBody.create(mediaType, json)

            val request = Request.Builder()
                .url("https://onesignal.com/api/v1/notifications")
                .post(body)
                .addHeader("accept", "application/json")
                .addHeader(
                    "Authorization",
                    "Basic $bearerToken"
                )
                .addHeader("content-type", "application/json")
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {}

                override fun onFailure(call: Call, e: IOException) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}