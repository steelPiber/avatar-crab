package com.example.avatar_crab.presentation.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.avatar_crab.R
import com.example.avatar_crab.presentation.MainViewModel
import com.example.avatar_crab.presentation.RetrofitClient
import com.example.avatar_crab.presentation.data.MapPolygonData
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PolygonOverlay
import com.naver.maps.map.util.FusedLocationSource
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationSource: FusedLocationSource
    private lateinit var naverMap: NaverMap
    private lateinit var saveButton: Button
    private lateinit var addAreaButton: Button
    private lateinit var instructionTextView: TextView
    private var polygonPoints: MutableList<LatLng> = mutableListOf()
    private var polygons: MutableList<PolygonOverlay> = mutableListOf()
    private var markers: MutableList<Marker> = mutableListOf()
    private var isDrawingPolygon = false
    private var currentMarker: Marker? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // MapView 초기화
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        // 버튼 및 텍스트 초기화
        saveButton = view.findViewById(R.id.btn_save_polygon)
        saveButton.visibility = View.GONE
        addAreaButton = view.findViewById(R.id.btn_add_area)
        instructionTextView = view.findViewById(R.id.instruction_text)

        // 영역 추가 버튼 클릭 시 폴리곤 그리기 시작
        addAreaButton.setOnClickListener {
            startPolygonDrawing()
        }

        // 위치 소스 초기화
        fusedLocationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

        // NaverMap 설정
        mapView.getMapAsync(this)

        return view
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap

        // 위치 추적 소스 설정
        naverMap.locationSource = fusedLocationSource

        // 위치 권한 확인 및 요청
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // 위치 추적 모드 설정
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        // 서버에서 폴리곤 데이터 불러오기
        loadPolygonDataFromServer()

        // 폴리곤 그리기 시작
        naverMap.setOnMapClickListener { _, latLng ->
            if (isDrawingPolygon) {
                polygonPoints.add(latLng)  // 점을 계속 추가
                drawPolygon()              // 폴리곤을 그리기 위한 업데이트
            }
        }

        // 마커 또는 폴리곤 클릭 시 반응하도록 설정
        naverMap.setOnMapLongClickListener { _, latLng ->
            markers.forEach { marker ->
                if (marker.position == latLng) {
                    showBottomSheet(marker)
                }
            }
        }

        // 저장 버튼 클릭 시 폴리곤 저장
        saveButton.setOnClickListener {
            if (polygonPoints.size >= 3) {
                showSavePolygonDialog()
            } else {
                Toast.makeText(requireContext(), "폴리곤을 완성해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPolygonDataFromServer() {
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(requireContext())
        val email = account?.email ?: run {
            Toast.makeText(requireContext(), "사용자 이메일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val call = RetrofitClient.heartRateInstance.getPolygonData(email)
        call.enqueue(object : Callback<List<MapPolygonData>> {
            override fun onResponse(
                call: Call<List<MapPolygonData>>,
                response: Response<List<MapPolygonData>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { polygonDataList ->
                        for (polygonData in polygonDataList) {
                            val points = parseCoordinates(polygonData.coordinates)
                            val polygon = PolygonOverlay().apply {
                                coords = points
                                color = 0x80FF0000.toInt() // 반투명 빨간색
                                outlineColor = 0xFFFF0000.toInt() // 빨간색 테두리
                                outlineWidth = 5 // 테두리 두께
                                map = naverMap
                            }
                            polygons.add(polygon)

                            val centerLatLng = getPolygonCenter(points)
                            val marker = Marker().apply {
                                position = centerLatLng
                                captionText = polygonData.title
                                map = naverMap
                                setOnClickListener {
                                    showBottomSheet(this)
                                    true
                                }
                            }
                            markers.add(marker)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "폴리곤 데이터 로드 실패: ${response.message()}", Toast.LENGTH_SHORT).show()
                    Log.e("MapFragment", "Server Error: ${response.code()} ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<MapPolygonData>>, t: Throwable) {
                Toast.makeText(requireContext(), "서버 통신 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("MapFragment", "Network Failure: ${t.message}", t)
            }
        })
    }

    private fun parseCoordinates(coordinates: String): List<LatLng> {
        // JSON 형식의 문자열을 파싱하여 LatLng 리스트로 변환
        // 예: [{"lat":37.566, "lng":126.9784}, ...]
        val regex = Regex("\\{\"lat\":(.*?), \"lng\":(.*?)\\}")
        return regex.findAll(coordinates).map {
            LatLng(it.groupValues[1].toDouble(), it.groupValues[2].toDouble())
        }.toList()
    }

    private fun startPolygonDrawing() {
        isDrawingPolygon = true
        polygonPoints = mutableListOf()

        // "건물 영역을 그려주세요" 텍스트 표시
        instructionTextView.visibility = View.VISIBLE
        addAreaButton.visibility = View.GONE
        saveButton.visibility = View.VISIBLE // 저장 버튼 표시

        Toast.makeText(requireContext(), "건물 영역을 그려주세요.", Toast.LENGTH_SHORT).show()
    }

    private fun drawPolygon() {
        if (polygonPoints.size >= 3) {
            // 기존 폴리곤을 지우지 않고 새로운 점을 추가하면서 폴리곤 업데이트
            val polygon = PolygonOverlay().apply {
                coords = polygonPoints
                color = 0x80FF0000.toInt() // 반투명 빨간색
                outlineColor = 0xFFFF0000.toInt() // 빨간색 테두리
                outlineWidth = 5 // 테두리 두께
                map = naverMap
            }

            // 폴리곤이 그려질 때마다 마커는 하나만 생성되도록 설정
            if (currentMarker == null) {
                val centerLatLng = getPolygonCenter(polygonPoints)
                currentMarker = Marker().apply {
                    position = centerLatLng
                    map = naverMap
                    setOnClickListener {
                        showBottomSheet(this)
                        true
                    }
                }
                markers.add(currentMarker!!)
            }

            // 폴리곤을 목록에 추가
            polygons.add(polygon)
        }
    }

    private fun showSavePolygonDialog() {
        // 제목 입력을 위한 BottomSheetDialog 생성
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_area_info, null)
        bottomSheetDialog.setContentView(view)

        val etTitle = view.findViewById<EditText>(R.id.et_area_title)
        val btnSave = view.findViewById<Button>(R.id.btn_save)

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            if (title.isNotBlank()) {
                savePolygon(title)
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.show()
    }

    private fun savePolygon(title: String) {
        // 현재 마커에 제목 설정
        currentMarker?.captionText = title

        // 상태 초기화
        saveButton.visibility = View.GONE // 저장 후 버튼 숨김
        addAreaButton.visibility = View.VISIBLE
        instructionTextView.visibility = View.GONE // 지시 문구 숨김
        isDrawingPolygon = false

        Toast.makeText(requireContext(), "$title 영역이 저장되었습니다.", Toast.LENGTH_SHORT).show()

        // Retrofit을 사용해 폴리곤 데이터 전송
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(requireContext())
        val email = account?.email ?: run {
            Toast.makeText(requireContext(), "사용자 이메일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val coordinatesJson = polygonPoints.joinToString(prefix = "[", postfix = "]") { "{\"lat\":${it.latitude}, \"lng\":${it.longitude}}" }
        val polygonData = MapPolygonData(email = email, title = title, coordinates = coordinatesJson)
        Log.d("MapFragment", "Polygon Data to send: $coordinatesJson")

        val call = RetrofitClient.heartRateInstance.sendPolygonData(polygonData)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "폴리곤 데이터가 서버에 저장되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "서버 저장 실패: ${response.message()}", Toast.LENGTH_SHORT).show()
                    Log.e("MapFragment", "Server Error: ${response.code()} ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(requireContext(), "서버 통신 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("MapFragment", "Network Failure: ${t.message}", t)
            }
        })

        // 다음 폴리곤 그리기를 위해 현재 마커 초기화
        currentMarker = null
    }

    private fun getPolygonCenter(points: List<LatLng>): LatLng {
        val latitude = points.map { it.latitude }.average()
        val longitude = points.map { it.longitude }.average()
        return LatLng(latitude, longitude)
    }

    private fun showBottomSheet(marker: Marker) {
        // 마커 클릭 시 제목 수정 및 슬라이더 애니메이션 표시
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_area_info, null)
        bottomSheetDialog.setContentView(view)

        val etTitle = view.findViewById<EditText>(R.id.et_area_title)
        etTitle.setText(marker.captionText)

        val btnSave = view.findViewById<Button>(R.id.btn_save)
        btnSave.setOnClickListener {
            val newTitle = etTitle.text.toString()
            if (newTitle.isNotBlank()) {
                marker.captionText = newTitle
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        bottomSheetDialog.show()
    }
}