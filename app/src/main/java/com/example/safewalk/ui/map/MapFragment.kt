package com.example.safewalk.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.safewalk.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker
import androidx.navigation.fragment.findNavController
import kotlin.concurrent.thread

import androidx.core.os.bundleOf
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import android.location.Geocoder
import java.util.Locale

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: GeoPoint? = null
    private var isPickMode = false
    private var selectedPoint: GeoPoint? = null
    private var selectionMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        isPickMode = arguments?.getBoolean("pick_mode") ?: false
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        getCurrentLocation()
        
        if (isPickMode) {
            setupPickMode()
        }
    }

    private fun setupPickMode() {
        binding.selectionLayout.visibility = View.VISIBLE
        
        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { updateSelectedLocation(it) }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        
        val mapEventsOverlay = MapEventsOverlay(eventsReceiver)
        binding.mapView.overlays.add(0, mapEventsOverlay)

        binding.btnConfirmLocation.setOnClickListener {
            selectedPoint?.let { point ->
                val address = getAddressFromLocation(point.latitude, point.longitude)
                parentFragmentManager.setFragmentResult("location_request", bundleOf(
                    "lat" to point.latitude,
                    "lng" to point.longitude,
                    "address" to address
                ))
                findNavController().popBackStack()
            } ?: Toast.makeText(context, "Please tap on map to select a location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectedLocation(point: GeoPoint) {
        selectedPoint = point
        
        if (selectionMarker == null) {
            selectionMarker = Marker(binding.mapView)
            selectionMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            binding.mapView.overlays.add(selectionMarker)
        }
        
        selectionMarker?.position = point
        selectionMarker?.title = "Selected Location"
        binding.mapView.invalidate()
        
        binding.selectedLocationText.text = String.format(Locale.US, "%.4f, %.4f", point.latitude, point.longitude)
    }

    private fun getAddressFromLocation(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.US)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (addresses?.isNotEmpty() == true) {
                addresses[0].getAddressLine(0)
            } else {
                "Selected Location"
            }
        } catch (e: Exception) {
            "Selected Location"
        }
    }

    private fun setupMap() {
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(15.0)
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = GeoPoint(it.latitude, it.longitude)
                binding.mapView.controller.setCenter(currentLocation)
                
                val startMarker = Marker(binding.mapView)
                startMarker.position = currentLocation
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                startMarker.title = "You are here"
                binding.mapView.overlays.add(startMarker)
            }
        }
    }

    private fun fetchRoutes(destination: GeoPoint) {
        val start = currentLocation ?: return
        
        thread {
            val roadManager: RoadManager = OSRMRoadManager(requireContext(), "SafeWalk/1.0")
            val waypoints = ArrayList<GeoPoint>()
            waypoints.add(start)
            waypoints.add(destination)

            // In a real scenario with OSRM, fetching alternatives is slightly different
            // For this demo, we'll fetch the main road and simulate 2 alternatives 
            // OR use a service that supports alternatives.
            val roads = roadManager.getRoads(waypoints)
            
            activity?.runOnUiThread {
                displayRoutes(roads)
            }
        }
    }

    private fun displayRoutes(roads: Array<Road>) {
        // Clear overlays but keep the events overlay if in pick mode
        val eventsOverlay = binding.mapView.overlays.find { it is MapEventsOverlay }
        binding.mapView.overlays.clear()
        eventsOverlay?.let { binding.mapView.overlays.add(it) }
        
        getCurrentLocation() // Redraw my position
        
        // Redraw selection marker if it exists
        selectionMarker?.let { binding.mapView.overlays.add(it) }
        
        for (road in roads) {
            val safetyScore = scoreRouteWithML(road)
            val roadOverlay = RoadManager.buildRoadOverlay(road)
            
            // Highlight based on ML Score
            if (safetyScore > 0.8) {
                roadOverlay.outlinePaint.color = android.graphics.Color.GREEN
                roadOverlay.outlinePaint.strokeWidth = 15f
            } else {
                roadOverlay.outlinePaint.color = android.graphics.Color.GRAY
                roadOverlay.outlinePaint.strokeWidth = 10f
            }
            
            binding.mapView.overlays.add(roadOverlay)
        }
        binding.mapView.invalidate()
    }

    private fun scoreRouteWithML(road: Road): Double {
        // TODO: Integrate your ML model here!
        // You can analyze road.mRoute (list of GeoPoints)
        // For now, return a dummy score
        return Math.random() 
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
