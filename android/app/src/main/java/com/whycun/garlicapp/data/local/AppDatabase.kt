package com.whycun.garlicapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.whycun.garlicapp.data.local.dao.BatchDao
import com.whycun.garlicapp.data.local.dao.CalcSaveDao
import com.whycun.garlicapp.data.local.entity.Batch
import com.whycun.garlicapp.data.local.entity.CalcSave

@Database(entities = [Batch::class, CalcSave::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao
    abstract fun calcSaveDao(): CalcSaveDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context, AppDatabase::class.java, "garlic.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
