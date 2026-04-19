package com.example.safewalk.ui.report

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.safewalk.data.model.Report
import com.example.safewalk.databinding.ItemReportBinding

class ReportAdapter : ListAdapter<Report, ReportAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReportViewHolder(private val binding: ItemReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(report: Report) {
            binding.reportCategory.text = report.category
            binding.reportDescription.text = report.description
            binding.reportLocation.text = report.locationName
            
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                report.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            binding.reportTime.text = timeAgo

            if (!report.imageUrl.isNullOrEmpty()) {
                binding.reportImage.visibility = View.VISIBLE
                Glide.with(binding.reportImage.context)
                    .load(report.imageUrl)
                    .into(binding.reportImage)
            } else {
                binding.reportImage.visibility = View.GONE
            }
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem == newItem
        }
    }
}
