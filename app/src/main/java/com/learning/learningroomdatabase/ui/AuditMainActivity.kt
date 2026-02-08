package com.learning.learningroomdatabase.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.learning.learningroomdatabase.R
import com.learning.learningroomdatabase.data.local.entity.AuditEntity
import com.learning.learningroomdatabase.databinding.ActivityMainBinding

class AuditMainActivity : AppCompatActivity() {
    private lateinit var auditViewModel: AuditViewModel
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val view = binding.root
        setContentView(view)

        auditViewModel = ViewModelProvider(this).get(AuditViewModel::class.java)

        binding.btnSaveData.setOnClickListener {
            val dataBaru = AuditEntity(
                nama_petugas = "John Doe",
                lokasi_temuan = "Gudang A",
                status_prioritasi = 1
            )
            auditViewModel.insert(dataBaru)
        }

        auditViewModel.allAudits.observe(this){ listAudit ->
            Log.d("AuditMainActivity", "Number of audits: ${listAudit.size}")
            val nama =  listAudit.joinToString { it.nama_petugas }
            binding.tvStatus.text = " total data di Database: $nama"

        }


    }
}