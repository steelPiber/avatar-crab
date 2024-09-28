package com.example.avatar_crab.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface ExerciseRecordDao {
    @Insert
    suspend fun insert(record: ExerciseRecordEntity)

    @Query("SELECT * FROM exercise_records")
    suspend fun getAllRecords(): List<ExerciseRecordEntity>

    @Delete
    suspend fun delete(record: ExerciseRecordEntity)

    @Query("SELECT * FROM exercise_records WHERE date BETWEEN :start AND :end")
    suspend fun getExerciseRecordsBetween(start: Long, end: Long): List<ExerciseRecordEntity>

    @Update
    suspend fun update(record: ExerciseRecordEntity)
}
