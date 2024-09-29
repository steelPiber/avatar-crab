package com.example.avatar_crab.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.avatar_crab.R
import com.example.avatar_crab.presentation.data.UserInfo
import com.example.avatar_crab.presentation.userinfo.UserInfoActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Google Sign-In 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id)) // 웹 클라이언트 ID 사용
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 이미 로그인된 계정이 있는지 확인
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // 이미 로그인된 경우 UserInfo 확인
            checkUserInfo(account.email)
        } else {
            // 처음 로그인인 경우 로그인 시도
            signIn()
        }
    }

    // Google Sign-In 시작
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    // 로그인 결과 처리
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Toast.makeText(this, "Google 로그인이 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 로그인 성공 후 결과 처리
    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                // UserInfo 확인
                checkUserInfo(account.email)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google 로그인 실패: ${e.statusCode}, ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // 서버 또는 로컬 저장소에서 UserInfo 확인하는 함수
    private fun checkUserInfo(email: String?) {
        if (email == null) {
            Toast.makeText(this, "이메일 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // SharedPreferences에서 UserInfo 확인
        val sharedPreferences = getSharedPreferences("AvatarCrabPrefs", Context.MODE_PRIVATE)
        val userInfoJson = sharedPreferences.getString("userInfo", null)

        if (userInfoJson != null) {
            // UserInfo 데이터가 존재하는 경우 HomeFragment로 이동
            navigateToMain()
        } else {
            // UserInfo 데이터가 없는 경우 UserInfoActivity로 이동
            val intent = Intent(this, UserInfoActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)
            finish() // LoginActivity 종료
        }
    }

    // MainActivity로 이동
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // LoginActivity 종료
    }
}


