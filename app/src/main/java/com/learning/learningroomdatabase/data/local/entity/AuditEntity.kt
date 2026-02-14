package com.learning.learningroomdatabase.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temuan_table")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama_petugas: String,
    val lokasi_temuan: String,
    val status_prioritasi: Int,
    val lokasi: String?,
    val fotoPath: String?,
    val latitude: Double?,
    val longitude: Double?
)
