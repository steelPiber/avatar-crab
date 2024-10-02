package com.example.avatar_crab.presentation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.avatar_crab.R

import com.example.avatar_crab.presentation.data.UserInfo
class ProfileDialogFragment(private val userInfo: UserInfo, private val profileImageUrl: String?) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_profile, container, false)

        val profileImageView = view.findViewById<ImageView>(R.id.dialogProfileImageView)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvHeight = view.findViewById<TextView>(R.id.tvHeight)
        val tvWeight = view.findViewById<TextView>(R.id.tvWeight)
        val tvGender = view.findViewById<TextView>(R.id.tvGender)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // 유저 정보 설정
        tvName.text = userInfo.name
        tvHeight.text = "키: ${userInfo.height} cm"
        tvWeight.text = "몸무게: ${userInfo.weight} kg"
        tvGender.text = "성별: ${userInfo.gender}"

        // 프로필 이미지 로드
        profileImageUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_profile_placeholder) // 이미지 로드 전 임시 이미지
                .error(R.drawable.ic_profile_placeholder) // 이미지 로드 실패 시 대체 이미지
                .into(profileImageView)
        }

        // 로그아웃 버튼 클릭 이벤트 처리
        btnLogout.setOnClickListener {
            // 로그아웃 처리 로직 추가
            dismiss()
        }

        return view
    }
}
