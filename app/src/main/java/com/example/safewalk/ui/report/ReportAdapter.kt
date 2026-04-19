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
                if (report.imageUrl.startsWith("data:image")) {
                    // Decode Base64 data URI
                    val base64Data = report.imageUrl.substringAfter("base64,")
                    val bytes = android.util.Base64.decode(base64Data, android.util.Base64.NO_WRAP)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.reportImage.setImageBitmap(bitmap)
                } else {
                    Glide.with(binding.reportImage.context)
                        .load(report.imageUrl)
                        .into(binding.reportImage)
                }
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
