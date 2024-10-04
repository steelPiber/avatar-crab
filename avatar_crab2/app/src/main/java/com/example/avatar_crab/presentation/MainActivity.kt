package com.example.avatar_crab.presentation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.avatar_crab.MyApplication
import com.example.avatar_crab.R
import com.example.avatar_crab.presentation.dashboard.DashboardFragment
import com.example.avatar_crab.presentation.measure.MeasureFragment
import com.example.avatar_crab.presentation.monitor.HeartRateMonitor
import com.example.avatar_crab.presentation.settings.SettingsFragment
import com.example.avatar_crab.work.HeartRateWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.avatar_crab.presentation.userinfo.UserInfoActivity

class MainActivity : AppCompatActivity(), DataClient.OnDataChangedListener {

    // 위치 정보를 제공하는 FusedLocationProviderClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // MainViewModel을 뷰모델 패턴으로 사용
    private val viewModel: MainViewModel by viewModels {
        val application = application as MyApplication
        MainViewModelFactory(application.challengeRepository, application)
    }

    // HeartRateMonitor 클래스 초기화
    private lateinit var heartRateMonitor: HeartRateMonitor

    // GoogleSignInClient를 사용하여 Google 로그인 처리
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 로컬 저장소에서 저장된 사용자 정보를 확인
        val sharedPreferences = getSharedPreferences("AvatarCrabPrefs", Context.MODE_PRIVATE)
        val userInfoJson = sharedPreferences.getString("userInfo", null)

        Log.d("MainActivity", "userInfoJson: $userInfoJson") // 로그로 userInfoJson 값 확인

        // HeartRateMonitor 초기화
        heartRateMonitor = HeartRateMonitor(this)

        // 위치 제공자 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 권한을 확인하고, 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getLocation() // 위치 정보 획득
        }

        // Google Sign-In 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id)) // 웹 클라이언트 ID
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 이미 로그인된 계정이 있는지 확인
        val account = GoogleSignIn.getLastSignedInAccount(this)

        Log.d("MainActivity", "GoogleSignIn account: $account") // 로그로 GoogleSignIn 상태 확인

        if (account != null) {
            // 로그인된 계정이 있으면 ViewModel에 계정 정보 설정
            viewModel.setUserAccount(account)
            // 서버에서 해당 사용자의 유저 정보가 있는지 확인
            lifecycleScope.launch {
                checkUserOnServer(account.email) // 서버에서 사용자 정보 확인 후 UI 업데이트
            }
        } else {
            // 로그인되지 않았으면 로그인 프로세스 시작
            navigateToLogin()
        }

        // 웨어러블 데이터 리스너 등록
        Wearable.getDataClient(this).addListener(this)
    }

    // Google Sign-In 실행
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    // Google Sign-In 결과 처리
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Toast.makeText(this, "Google 로그인이 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 로그인 결과 처리
    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.setUserAccount(account)

                lifecycleScope.launch {
                    checkUserOnServer(account.email) // 서버에서 사용자 정보 확인
                }
            }
        } catch (e: ApiException) {
            Log.w("MainActivity", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this, "Google 로그인 실패: ${e.statusCode}, ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // 서버에서 해당 이메일의 사용자가 있는지 확인하는 함수
    private suspend fun checkUserOnServer(email: String?) {
        if (email != null) {
            val call = RetrofitClient.heartRateInstance.checkUserInfo(email)
            call.enqueue(object : Callback<Boolean> {
                override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                    if (response.isSuccessful && response.body() == true) {
                        // 사용자 정보가 존재하면 HomeFragment로 이동
                        navigateToHome()
                    } else {
                        // 사용자 정보가 없으면 UserInfoActivity로 이동
                        val intent = Intent(this@MainActivity, UserInfoActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onFailure(call: Call<Boolean>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "서버 확인 실패: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // HomeFragment로 이동하는 함수
    private fun navigateToHome() {
        val homeFragment = HomeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, homeFragment)
            .commit()
    }

    // LoginActivity로 이동하는 함수
    private fun navigateToLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
        finish() // MainActivity 종료
    }

    // 현재 위치 정보를 얻는 메서드
    private fun getLocation(): Location? {
        var currentLocation: Location? = null
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                currentLocation = location
            }
        }
        return currentLocation
    }

    // 웨어러블 기기에서 데이터가 변경되었을 때 호출되는 메서드
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == "/heart_rate") {
                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                    val bpm = dataMap.getString("bpm")?.toIntOrNull()
                    val tag = dataMap.getString("tag")
                    val timestampString = dataMap.getString("timestamp")

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val timestamp: Long? = try {
                        val date = dateFormat.parse(timestampString)
                        date?.time
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to parse timestamp: $timestampString", e)
                        null
                    }

                    Log.d("MainActivity", "데이터 수신: $bpm BPM, 태그: $tag at $timestamp")

                    if (timestamp != null && bpm != null && tag != null) {
                        viewModel.addHeartRateDataToBuffer(bpm, tag, timestamp)
                    }
                    if (bpm != null) {
                        // ViewModel에 실시간 심박수 업데이트
                        viewModel.updateRealTimeHeartRate(bpm)
                    }
                }
            }
        }
    }

    // 액티비티가 종료될 때 호출되는 메서드
    override fun onDestroy() {
        super.onDestroy()
        Wearable.getDataClient(this).removeListener(this) // 웨어러블 데이터 리스너 해제
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1 // 위치 권한 요청 코드
        const val RC_SIGN_IN = 9001 // Google Sign-In 요청 코드
    }
}