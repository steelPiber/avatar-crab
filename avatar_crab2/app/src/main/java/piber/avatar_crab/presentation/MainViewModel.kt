package piber.avatar_crab.presentation

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import piber.avatar_crab.data.AppDatabase
import piber.avatar_crab.data.ActivityData
import piber.avatar_crab.data.challenge.ChallengeRepository
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
import piber.avatar_crab.presentation.data.UserInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay

class MainViewModel(
    private val challengeRepository: ChallengeRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _userInfo = MutableLiveData<UserInfo>()
    val userInfo: LiveData<UserInfo> get() = _userInfo

    // MutableLiveData to hold heart rate
    private val _realTimeHeartRate = MutableLiveData<Int>()
    val realTimeHeartRate: LiveData<Int> get() = _realTimeHeartRate

    private val sharedPreferences = application.getSharedPreferences("AvatarCrabPrefs", Context.MODE_PRIVATE)

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
        startSendingHeartRateToServer()
    }

    // 실시간 심박수 값
    fun updateRealTimeHeartRate(heartRate: Int) {
        _realTimeHeartRate.value = heartRate
        sendHeartRateData(HeartRateData(
            bpm = heartRate,
            tag = "rest",  // You may change the tag accordingly
            timestamp = System.currentTimeMillis().toString(),
            email = getEmailFromPreferences(),
            latitude = null,
            longitude = null
        ))
    }

    // 수신된 심박수 데이터를 버퍼에 추가
    fun addHeartRateDataToBuffer(bpm: Int, tag: String, timestamp: Long) {
        val heartRateData = ActivityData(bpm = bpm, tag = tag, timestamp = timestamp)
        _heartRateBuffer.add(heartRateData)
    }

    private fun startSendingHeartRateToServer() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (_heartRateBuffer.isNotEmpty()) {
                    val heartRateToSend = _heartRateBuffer.toList()
                    _heartRateBuffer.clear()

                    val location = getLocation() // 위치 가져오기
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude

                        // bpm 값이 0이 아닌 데이터만 필터링
                        val filteredHeartRateData = heartRateToSend.filter { it.bpm > 0 }

                        filteredHeartRateData.forEach { data ->
                            val heartRateData = HeartRateData(
                                bpm = data.bpm,
                                tag = data.tag,
                                timestamp = data.timestamp.toString(),
                                email = getEmailFromPreferences(),
                                latitude = location?.latitude?.toString(),
                                longitude = location?.longitude?.toString()
                            )
                            sendHeartRateData(heartRateData) // 서버로 데이터 전송
                        }
                    } else {
                        Log.e("MainViewModel", "위치를 가져올 수 없어 데이터를 전송하지 않습니다.")
                    }
                }
                delay(10000) // 10초 대기
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
        _userEmail.value = account?.email
        account?.let {
            sharedPreferences.edit().apply {
                putString("userName", it.displayName)
                putString("userEmail", it.email)
                putString("userPhotoUrl", it.photoUrl?.toString())
                apply()
            }
        }
    }

    // 심박수를 가져오는 메서드 (로컬 또는 센서에서 가져오기)
    private suspend fun getHeartRate(): Int? {
        return _heartRate.value?.toInt()
    }

    // 위치 가져오는 메서드
    private suspend fun getLocation(): Location? = withContext(Dispatchers.IO) {
        var currentLocation: Location? = null

        if (ContextCompat.checkSelfPermission(getApplication<Application>(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLocation = location
                        Log.d("MainViewModel", "위치 가져오기 성공: ${location.latitude}, ${location.longitude}")
                    } else {
                        Log.e("MainViewModel", "위치 정보를 가져올 수 없습니다.")
                    }
                }
                .addOnFailureListener {
                    Log.e("MainViewModel", "위치 가져오기 실패", it)
                }
        }
        delay(1000) // 위치 가져오는 시간이 있으므로 대기 (필요시 설정)
        currentLocation
    }

    // Retrofit을 사용하여 심박수 데이터를 서버로 전송하는 메서드
    private fun sendHeartRateData(heartRateData: HeartRateData) {
        // 전송할 데이터를 로그로 출력하여 확인
        Log.d("HeartRate", "전송 데이터: BPM=${heartRateData.bpm}, Email=${heartRateData.email}, Tag=${heartRateData.tag}, Timestamp=${heartRateData.timestamp}, Latitude=${heartRateData.latitude}, Longitude=${heartRateData.longitude}")

        val call: Call<Void> = RetrofitClient.heartRateInstance.sendHeartRateData(heartRateData)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (!response.isSuccessful) {
                    Log.e("HeartRate", "데이터 전송 실패: ${response.code()} ${response.message()}")
                } else {
                    Log.d("HeartRate", "데이터 전송 성공")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("HeartRate", "서버로 전송하는 도중 오류 발생: ${t.message}")
            }
        })
    }

    private var isDataLoaded = false  // 데이터가 이미 로드되었는지 확인하는 변수

    private fun loadSavedData() {
        if (isDataLoaded) {
            Log.d("MainViewModel", "Data already loaded, skipping")
            return
        }

        _heartRate.value = sharedPreferences.getString("heartRate", "")
        _dataStatus.value = sharedPreferences.getString("dataStatus", "")
        _activityTag.value = sharedPreferences.getString("activityTag", "")

        val userName = sharedPreferences.getString("userName", null)
        val userEmail = sharedPreferences.getString("userEmail", null)
        val userPhotoUrl = sharedPreferences.getString("userPhotoUrl", null)

        Log.d("MainViewModel", "loadSavedData - userName: $userName, userEmail: $userEmail, userPhotoUrl: $userPhotoUrl")

        if (userName != null && userEmail != null) {
            _userEmail.value = userEmail

            _userAccount.value = GoogleSignInAccount.createDefault().apply {
                val emailField = this.email ?: userEmail
                val displayNameField = this.displayName ?: userName
                _userEmail.value = emailField

                val photoUri = this.photoUrl ?: Uri.parse(userPhotoUrl)
                Log.d("MainViewModel", "loadSavedData - photoUri: $photoUri")

                _userAccount.value = this.apply {
                    Log.d("MainViewModel", "loadSavedData - userAccount updated")
                }
            }
        } else {
            Log.e("MainViewModel", "loadSavedData - Missing userName or userEmail in SharedPreferences")
        }

        isDataLoaded = true  // 데이터가 로드되었음을 기록
    }

    // 이메일 가져오기
    private fun getEmailFromPreferences(): String? {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("AvatarCrabPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userEmail", null)
    }

    fun fetchUserInfo(email: String) {
        val call: Call<UserInfo> = RetrofitClient.heartRateInstance.getUserInfo(email)
        call.enqueue(object : Callback<UserInfo> {
            override fun onResponse(call: Call<UserInfo>, response: Response<UserInfo>) {
                if (response.isSuccessful) {
                    // 서버로부터 유저 정보를 성공적으로 받았을 경우
                    response.body()?.let { userInfo ->
                        _userInfo.postValue(userInfo) // LiveData 업데이트
                    }
                } else {
                    Log.e("UserInfo", "서버 응답 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserInfo>, t: Throwable) {
                Log.e("UserInfo", "서버 요청 실패: ${t.message}")
            }
        })
    }

    fun updateUserInfo(updatedUserInfo: UserInfo) {
        val call: Call<Void> = RetrofitClient.heartRateInstance.updateUserInfo(updatedUserInfo)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("UserInfo", "User info updated successfully")
                    _userInfo.postValue(updatedUserInfo) // Update LiveData
                } else {
                    Log.e("UserInfo", "Failed to update user info: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("UserInfo", "Error updating user info: ${t.message}")
            }
        })
    }
}
