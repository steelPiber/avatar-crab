package piber.avatar_crab.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import piber.avatar_crab.presentation.HeartRateData
import piber.avatar_crab.presentation.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class HeartRateWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val bpm = inputData.getString("bpm")?.toIntOrNull()
        val tag = inputData.getString("tag")
        val timestamp = inputData.getString("timestamp")
        val email = inputData.getString("email")
        val latitude = inputData.getString("latitude")
        val longitude = inputData.getString("longitude")

        if (bpm == null || tag == null || timestamp == null || email == null) {
            Log.e("HeartRateWorker", "입력 데이터 누락")
            return Result.failure()
        }

        // latitude와 longitude를 포함한 데이터를 생성합니다.
        val data = HeartRateData(bpm, tag, timestamp, email, latitude, longitude)

        return try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.heartRateInstance.sendHeartRateData(data).execute()
            }
            if (response.isSuccessful) {
                Log.d("HeartRateWorker", "서버 응답 코드: ${response.code()}")
                Log.d("HeartRateWorker", "데이터 전송 성공")
                Result.success()
            } else {
                Log.e("HeartRateWorker", "서버 응답 오류: ${response.errorBody()?.string()}")
                Result.retry()
            }
        } catch (e: HttpException) {
            Log.e("HeartRateWorker", "데이터 전송 실패", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e("HeartRateWorker", "예외 발생", e)
            Result.retry()
        }
    }
}
