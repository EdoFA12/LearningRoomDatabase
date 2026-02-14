package com.learning.learningroomdatabase.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.learning.learningroomdatabase.R
import com.learning.learningroomdatabase.data.local.entity.AuditEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AuditMainActivity : AppCompatActivity() {
    private lateinit var auditViewModel: AuditViewModel
    private var currentLat: Double? = null
    private var currentLong: Double? = null
    private var currentAddress: String? = null
    private val binding: com.learning.learningroomdatabase.databinding.ActivityMainBinding by lazy {
        com.learning.learningroomdatabase.databinding.ActivityMainBinding.inflate(layoutInflater)
    }

    private val takePictureLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                checkLocationPermissionAndGet()
            }
        }

    private fun updateAddressTask(audit: com.learning.learningroomdatabase.data.local.entity.AuditEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val alamatBaru = getAddressFromLocation(audit.latitude ?: 0.0, audit.longitude ?: 0.0)

                if (alamatBaru != "${audit.latitude}, ${audit.longitude}") {
                    val updatedAudit = audit.copy(lokasi = alamatBaru)

                    auditViewModel.insert(updatedAudit)
                }
            } catch (e: Exception) {
                Log.e("AuditUpdate", "Gagal konversi otomatis: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val view = binding.root
        setContentView(view)

        auditViewModel = ViewModelProvider(this).get(AuditViewModel::class.java)

        // Setup RecyclerView
        val adapter = AuditAdapter { auditToDelete -> auditViewModel.delete(auditToDelete) }
        binding.rvAudits.layoutManager = LinearLayoutManager(this)
        binding.rvAudits.adapter = adapter

        // Observer Data Room
        auditViewModel.allAudits.observe(this) { listAudit ->
            if (listAudit.isEmpty()) {
                binding.tvStatus.text = getString(R.string.no_data)
                binding.tvStatus.visibility = View.VISIBLE
                binding.rvAudits.visibility = View.GONE
            } else {
                binding.tvStatus.visibility = View.GONE
                binding.rvAudits.visibility = View.VISIBLE
                adapter.submitList(listAudit)
            }

            if (isNetworkAvailable(this)) {
                listAudit.forEach { audit ->
                    if (audit.lokasi?.contains(",") == true && audit.latitude != null && audit.longitude != null) {

                        Log.d("Sync", "Mengonversi koordinat ID ${audit.id} menjadi alamat...")

                        updateAddressTask(audit)
                    }
                }
            }
        }

        binding.btnSaveData.setOnClickListener {
            saveDataToRoom()
        }
        setupLocationTouchListener()


    }

    private fun saveDataToRoom() {
        val namaPetugas = binding.etInputDataNama.text.toString().trim()
        val lokasiTemuan = binding.etInputDataLokasiTemuan.text.toString().trim()
        val statusPrioritasi = binding.etInputDataStatus.text.toString().toIntOrNull() ?: 0
        val alamatFinal = binding.etInputDataLokasi.text.toString().trim()

        if (namaPetugas.isEmpty() || alamatFinal.isEmpty()) {
            Toast.makeText(this, "Nama dan Lokasi (Geotag) harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val dataBaru = AuditEntity(
            nama_petugas = namaPetugas,
            lokasi_temuan = lokasiTemuan,
            status_prioritasi = statusPrioritasi,
            fotoPath = null,
            lokasi = alamatFinal,
            latitude = currentLat,
            longitude = currentLong
        )

        auditViewModel.insert(dataBaru)
        clearInputs()
    }

    private fun clearInputs() {
        binding.etInputDataNama.text?.clear()
        binding.etInputDataLokasiTemuan.text?.clear()
        binding.etInputDataLokasi.text?.clear()
        binding.etInputDataStatus.text?.clear()
        currentLat = null
        currentLong = null
        currentAddress = null
    }


    @Suppress("ClickableViewAccessibility")
    private fun setupLocationTouchListener() {
        binding.etInputDataLokasi.setOnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                val drawable = binding.etInputDataLokasi.compoundDrawables[DRAWABLE_RIGHT]
                if (drawable != null) {
                    if (event.rawX >= (binding.etInputDataLokasi.right - drawable.bounds.width())) {
                        checkCameraPermission()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }


    // --- PERMISSION SECTIONS ---

    private fun checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 102)
        } else {
            takePictureLauncher.launch(null)
        }
    }

    private fun checkLocationPermissionAndGet() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
        } else {
            getDeviceLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                102 -> takePictureLauncher.launch(null)
                101 -> getDeviceLocation()
            }
        } else {
            Toast.makeText(this, "Izin ditolak, fitur tidak bisa berjalan", Toast.LENGTH_SHORT).show()
        }
    }

    // --- CORE LOGIC: GPS & GEOCODER ---

    private fun getDeviceLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Gunakan High Accuracy agar setara Google Maps
        val priority = Priority.PRIORITY_HIGH_ACCURACY

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            binding.etInputDataLokasi.setText("Mencari lokasi presisi...")

            fusedLocationClient.getCurrentLocation(priority, null).addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLong = location.longitude

                    // Default jika offline: Tampilkan koordinat mentah
                    val rawCoords = "${location.latitude}, ${location.longitude}"
                    binding.etInputDataLokasi.setText(rawCoords)

                    // Coba konversi ke alamat jika online
                    if (isNetworkAvailable(this)) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val alamat = getAddressFromLocation(location.latitude, location.longitude)
                            withContext(Dispatchers.Main) {
                                binding.etInputDataLokasi.setText(alamat)
                                currentAddress = alamat
                            }
                        }
                    }
                } else {
                    binding.etInputDataLokasi.setText("Gagal mengunci GPS. Coba lagi.")
                }
            }
        }
    }

    private fun getAddressFromLocation(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                "$lat, $lng"
            }
        } catch (e: Exception) {
            "$lat, $lng"
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } ?: false
    }

}