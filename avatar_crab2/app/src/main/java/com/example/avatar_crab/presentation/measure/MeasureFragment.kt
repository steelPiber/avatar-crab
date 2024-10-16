package com.example.avatar_crab.presentation.measure

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import piber.avatar_crab.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc

class MeasureFragment : Fragment() {

    // UI 요소 정의
    private lateinit var progressBar: ProgressBar
    private lateinit var heartRateChart: LineChart
    private lateinit var textureView: TextureView
    private lateinit var heartRateValue: TextView

    // 카메라 관련 변수들 정의
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    // 측정 중인지 여부와 처리 중인지 여부를 나타내는 변수
    private var isMeasuring = false
    private var processing = false

    // 이미지 리더 정의
    private lateinit var imageReader: ImageReader

    // 심박수 계산을 위한 데이터 버퍼와 샘플 관련 설정
    private val brightnessValues = mutableListOf<Double>()
    private val timeValues = mutableListOf<Float>()
    private val heartRates = mutableListOf<Int>() // 심박수 값을 저장하는 리스트 추가
    private val SAMPLE_INTERVAL = 100L // 100ms 간격
    private var startTime: Long = 0L

    // 프래그먼트의 뷰 생성
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_measure, container, false)
    }

    // 뷰가 생성되었을 때 호출되는 메서드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // UI 요소들 연결
        progressBar = view.findViewById(R.id.progressBar)
        heartRateChart = view.findViewById(R.id.heartRateChart)
        textureView = view.findViewById(R.id.cameraView)
        heartRateValue = view.findViewById(R.id.heartRateValue)

        // 카메라 매니저 초기화
        cameraManager = requireContext().getSystemService(CameraManager::class.java)

        setupHeartRateChart()  // 심박수 차트 설정
    }

    // 차트 기본 설정
    private fun setupHeartRateChart() {
        heartRateChart.apply {
            description.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            axisRight.isEnabled = false
            legend.isEnabled = false  // 범례 비활성화
            axisLeft.axisMinimum = 40f  // Y축 최소값 설정 (심박수 최소값)
            axisLeft.axisMaximum = 180f  // Y축 최대값 설정 (심박수 최대값)
        }
    }

    // OpenCV 초기화
    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV initialization failed")
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully")
        }

        startBackgroundThread()  // 백그라운드 스레드 시작
        if (textureView.isAvailable) {
            openCamera()  // 카메라 열기
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener  // 카메라 텍스처 리스너 설정
        }
    }

    // 프래그먼트가 일시 중지될 때 호출되는 메서드
    override fun onPause() {
        closeCamera()  // 카메라 닫기
        stopBackgroundThread()  // 백그라운드 스레드 중지
        super.onPause()
    }

    // 백그라운드 스레드 시작
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackgroundThread").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    // 백그라운드 스레드 중지
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // 텍스처 뷰 리스너 정의 (카메라 텍스처가 준비되었을 때 처리)
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surfaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            openCamera()  // 카메라 열기
        }

        override fun onSurfaceTextureSizeChanged(
            surfaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            return true  // 텍스처가 파괴되었을 때 처리
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }

    // 카메라 열기
    private fun openCamera() {
        // 카메라 권한 확인 및 요청
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
            return
        }

        try {
            val cameraId = cameraManager.cameraIdList[0]  // 기본 카메라 ID 선택

            // 이미지 리더 초기화 (해상도 설정)
            imageReader = ImageReader.newInstance(
                320, // 해상도 낮춤
                240,
                ImageFormat.YUV_420_888,
                2
            )
            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)

            // 카메라 열기
            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // 카메라 상태 콜백 (카메라 연결, 에러 처리)
    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            startPreview()  // 프리뷰 시작
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), "카메라 에러 발생", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 카메라 프리뷰 시작
    private fun startPreview() {
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(imageReader.width, imageReader.height)  // 텍스처 크기 설정
        val previewSurface = Surface(surfaceTexture)
        val imageSurface = imageReader.surface

        try {
            cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.apply {
                addTarget(previewSurface)
                addTarget(imageSurface)  // 이미지 리더 추가

                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)  // 플래시 설정

                // 카메라 세션 생성 및 시작
                cameraDevice?.createCaptureSession(
                    listOf(previewSurface, imageSurface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            try {
                                session.setRepeatingRequest(
                                    this@apply.build(),
                                    null,
                                    backgroundHandler
                                )
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            activity?.runOnUiThread {
                                Toast.makeText(requireContext(), "카메라 구성 실패", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    },
                    backgroundHandler
                )
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // 이미지 리더 리스너 (이미지가 도착할 때 호출)
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
        if (!processing) {
            processing = true

            // 이미지에서 평균 RGB 값 추출
            val (meanR, meanG, meanB) = analyzeImage(image)
            image.close()

            val brightnessValue = (meanR + meanG + meanB) / 3.0  // RGB 평균값 사용

            if (meanR > 150 && meanG < 100 && meanB < 100) {
                if (!isMeasuring) {
                    startHeartRateMeasurement()
                } else {
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = (currentTime - startTime).toFloat() / 1000f
                    brightnessValues.add(brightnessValue)
                    timeValues.add(elapsedTime)

                    // 실시간으로 심박수 계산
                    if (brightnessValues.size >= 64) { // 약 6초 분량의 데이터 (100ms 간격 * 64)
                        val recentValues = brightnessValues.takeLast(64)
                        val heartRate = calculateHeartRate(recentValues)
                        if (heartRate > 0) {
                            heartRates.add(heartRate)
                            activity?.runOnUiThread {
                                val averageHeartRate = heartRates.average().toInt()
                                heartRateValue.text = "심박수: $averageHeartRate BPM"
                            }
                        }
                    }

                    updateHeartRateChart(brightnessValues) // 그래프 업데이트
                }
            } else {
                if (isMeasuring) {
                    stopMeasurement()
                }
            }

            processing = false
        } else {
            image.close()
        }
    }

    // 이미지 밝기 분석 (OpenCV 사용)
    private fun analyzeImage(image: Image): Triple<Double, Double, Double> {
        val nv21 = yuv420ToNV21(image)
        val matYuv = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        matYuv.put(0, 0, nv21)

        // YUV를 RGB로 변환
        val matRgb = Mat()
        Imgproc.cvtColor(matYuv, matRgb, Imgproc.COLOR_YUV2RGB_NV21)

        // 이미지의 평균 RGB 값을 계산
        val meanScalar = Core.mean(matRgb)
        val meanR = meanScalar.`val`[0]
        val meanG = meanScalar.`val`[1]
        val meanB = meanScalar.`val`[2]

        matYuv.release()
        matRgb.release()

        return Triple(meanR, meanG, meanB)
    }

    // YUV_420_888 이미지를 NV21 포맷으로 변환
    private fun yuv420ToNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yPlane.buffer.get(nv21, 0, ySize)

        val chromaRowStride = uPlane.rowStride
        val chromaPixelStride = uPlane.pixelStride

        val rowLength = width / 2

        var offset = ySize

        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        for (row in 0 until height / 2) {
            var uRowPos = row * chromaRowStride
            var vRowPos = row * chromaRowStride

            for (col in 0 until rowLength) {
                val uIndex = uRowPos + col * chromaPixelStride
                val vIndex = vRowPos + col * chromaPixelStride

                if (uBuffer.remaining() > uIndex && vBuffer.remaining() > vIndex) {
                    nv21[offset++] = vBuffer.get(vIndex)
                    nv21[offset++] = uBuffer.get(uIndex)
                }
            }
        }

        return nv21
    }

    // 심박수 측정 시작
    private fun startHeartRateMeasurement() {
        isMeasuring = true
        brightnessValues.clear()  // brightnessBuffer 대신 brightnessValues 사용
        startTime = System.currentTimeMillis()

        activity?.runOnUiThread {
            progressBar.progress = 0
            progressBar.max = 5000 // 5초로 측정 시간 설정
            heartRateValue.text = "심박수 측정 중..."
        }

        backgroundHandler?.post(measureRunnable)
    }


    private val measureRunnable = object : Runnable {
        override fun run() {
            if (!isMeasuring) return

            val elapsedTime = System.currentTimeMillis() - startTime

            activity?.runOnUiThread {
                progressBar.progress = elapsedTime.toInt()
            }

            if (elapsedTime >= 5000) { // 5초 후에 측정 완료
                isMeasuring = false

                // 5초간 그래프의 Y축 값(밝기 값)들의 평균 계산
                val averageBrightness = brightnessValues.average()

                // 평균 밝기 값으로 심박수 계산 (계산 방식에 따라 달라질 수 있음)
                val heartRate = calculateHeartRate(brightnessValues)

                activity?.runOnUiThread {
                    // heartRateValue에 평균 심박수 표시
                    heartRateValue.text = "평균 심박수: ${heartRate.toInt()} BPM"
                }

                updateHeartRateChart(brightnessValues)  // 차트 업데이트
            } else {
                backgroundHandler?.postDelayed(this, SAMPLE_INTERVAL)
            }
        }
    }

    // 심박수 계산 (FFT를 사용하여 주파수 분석)
    private fun calculateHeartRate(brightnessValues: List<Double>): Int {
        val n = brightnessValues.size

        // 데이터 길이가 2의 거듭제곱이 되도록 패딩 추가
        val paddedSize = Integer.highestOneBit(n - 1) shl 1
        val paddedValues = brightnessValues.toMutableList()
        for (i in n until paddedSize) {
            paddedValues.add(0.0)
        }

        val mean = paddedValues.average()
        val detrended = paddedValues.map { it - mean }

        // Hamming 윈도우 적용
        val windowed = detrended.mapIndexed { index, value ->
            val windowValue = 0.54 - 0.46 * Math.cos(2 * Math.PI * index / (paddedSize - 1))
            value * windowValue
        }

        // 실수 배열을 복소수 배열로 변환
        val fftInput = windowed.map { Complex(it, 0.0) }.toTypedArray()

        // FFT 계산
        val fftResult = FFT.fft(fftInput)

        // 주파수 스펙트럼 계산
        val magnitude = fftResult.map { it.abs() }

        // 주파수 해상도 계산
        val samplingRate = 1000.0 / SAMPLE_INTERVAL
        val freqResolution = samplingRate / paddedSize

        // 심박수 범위에 해당하는 인덱스 계산 (40~180 BPM)
        val minIndex = (40.0 / 60 / freqResolution).toInt()
        val maxIndex = (180.0 / 60 / freqResolution).toInt()

        // 최대 스펙트럼 값의 인덱스 찾기
        val maxIndexInRange = magnitude.subList(minIndex, maxIndex).withIndex().maxByOrNull { it.value }?.index
        val peakIndex = if (maxIndexInRange != null) minIndex + maxIndexInRange else -1

        // 심박수 계산
        val heartRate = if (peakIndex > 0) {
            (peakIndex * freqResolution * 60).toInt()
        } else {
            0
        }

        return heartRate
    }

    // 심박수 차트 업데이트
    private fun updateHeartRateChart(brightnessValues: List<Double>) {
        val entries = brightnessValues.mapIndexed { index, brightness ->
            Entry(index.toFloat(), brightness.toFloat())
        }

        val dataSet = LineDataSet(entries, "심박수 변동 (Delta)").apply {
            setDrawCircles(false)
            lineWidth = 2f
            color = resources.getColor(R.color.teal_200, null)
        }

        val lineData = LineData(dataSet)

        // 차트 업데이트 (UI 스레드에서 수행)
        activity?.runOnUiThread {
            heartRateChart.data = lineData
            heartRateChart.invalidate()  // 차트 새로고침
        }

        // 5초 동안의 Y축 값들의 평균을 계산하여 heartRateValue 갱신
        val averageYValue = brightnessValues.average().toFloat()
        activity?.runOnUiThread {
            heartRateValue.text = "평균 Y 값: $averageYValue"
        }
    }

    // 측정 중지
    private fun stopMeasurement() {
        isMeasuring = false
        activity?.runOnUiThread {
            progressBar.progress = 0
            heartRateValue.text = "심박수 측정 중지"
        }
    }

    // 카메라 닫기
    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader.close()
    }

    // 프래그먼트 종료 시 백그라운드 스레드 중지
    override fun onDestroy() {
        super.onDestroy()
        stopBackgroundThread()
    }

    // 카메라 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "카메라 권한이 필요합니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 복소수 클래스 정의
    data class Complex(val re: Double, val im: Double) {
        fun abs(): Double = Math.hypot(re, im)
        operator fun plus(other: Complex) = Complex(re + other.re, im + other.im)
        operator fun minus(other: Complex) = Complex(re - other.re, im - other.im)
        operator fun times(other: Complex) = Complex(re * other.re - im * other.im, re * other.im + im * other.re)
    }

    // FFT 구현
    object FFT {
        fun fft(input: Array<Complex>): Array<Complex> {
            val n = input.size
            if (n == 1) return arrayOf(input[0])

            if (n % 2 != 0) throw IllegalArgumentException("n은 2의 거듭제곱이어야 합니다.")

            val even = fft(input.filterIndexed { index, _ -> index % 2 == 0 }.toTypedArray())
            val odd = fft(input.filterIndexed { index, _ -> index % 2 != 0 }.toTypedArray())

            val result = Array(n) { Complex(0.0, 0.0) }
            for (k in 0 until n / 2) {
                val t = odd[k] * Complex(Math.cos(-2 * Math.PI * k / n), Math.sin(-2 * Math.PI * k / n))
                result[k] = even[k] + t
                result[k + n / 2] = even[k] - t
            }
            return result
        }
    }
}
