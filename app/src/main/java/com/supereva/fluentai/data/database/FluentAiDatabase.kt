package com.supereva.fluentai.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.supereva.fluentai.data.database.dao.SessionDao
import com.supereva.fluentai.data.database.entity.SessionEntity

@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class FluentAiDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
