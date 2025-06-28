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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MeetingsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var meetingsList: ArrayList<Meetings>
    private lateinit var meetingsAdapter: MeetingsAdapter
    private lateinit var db: FirebaseFirestore
    private var listenerRegistration: ListenerRegistration? = null

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

        db = FirebaseFirestore.getInstance()

        recyclerView = view.findViewById(R.id.recyclerViewMeetings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        meetingsList = arrayListOf()
        meetingsAdapter = MeetingsAdapter(meetingsList)
        recyclerView.adapter = meetingsAdapter

        listenToMeetingsRealtime(courseId, studentId)
    }

    private fun listenToMeetingsRealtime(courseId: String?, studentId: String) {
        if (courseId == null) {
            Log.e("MeetingsFragment", "courseId is null")
            return
        }

        listenerRegistration?.remove() // Stop listener if already exists

        listenerRegistration = db.collection("course")
            .document(courseId)
            .collection("meetings")
            .orderBy("datetime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MeetingsFragment", "Error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val tempList = ArrayList<Meetings>()
                    var loadedCount = 0
                    val totalMeetings = snapshot.size()

                    if (totalMeetings == 0) {
                        updateRecyclerView(tempList)
                        return@addSnapshotListener
                    }

                    for (doc in snapshot.documents) {
                        val meeting = doc.toObject(Meetings::class.java)
                        val meetingId = doc.id
                        val sessionId = doc.getString("session_id")

                        if (meeting != null) {
                            if (sessionId.isNullOrBlank()) {
                                meeting.status = "Alpa"
                                tempList.add(meeting)
                                loadedCount++
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
                                .addSnapshotListener { attDoc, _ ->
                                    meeting.status = if (attDoc != null && attDoc.exists()) {
                                        attDoc.getString("status") ?: "Hadir"
                                    } else {
                                        "Alpa"
                                    }

                                    tempList.add(meeting)
                                    loadedCount++

                                    if (loadedCount == totalMeetings) {
                                        updateRecyclerView(tempList)
                                    }
                                }
                        } else {
                            loadedCount++
                            if (loadedCount == totalMeetings) {
                                updateRecyclerView(tempList)
                            }
                        }
                    }
                }
            }
    }

    private fun updateRecyclerView(data: List<Meetings>) {
        meetingsList.clear()
        meetingsList.addAll(data.sortedByDescending { it.datetime?.toDate() })
        recyclerView.post {
            meetingsAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }
}