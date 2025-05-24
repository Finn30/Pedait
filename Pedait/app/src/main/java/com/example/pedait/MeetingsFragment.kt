package com.example.pedait

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class MeetingsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var meetingsList: ArrayList<Meetings>
    private lateinit var meetingsAdapter: MeetingsAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_meetings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val courseId = arguments?.getString("courseId")
        Log.d("MeetingsFragment", "courseId: $courseId")


        recyclerView = view.findViewById(R.id.recyclerViewMeetings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        meetingsList = arrayListOf()
        meetingsAdapter = MeetingsAdapter(meetingsList)
        recyclerView.adapter = meetingsAdapter

//        val courseId = arguments?.getString("courseId") ?: return
//        if (courseId == null) {
//            Log.e("MeetingsFragment", "courseId is null!")
//            return
//        }
        EventChangeListener()


    }

    private fun EventChangeListener(){
        db = FirebaseFirestore.getInstance()
        val courseId = arguments?.getString("courseId") ?: return
        db.collection("course")
            .document(courseId)
            .collection("meetings")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("Firestore", "Error: ${error.message}")
                    return@addSnapshotListener
                }

                meetingsList.clear()
                for (doc in value!!) {
                    val meeting = doc.toObject(Meetings::class.java)
                    meetingsList.add(meeting)
                }
                meetingsAdapter.notifyDataSetChanged()
            }
    }
}

