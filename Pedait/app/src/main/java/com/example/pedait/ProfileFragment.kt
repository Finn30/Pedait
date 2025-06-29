package com.example.pedait

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var ivProfilePicture: ImageView
    private val IMAGE_PICK_CODE = 1001
    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val uid = auth.currentUser?.uid ?: return view

        ivProfilePicture = view.findViewById(R.id.ivProfilePicture)
        ivProfilePicture.setOnClickListener {
            pickImageFromGallery()
        }

        setupTextField(view.findViewById(R.id.rowNim), "NIM", "nim")
        setupTextField(view.findViewById(R.id.rowNama), "Nama", "nama")
        setupTextField(view.findViewById(R.id.rowTanggalLahir), "Tanggal Lahir", "tanggalLahir")
        setupTextField(view.findViewById(R.id.rowTempatLahir), "Tempat Lahir", "tempatLahir")
        setupTextField(view.findViewById(R.id.rowNoHp), "No. HP", "noHp")

        val genderOptions = arrayOf("Laki-laki", "Perempuan")
        val religionOptions = arrayOf("Islam", "Kristen", "Katolik", "Hindu", "Buddha", "Konghucu", "Lainnya")
        val provinceOptions = arrayOf(
            // Sumatera
            "Aceh",
            "Sumatera Utara",
            "Sumatera Barat",
            "Riau",
            "Kepulauan Riau",
            "Jambi",
            "Sumatera Selatan",
            "Bangka Belitung",
            "Bengkulu",
            "Lampung",

            // Jawa
            "DKI Jakarta",
            "Jawa Barat",
            "Banten",
            "Jawa Tengah",
            "DI Yogyakarta",
            "Jawa Timur",

            // Bali dan Nusa Tenggara
            "Bali",
            "Nusa Tenggara Barat",
            "Nusa Tenggara Timur",

            // Kalimantan
            "Kalimantan Barat",
            "Kalimantan Tengah",
            "Kalimantan Selatan",
            "Kalimantan Timur",
            "Kalimantan Utara",

            // Sulawesi
            "Sulawesi Utara",
            "Gorontalo",
            "Sulawesi Tengah",
            "Sulawesi Barat",
            "Sulawesi Selatan",
            "Sulawesi Tenggara",

            // Maluku dan Papua
            "Maluku",
            "Maluku Utara",
            "Papua",
            "Papua Barat",
            "Papua Selatan",
            "Papua Tengah",
            "Papua Pegunungan",
            "Papua Barat Daya"
        )

        setupTextField(view.findViewById(R.id.rowJenisKelamin), "Jenis Kelamin", "jenisKelamin", true, genderOptions)
        setupTextField(view.findViewById(R.id.rowAgama), "Agama", "agama", true, religionOptions)
        setupTextField(view.findViewById(R.id.rowProvinsiLahir), "Provinsi Lahir", "provinsiLahir", true, provinceOptions)

        val emailRow = view.findViewById<View>(R.id.rowEmail)
        val emailLabel = emailRow.findViewById<TextView>(R.id.label)
        val emailValue = emailRow.findViewById<TextView>(R.id.value)
        emailLabel.text = "Email"
        emailValue.text = auth.currentUser?.email ?: "-"

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    setValue(view, R.id.rowNim, doc.getString("nim"))
                    setValue(view, R.id.rowNama, doc.getString("nama"))
                    setValue(view, R.id.rowTanggalLahir, doc.getString("tanggalLahir"))
                    setValue(view, R.id.rowTempatLahir, doc.getString("tempatLahir"))
                    setValue(view, R.id.rowProvinsiLahir, doc.getString("provinsiLahir"))
                    setValue(view, R.id.rowJenisKelamin, doc.getString("jenisKelamin"))
                    setValue(view, R.id.rowAgama, doc.getString("agama"))
                    setValue(view, R.id.rowNoHp, doc.getString("noHp"))
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }

        view.findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            requireActivity().finish()
        }

        loadImagePath()?.let { path ->
            val file = File(path)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .into(ivProfilePicture)
            }
        }

        return view
    }

    private fun setupTextField(
        row: View,
        label: String,
        key: String,
        isDropdown: Boolean = false,
        options: Array<String>? = null
    ) {
        val labelView = row.findViewById<TextView>(R.id.label)
        val valueView = row.findViewById<TextView>(R.id.value)
        labelView.text = label

        row.setOnClickListener {
            if (isDropdown && options != null) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Pilih $label")
                    .setItems(options) { _, which ->
                        val selected = options[which]
                        valueView.text = selected
                        saveSingleField(key, selected, label)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            } else {
                val input = EditText(requireContext())
                input.setText(valueView.text.toString())
                input.setSelection(input.text.length)

                AlertDialog.Builder(requireContext())
                    .setTitle("Edit $label")
                    .setView(input)
                    .setPositiveButton("Simpan") { _, _ ->
                        val newValue = input.text.toString()
                        valueView.text = newValue
                        saveSingleField(key, newValue, label)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }
    }

    private fun saveSingleField(key: String, value: String, label: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update(key, value)
            .addOnSuccessListener {
                Toast.makeText(context, "$label diperbarui", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal memperbarui $label", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setValue(view: View, rowId: Int, value: String?) {
        val row = view.findViewById<View>(rowId)
        val valueView = row.findViewById<TextView>(R.id.value)
        valueView.text = value ?: "-"
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imageUri?.let { uri ->
                ivProfilePicture.setImageURI(uri)
                val savedPath = saveImageToInternalStorage(uri)
                savedPath?.let {
                    saveImagePath(it)
                    Toast.makeText(context, "Foto profil disimpan lokal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileName = "profile.jpg"
            val file = File(requireContext().filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveImagePath(path: String) {
        val prefs = requireContext().getSharedPreferences("profile_prefs", Activity.MODE_PRIVATE)
        prefs.edit().putString("profile_image_path", path).apply()
    }

    private fun loadImagePath(): String? {
        val prefs = requireContext().getSharedPreferences("profile_prefs", Activity.MODE_PRIVATE)
        return prefs.getString("profile_image_path", null)
    }
}