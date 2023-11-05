package com.example.sensorlistcheckbox3

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.sensorlistcheckbox3.databinding.FragmentFirstBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar


class FirstFragment : Fragment() , SensorEventListener {
    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private var udpPortSensors = 7000
    private var udpPortLocation = 7000

    private lateinit var sensorManager: SensorManager
    private lateinit var allSensors: List<Sensor>
    // Two parallel lists
    private val checkBoxList = mutableListOf<CheckBox>()

    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        createLocationPermissionRequestObject()
        createLocationUpdateObject()

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editTextUdpPortSensors.setText(udpPortSensors.toString())
        binding.editTextUdpPortLocation.setText(udpPortLocation.toString())

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

        val layout = binding.mainLinearLayout
        for (sensor in allSensors) {
            val checkBox = CheckBox(requireContext())
            checkBoxList.add(checkBox)
            checkBox.text = sensor.name
            layout.addView(checkBox)
        }

        binding.switchSensors.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                registerListeners()
            } else {
                unregisterListeners()
            }
        }

        locationPermissionRequest = createLocationPermissionRequestObject()
        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Before you perform the actual permission request, check whether your app
                // already has the permissions, and whether your app needs to show a permission
                // rationale dialog. For more details, see Request permissions.
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION // TODO no coarse location
                    )
                )
            } else {
                stopLocationUpdates()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // unregisterListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createLocationPermissionRequestObject(): ActivityResultLauncher<Array<String>> {
        locationPermissionRequest =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
            { permissions ->
                when {
                    permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                        // Precise location access granted.
                        doIt()
                    }
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                        // Only approximate location access granted.
                        doIt()
                    }
                    else -> {
                        // No location access granted.
                        Snackbar.make(
                            binding.firstFragment,
                            "Sorry no location for you",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        return locationPermissionRequest
    }

    private fun doIt() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        startLocationUpdates()
    }

    private fun createLocationUpdateObject() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location: Location? = locationResult.lastLocation
                if (location == null) {
                    Log.d("APPLE", "No location")
                } else {
                    val locationString =
                        "${location.latitude} ${location.longitude} ${location.bearing}"
                    val message = "current location\n${locationString}"
                    binding.textviewSensorName.text = "Location"
                    binding.textviewSensorData.text = locationString
                    val udpPort = getPortNumber(binding.editTextUdpPortLocation)
                    if (udpPort != -1)
                        UdpBroadcastHelper().sendUdpBroadcast(message, udpPort)
                }
            }
        }
    }

    // return -1 on illegal cases
    private fun getPortNumber(editText: EditText): Int {
        val udpPortStr = editText.text.trim().toString()
        if (udpPortStr.isBlank()) {
            editText.error = "Missing port number"
            return -1
        }
        val udpPort = udpPortStr.toInt()
        if (udpPort < 0 || udpPort > 65353) {
            editText.error = "Illegal port number"
            return -1
        }
        return udpPort
    }

    private fun registerListeners() {
        for (i in allSensors.indices) {
            if (checkBoxList[i].isChecked) {
                sensorManager.registerListener(
                    this, allSensors[i],
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
        }
    }

    private fun unregisterListeners() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sensorName = event?.sensor?.name
        binding.textviewSensorName.text = sensorName
        binding.textviewSensorData.text = event?.values?.joinToString(", ")
        if (sensorName != null && event.values != null) {
            val udpPort = getPortNumber(binding.editTextUdpPortSensors)
            if (udpPort != -1)
                UdpBroadcastHelper().sendUdpBroadcast(sensorName, event.values, udpPort)
        }

        val message = "${event?.sensor?.name}\t ${event?.values?.joinToString(", ")}"
        Log.d("APPLE", message)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Nothing
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.Builder(1000)
                .build(), // https://developer.android.com/training/location/change-location-settings
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}