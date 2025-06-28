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
    private var studentId: String? = null
    private var courseId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_meetings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        courseId = arguments?.getString("courseId")
        studentId = arguments?.getString("nim")

        if (courseId == null || studentId == null) {
            Log.e("MeetingsFragment", "courseId or studentId is null")
            return
        }

        db = FirebaseFirestore.getInstance()

        recyclerView = view.findViewById(R.id.recyclerViewMeetings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)

        meetingsList = arrayListOf()
        meetingsAdapter = MeetingsAdapter(meetingsList)
        recyclerView.adapter = meetingsAdapter

        listenToMeetingsRealtime()
    }

    private val attendanceListeners = mutableMapOf<String, ListenerRegistration>()

    private fun listenToMeetingsRealtime() {
        listenerRegistration?.remove()
        attendanceListeners.values.forEach { it.remove() }
        attendanceListeners.clear()

        listenerRegistration = db.collection("course")
            .document(courseId!!)
            .collection("meetings")
            .orderBy("datetime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MeetingsFragment", "Error listening to meetings: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    updateRecyclerView(emptyList())
                    return@addSnapshotListener
                }

                val tempList = mutableListOf<Meetings>()
                val meetingsDocs = snapshot.documents
                var loadedCount = 0

                for (doc in meetingsDocs) {
                    val meeting = doc.toObject(Meetings::class.java)
                    val meetingId = doc.id
                    val sessionId = doc.getString("session_id")

                    if (meeting != null) {
                        if (sessionId.isNullOrBlank()) {
                            meeting.status = "Alpa"
                            tempList.add(meeting)
                            loadedCount++
                            if (loadedCount == meetingsDocs.size) {
                                updateRecyclerView(tempList)
                            }
                            continue
                        }

                        // Tambahkan listener untuk attendance studentId di tiap session
                        val attendanceRef = db.collection("course").document(courseId!!)
                            .collection("meetings").document(meetingId)
                            .collection("sessions").document(sessionId)
                            .collection("attendances").document(studentId!!)

                        val listener = attendanceRef.addSnapshotListener { attDoc, _ ->
                            meeting.status = if (attDoc != null && attDoc.exists()) {
                                attDoc.getString("status") ?: "Hadir"
                            } else {
                                "Alpa"
                            }

                            // Update atau replace di tempList
                            val index = tempList.indexOfFirst { it.datetime == meeting.datetime }
                            if (index >= 0) {
                                tempList[index] = meeting
                            } else {
                                tempList.add(meeting)
                            }

                            updateRecyclerView(tempList)
                        }

                        attendanceListeners["$meetingId-$sessionId"] = listener
                    } else {
                        loadedCount++
                        if (loadedCount == meetingsDocs.size) {
                            updateRecyclerView(tempList)
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
        attendanceListeners.values.forEach { it.remove() }
        attendanceListeners.clear()
    }

    companion object {
        fun newInstance(courseId: String, nim: String): MeetingsFragment {
            return MeetingsFragment().apply {
                arguments = Bundle().apply {
                    putString("courseId", courseId)
                    putString("nim", nim)
                }
            }
        }
    }
}