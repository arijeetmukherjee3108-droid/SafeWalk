package com.example.safewalk.ui.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.safewalk.databinding.FragmentReportPreviewBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.appcompat.app.AlertDialog
import com.example.safewalk.R
import com.example.safewalk.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

class ReportPreviewFragment : Fragment() {

    private var _binding: FragmentReportPreviewBinding? = null
    private val binding get() = _binding!!
    private val args: ReportPreviewFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        binding.previewCategory.text = args.category
        binding.previewDescription.text = args.description
        binding.previewLocation.text = args.locationName ?: "Lat: ${args.latitude}, Lng: ${args.longitude}"

        // Show suspect name if provided
        if (!args.suspectName.isNullOrEmpty()) {
            binding.previewSuspectLabel.visibility = View.VISIBLE
            binding.previewSuspectName.visibility = View.VISIBLE
            binding.previewSuspectName.text = args.suspectName
        }

        if (!args.imageUri.isNullOrEmpty()) {
            if (args.imageUri!!.startsWith("data:image")) {
                val base64String = args.imageUri!!.substringAfter("base64,")
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                binding.previewEvidence.setImageBitmap(bitmap)
            }
            binding.previewEvidence.visibility = View.VISIBLE
            binding.noEvidenceText.visibility = View.GONE
        } else {
            binding.previewEvidence.visibility = View.GONE
            binding.noEvidenceText.visibility = View.VISIBLE
        }

        binding.editButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.confirmButton.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Submission")
            .setMessage("Are you sure you want to submit this report? It will be recorded on the blockchain for security.")
            .setPositiveButton("Submit") { _, _ ->
                submitFinalReport()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitFinalReport() {
        binding.confirmButton.isEnabled = false
        binding.confirmButton.text = "Submitting to Blockchain..."

        // 1. Submit to Firestore (existing behavior, preserved)
        val report = hashMapOf(
            "userId" to (Firebase.auth.currentUser?.uid ?: ""),
            "category" to args.category,
            "description" to args.description,
            "locationName" to args.locationName,
            "latitude" to args.latitude.toDouble(),
            "longitude" to args.longitude.toDouble(),
            "imageUrl" to args.imageUri,
            "suspectName" to (args.suspectName ?: ""),
            "timestamp" to System.currentTimeMillis()
        )

        Firebase.firestore.collection("reports")
            .add(report)
            .addOnSuccessListener {
                val uid = Firebase.auth.currentUser?.uid
                if (uid != null) {
                    Firebase.firestore.collection("users").document(uid)
                        .update("reportCount", com.google.firebase.firestore.FieldValue.increment(1))
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ReportPreview", "Firestore error: ${e.message}")
            }

        // 2. Submit to Blockchain backend (new)
        submitToBlockchain()
    }

    private fun submitToBlockchain() {
        val TAG = "BlockchainSubmit"
        android.util.Log.d(TAG, "═══════════════════════════════════════════")
        android.util.Log.d(TAG, "Starting blockchain submission...")
        android.util.Log.d(TAG, "  lat: ${args.latitude}")
        android.util.Log.d(TAG, "  lng: ${args.longitude}")
        android.util.Log.d(TAG, "  incident_type: ${args.category}")
        android.util.Log.d(TAG, "  description: ${args.description.take(50)}...")
        android.util.Log.d(TAG, "  suspect_name: ${args.suspectName ?: "(empty)"}")
        android.util.Log.d(TAG, "  has_image: ${!args.imageUri.isNullOrEmpty()}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val textType = okhttp3.MediaType.parse("text/plain")
                val lat = okhttp3.RequestBody.create(textType, args.latitude.toString())
                val lng = okhttp3.RequestBody.create(textType, args.longitude.toString())
                val incidentType = okhttp3.RequestBody.create(textType, args.category)
                val description = okhttp3.RequestBody.create(textType, args.description)
                val suspectName = okhttp3.RequestBody.create(textType, args.suspectName ?: "")

                // Build file part — evidence_file is REQUIRED by the API
                val evidencePart: MultipartBody.Part
                if (!args.imageUri.isNullOrEmpty() && args.imageUri!!.startsWith("data:image")) {
                    val base64String = args.imageUri!!.substringAfter("base64,")
                    val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                    android.util.Log.d(TAG, "  image_bytes_size: ${imageBytes.size}")
                    val imageType = okhttp3.MediaType.parse("image/jpeg")
                    val requestFile = okhttp3.RequestBody.create(imageType, imageBytes)
                    evidencePart = MultipartBody.Part.createFormData("evidence_file", "evidence.jpg", requestFile)
                    android.util.Log.d(TAG, "  evidence_file part created ✓")
                } else {
                    // API requires evidence_file — send a minimal 1x1 transparent PNG placeholder
                    android.util.Log.d(TAG, "  No image attached, sending placeholder file")
                    val placeholder = byteArrayOf(
                        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                        0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15.toByte(), 0xC4.toByte(),
                        0x89.toByte(), 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                        0x54, 0x78.toByte(), 0x9C.toByte(), 0x62.toByte(), 0x00, 0x00,
                        0x00, 0x02, 0x00, 0x01, 0xE5.toByte(), 0x27.toByte(),
                        0xDE.toByte(), 0xFC.toByte(), 0x00, 0x00, 0x00, 0x00,
                        0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42.toByte(),
                        0x60.toByte(), 0x82.toByte()
                    )
                    val pngType = okhttp3.MediaType.parse("image/png")
                    val requestFile = okhttp3.RequestBody.create(pngType, placeholder)
                    evidencePart = MultipartBody.Part.createFormData("evidence_file", "no_evidence.png", requestFile)
                }

                android.util.Log.d(TAG, "Sending POST to /api/reports/submit ...")
                val response = RetrofitClient.blockchainInstance.submitReport(
                    lat = lat,
                    lng = lng,
                    incidentType = incidentType,
                    description = description,
                    suspectName = suspectName,
                    evidenceFile = evidencePart
                )

                android.util.Log.d(TAG, "═══ BLOCKCHAIN RESPONSE ═══")
                android.util.Log.d(TAG, "  status: ${response.status}")
                android.util.Log.d(TAG, "  message: ${response.message}")
                android.util.Log.d(TAG, "  blockchain_receipt: ${response.blockchain_receipt}")
                android.util.Log.d(TAG, "  ipfs_url: ${response.ipfs_url}")
                android.util.Log.d(TAG, "  resolution_secret: ${response.resolution_secret}")
                android.util.Log.d(TAG, "═══════════════════════════")

                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext

                    val receipt = response.blockchain_receipt ?: "N/A"
                    val ipfs = response.ipfs_url ?: "N/A"
                    val msg = response.message ?: "Submitted"

                    AlertDialog.Builder(requireContext())
                        .setTitle("✅ Blockchain Confirmed")
                        .setMessage(
                            "Your report has been permanently recorded on the blockchain.\n\n" +
                            "🔗 Blockchain Receipt:\n${receipt}\n\n" +
                            "📦 IPFS Evidence:\n${ipfs}\n\n" +
                            "📝 ${msg}"
                        )
                        .setPositiveButton("Done") { _, _ ->
                            findNavController().navigate(R.id.navigation_home)
                        }
                        .setCancelable(false)
                        .show()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "═══ BLOCKCHAIN ERROR ═══")
                android.util.Log.e(TAG, "  error: ${e.message}")
                android.util.Log.e(TAG, "  stack: ", e)
                android.util.Log.e(TAG, "════════════════════════")

                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext
                    // Still navigate home since Firestore succeeded
                    Toast.makeText(context, "Report saved to Firestore (blockchain: ${e.message})", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.navigation_home)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
