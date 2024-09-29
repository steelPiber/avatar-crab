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

class MainActivity : AppCompatActivity(), DataClient.OnDataChangedListener {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: MainViewModel by viewModels {
        val application = application as MyApplication
        MainViewModelFactory(application.challengeRepository, application)
    }

    //heartrateMonitor클래스 가져 옴
    private lateinit var heartRateMonitor: HeartRateMonitor
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 로컬 저장소에서 UserInfo 데이터 확인
        val sharedPreferences = getSharedPreferences("AvatarCrabPrefs", Context.MODE_PRIVATE)
        val userInfoJson = sharedPreferences.getString("userInfo", null)

        if (userInfoJson != null) {
            // UserInfo 데이터가 있을 경우 HomeFragment로 이동
            val intent = Intent(this, HomeFragment::class.java)
            startActivity(intent)
        } else {
            // UserInfo 데이터가 없을 경우 LoginActivity로 이동
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        finish() // MainActivity 종료


        // HeartRateMonitor 초기화
        heartRateMonitor = HeartRateMonitor(this)

        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getLocation()
        }

        // Google Sign-In 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            viewModel.setUserAccount(account)
            lifecycleScope.launch {
                updateUIWithUserAccount(account)
            }
        } else {
            signIn()
        }

        // 워치 데이터 리스너 등록
        Wearable.getDataClient(this).addListener(this)
    }

    private fun navigateToHome() {
        val homeFragment = HomeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, homeFragment)
            .commit()
    }

    private fun navigateToLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
        finish() // 현재 액티비티 종료
    }

    // 위치를 가져오는 메서드
    private fun getLocation(): Location? {
        var currentLocation: Location? = null
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location
                }
            }
        }
        return currentLocation
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("activity_reminder_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun openFragment(fragment: Fragment, tag: String? = null) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment, tag)
        transaction.addToBackStack(null)
        transaction.commitAllowingStateLoss()
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Toast.makeText(this, "Google 로그인이 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.setUserAccount(account)
                lifecycleScope.launch {
                    updateUIWithUserAccount(account)
                }
            }
        } catch (e: ApiException) {
            Log.w("MainActivity", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this, "Google 로그인 실패: ${e.statusCode}, ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun updateUIWithUserAccount(account: GoogleSignInAccount) {
        val homeFragment = supportFragmentManager.findFragmentByTag("HomeFragment") as? HomeFragment
        homeFragment?.updateUserProfile(account)
        viewModel.setUserEmail(account.email) // 이메일 저장
    }

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
                        // Update ViewModel with new heart rate
                        viewModel.updateRealTimeHeartRate(bpm)
                    }
                }
            }
        }
    }

    private fun sendDataToServer(
        bpm: String?,
        tag: String?,
        timestamp: String?,
        email: String?,
        idToken: String,
    ) {
        // 위치값을 추가로 가져와서 넘김
        val location = getLocation()
        val latitude = location?.latitude
        val longitude = location?.longitude

        val inputData = workDataOf(
            "bpm" to bpm,
            "tag" to tag,
            "timestamp" to timestamp,
            "email" to email,
            "idToken" to idToken,
            "latitude" to latitude,   // 위도 추가
            "longitude" to longitude  // 경도 추가
        )

        val heartRateWorkRequest = OneTimeWorkRequestBuilder<HeartRateWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(this).enqueue(heartRateWorkRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getDataClient(this).removeListener(this)
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1 // 상수 정의
        const val RC_SIGN_IN = 9001
    }
}
