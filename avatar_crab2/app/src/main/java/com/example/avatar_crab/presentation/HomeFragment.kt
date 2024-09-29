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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.avatar_crab.MyApplication
import com.example.avatar_crab.R
import com.example.avatar_crab.presentation.data.UserInfo
import com.example.avatar_crab.presentation.map.MapFragment
import com.example.avatar_crab.presentation.measure.MeasureFragment
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
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels {
        val application = requireActivity().application as MyApplication
        MainViewModelFactory(application.challengeRepository, application)
    }
    private lateinit var heartRateChart: LineChart
    private lateinit var stressChart: LineChart
    private lateinit var profileImageView: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var tvChallenge: TextView

    // Google Sign-In Launcher
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

        Log.d("HomeFragment", "onViewCreated called")

        profileImageView = view.findViewById(R.id.profileImageView)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvChallenge = view.findViewById(R.id.tvChallenge)
        heartRateChart = view.findViewById(R.id.heartRateChart)
        stressChart = view.findViewById(R.id.stressChart)

        // 로컬에서 유저 정보 로드
        loadUserInfo()

        setupChart(heartRateChart, "심박수")
        setupChart(stressChart, "스트레스")
        populateChart(heartRateChart, listOf(70f, 75f, 80f, 85f, 78f))
        populateChart(stressChart, listOf(20f, 30f, 25f, 35f, 40f))

        // GoogleSignInOptions 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        tvUserName.setOnClickListener {
            signIn()
        }

        val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
        account?.let { updateUserProfile(it) }

        val measureCard = view.findViewById<CardView>(R.id.measureCard)
        measureCard.setOnClickListener {
            Toast.makeText(requireContext(), "Measure Card Clicked", Toast.LENGTH_SHORT).show()
            openMeasureFragment()
        }

        val mapCard = view.findViewById<CardView>(R.id.mapCard)
        if (mapCard == null) {
            Log.d("HomeFragment", "mapCard is null")
        } else {
            Log.d("HomeFragment", "mapCard is found")
        }

        mapCard.setOnClickListener {
            Toast.makeText(requireContext(), "Map Card Clicked", Toast.LENGTH_SHORT).show()
            openMapFragment()
        }

        setCurrentDate()

        // Observe the real-time heart rate data from the ViewModel
        viewModel.realTimeHeartRate.observe(viewLifecycleOwner, Observer { heartRate ->
            // Update the UI with the new heart rate
            val heartRateTextView = view.findViewById<TextView>(R.id.tvHeartRate)
            heartRateTextView.text = "$heartRate BPM"
        })

    }

    private fun loadUserInfo() {
        val sharedPreferences = requireActivity().getSharedPreferences("AvatarCrabPrefs", Context.MODE_PRIVATE)
        val userInfoJson = sharedPreferences.getString("userInfo", null)
        val photoUrl = sharedPreferences.getString("photoUrl", null) // 사진 URL 로드

        if (userInfoJson != null) {
            val userInfo = Gson().fromJson(userInfoJson, UserInfo::class.java)
            tvUserName.text = userInfo.name

            // 저장된 사진 URL이 있다면 로드
            if (photoUrl != null) {
                Glide.with(this).load(photoUrl).into(profileImageView)
            }
        } else {
            // 유저 정보가 없으면 로그인으로 이동
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
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
            account?.let {
                updateUserProfile(it)
                savePhotoUrlToLocal(it.photoUrl.toString()) // photoUrl 로컬 저장
            }
        } catch (e: ApiException) {
            Log.w("HomeFragment", "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun savePhotoUrlToLocal(photoUrl: String) {
        val sharedPreferences = requireActivity().getSharedPreferences("AvatarCrabPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("photoUrl", photoUrl)
        editor.apply()
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
