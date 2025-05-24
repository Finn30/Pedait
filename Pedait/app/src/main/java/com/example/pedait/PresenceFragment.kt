package com.example.pedait

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class PresenceFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var courseArrayList: ArrayList<Course>
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_presence, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        courseArrayList = arrayListOf()
        courseAdapter = CourseAdapter(courseArrayList) { course ->
            val bundle = Bundle()
            bundle.putString("courseId", course.id)

            val fragment = MeetingsFragment()
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.map_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = courseAdapter

        EventChangeListener()
    }

    private fun EventChangeListener() {
        db = FirebaseFirestore.getInstance()
        db.collection("course").addSnapshotListener { value, error ->
            if (error != null) {
                Log.e("Firestore Error", error.message.toString())
                return@addSnapshotListener
            }

            for (dc in value?.documentChanges!!) {
                if (dc.type == DocumentChange.Type.ADDED) {
                    val course = dc.document.toObject(Course::class.java)
                    course.id = dc.document.id
//                    courseArrayList.add(dc.document.toObject(Course::class.java))
                    courseArrayList.add(course)
                }
            }
            courseAdapter.notifyDataSetChanged()
        }
    }
}