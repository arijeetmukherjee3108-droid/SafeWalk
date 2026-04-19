package com.example.safewalk.ui.alerts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safewalk.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

data class AlertItem(
    val id: String,
    val fromName: String,
    val locationUrl: String,
    val timestamp: Timestamp?,
    val type: String,
    val status: String,
    val source: String   // "GUARDIAN" or "COMMUNITY"
)

class AlertsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoAlerts: TextView
    private val alertsList = mutableListOf<AlertItem>()
    private lateinit var adapter: AlertsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_alerts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerViewAlerts)
        tvNoAlerts = view.findViewById(R.id.tvNoAlerts)

        adapter = AlertsAdapter(alertsList) { alert ->
            // Open location in browser/maps when tapped
            if (alert.locationUrl.isNotEmpty() && alert.locationUrl.startsWith("http")) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(alert.locationUrl)))
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadAlerts()
    }

    private fun loadAlerts() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val db = Firebase.firestore

        // Fetch alerts where the current user is the recipient (guardian alerts + community alerts)
        db.collection("app_alerts")
            .whereEqualTo("toUid", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || !isAdded) return@addSnapshotListener

                alertsList.clear()
                snapshot?.documents?.forEach { doc ->
                    val fromName = doc.getString("fromName") ?: "Unknown"
                    val locationUrl = doc.getString("locationUrl") ?: ""
                    val timestamp = doc.getTimestamp("timestamp")
                    val type = doc.getString("type") ?: "SOS"
                    val status = doc.getString("status") ?: "SENT"

                    // Determine if it was a guardian alert or community alert
                    val source = if (fromName.startsWith("COMMUNITY SOS")) "COMMUNITY" else "GUARDIAN"
                    val displayName = fromName.removePrefix("COMMUNITY SOS: ")

                    alertsList.add(
                        AlertItem(
                            id = doc.id,
                            fromName = displayName,
                            locationUrl = locationUrl,
                            timestamp = timestamp,
                            type = type,
                            status = status,
                            source = source
                        )
                    )
                }
                adapter.notifyDataSetChanged()
                tvNoAlerts.visibility = if (alertsList.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (alertsList.isEmpty()) View.GONE else View.VISIBLE
            }
    }
}

// ── Adapter ──────────────────────────────────────────────────────────

class AlertsAdapter(
    private val alerts: List<AlertItem>,
    private val onClick: (AlertItem) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    inner class AlertViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAlertIcon: TextView = view.findViewById(R.id.tvAlertIcon)
        val tvAlertTitle: TextView = view.findViewById(R.id.tvAlertTitle)
        val tvAlertFrom: TextView = view.findViewById(R.id.tvAlertFrom)
        val tvAlertTime: TextView = view.findViewById(R.id.tvAlertTime)
        val tvAlertStatus: TextView = view.findViewById(R.id.tvAlertStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]

        // Icon based on source
        holder.tvAlertIcon.text = if (alert.source == "COMMUNITY") "📢" else "🚨"

        // Title
        holder.tvAlertTitle.text = when (alert.source) {
            "COMMUNITY" -> "Community SOS Alert"
            else -> "Guardian SOS Alert"
        }

        // Title color
        val titleColor = if (alert.source == "COMMUNITY")
            holder.itemView.context.getColor(R.color.amber)
        else
            holder.itemView.context.getColor(R.color.ember)
        holder.tvAlertTitle.setTextColor(titleColor)

        // From
        holder.tvAlertFrom.text = "From: ${alert.fromName}"

        // Timestamp
        holder.tvAlertTime.text = alert.timestamp?.let { formatTimeAgo(it) } ?: "Unknown time"

        // Status badge
        holder.tvAlertStatus.text = alert.status
        holder.tvAlertStatus.setTextColor(
            when (alert.status) {
                "ACTIVE" -> holder.itemView.context.getColor(R.color.ember)
                "RESOLVED" -> holder.itemView.context.getColor(R.color.safe)
                else -> holder.itemView.context.getColor(R.color.amber)
            }
        )

        holder.itemView.setOnClickListener { onClick(alert) }
    }

    override fun getItemCount() = alerts.size

    private fun formatTimeAgo(timestamp: Timestamp): String {
        val now = System.currentTimeMillis()
        val then = timestamp.toDate().time
        val diff = now - then

        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} min ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(timestamp.toDate())
        }
    }
}
