package com.example.avatar_crab.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.avatar_crab.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET

class ServerResponseFragment : Fragment() {

    data class ServerStatusResponse(
        val server: String,
        val db: String
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_server_response, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkServerStatus() // 서버 상태를 확인하는 메서드 호출
    }

    // 서버 상태를 확인하는 메서드
    private fun checkServerStatus() {
        val apiService = RetrofitClient.heartRateInstance
        apiService.serverCheck().enqueue(object : Callback<ServerStatusResponse> {
            override fun onResponse(call: Call<ServerStatusResponse>, response: Response<ServerStatusResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.server == "on" && it.db == "on") {
                            Toast.makeText(requireContext(), "서버와 데이터베이스가 정상적으로 실행 중입니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "서버 또는 데이터베이스 상태에 문제가 있습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "서버 응답 오류: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ServerStatusResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "서버 요청 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })


    }
}
