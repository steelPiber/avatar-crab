package com.example.avatar_crab.presentation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.avatar_crab.R
import com.example.avatar_crab.presentation.RetrofitClient
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

    private lateinit var googleSignInClient: GoogleSignInClient  // GoogleSignInClient 객체 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)  // 레이아웃 설정

        // Google Sign-In 옵션 구성
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()  // 이메일 요청
            .requestIdToken(getString(R.string.default_web_client_id))  // ID 토큰 요청
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)  // GoogleSignInClient 초기화

        // 로그인 버튼 클릭 리스너 설정
        findViewById<com.google.android.gms.common.SignInButton>(R.id.sign_in_button).setOnClickListener {
            signIn()  // 로그인 함수 호출
        }
    }

    // Google Sign-In 프로세스 시작
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)  // 결과를 받기 위해 런처 실행
    }

    // Google Sign-In 결과 처리
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)  // 로그인 결과 처리 함수 호출
        } else {
            Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // Google Sign-In 결과 처리 함수
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                val email = account.email
                if (email != null) {
                    // 로그인 성공 후 UserInfoActivity로 이동
                    navigateToUserInfoActivity(email)
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google 로그인 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    // UserInfoActivity로 이동하여 사용자 정보를 수집하거나 업데이트하는 함수
    private fun navigateToUserInfoActivity(email: String) {
        val intent = Intent(this, UserInfoActivity::class.java)
        intent.putExtra("email", email)  // 이메일 정보를 UserInfoActivity로 전달
        startActivity(intent)
        finish()  // 현재 Activity 종료
    }

    // 로그아웃 기능 (필요한 경우에만 사용)
    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
