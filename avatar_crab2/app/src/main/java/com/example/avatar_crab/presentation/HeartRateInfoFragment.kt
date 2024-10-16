package com.example.avatar_crab.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import piber.avatar_crab.R
import com.google.android.material.tabs.TabLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.avatar_crab.presentation.data.HeartInfo
import com.example.avatar_crab.presentation.RetrofitClient
import com.example.avatar_crab.presentation.data.HeartDataPoint
import com.example.avatar_crab.presentation.data.UserInfo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import android.widget.ImageView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class HeartRateInfoFragment : Fragment() {

    private lateinit var heartRateChart: BarChart
    private lateinit var heartRateTabLayout: TabLayout
    private lateinit var heartRateInfoTextView: TextView

    private lateinit var backButton: ImageView
    private val apiService by lazy { RetrofitClient.heartRateInstance }
    private var userEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_heart_rate_info, container, false)

        heartRateChart = view.findViewById(R.id.heartRateChart)
        heartRateTabLayout = view.findViewById(R.id.heartRateTabLayout)
        heartRateInfoTextView = view.findViewById(R.id.heartRateInfoTextView)
        backButton = view.findViewById(R.id.backButton)

        backButton.setOnClickListener {
            activity?.onBackPressed()
        }

        fetchUserInfo()
        setupChart()
        setupTabs()

        return view
    }

    // 심박수 차트를 설정하는 메서드
    private fun setupChart() {
        val entries = listOf(
            BarEntry(0f, 72f),
            BarEntry(1f, 79f),
            BarEntry(2f, 87f),
            BarEntry(3f, 88f)
        )
        val dataSet = BarDataSet(entries, "Daily Heart Rate")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.red)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)

        val barData = BarData(dataSet)
        heartRateChart.data = barData
        heartRateChart.setTouchEnabled(true)
        heartRateChart.setPinchZoom(false)
        heartRateChart.description.isEnabled = false
        heartRateChart.axisRight.isEnabled = false
        heartRateChart.xAxis.isEnabled = true
        heartRateChart.axisLeft.isEnabled = true
        heartRateChart.axisLeft.axisMinimum = 0f // Ensure y-axis starts at 0
        heartRateChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        heartRateChart.xAxis.valueFormatter = IndexAxisValueFormatter(entries.map { "날짜: ${it.x.toInt() + 1}" })
        heartRateChart.invalidate()

        heartRateChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {
                    val index = e.x.toInt()
                    val value = e.y.toInt()
                    val label = heartRateChart.xAxis.valueFormatter.getFormattedValue(index.toFloat(), heartRateChart.xAxis)
                    heartRateInfoTextView.text = "$label, 심박수: ${value} BPM"
                }
            }

            override fun onNothingSelected() {
                heartRateInfoTextView.text = ""
            }
        })
    }

    // 탭 레이아웃을 설정하는 메서드
    private fun setupTabs() {
        heartRateTabLayout.addTab(heartRateTabLayout.newTab().setText("일일"))
        heartRateTabLayout.addTab(heartRateTabLayout.newTab().setText("주간"))
        heartRateTabLayout.addTab(heartRateTabLayout.newTab().setText("1개월"))
        heartRateTabLayout.addTab(heartRateTabLayout.newTab().setText("6개월"))
        heartRateTabLayout.addTab(heartRateTabLayout.newTab().setText("1년"))

        heartRateTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    when (it.position) {
                        0 -> fetchHeartInfo("daily")
                        1 -> fetchHeartInfo("weekly")
                        2 -> fetchHeartInfo("monthly")
                        3 -> fetchHeartInfo("six_months")
                        4 -> fetchHeartInfo("yearly")
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // 서버에서 사용자 정보를 가져오는 메서드
    private fun fetchUserInfo() {
        val userInfoService = RetrofitClient.heartRateInstance
        userInfoService.getUserInfo(GoogleSignIn.getLastSignedInAccount(requireContext())?.email ?: "").enqueue(object : Callback<UserInfo> {
            override fun onResponse(call: Call<UserInfo>, response: Response<UserInfo>) {
                if (response.isSuccessful) {
                    userEmail = response.body()?.email ?: ""
                    if (userEmail.isNotEmpty()) {
                        fetchHeartInfo()
                    }
                } else {
                    // 오류 응답 처리
                }
            }

            override fun onFailure(call: Call<UserInfo>, t: Throwable) {
                // 실패 처리
            }
        })
    }

    // 서버에서 심박수 정보를 가져오는 메서드
    private fun fetchHeartInfo(period: String = "daily") {
        if (userEmail.isNotEmpty()) {
            apiService.getHeartInfo(userEmail).enqueue(object : Callback<HeartInfo> {
                override fun onResponse(call: Call<HeartInfo>, response: Response<HeartInfo>) {
                    if (response.isSuccessful) {
                        val heartInfo = response.body()
                        val data = when (period) {
                            "daily" -> heartInfo?.periodicData?.daily
                            "weekly" -> heartInfo?.periodicData?.weekly?.map {
                                it.copy(label = translateDayOfWeek(it.label))
                            }
                            "monthly" -> heartInfo?.periodicData?.monthly
                            "six_months" -> heartInfo?.periodicData?.sixMonths
                            "yearly" -> heartInfo?.periodicData?.yearly
                            else -> null
                        }
                        if (data != null && data.isNotEmpty()) {
                            updateChartWithData(data)
                        }
                    }
                }

                override fun onFailure(call: Call<HeartInfo>, t: Throwable) {
                    // 실패 처리
                }
            })
        }
    }

    // 요일을 한글로 변환하는 메서드
    private fun translateDayOfWeek(day: String): String {
        return when (day) {
            "Sunday" -> "일요일"
            "Monday" -> "월요일"
            "Tuesday" -> "화요일"
            "Wednesday" -> "수요일"
            "Thursday" -> "목요일"
            "Friday" -> "금요일"
            "Saturday" -> "토요일"
            else -> day
        }
    }

    // 차트를 업데이트하는 메서드
    private fun updateChartWithData(data: List<HeartDataPoint>) {
        val entries = data.mapIndexed { index, heartDataPoint ->
            BarEntry(index.toFloat(), heartDataPoint.values.toFloat())
        }
        val dataSet = BarDataSet(entries, "Heart Rate Data")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.red)
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)

        val barData = BarData(dataSet)
        heartRateChart.data = barData
        heartRateChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        heartRateChart.xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.label })
        heartRateChart.axisLeft.axisMinimum = 0f // Ensure y-axis starts at 0
        heartRateChart.invalidate()
    }
}