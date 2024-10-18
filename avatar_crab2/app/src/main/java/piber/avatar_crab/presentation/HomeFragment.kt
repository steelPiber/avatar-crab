package piber.avatar_crab.presentation

import android.annotation.SuppressLint
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
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import piber.avatar_crab.MyApplication
import piber.avatar_crab.presentation.data.HeartDataPoint
import piber.avatar_crab.presentation.data.HeartInfo
import piber.avatar_crab.presentation.map.MapFragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import piber.avatar_crab.R


import piber.avatar_crab.presentation.data.HdaDataPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

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
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var userCard: CardView
    private lateinit var scrollView: NestedScrollView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var hdaCard: CardView
    private lateinit var a1TextView: TextView

    private val apiService by lazy { RetrofitClient.heartRateInstance }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

        // XML에 있는 요소들 바인딩
        profileImageView = view.findViewById(R.id.profileImageView)
        heartRateChart = view.findViewById(R.id.heartRateChart)
        userCard = view.findViewById(R.id.userCard)
        scrollView = view.findViewById(R.id.scrollView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        // HDA 카드뷰 바인딩
        hdaCard = view.findViewById(R.id.hdaCard)
        a1TextView = view.findViewById(R.id.tvA1)
        // HomeFragment 시작 시 자동으로 A1 값을 업데이트
        updateA1Value()

        // Google Sign-In 설정 및 이벤트 처리
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // Google 계정 정보 업데이트
        val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
        account?.let { updateUserProfile(it) }


        hdaCard.setOnClickListener {
            updateA1Value()
        }

        userCard.setOnClickListener {
            // ViewModel에서 사용자 이메일을 가져옴
            val email = viewModel.userEmail.value
            val profileImageUrl =
                viewModel.userAccount.value?.photoUrl?.toString() // 구글 프로필 이미지 URL 가져오기

            if (email != null) {
                // 서버에서 사용자 정보를 가져오는 메서드 호출
                viewModel.fetchUserInfo(email)

                // 사용자 정보가 업데이트되면 프로필 다이얼로그 표시
                viewModel.userInfo.observe(viewLifecycleOwner, Observer { userInfo ->
                    userInfo?.let {
                        // email, profileImageUrl과 함께 사용자 세부 정보 (이름, 키, 몸무게, 성별, 나이) 가져와서 표시
                        val profileDialog = ProfileDialogFragment(it, profileImageUrl)
                        profileDialog.show(parentFragmentManager, "ProfileDialog")
                    }
                })
            } else {
                Toast.makeText(requireContext(), "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // MapCard 클릭 이벤트 처리
        val mapCard = view.findViewById<CardView>(R.id.mapCard)
        mapCard?.let {
            it.setOnClickListener {
                Toast.makeText(requireContext(), "Map Card Clicked", Toast.LENGTH_SHORT).show()
                openMapFragment()
            }
        } ?: Log.d("HomeFragment", "mapCard is null")

        // 심박수 카드뷰 클릭 이벤트 처리
        val heartRateCard = view.findViewById<CardView>(R.id.heartRateCard)
        heartRateCard.setOnClickListener {
            openHeartRateInfoFragment()
        }

        // 실시간 심박수 데이터를 관찰
        viewModel.realTimeHeartRate.observe(viewLifecycleOwner, Observer { heartRate ->
            val heartRateTextView = view.findViewById<TextView>(R.id.tvHeartRate)
            heartRateTextView.text = "$heartRate BPM"
        })

        // 서버에서 심박수 정보를 가져와 그래프 업데이트
        fetchHeartInfo()

        // SwipeRefreshLayout 설정
        swipeRefreshLayout.setOnRefreshListener {
            fetchHeartInfo() // 데이터 새로고침
            swipeRefreshLayout.isRefreshing = false // 새로고침 완료 후 인디케이터 해제
        }
    }
    // A1 값을 갱신하는 함수
    private fun updateA1Value() {
        val email = viewModel.userEmail.value
        if (!email.isNullOrEmpty()) {
            apiService.getHDAData(email).enqueue(object : Callback<List<HdaDataPoint>> {
                override fun onResponse(
                    call: Call<List<HdaDataPoint>>,
                    response: Response<List<HdaDataPoint>>
                ) {
                    if (response.isSuccessful) {
                        val hdaData = response.body()
                        if (!hdaData.isNullOrEmpty()) {
                            updateA1WithCurrentTime(hdaData)
                        }
                    } else {
                        Toast.makeText(requireContext(), "HDA 데이터를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<HdaDataPoint>>, t: Throwable) {
                    Toast.makeText(requireContext(), "서버 연결에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "유효하지 않은 이메일입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 현재 시간에 맞는 데이터로 A1 값을 갱신
    private fun updateA1WithCurrentTime(hdaData: List<HdaDataPoint>) {
        // 현재 시간 가져오기
        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val currentMinute = currentTime.get(Calendar.MINUTE)

        // 현재 시간에 맞는 HDA 데이터 찾기
        val currentData = hdaData.find { dataPoint ->
            dataPoint.hour == currentHour && dataPoint.min == currentMinute
        }

        // A1 값을 업데이트
        currentData?.let { dataPoint ->
            val a1Value = (dataPoint.q1 + dataPoint.q2) / 2
            Log.d("HomeFragment", "A1 값 갱신: $a1Value")

            // A1 값을 화면에 표시
            a1TextView.text = "A1: $a1Value"
        } ?: run {
            Toast.makeText(requireContext(), "현재 시간에 대한 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchHeartInfo() {
        val email = viewModel.userEmail.value
        if (!email.isNullOrEmpty()) {
            apiService.getHeartInfo(email).enqueue(object : Callback<HeartInfo> {
                override fun onResponse(call: Call<HeartInfo>, response: Response<HeartInfo>) {
                    if (response.isSuccessful) {
                        val heartInfo = response.body()
                        Log.d("HomeFragment", "Heart info response: $heartInfo")
                        if (heartInfo?.periodicData?.daily != null && heartInfo.periodicData.daily.isNotEmpty()) {
                            updateHeartRateChart(heartInfo.periodicData.daily)
                        } else {
                            Log.e(
                                "HomeFragment",
                                "Heart info or periodic data is null or empty: periodicData=\${heartInfo?.periodicData}"
                            )
                            Toast.makeText(
                                requireContext(),
                                "Daily heart rate data is not available",
                                Toast.LENGTH_SHORT
                            ).show()
                            heartRateChart.clear()
                            heartRateChart.invalidate() // 차트 갱신
                        }
                    } else {
                        Log.e(
                            "HomeFragment",
                            "Failed to get heart info: \${response.errorBody()?.string()}"
                        )
                        Toast.makeText(
                            requireContext(),
                            "Failed to get heart info",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<HeartInfo>, t: Throwable) {
                    Log.e("HomeFragment", "Failed to get heart info", t)
                    Toast.makeText(
                        requireContext(),
                        "Failed to connect to server",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            Log.e("HomeFragment", "Email is null or empty")
            Toast.makeText(requireContext(), "Invalid email", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun updateHeartRateChart(dailyData: List<HeartDataPoint>) {
        if (dailyData.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No daily heart rate data available",
                Toast.LENGTH_SHORT
            ).show()
            heartRateChart.clear()
            heartRateChart.invalidate()
            return
        }

        val entries = dailyData.mapIndexed { index, heartDataPoint ->
            Entry(index.toFloat(), heartDataPoint.values.toFloat())
        }

        val lineDataSet = LineDataSet(entries, "Daily Heart Rate")
        lineDataSet.setDrawCircles(false)
        lineDataSet.setDrawValues(false)
        lineDataSet.setDrawFilled(true)
        lineDataSet.fillDrawable = requireContext().getDrawable(R.drawable.chart_fill_gradient)

        val lineData = LineData(lineDataSet)
        heartRateChart.data = lineData

        val xAxis = heartRateChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index in dailyData.indices) {
                    val label = dailyData[index].label
                    // Extract hour from label (assuming format "yyyy-MM-dd HH:mm")
                    label.split(" ")[1].split(":")[0] + "시"
                } else ""
            }
        }

        heartRateChart.axisRight.isEnabled = false
        val leftAxis = heartRateChart.axisLeft
        leftAxis.granularity = 10f
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = requireContext().getColor(R.color.light_gray)
        leftAxis.textColor = requireContext().getColor(R.color.black)
        leftAxis.textSize = 12f

        heartRateChart.description.isEnabled = false
        heartRateChart.setTouchEnabled(true)
        heartRateChart.setPinchZoom(true)
        heartRateChart.setScaleEnabled(true)
        heartRateChart.setExtraOffsets(10f, 10f, 10f, 10f)
        heartRateChart.animateX(1500)
        heartRateChart.invalidate() // 차트 갱신
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // 다른 프래그먼트로 이동할 때 하단 네비게이션 바 다시 보이도록 설정
        requireActivity().window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Google 로그인을 실행하는 메서드
    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    // Google 로그인 결과를 처리하는 메서드
    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            account?.let { updateUserProfile(it) }
        } catch (e: ApiException) {
            Log.w("HomeFragment", "signInResult:failed code=" + e.statusCode)
        }
    }

    // Google 계정 정보를 업데이트하는 메서드
    fun updateUserProfile(account: GoogleSignInAccount) {
        Glide.with(this).load(account.photoUrl).circleCrop().into(profileImageView)
    }

    // MapFragment를 여는 메서드
    private fun openMapFragment() {
        Log.d("HomeFragment", "Opening MapFragment")
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, MapFragment())
            .addToBackStack(null)
            .commit()
    }

    // HeartRateInfoFragment를 여는 메서드
    private fun openHeartRateInfoFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HeartRateInfoFragment())
            .addToBackStack(null)
            .commit()
    }
}