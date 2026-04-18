package com.example.safeher.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.safeher.R
import com.example.safeher.databinding.DialogSosBinding

class SOSDialog : DialogFragment() {

    private var _binding: DialogSosBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.FullScreenDialog)
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

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
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
