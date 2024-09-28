package com.example.avatar_crab.presentation.dashboard

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.avatar_crab.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class ClearDataWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val activityDataDao = AppDatabase.getDatabase(applicationContext).activityDataDao()
            val currentTime = Calendar.getInstance().timeInMillis

            activityDataDao.deleteOldData(currentTime)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
