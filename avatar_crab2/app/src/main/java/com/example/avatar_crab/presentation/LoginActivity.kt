package com.example.avatar_crab.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.avatar_crab.R
import com.example.avatar_crab.presentation.userinfo.UserInfoActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 최신 메서드를 사용하여 GoogleSignInOptions 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 최신 Google 계정 확인 방법 사용
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // 사용자가 이미 로그인되어 있으면 MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // 로그인되지 않은 경우 로그인 화면 표시
            setContentView(R.layout.activity_login)
            setupSignInButton()
        }
    }

    // 구글 로그인 버튼 설정
    private fun setupSignInButton() {
        findViewById<com.google.android.gms.common.SignInButton>(R.id.sign_in_button).setOnClickListener {
            signIn()
        }
    }

    // 구글 로그인 처리
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                val email = account.email
                if (email != null) {
                    checkIfUserInfoExists(email)
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google 로그인 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    // 서버에서 사용자 정보 확인
    private fun checkIfUserInfoExists(email: String) {
        RetrofitClient.recordsInstance.checkUserInfo(email).enqueue(object : Callback<Boolean> {
            override fun onResponse(call: Call<Boolean>, response: Response<Boolean>) {
                if (response.isSuccessful && response.body() == true) {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this@LoginActivity, UserInfoActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onFailure(call: Call<Boolean>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "서버 요청 실패: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
