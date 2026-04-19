package com.example.safewalk.ui.report

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.example.safewalk.R
import com.example.safewalk.databinding.FragmentReportBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.ByteArrayOutputStream

import androidx.fragment.app.viewModels

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportViewModel by viewModels()
    
    private var selectedImageUri: Uri? = null
    private var selectedLocation: String? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var lastCheckedId = -1

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.selectedImageUri.value = it
            // Also store as bytes for bulletproof upload
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                viewModel.selectedImageData.value = inputStream?.readBytes()
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val captureImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                binding.evidenceImage.setImageBitmap(it)
                binding.evidenceImage.visibility = View.VISIBLE
                binding.uploadPlaceholder.visibility = View.GONE
                
                val bytes = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
                viewModel.selectedImageData.value = bytes.toByteArray()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener("location_request") { _, bundle ->
            val lat = bundle.getDouble("lat")
            val lng = bundle.getDouble("lng")
            val address = bundle.getString("address") ?: "Custom Location"
            
            viewModel.selectedLatitude.value = lat
            viewModel.selectedLongitude.value = lng
            viewModel.selectedLocation.value = address
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        setupUI()
        observeViewModel()
        return binding.root
    }

    private fun observeViewModel() {
        viewModel.selectedLocation.observe(viewLifecycleOwner) { 
            binding.locationText.text = it ?: "Select location of the incident"
        }
        viewModel.selectedImageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                binding.evidenceImage.setImageURI(uri)
                binding.evidenceImage.visibility = View.VISIBLE
                binding.uploadPlaceholder.visibility = View.GONE
            } else {
                binding.evidenceImage.visibility = View.GONE
                binding.uploadPlaceholder.visibility = View.VISIBLE
            }
        }
    }

    private fun setupUI() {
        val categoryButtons = listOf(
            binding.btnHarassment,
            binding.btnStalking,
            binding.btnSuspicious,
            binding.btnOther
        )

        categoryButtons.forEach { button ->
            button.setOnClickListener {
                if (lastCheckedId == button.id) {
                    // Deselect if already selected
                    button.isChecked = false
                    lastCheckedId = -1
                } else {
                    // Select new one, deselect all others
                    categoryButtons.forEach { it.isChecked = (it.id == button.id) }
                    lastCheckedId = button.id
                }
                
                // Show/hide "Other" input field
                binding.otherCategoryInput.visibility = if (lastCheckedId == R.id.btnOther) View.VISIBLE else View.GONE
                if (lastCheckedId != R.id.btnOther) {
                    binding.otherCategoryInput.text.clear()
                }
            }
        }

        binding.evidenceUpload.setOnClickListener {
            showImagePickerOptions()
        }

        binding.locationPicker.setOnClickListener {
            val bundle = Bundle()
            bundle.putBoolean("pick_mode", true)
            findNavController().navigate(R.id.navigation_map, bundle)
        }

        binding.submitButton.setOnClickListener {
            submitReport()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Gallery", "Camera")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Evidence From")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImage.launch("image/*")
                    1 -> checkCameraPermissionAndLaunch()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        captureImage.launch(intent)
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(requireContext().contentResolver, bitmap, "Evidence", null)
        return Uri.parse(path)
    }

    private fun submitReport() {
        val checkedId = lastCheckedId
        if (checkedId == -1) {
            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val category = when (checkedId) {
            R.id.btnHarassment -> "Harassment"
            R.id.btnStalking -> "Stalking"
            R.id.btnSuspicious -> "Suspicious"
            R.id.btnOther -> binding.otherCategoryInput.text.toString().trim()
            else -> ""
        }

        if (category.isEmpty()) {
            Toast.makeText(context, "Please specify the issue", Toast.LENGTH_SHORT).show()
            return
        }

        val description = binding.descriptionInput.text.toString()
        if (description.isEmpty()) {
            Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
            return
        }

        if (viewModel.selectedLocation.value == null) {
            Toast.makeText(context, "Please select a location", Toast.LENGTH_SHORT).show()
            return
        }

        binding.submitButton.isEnabled = false
        binding.submitButton.text = "Submitting..."

        val imageData = viewModel.selectedImageData.value
        val imageUrl = if (imageData != null) {
            // Decode and scale down to fit within Firestore's 1MB doc limit
            val original = android.graphics.BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            val maxDim = 400
            val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
            } else {
                original
            }
            val compressed = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 40, compressed)
            val base64 = android.util.Base64.encodeToString(compressed.toByteArray(), android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } else {
            null
        }

        saveToFirestore(category, description, imageUrl)
    }

    private fun saveToFirestore(category: String, description: String, imageUrl: String?) {
        val report = hashMapOf(
            "userId" to (Firebase.auth.currentUser?.uid ?: ""),
            "category" to category,
            "description" to description,
            "locationName" to viewModel.selectedLocation.value,
            "latitude" to (viewModel.selectedLatitude.value ?: 0.0),
            "longitude" to (viewModel.selectedLongitude.value ?: 0.0),
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        Firebase.firestore.collection("reports")
            .add(report)
            .addOnSuccessListener {
                // Increment report count on user profile
                val uid = Firebase.auth.currentUser?.uid
                if (uid != null) {
                    Firebase.firestore.collection("users").document(uid)
                        .update("reportCount", com.google.firebase.firestore.FieldValue.increment(1))
                }
                Toast.makeText(context, "Report submitted successfully", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Submit error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                android.util.Log.e("ReportFragment", "Firestore write failed", e)
                binding.submitButton.isEnabled = true
                binding.submitButton.text = "Submit Report →"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
