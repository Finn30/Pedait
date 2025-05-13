package com.example.pedait

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etNim: EditText
    private lateinit var etNama: EditText
    private lateinit var etTanggalLahir: EditText
    private lateinit var etTempatLahir: EditText
    private lateinit var etProvinsiLahir: EditText
    private lateinit var etJenisKelamin: EditText
    private lateinit var etAgama: EditText
    private lateinit var etNoHp: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        etNim = view.findViewById(R.id.etNim)
        etNama = view.findViewById(R.id.etNama)
        etTanggalLahir = view.findViewById(R.id.etTanggalLahir)
        etTempatLahir = view.findViewById(R.id.etTempatLahir)
        etProvinsiLahir = view.findViewById(R.id.etProvinsiLahir)
        etJenisKelamin = view.findViewById(R.id.etJenisKelamin)
        etAgama = view.findViewById(R.id.etAgama)
        etNoHp = view.findViewById(R.id.etNoHp)
        etEmail = view.findViewById(R.id.etEmail)
        btnSave = view.findViewById(R.id.btnSave)
        btnLogout = view.findViewById(R.id.btnLogout)

        loadUserProfile()

        btnSave.setOnClickListener { saveUserProfile() }
        btnLogout.setOnClickListener {
            auth.signOut()
            requireActivity().finish()
        }

        return view
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etNim.setText(doc.getString("nim"))
                    etNama.setText(doc.getString("nama"))
                    etTanggalLahir.setText(doc.getString("tanggalLahir"))
                    etTempatLahir.setText(doc.getString("tempatLahir"))
                    etProvinsiLahir.setText(doc.getString("provinsiLahir"))
                    etJenisKelamin.setText(doc.getString("jenisKelamin"))
                    etAgama.setText(doc.getString("agama"))
                    etNoHp.setText(doc.getString("noHp"))
                    etEmail.setText(doc.getString("email"))
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        val userData = mapOf(
            "nim" to etNim.text.toString(),
            "nama" to etNama.text.toString(),
            "tanggalLahir" to etTanggalLahir.text.toString(),
            "tempatLahir" to etTempatLahir.text.toString(),
            "provinsiLahir" to etProvinsiLahir.text.toString(),
            "jenisKelamin" to etJenisKelamin.text.toString(),
            "agama" to etAgama.text.toString(),
            "noHp" to etNoHp.text.toString(),
            "email" to etEmail.text.toString()
        )

        firestore.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(context, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
            }
    }
}
