package com.mranasspro.gpslivetracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.mranasspro.gpslivetracker.utils.Constants
import timber.log.Timber

class LocationService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implementend")
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val latitude = locationResult.lastLocation.latitude
            val longitude = locationResult.lastLocation.longitude
            Timber.d("LOCATION_UPDATE: ${latitude.toString().plus(',').plus(longitude)} ")
        }
    }

    private fun startLocationService() {
        val channelId = "location_notification_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        val resultIntent = Intent()
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            resultIntent,
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                else -> PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val notifBuilder = NotificationCompat.Builder(
            applicationContext,
            channelId
        )

        notifBuilder.setSmallIcon(R.mipmap.ic_launcher)
        notifBuilder.setContentTitle("Location Service")
        notifBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
        notifBuilder.setContentTitle("Running")
        notifBuilder.setContentIntent(pendingIntent)
        notifBuilder.setAutoCancel(false)
        notifBuilder.priority = NotificationCompat.PRIORITY_MAX

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val notificationChannel = NotificationChannel(
                    channelId,
                    "LOCATION SERVICE",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationChannel.description = "This channel is used by Location Service"
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 100
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "First enable LOCATION ACCESS in settings.", Toast.LENGTH_LONG)
                .show()
            return
        }
        LocationServices.getFusedLocationProviderClient(this)
            .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        startForeground(Constants.LOCATION_SERVICE_ID, notifBuilder.build())
    }

    private fun stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this)
            .removeLocationUpdates(locationCallback)
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val intentAction = intent.action
            if (intentAction != null) {
                if (intentAction == Constants.ACTION_START_LOCATION_SERVICE) {
                    startLocationService()
                } else if (intentAction == Constants.ACTION_STOP_LOCATION_SERVICE) {
                    stopLocationService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}