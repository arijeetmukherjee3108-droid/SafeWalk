package com.example.safewalk.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.safewalk.R
import com.example.safewalk.databinding.FragmentProfileBinding
import com.example.safewalk.ui.auth.AuthActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = Firebase.auth
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayUserInfo()
        setupLogout()
    }

    private fun displayUserInfo() {
        val user = auth.currentUser
        user?.let {
            binding.profileName.text = it.displayName ?: "SafeWalk User"
            binding.profileEmail.text = it.email
            
            val initials = if (!it.displayName.isNullOrEmpty()) {
                it.displayName!!.split(" ").mapNotNull { name -> name.take(1) }.take(2).joinToString("").uppercase()
            } else "SW"
            binding.profileInitials.text = initials

            if (it.photoUrl != null) {
                binding.profileInitials.visibility = View.GONE
                Glide.with(this)
                    .load(it.photoUrl)
                    .circleCrop()
                    .into(binding.profileImage)
            } else {
                binding.profileInitials.visibility = View.VISIBLE
            }
        }
    }

    private fun setupLogout() {
        binding.logoutButton.setOnClickListener {
            // Firebase Sign Out
            auth.signOut()

            // Google Sign Out (to allow choosing account again)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignInClient.signOut().addOnCompleteListener {
                navigateToAuth()
            }
        }
    }

    private fun navigateToAuth() {
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
