package com.example.pedait

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val user = auth.currentUser

        val etName = view.findViewById<EditText>(R.id.etName)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val tvUid = view.findViewById<TextView>(R.id.tvUid)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        if (user != null) {
            etName.setText(user.displayName ?: "")
            tvEmail.text = "Email: ${user.email}"
            tvUid.text = "UID: ${user.uid}"
        }

        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            val email = user?.email ?: ""
            val uid = user?.uid ?: ""

            if (newName.isNotEmpty()) {
                // Update displayName di Firebase Auth
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()

                user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Simpan ke Firestore
                        val userData = hashMapOf(
                            "name" to newName,
                            "email" to email,
                            "uid" to uid
                        )

                        firestore.collection("users").document(uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Data saved to Firestore", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Failed to save to Firestore", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        Toast.makeText(context, "Failed to update Auth profile", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
