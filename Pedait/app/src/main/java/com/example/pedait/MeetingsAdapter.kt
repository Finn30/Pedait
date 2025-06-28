package com.example.pedait

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.toColorInt
import java.util.TimeZone

class MeetingsAdapter(private val meetingsList: ArrayList<Meetings>) : RecyclerView.Adapter<MeetingsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MeetingsAdapter.ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_meetings, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MeetingsAdapter.ViewHolder, position: Int) {
        val meetings: Meetings = meetingsList[position]
        Log.d("MeetingsAdapter", "Meeting timestamp: ${meetings.datetime}")

        val formatDate = meetings.datetime?.toDate()?.let { date ->
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("id"))
            sdf.timeZone = TimeZone.getDefault()
            sdf.format(date)
        } ?: "Unknown Date"
        val timestamp = meetings.datetime
        val formattedTime = timestamp?.toDate()?.toString() ?: "Unknown Time"

        holder.tvDateTime.text = formatDate
        holder.tvTopic.text = meetings.topic
        holder.tvStatus.text = meetings.status ?: "Belum hadir"

        // Ubah warna teks status berdasarkan nilainya
        val status = meetings.status?.lowercase() ?: "alpa"
        when (status) {
            "hadir" -> holder.tvStatus.setTextColor("#4CAF50".toColorInt()) // Hijau
            "izin" -> holder.tvStatus.setTextColor("#2196F3".toColorInt()) // Biru
            "sakit" -> holder.tvStatus.setTextColor("#9C27B0".toColorInt()) // Ungu
            "lain" -> holder.tvStatus.setTextColor("#FF9800".toColorInt()) // Oranye
            "alpa" -> holder.tvStatus.setTextColor("#F44336".toColorInt()) // Merah
            else -> holder.tvStatus.setTextColor(android.graphics.Color.GRAY)
        }

    }

    override fun getItemCount(): Int {
        return meetingsList.size
    }

    public class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvDateTime = itemView.findViewById<TextView>(R.id.tvDateTime)
        val tvTopic = itemView.findViewById<TextView>(R.id.tvTopic)
        val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)

    }

}