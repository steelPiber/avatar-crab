package com.example.avatar_crab.presentation

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.avatar_crab.data.AppDatabase
import com.example.avatar_crab.data.ActivityData
import com.example.avatar_crab.data.challenge.ChallengeRepository
import com.example.avatar_crab.data.exercise.ExerciseRecord
import com.example.avatar_crab.data.exercise.ExerciseRecordEntity
import com.example.avatar_crab.data.exercise.toExerciseRecordEntity
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await


class MainViewModel(
    private val challengeRepository: ChallengeRepository,
    application: Application
) : AndroidViewModel(application) {

    // MutableLiveData to hold heart rate
    private val _realTimeHeartRate = MutableLiveData<Int>()
    val realTimeHeartRate: LiveData<Int> get() = _realTimeHeartRate

    private val sharedPreferences = application.getSharedPreferences("AvatarCrabPrefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    private val activityDataDao = database.activityDataDao()
    private val exerciseRecordDao = database.exerciseRecordDao()

    private val _heartRate = MutableLiveData<String>()
    val heartRate: LiveData<String> get() = _heartRate

    private val _dataStatus = MutableLiveData<String>()
    val dataStatus: LiveData<String> get() = _dataStatus

    private val _activityTag = MutableLiveData<String>()
    val activityTag: LiveData<String> get() = _activityTag

    private val _activityData = MutableLiveData<List<ActivityData>>()
    val activityData: LiveData<List<ActivityData>> get() = _activityData

    private val _userAccount = MutableLiveData<GoogleSignInAccount?>()
    val userAccount: LiveData<GoogleSignInAccount?> get() = _userAccount

    private val _exerciseRecords = MutableLiveData<List<ExerciseRecordEntity>>()
    val exerciseRecords: LiveData<List<ExerciseRecordEntity>> get() = _exerciseRecords

    private val _isTracking = MutableLiveData<Boolean>()
    val isTracking: LiveData<Boolean> get() = _isTracking

    private val _userEmail = MutableLiveData<String?>()  // Allow nullable values initially
    val userEmail: LiveData<String?> get() = _userEmail  // Allow nullable values

    private val _dailyAverageHeartRate = MutableLiveData<Int>()
    val dailyAverageHeartRate: LiveData<Int> get() = _dailyAverageHeartRate

    // New LiveData properties for settings
    private val _isBradycardiaDetectionEnabled = MutableLiveData<Boolean>()
    val isBradycardiaDetectionEnabled: LiveData<Boolean> get() = _isBradycardiaDetectionEnabled

    private val _isTachycardiaDetectionEnabled = MutableLiveData<Boolean>()
    val isTachycardiaDetectionEnabled: LiveData<Boolean> get() = _isTachycardiaDetectionEnabled

    private val _isECGDetectionEnabled = MutableLiveData<Boolean>()
    val isECGDetectionEnabled: LiveData<Boolean> get() = _isECGDetectionEnabled

    private val _heartRateBuffer = mutableListOf<ActivityData>()
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)

    init {
        _isTracking.value = false
        loadSavedData()
        resetDataIfNewDay()
        loadActivityDataFromDb()
        loadExerciseRecords()
        calculateAndSaveDailyAverageHeartRate()
        startSendingHeartRateToServer()
    }

    //실시간심박 수 값
    fun updateRealTimeHeartRate(heartRate: Int) {
        _realTimeHeartRate.value = heartRate
    }
    // 수신된 심박수 데이터를 버퍼에 추가
    fun addHeartRateDataToBuffer(bpm: Int, tag: String, timestamp: Long) {
        val heartRateData = ActivityData(bpm = bpm, tag = tag, timestamp = timestamp)
        _heartRateBuffer.add(heartRateData)
    }
    // 10초 간격으로 심박수와 위치 데이터를 전송하는 메서드
    private fun startSendingHeartRateToServer() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (_heartRateBuffer.isNotEmpty()) {
                    val heartRateToSend = _heartRateBuffer.toList()
                    _heartRateBuffer.clear()

                    val location = getLocation()
                    val latitude = location?.latitude
                    val longitude = location?.longitude

                    heartRateToSend.forEach { data ->
                        val heartRateData = HeartRateData(
                            bpm = data.bpm.toString(),
                            tag = data.tag,
                            timestamp = data.timestamp.toString(),
                            email = getEmailFromPreferences(),
                            latitude = latitude?.toString(),
                            longitude = longitude?.toString()
                        )
                        sendHeartRateData(heartRateData)
                    }
                }
                delay(10000)
            }
        }
    }


    fun setBradycardiaDetectionEnabled(isEnabled: Boolean) {
        _isBradycardiaDetectionEnabled.value = isEnabled
    }

    fun setTachycardiaDetectionEnabled(isEnabled: Boolean) {
        _isTachycardiaDetectionEnabled.value = isEnabled
    }

    fun setECGDetectionEnabled(isEnabled: Boolean) {
        _isECGDetectionEnabled.value = isEnabled
    }

    //dashboard 리셋
    private fun resetDataIfNewDay() {
        viewModelScope.launch(Dispatchers.IO) {
            val lastSavedDay = sharedPreferences.getLong("lastSavedDay", -1L)
            val todayDate = getTodayDate()

            if (lastSavedDay != todayDate) {
                activityDataDao.deleteAll()
                sharedPreferences.edit().putLong("lastSavedDay", todayDate).apply()
            }
        }
    }

    fun setUserEmail(email: String?) {
        _userEmail.value = email  // Allow nullable values
    }

    fun startTracking() {
        _isTracking.value = true
    }

    fun stopTracking() {
        _isTracking.value = false
    }

    fun updateHeartRate(heartRate: String) {
        _heartRate.value = heartRate
        sharedPreferences.edit().putString("heartRate", heartRate).apply()
    }

    fun updateDataStatus(status: String) {
        _dataStatus.value = status
        sharedPreferences.edit().putString("dataStatus", status).apply()
        val tag = status.substringAfter("태그: ").trim()
        updateActivityTag(tag)
    }

    fun updateActivityTag(tag: String) {
        _activityTag.value = tag
        sharedPreferences.edit().putString("activityTag", tag).apply()
    }

    fun setUserAccount(account: GoogleSignInAccount?) {
        _userAccount.value = account
        sharedPreferences.edit().putString("userName", account?.displayName)
            .putString("userEmail", account?.email)
            .putString("userPhotoUrl", account?.photoUrl?.toString())
            .apply()
        _userEmail.value = account?.email  // Ensure email is updated here
    }

    fun addActivityData(data: ActivityData) {
        viewModelScope.launch(Dispatchers.IO) {
            activityDataDao.insert(data)
            loadActivityDataFromDb()
            calculateAndSaveDailyAverageHeartRate()
        }
    }

    // 심박수를 가져오는 메서드 (로컬 또는 센서에서 가져오기)
    private suspend fun getHeartRate(): Int? {
        return _heartRate.value?.toInt()
    }
    // 위치 가져오는 메서드
    private fun getLocation(): Location? {
        var currentLocation: Location? = null

        if (ContextCompat.checkSelfPermission(getApplication<Application>(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLocation = location
                        Log.d("MainActivity", "위치 가져오기 성공: ${location.latitude}, ${location.longitude}")
                    } else {
                        Log.e("MainActivity", "위치 정보를 가져올 수 없습니다.")
                    }
                }
                .addOnFailureListener {
                    Log.e("MainActivity", "위치 가져오기 실패", it)
                }
        }
        return currentLocation
    }



    // Retrofit을 사용하여 심박수 데이터를 서버로 전송하는 메서드
    // 심박수를 서버로 전송하는 메서드
    private fun sendHeartRateData(heartRateData: HeartRateData) {
        val call: Call<Void> = RetrofitClient.heartRateInstance.sendHeartRateData(heartRateData)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    Log.e("HeartRate", "데이터 전송 실패")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("HeartRate", "서버로 전송하는 도중 오류 발생: ${t.message}")
            }
        })
    }


    private fun loadSavedData() {
        _heartRate.value = sharedPreferences.getString("heartRate", "")
        _dataStatus.value = sharedPreferences.getString("dataStatus", "")
        _activityTag.value = sharedPreferences.getString("activityTag", "")
        val userName = sharedPreferences.getString("userName", null)
        val userEmail = sharedPreferences.getString("userEmail", null)
        val userPhotoUrl = sharedPreferences.getString("userPhotoUrl", null)
        if (userName != null && userEmail != null) {
            val account = GoogleSignInAccount.createDefault().apply {
                val clazz = this.javaClass
                try {
                    clazz.getDeclaredField("zzbe").apply {
                        isAccessible = true
                        set(this@apply, userName)
                    }
                    clazz.getDeclaredField("zzbq").apply {
                        isAccessible = true
                        set(this@apply, userEmail)
                    }
                    if (userPhotoUrl != null) {
                        clazz.getDeclaredField("zzbr").apply {
                            isAccessible = true
                            set(this@apply, Uri.parse(userPhotoUrl))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _userAccount.value = account
            _userEmail.value = userEmail  // Ensure email is updated here
        }
    }

    private fun loadActivityDataFromDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = activityDataDao.getAllActivityData()
            _activityData.postValue(data)
        }
    }

    private fun loadExerciseRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val records = exerciseRecordDao.getAllRecords()
            _exerciseRecords.postValue(records)
        }
    }

    fun saveExerciseRecord(record: ExerciseRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            val recordEntity = record.toExerciseRecordEntity()
            exerciseRecordDao.insert(recordEntity)
            loadExerciseRecords()
        }
    }

    fun deleteExerciseRecord(record: ExerciseRecordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            exerciseRecordDao.delete(record)
            loadExerciseRecords()
        }
    }
    // 하루 평균 심박수를 계산 -> chartViewModel 전달 (차트 업데이트)
    fun calculateAndSaveDailyAverageHeartRate() {
        viewModelScope.launch(Dispatchers.IO) {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val todaysData: List<ActivityData> = activityDataDao.getActivityDataBetween(todayStart, todayEnd)
            if (todaysData.isNotEmpty()) {
                val averageHeartRate = todaysData.map { data -> data.bpm }.average().toInt()
                _dailyAverageHeartRate.postValue(averageHeartRate)

                val dailyData = ActivityData(bpm = averageHeartRate, tag = "dailyAverage", timestamp = System.currentTimeMillis())
                activityDataDao.insert(dailyData)
            }
        }
    }

    fun updateChallengesWithActivityData(activityData: List<ActivityData>, exerciseRecords: List<ExerciseRecordEntity>) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            challengeRepository.updateChallengesWithActivityData(activityData, exerciseRecords, calendar)
        }
    }

    private fun getTodayDate(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun loadTodayActivityData() {
        viewModelScope.launch {
            val todayData = withContext(Dispatchers.IO) {
                val startOfDay = getStartOfDayTimestamp()
                val endOfDay = getEndOfDayTimestamp()
                activityDataDao.getActivityDataBetween(startOfDay, endOfDay)
            }
            _activityData.value = todayData
        }
    }

    fun saveHeartRateData(heartRate: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val heartRateData = ActivityData(
                bpm = heartRate,
                tag = "heartRate",
                timestamp = System.currentTimeMillis()
            )
            activityDataDao.insert(heartRateData)
            loadHeartRateDataFromDb()
        }
    }

    private fun loadHeartRateDataFromDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val data = activityDataDao.getAllActivityData()
            _activityData.postValue(data)
        }
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun fetchUserRecords(email: String) {
        val call: Call<List<ExerciseRecordEntity>> = RetrofitClient.recordsInstance.getRecords(email)
        call.enqueue(object : Callback<List<ExerciseRecordEntity>> {
            override fun onResponse(call: Call<List<ExerciseRecordEntity>>, response: Response<List<ExerciseRecordEntity>>) {
                if (response.isSuccessful) {
                    _exerciseRecords.postValue(response.body())
                } else {
                    // Handle error
                }
            }

            override fun onFailure(call: Call<List<ExerciseRecordEntity>>, t: Throwable) {
                // Handle failure
            }
        })
    }

    // 이메일 가져오기
    private fun getEmailFromPreferences(): String? {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("AvatarCrabPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userEmail", null)
    }
}
