package com.example.safewalk.ui.dialogs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.example.safewalk.R
import com.example.safewalk.databinding.DialogSosBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SOSDialog : DialogFragment() {

    private var _binding: DialogSosBinding? = null
    private val binding get() = _binding!!
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.FullScreenDialog)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        db = Firebase.firestore
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        
        startPulseAnimation()
        triggerSOSAlerts()

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun triggerSOSAlerts() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Location permission required for SOS", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val locationUrl = if (location != null) {
                "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            } else {
                "Location unavailable"
            }
            
            sendAlertsToGuardians(locationUrl)
            notifyPoliceStation(location)
        }
    }

    private fun sendAlertsToGuardians(locationUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "A contact"
        val message = "EMERGENCY: $userName is in danger! Location: $locationUrl"

        db.collection("users").document(userId)
            .collection("guardians")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(requireContext(), "No guardians found to alert!", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                for (document in result) {
                    val phone = document.getString("phone") ?: ""
                    val isAppUser = document.getBoolean("isAppUser") ?: false
                    val guardianUid = document.getString("guardianUid") ?: ""
                    
                    if (phone.isNotEmpty()) {
                        // 1. Send SMS (traditional message)
                        sendSMS(phone, message)
                        
                        // 2. Send App Alert if they have the app
                        if (isAppUser && guardianUid.isNotEmpty()) {
                            createAppAlert(guardianUid, userName, locationUrl)
                        }
                    }
                }
                Toast.makeText(requireContext(), "Alerts sent to ${result.size()} guardians", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createAppAlert(toUid: String, fromName: String, locationUrl: String) {
        val alert = hashMapOf(
            "toUid" to toUid,
            "fromUid" to (auth.currentUser?.uid ?: ""),
            "fromName" to fromName,
            "locationUrl" to locationUrl,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "type" to "SOS",
            "status" to "SENT"
        )
        db.collection("app_alerts").add(alert)
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                val smsManager: SmsManager = requireContext().getSystemService(SmsManager::class.java)
                
                // Standardize phone: remove all non-digits
                val cleanPhone = phoneNumber.replace("\\D".toRegex(), "")
                
                // If it's a 10-digit number (India), optionally add +91 or keep as is.
                // Most modern networks handle this, but let's ensure it's not empty.
                if (cleanPhone.isNotEmpty()) {
                    smsManager.sendTextMessage(cleanPhone, null, message, null, null)
                    android.util.Log.d("SOSDialog", "SMS Sent to $cleanPhone")
                } else {
                    Toast.makeText(requireContext(), "Invalid phone number: $phoneNumber", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "SMS permission NOT granted!", Toast.LENGTH_LONG).show()
                // Re-request if possible or show a settings shortcut
            }
        } catch (e: Exception) {
            android.util.Log.e("SOSDialog", "SMS Failed", e)
            Toast.makeText(requireContext(), "SMS Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun notifyPoliceStation(location: android.location.Location?) {
        // In a real app, this would call a backend API that finds the nearest police station 
        // using Geofencing or a directory service and notifies their dispatch.
        // For this demo, we'll simulate the call.
        val station = "Central Police Station"
        Toast.makeText(requireContext(), "Notifying nearest police station: $station", Toast.LENGTH_LONG).show()
        
        // Example: Log the alert to a global 'emergency_alerts' collection in Firestore
        val alert = hashMapOf(
            "userId" to (auth.currentUser?.uid ?: "anonymous"),
            "userName" to (auth.currentUser?.displayName ?: "Unknown"),
            "location" to location?.let { mapOf("lat" to it.latitude, "lng" to it.longitude) },
            "timestamp" to com.google.firebase.Timestamp.now(),
            "status" to "ACTIVE"
        )
        db.collection("emergency_alerts").add(alert)
    }


    private fun startPulseAnimation() {
        val pulse = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)
        binding.pulseRing.startAnimation(pulse)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
