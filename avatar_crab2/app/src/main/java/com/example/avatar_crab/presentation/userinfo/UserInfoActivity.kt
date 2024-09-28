package com.example.avatar_crab.presentation.userinfo

import android.content.Intent
import android.os.Bundle
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserInfoActivity : AppCompatActivity() {
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        email = intent.getStringExtra("email") ?: ""

        // 신체 정보 입력 폼 제출 시
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

            val age = findViewById<EditText>(R.id.age_input).text.toString().toInt()
            val height = findViewById<EditText>(R.id.height_input).text.toString().toDouble()
            val weight = findViewById<EditText>(R.id.weight_input).text.toString().toDouble()

            if (gender.isNotEmpty()) {
                val userInfo = UserInfo(email, name, gender, age, height, weight)
                // 서버에 신체 정보 전송
                sendUserInfoToServer(userInfo)
            } else {
                Toast.makeText(this, "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 서버에 신체 정보를 저장하는 함수
    private fun sendUserInfoToServer(userInfo: UserInfo) {
        RetrofitClient.heartRateInstance.sendUserInfo(userInfo).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // 데이터 저장 성공 시 데이터 확인을 위해 다시 조회
                    checkSavedUserInfo()
                } else {
                    Toast.makeText(this@UserInfoActivity, "저장 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@UserInfoActivity, "서버 요청 실패: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 서버에서 해당 이메일의 신체 정보를 조회하는 함수
    private fun checkSavedUserInfo() {
        RetrofitClient.heartRateInstance.getUserInfo(email).enqueue(object : Callback<UserInfo> {
            override fun onResponse(call: Call<UserInfo>, response: Response<UserInfo>) {
                if (response.isSuccessful && response.body() != null) {
                    val savedUserInfo = response.body()
                    if (savedUserInfo != null && validateUserInfo(savedUserInfo)) {
                        // 조회한 데이터가 입력한 데이터와 일치하면 메인 화면으로 이동
                        Toast.makeText(this@UserInfoActivity, "신체 정보가 정확히 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@UserInfoActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@UserInfoActivity, "저장된 정보가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@UserInfoActivity, "저장된 정보를 조회할 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UserInfo>, t: Throwable) {
                Toast.makeText(this@UserInfoActivity, "서버 요청 실패: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 조회한 정보가 입력한 정보와 일치하는지 확인하는 함수
    private fun validateUserInfo(savedUserInfo: UserInfo): Boolean {
        val name = findViewById<EditText>(R.id.name_input).text.toString()
        val gender = findViewById<RadioButton>(findViewById<RadioGroup>(R.id.gender_group).checkedRadioButtonId).text.toString()
        val age = findViewById<EditText>(R.id.age_input).text.toString().toInt()
        val height = findViewById<EditText>(R.id.height_input).text.toString().toDouble()
        val weight = findViewById<EditText>(R.id.weight_input).text.toString().toDouble()

        return savedUserInfo.name == name &&
                savedUserInfo.gender == gender &&
                savedUserInfo.age == age &&
                savedUserInfo.height == height &&
                savedUserInfo.weight == weight
    }
}
