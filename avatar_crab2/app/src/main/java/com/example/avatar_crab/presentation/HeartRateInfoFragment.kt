package com.example.avatar_crab.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.avatar_crab.R

class HeartRateInfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_heart_rate_info, container, false)
    }

    // 추가적인 심박수 정보 로직을 이곳에 추가하세요
}