package com.route.gpstracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.route.notesappc33gsun.base.BaseActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity(),OnMapReadyCallback {

    val LOCATION_PERMISSION_REQUEST_CODE = 1000;
    val CAMERA_PERMISSION_REQUEST_CODE = 2000;
    // foreground service -> show notification
    // change this code and function names to accept any permission

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // check perimission if granted
           // if granted -> call your function
        // if not granted
        // if app should show explanation -- shouldShowRequestPermissionRationale()
        // -> request permission
        if (isLocationGranted()){
            //call accesss location function
            showUserLocation()
        }else {
            reuestLocationPermissionFromUser();
        }


        val map = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        map.getMapAsync(this) //load lel map

    }

    var googleMap:GoogleMap? =null
    var userLocation:Location?=null
    var userMarker : Marker? = null
    override fun onMapReady(map: GoogleMap?) {
        this.googleMap = map;
        changeUserLocationOnMap()
    }

    // java-document

    fun changeUserLocationOnMap(){

        if (googleMap==null)return
        if (userLocation==null)return

        val markerOptions = MarkerOptions();
        markerOptions.position(LatLng(userLocation!!.latitude,userLocation!!.longitude));
        markerOptions.title("user location")
       // markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
        if (userMarker==null)
         userMarker = googleMap?.addMarker(markerOptions)
        else {
            userMarker?.position= LatLng(userLocation!!.latitude,userLocation!!.longitude)
        }
        googleMap?.
        animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(userLocation!!.latitude,userLocation!!.longitude),12.0f))

        googleMap?.isMyLocationEnabled = true

    }

    // LocationManager -> native class
    // google client api -> battery . enable location
    //
    private fun reuestLocationPermissionFromUser() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this
                        ,Manifest.permission.ACCESS_FINE_LOCATION)){// if true
            // show explanation
            showMessage(message = "application wants to access your location because of ...",
                    posActionName = "ok",
                    posAction = DialogInterface.OnClickListener{
                        dialog, which ->
                        dialog.dismiss()

                        // request permission
                        ActivityCompat.requestPermissions(this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                LOCATION_PERMISSION_REQUEST_CODE)

                    },negActionName = "cancel",
                    negAction = DialogInterface.OnClickListener{
                        dialog, which ->
                        dialog.dismiss()
                    }
                    )

        }else {
            // request permission
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    val LOCATION_SETTINGS_DIALOGE_REQUEST=200

    val locationRequest = LocationRequest.create().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    // google maps

    @SuppressLint("MissingPermission")
    fun showUserLocation(){
      //  Toast.makeText(this,"showing user location",Toast.LENGTH_LONG).show()


        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

        val result = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
        result.addOnCompleteListener {

            try {
                val response = it.getResult(ApiException::class.java);
                // All location settings are satisfied. The client can initialize location
                // requests here.
                getLocationFromClientApi()
            } catch ( exception:ApiException) {
                when (exception.getStatusCode()) {
                     LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                         // Location settings are not satisfied. But could be fixed by showing the
                         // user a dialog.
                         try {
                             // Cast to a resolvable exception.
                             val resolvable = exception as ResolvableApiException;
                             // Show the dialog by calling startResolutionForResult(),
                             // and check the result in onActivityResult().
                             resolvable.startResolutionForResult(
                                     this@MainActivity,
                                     LOCATION_SETTINGS_DIALOGE_REQUEST);
                         } catch (e: IntentSender.SendIntentException) {
                             // Ignore the error.
                         } catch (e: ClassCastException) {
                             // Ignore, should be an impossible error.
                         }
                     }
                     LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE->{
                    // Location settings are not satisfied. However, we have no way to fix the
                    // settings so we won't show the dialog.
                     }
                }
            }
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val  states = LocationSettingsStates.fromIntent(intent);
        when (requestCode) {
             LOCATION_SETTINGS_DIALOGE_REQUEST->
            if  (resultCode == Activity.RESULT_OK) {
                // All required changes were successfully made
                getLocationFromClientApi();

                }else if (Activity.RESULT_CANCELED == resultCode){
                // The user was asked to change settings, but chose not to
                Toast.makeText(this,"can't access your location",Toast.LENGTH_LONG).show()
            }
        }
    }

    val locationCallback = object :LocationCallback(){
        override fun onLocationResult(result: LocationResult?) {

            if (result==null)return

            for (location in result.locations){
                // Update UI with location data
                // ...
                Log.e("location",""+
                        location.latitude+" "+location.longitude)
                userLocation  =location
                changeUserLocationOnMap()

            }

        }

        override fun onLocationAvailability(p0: LocationAvailability?) {
            super.onLocationAvailability(p0)
        }
    };
    @SuppressLint("MissingPermission")
    fun getLocationFromClientApi(){


        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper())


//        fusedLocationClient.lastLocation
//                .addOnSuccessListener { location : Location? ->
//                    // Got last known location. In some rare situations this can be null.
//                    text.setText("lat "+location?.latitude+" long "+location?.longitude)
//                }

    }
    fun isLocationGranted():Boolean{
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {

        if (grantResults.size > 0 &&grantResults[0]==PackageManager.PERMISSION_GRANTED){

            if (requestCode == LOCATION_PERMISSION_REQUEST_CODE){ // result of location
                // call function
                showUserLocation()
            }

        }else {
            // no permission result
            Toast.makeText(this,"user denied permission",Toast.LENGTH_LONG).show()
        }


    }

}