package com.example.pedait

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CourseAdapter(
    private val courseList: ArrayList<Course>,
    private val onItemClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courseList[position]
        holder.tvKodeMK.text = course.kodeMK
        holder.tvNamaMK.text = course.namaMK

        holder.itemView.setOnClickListener {
            onItemClick(course)
        }
    }

    override fun getItemCount(): Int = courseList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvKodeMK: TextView = itemView.findViewById(R.id.tvKodeMK)
        val tvNamaMK: TextView = itemView.findViewById(R.id.tvNamaMK)
    }
}

