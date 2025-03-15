package com.expapps.monitorchildapp.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import org.json.JSONObject


@SuppressLint("StaticFieldLeak")
class UserLocationUtils {


    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null

    private var mCurrentLocation: Location? = null
    private var currentLocationTask: Task<Location>? = null

    private var userLocationListener: IUserLocationListener? = null

    fun setUserLocationListener(userLocationListener: IUserLocationListener?) {
        this.userLocationListener = userLocationListener
    }

    fun requestLocation(context: Context) {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        onGenerateLocationToken(context)
    }


    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation(context: Context) {
        currentLocationTask?.addOnSuccessListener { location: Location? ->
            mCurrentLocation = location
            userLocationListener?.onReceivedUserLocation(mCurrentLocation?.latitude.toString(), mCurrentLocation?.longitude.toString())
        }

        currentLocationTask?.addOnFailureListener { obj: Exception ->
            obj.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun onGenerateLocationToken(context: Context) {
        if (currentLocationTask == null) {
            currentLocationTask = mFusedLocationProviderClient?.getCurrentLocation(
                    LocationRequest.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                )
        }
        requestCurrentLocation(context)
    }

}