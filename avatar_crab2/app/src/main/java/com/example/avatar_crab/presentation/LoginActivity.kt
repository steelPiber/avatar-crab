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

        // 서버에서 사용자 정보 존재 여부 확인
        RetrofitClient.heartRateInstance.checkUserInfo(email).enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful && response.body() == true) {
                    // 사용자 정보가 이미 존재하는 경우 HomeFragment로 이동
                    navigateToHome()  // HomeFragment로 이동
                } else {
                    // 사용자 정보가 없는 경우 UserInfoActivity로 이동
                    val intent = Intent(this@LoginActivity, UserInfoActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()  // LoginActivity 종료
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "서버 요청 실패: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // MainActivity의 HomeFragment로 이동
    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("navigateTo", "HomeFragment")  // HomeFragment로 바로 이동하도록 설정
        startActivity(intent)
        finish()  // LoginActivity 종료
    }
}


