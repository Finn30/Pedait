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
        val studentId = arguments?.getString("nim") ?: return

        Log.d("MeetingsFragment", "courseId: $courseId, studentId: $studentId")

        db = FirebaseFirestore.getInstance()

        recyclerView = view.findViewById(R.id.recyclerViewMeetings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        meetingsList = arrayListOf()
        meetingsAdapter = MeetingsAdapter(meetingsList)

        // Pastikan adapter di-attach setelah view terbentuk
//        view.post {
//            recyclerView.adapter = meetingsAdapter
//        }
        Log.d("MeetingsFragment", "Setting adapter...")
        recyclerView.adapter = meetingsAdapter
        Log.d("MeetingsFragment", "Adapter set.")


        EventChangeListener(courseId, studentId)
    }


    private fun EventChangeListener(courseId: String?, studentId: String) {
        if (courseId == null) {
            Log.e("MeetingsFragment", "courseId is null")
            return
        }

        db = FirebaseFirestore.getInstance()

        db.collection("course")
            .document(courseId)
            .collection("meetings")
            .get()
            .addOnSuccessListener { meetingsSnapshot ->
                if (meetingsSnapshot.isEmpty) {
                    Log.d("MeetingsFragment", "No meetings found")
                    return@addOnSuccessListener
                }

                val tempList = ArrayList<Meetings>()
                var loadedCount = 0
                val totalMeetings = meetingsSnapshot.size()

                for (doc in meetingsSnapshot) {
                    val meeting = doc.toObject(Meetings::class.java)
                    val meetingId = doc.id
                    val sessionId = doc.getString("session_id")

                    if (sessionId == null) {
                        loadedCount++
                        Log.e("MeetingsFragment", "session_id is null for meeting $meetingId")
                        if (loadedCount == totalMeetings) {
                            updateRecyclerView(tempList)
                        }
                        continue
                    }

                    db.collection("course")
                        .document(courseId)
                        .collection("meetings")
                        .document(meetingId)
                        .collection("sessions")
                        .document(sessionId)
                        .collection("attendances")
                        .document(studentId)
                        .get()
                        .addOnSuccessListener { attendanceDoc ->
                            meeting.status = if (attendanceDoc.exists()) {
                                attendanceDoc.getString("status") ?: "belum hadir"
                            } else {
                                "belum hadir"
                            }

                            tempList.add(meeting)
                            loadedCount++

                            if (loadedCount == totalMeetings) {
                                updateRecyclerView(tempList)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("MeetingsFragment", "Error loading attendance: ${e.message}")
                            loadedCount++
                            if (loadedCount == totalMeetings) {
                                updateRecyclerView(tempList)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MeetingsFragment", "Error fetching meetings: ${e.message}")
            }
    }

    private fun updateRecyclerView(data: List<Meetings>) {
        meetingsList.clear()
        meetingsList.addAll(data)
        recyclerView.post {
            meetingsAdapter.notifyDataSetChanged()
        }
        Log.d("MeetingsFragment", "Updated RecyclerView with ${meetingsList.size} items")
    }

}
