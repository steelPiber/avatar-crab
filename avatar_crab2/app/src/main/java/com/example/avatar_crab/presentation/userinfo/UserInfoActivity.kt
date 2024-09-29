package com.example.avatar_crab.presentation.userinfo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.avatar_crab.R
import com.example.avatar_crab.presentation.MainActivity
import com.example.avatar_crab.presentation.RetrofitClient
import com.example.avatar_crab.presentation.data.UserInfo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserInfoActivity : AppCompatActivity() {
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        // Intent로 전달된 이메일 확인
        email = intent.getStringExtra("email") ?: ""

        val submitButton = findViewById<Button>(R.id.submit_button)
        submitButton.setOnClickListener {
            val name = findViewById<EditText>(R.id.name_input).text.toString()

            // 성별 선택
            val genderGroup = findViewById<RadioGroup>(R.id.gender_group)
            val selectedGenderId = genderGroup.checkedRadioButtonId
            val gender = if (selectedGenderId != -1) {
                findViewById<RadioButton>(selectedGenderId).text.toString()
            } else {
                ""
            }

            val age = findViewById<EditText>(R.id.age_input).text.toString().toIntOrNull()
            val height = findViewById<EditText>(R.id.height_input).text.toString().toDoubleOrNull()
            val weight = findViewById<EditText>(R.id.weight_input).text.toString().toDoubleOrNull()

            if (name.isNotEmpty() && gender.isNotEmpty() && age != null && height != null && weight != null) {
                val userInfo = UserInfo(email, name, gender, age, height, weight)
                Log.d("UserInfoActivity", "Sending UserInfo: $userInfo")

                // 서버에 UserInfo 저장
                sendUserInfoToServer(userInfo)
            } else {
                Toast.makeText(this, "모든 필드를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 서버에 UserInfo를 저장하는 함수
    private fun sendUserInfoToServer(userInfo: UserInfo) {
        RetrofitClient.heartRateInstance.sendUserInfo(userInfo).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // 저장 성공 시 MainActivity로 이동
                    val intent = Intent(this@UserInfoActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish() // UserInfoActivity 종료
                } else {
                    Toast.makeText(this@UserInfoActivity, "저장 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("UserInfoActivity", "서버 요청 실패: ${t.message}", t)
                Toast.makeText(this@UserInfoActivity, "서버 요청 실패: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
