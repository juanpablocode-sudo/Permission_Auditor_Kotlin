package com.juanpablo.permissionauditor

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodas(apps: List<AppInfo>)

    @Query("SELECT * FROM apps ORDER BY score DESC")
    suspend fun obtenerTodasOrdenadas(): List<AppInfo>

    @Query("DELETE FROM apps")
    suspend fun borrarTodas()
}