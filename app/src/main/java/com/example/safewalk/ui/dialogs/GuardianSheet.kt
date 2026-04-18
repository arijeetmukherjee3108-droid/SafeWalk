package com.example.safewalk.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.safewalk.R
import com.example.safewalk.databinding.BottomSheetGuardianBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GuardianSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetGuardianBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetGuardianBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = Firebase.auth
        db = Firebase.firestore

        setupListeners()
        loadGuardians()
    }

    private fun setupListeners() {
        binding.addGuardianButton.setOnClickListener {
            saveGuardian()
        }
        
        binding.dismissButton.setOnClickListener {
            dismiss()
        }
    }

    private fun saveGuardian() {
        val name = binding.guardianNameInput.text.toString().trim()
        val phone = binding.guardianPhoneInput.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId != null) {
            val guardian = hashMapOf(
                "name" to name,
                "phone" to phone,
                "timestamp" to System.currentTimeMillis()
            )

            binding.addGuardianButton.isEnabled = false
            binding.addGuardianButton.text = "Saving..."

            db.collection("users").document(userId)
                .collection("guardians")
                .add(guardian)
                .addOnSuccessListener {
                    binding.addGuardianButton.isEnabled = true
                    binding.addGuardianButton.text = "Add Guardian"
                    binding.guardianNameInput.text?.clear()
                    binding.guardianPhoneInput.text?.clear()
                    loadGuardians()
                }
                .addOnFailureListener {
                    binding.addGuardianButton.isEnabled = true
                    binding.addGuardianButton.text = "Add Guardian"
                    Toast.makeText(requireContext(), "Error saving", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadGuardians() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("users").document(userId)
            .collection("guardians")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                binding.guardianList.removeAllViews()
                for (document in result) {
                    val name = document.getString("name") ?: ""
                    val phone = document.getString("phone") ?: ""
                    addGuardianView(name, phone)
                }
            }
    }

    private fun addGuardianView(name: String, phone: String) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_guardian, binding.guardianList, false)
        view.findViewById<TextView>(R.id.itemGuardianName).text = name
        view.findViewById<TextView>(R.id.itemGuardianPhone).text = phone
        binding.guardianList.addView(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
