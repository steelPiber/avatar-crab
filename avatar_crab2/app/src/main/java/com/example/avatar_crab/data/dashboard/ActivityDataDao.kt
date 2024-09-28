package com.example.avatar_crab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActivityDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activityData: ActivityData)

    @Query("SELECT * FROM activity_data")
    suspend fun getAllActivityData(): List<ActivityData>

    @Query("DELETE FROM activity_data")
    suspend fun deleteAll()

    @Query("SELECT * FROM activity_data WHERE timestamp BETWEEN :start AND :end")
    fun getActivityDataBetween(start: Long, end: Long): List<ActivityData>

    @Query("DELETE FROM activity_data WHERE timestamp < :timestamp")
    suspend fun deleteOldData(timestamp: Long)
}
