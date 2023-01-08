package com.example.liveweather

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.hardware.HardwareBuffer.create
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity.apply
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.example.liveweather.databinding.ActivityMainBinding
import com.example.liveweather.utils.Constants
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationRequest.create
//import com.google.android.gms.location.R
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.weatherapp.models.WeatherResponse
import com.weatherapp.network.WeatherService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI.create
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mFusedLocationClient : FusedLocationProviderClient // TO get the latitude and longitude of the client.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding  = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if(!isLocationEnabled()){
            Toast.makeText(this,
                "Your Location provider is turned off. Please turn it on",
                Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            //requestLocationData()
        } //else{
            Dexter.withContext(this)
                    .withPermissions(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ).withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report : MultiplePermissionsReport) {
                        if(report!!.areAllPermissionsGranted()){
                            requestLocationData()
                            //stopLocationUpdates()
                        }
//                        if(report.isAnyPermissionPermanentlyDenied){
//                            Toast.makeText(this@MainActivity, "You have denied location permission", Toast.LENGTH_SHORT).show()
//                        }
                    }
                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token : PermissionToken?) {
                        showRationalDialogForPermission()
                        requestLocationData()
                        //val mLocationRequest = LocationRequest()
                    }
                }).onSameThread().check();
       // }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData(){
//        val mLocationRequest = com.google.android.gms.location.LocationRequest.create().apply {
//            interval = 1000
//            fastestInterval = 5000
//            priority = Priority.PRIORITY_HIGH_ACCURACY
//        }
        val mLocationRequest = LocationRequest()
        //mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }
    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            val latitude = mLastLocation?.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation?.longitude
            Log.i("Current Longitude", "$longitude")
        /////
            getLocationWeatherDetails(latitude!!, longitude!!)
        }
    }
    private fun showRationalDialogForPermission() {
        AlertDialog.Builder(this@MainActivity)
            .setMessage("It looks like you have turned off the permission required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS")
            {
                    _,_->
                try{
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null) //Opening setting for the particular app feature.
                    intent.data = uri // Add your uri of the link to our application itself so that it knows which settings should be opened.
                    startActivity(intent)
                }
                catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel"){ dialog,
                                          _->
                dialog.dismiss()
            }.show()
    }


    private fun isLocationEnabled(): Boolean{
        // THe Location Manager class provides access to the system location services.
        // These services allow applications to obtain periodic updates of the device's geographical location,
        // or to be notified when the device enters the proximity of a given geographical location.
        val locationManager : LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    private fun getLocationWeatherDetails(){

        if (Constants.isNetworkAvailable(this@MainActivity)) {

            Toast.makeText(
                this@MainActivity,
                "You have connected to the internet. Now you can make an api call.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
        // END
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {

        if (Constants.isNetworkAvailable(this@MainActivity)) {

            val retrofit: Retrofit = Retrofit.Builder()
                // API base URL.
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                /** Create the Retrofit instances. */
                .build()
            // END

            // TODO (STEP 5: Further step for API call)
            // START
            /**
             * Here we map the service interface in which we declares the end point and the API type
             *i.e GET, POST and so on along with the request parameter which are required.
             */
            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)

            /** An invocation of a Retrofit method that sends a request to a web-server and returns a response.
             * Here we pass the required param in the service
             */
            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            // Callback methods are executed using the Retrofit callback executor.
            listCall.enqueue(object : Callback<WeatherResponse> {
                @SuppressLint("SetTextI18n")

                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    // Check weather the response is success or not.
                    if (response.isSuccessful) {

                        /** The de-serialized response body of a successful response. */
                        val weatherList: WeatherResponse? = response.body()
                        setupUI(weatherList!!)
                        Log.i("Response Result", "$weatherList")
                    } else {
                        // If the response is not success then we check the response code.
                        val sc = response.code()
                        when (sc) {
                            400 -> {
                                Log.e("Error 400", "Bad Request")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("Errorrrrr", t.message.toString())
                }
            })
            // END

        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
/// setup UI
    private fun setupUI(weatherList: WeatherResponse) {
        // For loop to get the required data. And all are populated in the UI.
        for (z in weatherList.weather.indices) {
            Log.i("NAMEEEEEEEE", weatherList.weather[z].main)
            binding.tvMain.text = weatherList.weather[z].main
            binding.tvMainDescription.text = weatherList.weather[z].description
            binding.tvTemp.text =
                weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            binding.tvHumidity.text = weatherList.main.humidity.toString() + " per cent"
            binding.tvMin.text = weatherList.main.temp_min.toString() + " min"
            binding.tvMax.text = weatherList.main.temp_max.toString() + " max"
            binding.tvSpeed.text = weatherList.wind.speed.toString()
            binding.tvName.text = weatherList.name
            binding.tvCountry.text = weatherList.sys.country
            binding.tvSunriseTime.text = unixTime(weatherList.sys.sunrise.toLong())
            binding.tvSunsetTime.text = unixTime(weatherList.sys.sunset.toLong())

            // Here we update the main icon
            when (weatherList.weather[z].icon) {
                "01d" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.sunny)
                "02d" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.cloud)
               // "02d" -> binding.ivMain.setImageResource(R.drawable.)
                "03d" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.cloud)
                "04d" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.cloud)
                "04n" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.cloud)
                "10d" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.rain)
                "11d" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.storm)
                "13d" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.snowflake)
                "01n" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.cloud)
                "02n" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.cloud)
                "03n" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.cloud)
                "10n" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.cloud)
                "11n" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.rain)
                "13n" -> binding.ivMain.setImageResource(com.example.liveweather.R.drawable.snowflake)
            }
        }
    }

    /**
     * Function is used to get the temperature unit value.
     */
    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    /**
     * The function is used to get the formatted time based on the Format and the LOCALE we pass to it.
     */
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
    // END
}