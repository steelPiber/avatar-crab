package com.example.avatar_crab.presentation.userinfo

import com.example.avatar_crab.presentation.MainActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.avatar_crab.presentation.RetrofitClient
import com.example.avatar_crab.presentation.data.UserInfo
import com.google.firebase.auth.FirebaseAuth
import piber.avatar_crab.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserInfoActivity : AppCompatActivity() {
    private lateinit var email: String
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        // FirebaseAuth 초기화
        auth = FirebaseAuth.getInstance()

        // 이메일 정보를 Intent로부터 가져옴
        email = intent.getStringExtra("email") ?: ""

        // 신체 정보 입력 폼 제출 시
        val submitButton = findViewById<Button>(R.id.submit_button)
        submitButton.setOnClickListener {
            val emailInput = findViewById<EditText>(R.id.email_input).text.toString()
            val currentUser = auth.currentUser

            if (currentUser == null || currentUser.email != emailInput) {
                Toast.makeText(this, "입력한 이메일과 현재 사용자 이메일이 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveUserInfo()
        }
    }

    // 서버에 신체 정보를 저장하는 함수
    private fun saveUserInfo() {
        val name = findViewById<EditText>(R.id.name_input).text.toString()
        val genderGroup = findViewById<RadioGroup>(R.id.gender_group)
        val selectedGenderId = genderGroup.checkedRadioButtonId
        val selectedGender = if (selectedGenderId == R.id.gender_male) "남자" else "여자"

        val ageText = findViewById<EditText>(R.id.age_input).text.toString()
        val heightText = findViewById<EditText>(R.id.height_input).text.toString()
        val weightText = findViewById<EditText>(R.id.weight_input).text.toString()

        if (name.isEmpty() || ageText.isEmpty() || heightText.isEmpty() || weightText.isEmpty()) {
            Toast.makeText(this, "모든 정보를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageText.toInt()
        val height = heightText.toDouble()
        val weight = weightText.toDouble()

        val userInfo = UserInfo(email, name, selectedGender, age, height, weight)

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
        val genderGroup = findViewById<RadioGroup>(R.id.gender_group)
        val selectedGenderId = genderGroup.checkedRadioButtonId
        val gender = if (selectedGenderId == R.id.gender_male) "남자" else "여자"
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