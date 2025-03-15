package com.expapps.monitorchildapp.location

interface IUserLocationListener {
    fun onReceivedUserLocation(lat: String?, lng: String?)
}