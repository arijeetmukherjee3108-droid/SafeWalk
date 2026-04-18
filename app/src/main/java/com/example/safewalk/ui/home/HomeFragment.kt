package com.example.safewalk.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.safewalk.R
import com.example.safewalk.databinding.FragmentHomeBinding
import com.example.safewalk.ui.dialogs.GuardianSheet
import com.example.safewalk.ui.dialogs.SOSDialog

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        startPulseAnimation()
        setupListeners()
    }

    private fun startPulseAnimation() {
        val pulse = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)
        binding.pulseRing.startAnimation(pulse)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var progress = 0
    private val totalTime = 3000L // 3 seconds
    private val interval = 30L // update every 30ms

    private val progressRunnable = object : Runnable {
        override fun run() {
            progress += (interval * 100 / totalTime).toInt()
            if (progress >= 100) {
                binding.sosProgress.progress = 100
                triggerSOS()
            } else {
                binding.sosProgress.progress = progress
                handler.postDelayed(this, interval)
            }
        }
    }

    private fun setupListeners() {
        binding.sosButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    startSOSCounter()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    stopSOSCounter()
                    true
                }
                else -> false
            }
        }

        binding.actionSafeRoute.setOnClickListener {
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.navigation_map
        }

        binding.actionReport.setOnClickListener {
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.navigation_report
        }

        binding.actionGuardians.setOnClickListener {
            val sheet = GuardianSheet()
            sheet.show(parentFragmentManager, "GUARDIAN_SHEET")
        }

        binding.actionAlerts.setOnClickListener {
            // Handle Alerts click (e.g., navigate to notification history)
        }

        binding.guardianModeToggle.setOnCheckedChangeListener { _, isChecked ->
            // Handle toggle logic
        }
    }

    private fun startSOSCounter() {
        progress = 0
        binding.sosProgress.progress = 0
        binding.sosProgress.visibility = View.VISIBLE
        handler.post(progressRunnable)
    }

    private fun stopSOSCounter() {
        handler.removeCallbacks(progressRunnable)
        progress = 0
        binding.sosProgress.progress = 0
        // Optional: hide after a delay or just leave at 0
    }

    private fun triggerSOS() {
        stopSOSCounter()
        val dialog = SOSDialog()
        dialog.show(parentFragmentManager, "SOS_DIALOG")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
