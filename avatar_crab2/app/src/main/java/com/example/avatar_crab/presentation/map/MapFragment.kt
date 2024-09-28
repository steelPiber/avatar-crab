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
import com.example.avatar_crab.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PolygonOverlay
import com.naver.maps.map.util.FusedLocationSource
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

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                naverMap.locationTrackingMode = LocationTrackingMode.Follow
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
