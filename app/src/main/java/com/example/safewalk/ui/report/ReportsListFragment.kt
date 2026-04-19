package com.example.safewalk.ui.report

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safewalk.data.model.Report
import com.example.safewalk.databinding.FragmentReportsListBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReportsListFragment : Fragment() {

    private var _binding: FragmentReportsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var reportAdapter: ReportAdapter
    
    private var currentFilterCategory: String? = null
    private var showOnlyMyReports = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTabs()
        setupFilters()
        fetchReports()
    }

    private fun setupRecyclerView() {
        reportAdapter = ReportAdapter()
        binding.reportsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reportAdapter
        }
    }

    private fun setupTabs() {
        binding.reportTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showOnlyMyReports = tab?.position == 1
                fetchReports()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFilters() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            currentFilterCategory = when (checkedIds.firstOrNull()) {
                binding.chipHarassment.id -> "Harassment"
                binding.chipStalking.id -> "Stalking"
                binding.chipSuspicious.id -> "Suspicious"
                else -> null
            }
            fetchReports()
        }
    }

    private fun fetchReports() {
        var query: Query = Firebase.firestore.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        if (showOnlyMyReports) {
            val userId = Firebase.auth.currentUser?.uid ?: ""
            query = query.whereEqualTo("userId", userId)
        }

        if (currentFilterCategory != null) {
            query = query.whereEqualTo("category", currentFilterCategory)
        }

        query.addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            
            val reports = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Report::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            
            reportAdapter.submitList(reports)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
