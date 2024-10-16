package com.example.avatar_crab.presentation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import piber.avatar_crab.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SplashActivity : AppCompatActivity() {

    class ServerStatusResponse(
        val server: String,
        val db: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 서버 상태 확인
        checkServerStatus()
    }

    private fun checkServerStatus() {
        val apiService = RetrofitClient.heartRateInstance
        // 서버 상태를 확인하기 위한 서비스 호출 준비
        apiService.serverCheck().enqueue(object : Callback<SplashActivity.ServerStatusResponse> {
            override fun onResponse(call: Call<ServerStatusResponse>, response: Response<ServerStatusResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.server == "on" && it.db == "on") {
                            // 서버가 정상적이면 메인 화면으로 이동
                            moveToMainActivity()
                        } else {
                            // 서버 또는 DB 상태에 문제가 있을 때 처리
                            Toast.makeText(this@SplashActivity, "서버 또는 데이터베이스 상태에 문제가 있습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@SplashActivity, "서버 응답 오류: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ServerStatusResponse>, t: Throwable) {
                Toast.makeText(this@SplashActivity, "서버 요청 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun moveToMainActivity() {
        // 일정 시간 후 MainActivity로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000) // 2초 후 메인 화면으로 이동
    }
}
