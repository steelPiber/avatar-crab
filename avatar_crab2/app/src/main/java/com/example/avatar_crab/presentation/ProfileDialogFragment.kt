package com.example.avatar_crab.presentation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import piber.avatar_crab.R
import com.example.avatar_crab.presentation.data.UserInfo
import androidx.fragment.app.activityViewModels
import com.example.avatar_crab.MyApplication
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class ProfileDialogFragment(private val userInfo: UserInfo, private val profileImageUrl: String?) : DialogFragment() {

    // MainViewModel을 Fragment에서 사용하도록 초기화
    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            (requireActivity().application as MyApplication).challengeRepository,
            requireActivity().application
        )
    }

    // GoogleSignInClient를 사용하여 Google 로그아웃 처리
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_profile, container, false)

        val profileImageView = view.findViewById<ImageView>(R.id.dialogProfileImageView)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etHeight = view.findViewById<EditText>(R.id.etHeight)
        val etWeight = view.findViewById<EditText>(R.id.etWeight)
        val etAge = view.findViewById<EditText>(R.id.etAge)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val radioGroupGender = view.findViewById<RadioGroup>(R.id.radioGroupGender)
        val radioMale = view.findViewById<RadioButton>(R.id.radioMale)
        val radioFemale = view.findViewById<RadioButton>(R.id.radioFemale)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // GoogleSignInClient 초기화
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // 유저 정보 설정
        etName.setText(userInfo.name)
        etHeight.setText(userInfo.height.toString())
        etWeight.setText(userInfo.weight.toString())
        etAge.setText(userInfo.age.toString())
        tvEmail.text = userInfo.email

        // 성별 설정
        if (userInfo.gender == "남자") {
            radioMale.isChecked = true
        } else {
            radioFemale.isChecked = true
        }

        // 프로필 이미지 로드
        profileImageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(profileImageView)
        }

        // 수정 버튼 클릭 시 정보 저장
        btnSave.setOnClickListener {
            saveProfileData()
        }

        // 로그아웃 버튼 클릭 시 처리
        btnLogout.setOnClickListener {
            logoutUser()
        }

        return view
    }

    private fun saveProfileData() {
        val updatedName = view?.findViewById<EditText>(R.id.etName)?.text.toString()
        val updatedHeight = view?.findViewById<EditText>(R.id.etHeight)?.text.toString().toDoubleOrNull() ?: userInfo.height
        val updatedWeight = view?.findViewById<EditText>(R.id.etWeight)?.text.toString().toDoubleOrNull() ?: userInfo.weight
        val updatedAge = view?.findViewById<EditText>(R.id.etAge)?.text.toString().toIntOrNull() ?: userInfo.age

        // 성별 선택
        val updatedGender = if (view?.findViewById<RadioButton>(R.id.radioMale)?.isChecked == true) {
            "남자"
        } else {
            "여자"
        }

        // 서버로 수정된 정보 전송
        val updatedUserInfo = UserInfo(
            name = updatedName,
            height = updatedHeight,
            weight = updatedWeight,
            age = updatedAge,
            gender = updatedGender,
            email = userInfo.email
        )

        // 서버에 수정된 정보를 저장하는 ViewModel 메서드 호출
        viewModel.updateUserInfo(updatedUserInfo)

        dismiss() // 다이얼로그 닫기
    }

    private fun logoutUser() {
        // Google 로그아웃 처리
        googleSignInClient.signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Sign-out successful, proceed to LoginActivity
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                dismiss() // 다이얼로그 닫기
            } else {
                // Handle failure if needed
                Toast.makeText(requireContext(), "로그아웃 실패: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}