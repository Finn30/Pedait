package com.example.pedait

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

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
            val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault())
            sdf.format(date)
        } ?: "Unknown Date"
        val timestamp = meetings.datetime
        val formattedTime = timestamp?.toDate()?.toString() ?: "Unknown Time"

        holder.tvDateTime.text = formatDate
        holder.tvTopic.text = meetings.topic
    }

    override fun getItemCount(): Int {
        return meetingsList.size
    }

    public class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvDateTime = itemView.findViewById<TextView>(R.id.tvDateTime)
        val tvTopic = itemView.findViewById<TextView>(R.id.tvTopic)

    }

}