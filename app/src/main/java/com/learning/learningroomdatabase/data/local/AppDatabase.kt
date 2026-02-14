package com.learning.learningroomdatabase.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.learning.learningroomdatabase.data.local.dao.AuditDao
import com.learning.learningroomdatabase.data.local.entity.AuditEntity

@Database(entities = [AuditEntity::class], version = 2, exportSchema =  false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun auditDao(): AuditDao

    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context:Context): AppDatabase{
            return INSTANCE ?: synchronized(this){
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "audit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = instance
                    instance
            }
        }
    }
}