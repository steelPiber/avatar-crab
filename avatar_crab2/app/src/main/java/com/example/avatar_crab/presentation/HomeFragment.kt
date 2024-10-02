package com.example.avatar_crab.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.avatar_crab.MyApplication
import com.example.avatar_crab.R
import com.example.avatar_crab.presentation.challenge.ChallengeFragment
import com.example.avatar_crab.presentation.map.MapFragment
import com.example.avatar_crab.presentation.measure.MeasureFragment
import com.example.avatar_crab.presentation.settings.SettingsViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            (requireActivity().application as MyApplication).challengeRepository,
            requireActivity().application
        )
    }
    private lateinit var heartRateChart: LineChart
    private lateinit var stressChart: LineChart
    private lateinit var profileImageView: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var tvChallenge: TextView
    private lateinit var userCard: CardView

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // viewModel은 이미 activityViewModels로 초기화되었으므로 재할당 불필요

        // XML에 있는 요소들 바인딩
        profileImageView = view.findViewById(R.id.profileImageView)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvChallenge = view.findViewById(R.id.tvChallenge)
        heartRateChart = view.findViewById(R.id.heartRateChart)
        stressChart = view.findViewById(R.id.stressChart)
        userCard = view.findViewById(R.id.userCard)

        // 차트 설정
        setupChart(heartRateChart, "심박수")
        setupChart(stressChart, "스트레스")
        populateChart(heartRateChart, listOf(70f, 75f, 80f, 85f, 78f))
        populateChart(stressChart, listOf(20f, 30f, 25f, 35f, 40f))

        // Google Sign-In 설정 및 이벤트 처리
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        tvUserName.setOnClickListener {
            signIn()
        }

        // Google 계정 정보 업데이트
        val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
        account?.let { updateUserProfile(it) }

        userCard.setOnClickListener {
            // ViewModel에서 사용자 이메일을 가져옴
            val email = viewModel.userEmail.value
            val profileImageUrl = viewModel.userAccount.value?.photoUrl?.toString() // 구글 프로필 이미지 URL 가져오기

            if (email != null) {
                // 서버에서 사용자 정보를 가져오는 메서드 호출
                viewModel.fetchUserInfo(email)

                // 사용자 정보가 업데이트되면 프로필 다이얼로그 표시
                viewModel.userInfo.observe(viewLifecycleOwner, Observer { userInfo ->
                    userInfo?.let {
                        //email 과 profileImageUrl 가져
                        val profileDialog = ProfileDialogFragment(it,profileImageUrl)
                        profileDialog.show(parentFragmentManager, "ProfileDialog")
                    }
                })
            } else {
                Toast.makeText(requireContext(), "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }



        // MeasureCard 클릭 이벤트 처리
        val measureCard = view.findViewById<CardView>(R.id.measureCard)
        measureCard.setOnClickListener {
            Toast.makeText(requireContext(), "Measure Card Clicked", Toast.LENGTH_SHORT).show()
            openMeasureFragment()
        }

        // MapCard 클릭 이벤트 처리
        val mapCard = view.findViewById<CardView>(R.id.mapCard)
        mapCard?.let {
            it.setOnClickListener {
                Toast.makeText(requireContext(), "Map Card Clicked", Toast.LENGTH_SHORT).show()
                openMapFragment()
            }
        } ?: Log.d("HomeFragment", "mapCard is null")

        // 현재 날짜 설정
        setCurrentDate()

        // 실시간 심박수 데이터를 관찰
        viewModel.realTimeHeartRate.observe(viewLifecycleOwner, Observer { heartRate ->
            val heartRateTextView = view.findViewById<TextView>(R.id.tvHeartRate)
            heartRateTextView.text = "$heartRate BPM"
        })
    }

    private fun setCurrentDate() {
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        tvChallenge.text = currentDate
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            account?.let { updateUserProfile(it) }
        } catch (e: ApiException) {
            Log.w("HomeFragment", "signInResult:failed code=" + e.statusCode)
        }
    }

    fun updateUserProfile(account: GoogleSignInAccount) {
        tvUserName.text = account.displayName
        Glide.with(this).load(account.photoUrl).into(profileImageView)
    }

    private fun setupChart(chart: LineChart, description: String) {
        chart.description.text = description
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.axisRight.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
    }

    private fun populateChart(chart: LineChart, dataPoints: List<Float>) {
        val entries = dataPoints.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val dataSet = LineDataSet(entries, "Data").apply {
            axisDependency = YAxis.AxisDependency.LEFT
        }
        chart.data = LineData(dataSet)
        chart.invalidate()
    }

    private fun openMeasureFragment() {
        Log.d("HomeFragment", "Opening MeasureFragment")
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, MeasureFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun openMapFragment() {
        Log.d("HomeFragment", "Opening MapFragment")
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, MapFragment())
            .addToBackStack(null)
            .commit()
    }
}
