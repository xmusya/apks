package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ServerEntity::class, SearchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun searchDao(): SearchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "transmify_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).serverDao()
                                if (dao.getServerCount() == 0) {
                                    // 1. Real Transmission Server (active by default, NOT demo mode)
                                    dao.insertServer(
                                        ServerEntity(
                                            name = "Transmission Server",
                                            host = "198.51.100.10",
                                            port = 9091,
                                            path = "/transmission/rpc",
                                            isActive = true,
                                            isDemoMode = false
                                        )
                                    )
                                    // 2. Demo Server available as option if needed
                                    dao.insertServer(
                                        ServerEntity(
                                            name = "Песочница (Демо-симуляция)",
                                            host = "127.0.0.1",
                                            port = 9091,
                                            path = "/transmission/rpc",
                                            isActive = false,
                                            isDemoMode = true
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
