package com.mranasspro.gpslivetracker

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mranasspro.gpslivetracker.utils.Constants
import com.mranasspro.gpslivetracker.utils.Constants.REQUEST_CODE_LOCATION_PERMISSION_CODE
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        activateGps()

        val buttonStartService = findViewById<Button>(R.id.buttonStartService)
        val buttonStopService = findViewById<Button>(R.id.buttonStopService)

        buttonStartService.setOnClickListener {
            activateGps()
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    REQUEST_CODE_LOCATION_PERMISSION_CODE
                )
            } else launchLocationService()
        }

        buttonStopService.setOnClickListener {
            killLocationService()
        }
    }

    private fun activateGps() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Timber.d("isGpsEnabled $isGpsEnabled")
        if (!isGpsEnabled) {
            val gpsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(gpsIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "permission granted", Toast.LENGTH_LONG).show()
                launchLocationService()
            } else Toast.makeText(this, "Permission denied!!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isLocationServiceRunning(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (runningServiceInfo: ActivityManager.RunningServiceInfo in activityManager.getRunningServices(
            Int.MAX_VALUE
        )) {
            if (LocationService::class.java.name.equals(runningServiceInfo.service.className)) {
                if (runningServiceInfo.foreground) return true
            }
        }
        return false
    }

    private fun launchLocationService() {
        if (!isLocationServiceRunning()) {
            val serviceIntent = Intent(this, LocationService::class.java)
            serviceIntent.action = Constants.ACTION_START_LOCATION_SERVICE
            startService(serviceIntent)
            Toast.makeText(this, "Location Service Launched", Toast.LENGTH_SHORT).show()
        }
    }

    private fun killLocationService() {
        if (isLocationServiceRunning()) {
            val serviceIntent = Intent(this, LocationService::class.java)
            serviceIntent.action = Constants.ACTION_STOP_LOCATION_SERVICE
            startService(serviceIntent)
            Toast.makeText(this, "Location Service Killed", Toast.LENGTH_SHORT).show()
        }
    }
}