package com.example.androidassignment3

// Android imports
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

// Android permission and activity imports
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

// Google location service imports
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

// Sensor imports
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

// Main activity for the Find Your Way Back app
class MainActivity : AppCompatActivity(), SensorEventListener {

    // TextViews and buttons used by the app
    private lateinit var statusText: TextView
    private lateinit var startJourneyButton: Button
    private lateinit var endJourneyButton: Button

    // TextViews for displaying location information
    private lateinit var startLatitudeText: TextView
    private lateinit var startLongitudeText: TextView
    private lateinit var currentLatitudeText: TextView
    private lateinit var currentLongitudeText: TextView
    private lateinit var distanceText: TextView
    private lateinit var headingText: TextView

    // TextViews for displaying return information
    private lateinit var returnDirectionText: TextView
    private lateinit var distanceStatusText: TextView

    // Services used for GPS and device sensors
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager

    // Store the accelerometer and magnetic field sensors
    private var accelerometer: Sensor? = null
    private var magneticField: Sensor? = null

    // Store the latest sensor readings
    private val accelerometerValues = FloatArray(3)
    private val magneticFieldValues = FloatArray(3)

    // Track whether sensor data has been received
    private var hasAccelerometer = false
    private var hasMagneticField = false

    // Store the starting and current GPS locations
    private var startingLocation: Location? = null
    private var currentLocation: Location? = null

    // Store the previous distance from the starting point
    private var previousDistance: Float? = null

    // Distance used to determine whether the user is close enough
    private val DISTANCE_TOLERANCE = 3f

    // Tracks whether a journey is currently active
    private var journeyActive = false

    // Set up how often GPS location updates should occur
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L
    ).apply {
        setMinUpdateIntervalMillis(500L)
    }.build()

    // Receives GPS location updates
    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(result: LocationResult) {

            // Process each location received
            for (location in result.locations) {

                // Save the current location
                currentLocation = location

                // Update the location information on the screen
                updateLocationDisplay(location)

                // Calculate the distance from the starting point
                calculateDistance(location)
            }
        }
    }

    // Request location permission from the user
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            // Check if fine location permission was granted
            val fineLocation =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            // Check if coarse location permission was granted
            val coarseLocation =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            // Start receiving locations if permission was granted
            if (fineLocation || coarseLocation) {

                startLocationUpdates()

            } else {

                // Display a message if permission was denied
                statusText.text = "LOCATION PERMISSION DENIED"
            }
        }

    // Set up the activity when the app starts
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the activity layout
        setContentView(R.layout.activity_main)

        // Get access to the device sensor manager
        sensorManager =
            getSystemService(SENSOR_SERVICE) as SensorManager

        // Get the accelerometer sensor
        accelerometer =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )

        // Get the magnetic field sensor
        magneticField =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_MAGNETIC_FIELD
            )

        // Connect the Kotlin variables to the XML views
        statusText = findViewById(R.id.statusText)
        startJourneyButton = findViewById(R.id.startJourneyButton)
        endJourneyButton = findViewById(R.id.endJourneyButton)

        startLatitudeText = findViewById(R.id.startLatitudeText)
        startLongitudeText = findViewById(R.id.startLongitudeText)

        currentLatitudeText = findViewById(R.id.currentLatitudeText)
        currentLongitudeText = findViewById(R.id.currentLongitudeText)

        distanceText = findViewById(R.id.distanceText)
        headingText = findViewById(R.id.headingText)
        returnDirectionText = findViewById(R.id.returnDirectionText)
        distanceStatusText = findViewById(R.id.distanceStatusText)

        // Get the GPS location service
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        // Set up the Start Journey button
        startJourneyButton.setOnClickListener {

            startJourney()
        }

        // Set up the End Journey button
        endJourneyButton.setOnClickListener {

            endJourney()
        }

        // Disable the End button until a journey has started
        endJourneyButton.isEnabled = false
    }

    // Start listening for sensor updates
    override fun onResume() {
        super.onResume()

        // Register the accelerometer sensor
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        // Register the magnetic field sensor
        magneticField?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    // Stop listening for sensor updates when the app is paused
    override fun onPause() {
        super.onPause()

        sensorManager.unregisterListener(this)
    }

    // Called whenever new sensor data is received
    override fun onSensorChanged(event: SensorEvent?) {

        // Stop if there is no sensor event
        if (event == null) {
            return
        }

        // Determine which sensor provided the data
        when (event.sensor.type) {

            // Store accelerometer readings
            Sensor.TYPE_ACCELEROMETER -> {

                accelerometerValues[0] = event.values[0]
                accelerometerValues[1] = event.values[1]
                accelerometerValues[2] = event.values[2]

                hasAccelerometer = true
            }

            // Store magnetic field readings
            Sensor.TYPE_MAGNETIC_FIELD -> {

                magneticFieldValues[0] = event.values[0]
                magneticFieldValues[1] = event.values[1]
                magneticFieldValues[2] = event.values[2]

                hasMagneticField = true
            }
        }

        // Calculate the heading once both sensors have data
        if (hasAccelerometer && hasMagneticField) {
            calculateHeading()
        }
    }
    // Starts a new journey
    private fun startJourney() {

        // Mark the journey as active
        journeyActive = true

        // Tell the user that the app is looking for their starting location
        statusText.text = "GETTING STARTING LOCATION..."

        // Disable the Start button while the journey is active
        startJourneyButton.isEnabled = false

        // Enable the End Journey button
        endJourneyButton.isEnabled = true

        // Reset previous journey information
        startingLocation = null
        currentLocation = null
        previousDistance = null

        // Reset the starting location display
        startLatitudeText.text = "Latitude: --"
        startLongitudeText.text = "Longitude: --"

        // Reset the current location display
        currentLatitudeText.text = "Latitude: --"
        currentLongitudeText.text = "Longitude: --"

        // Reset the distance display
        distanceText.text = "-- m"

        // Tell the user the app is waiting for a GPS location
        distanceStatusText.text = "WAITING FOR LOCATION"

        // Check for location permission and start GPS updates
        checkLocationPermission()
    }

    // Calculates the direction the device is facing
    private fun calculateHeading() {

        // Create arrays to store the rotation and orientation information
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        // Use the accelerometer and magnetic field sensor data
        // to calculate the device's rotation
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerValues,
            magneticFieldValues
        )

        // Continue only if the rotation matrix was calculated successfully
        if (success) {

            // Calculate the device's orientation using the rotation matrix
            SensorManager.getOrientation(
                rotationMatrix,
                orientation
            )

            // Convert the heading from radians to degrees
            var heading = Math.toDegrees(
                orientation[0].toDouble()
            ).toFloat()

            // Convert negative heading values to a value between 0 and 360 degrees
            if (heading < 0) {
                heading += 360f
            }

            // Display the heading in degrees on the screen
            headingText.text =
                "${heading.toInt()}°"
        }
    }

    // Required method for the SensorEventListener
    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // No action is needed when sensor accuracy changes
    }

    // Check whether location permissions have been granted
    private fun checkLocationPermission() {

        // Check fine location permission
        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        // Check coarse location permission
        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        // Start location updates if permission is already available
        if (fineLocationGranted || coarseLocationGranted) {

            startLocationUpdates()

        } else {

            // Ask the user for location permissions
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Start receiving GPS location updates
    private fun startLocationUpdates() {

        // Make sure location permission is still available
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Request regular GPS location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    // Update the current location information on the screen
    private fun updateLocationDisplay(location: Location) {

        // Display the current latitude
        currentLatitudeText.text =
            "Latitude: %.6f".format(location.latitude)

        // Display the current longitude
        currentLongitudeText.text =
            "Longitude: %.6f".format(location.longitude)

        // Use the first location received as the starting location
        if (startingLocation == null) {

            startingLocation = Location(location)

            // Display the starting latitude
            startLatitudeText.text =
                "Latitude: %.6f".format(location.latitude)

            // Display the starting longitude
            startLongitudeText.text =
                "Longitude: %.6f".format(location.longitude)

            statusText.text = "JOURNEY ACTIVE"
        }
    }

    // Calculate the distance from the current location to the start
    private fun calculateDistance(location: Location) {

        // Stop if a starting location has not been saved
        val start = startingLocation ?: return

        // Calculate the distance between the two locations
        val distance = start.distanceTo(location)

        // Display the distance in meters or kilometers
        if (distance < 1000) {

            distanceText.text =
                "%.1f m".format(distance)

        } else {

            val kilometers = distance / 1000

            distanceText.text =
                "%.2f km".format(kilometers)
        }

        // Calculate the direction back to the starting location
        calculateReturnDirection(location)

        // Check whether the user has returned to the starting point
        if (distance <= DISTANCE_TOLERANCE) {

            distanceStatusText.text =
                "YOU ARE AT THE START"

            previousDistance = distance

            return
        }

        // Get the previous distance
        val oldDistance = previousDistance

        // Compare the current distance with the previous distance
        if (oldDistance != null) {

            val difference = distance - oldDistance

            // The user is getting closer
            if (difference < -DISTANCE_TOLERANCE) {

                distanceStatusText.text =
                    "GETTING CLOSER"

                // The user is getting farther away
            } else if (difference > DISTANCE_TOLERANCE) {

                distanceStatusText.text =
                    "GETTING FARTHER"

                // The distance has changed very little
            } else {

                distanceStatusText.text =
                    "DISTANCE UNCHANGED"
            }
        }

        // Save the current distance for the next update
        previousDistance = distance
    }

    // Calculate the direction the user needs to travel to return
    private fun calculateReturnDirection(location: Location) {

        // Stop if there is no starting location
        val start = startingLocation ?: return

        // Calculate the bearing from the current location to the start
        val bearing = location.bearingTo(start)

        // Store the bearing as the direction
        var direction = bearing

        // Make sure the direction is between 0 and 360 degrees
        if (direction < 0) {
            direction += 360f
        }

        // Convert the degree value into a readable direction
        val directionText = when {

            direction >= 337.5 || direction < 22.5 ->
                "NORTH ↑"

            direction < 67.5 ->
                "NORTHEAST ↗"

            direction < 112.5 ->
                "EAST →"

            direction < 157.5 ->
                "SOUTHEAST ↘"

            direction < 202.5 ->
                "SOUTH ↓"

            direction < 247.5 ->
                "SOUTHWEST ↙"

            direction < 292.5 ->
                "WEST ←"

            else ->
                "NORTHWEST ↖"
        }

        // Display the return direction
        returnDirectionText.text = directionText
    }

    // End the current journey
    private fun endJourney() {

        journeyActive = false

        // Stop receiving GPS updates
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Update the status message
        statusText.text = "JOURNEY ENDED"

        // Allow a new journey to be started
        startJourneyButton.isEnabled = true
        endJourneyButton.isEnabled = false
    }

    // Clean up GPS updates when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()

        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}