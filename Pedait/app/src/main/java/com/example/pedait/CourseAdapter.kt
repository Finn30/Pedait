package com.example.pedait

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CourseAdapter(private val courseList: ArrayList<Course>) : RecyclerView.Adapter<CourseAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CourseAdapter.ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CourseAdapter.ViewHolder, position: Int) {
        val course: Course = courseList[position]
        holder.tvKodeMK.text = course.kodeMK
        holder.tvNamaMK.text = course.namaMK
    }

    override fun getItemCount(): Int {
        return courseList.size
    }

    public class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvKodeMK = itemView.findViewById<TextView>(R.id.tvKodeMK)
        val tvNamaMK = itemView.findViewById<TextView>(R.id.tvNamaMK)

    }

}