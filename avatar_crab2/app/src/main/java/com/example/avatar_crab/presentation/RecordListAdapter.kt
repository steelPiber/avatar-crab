package com.example.avatar_crab.presentation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.avatar_crab.R
import com.example.avatar_crab.data.exercise.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Locale

class RecordListAdapter(
    private var records: List<ExerciseRecord>,
    private val fragmentActivity: FragmentActivity,
    private val viewModel: MainViewModel,
    private val onRecordClick: (ExerciseRecord) -> Unit
) : RecyclerView.Adapter<RecordListAdapter.RecordViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = records[position]
        holder.bind(record)
        holder.itemView.setOnClickListener {
            onRecordClick(record)
            val account = GoogleSignIn.getLastSignedInAccount(fragmentActivity)
            val email = account?.email ?: "unknown" // Get the email
            sendRecordToServer(record, email) // Pass email

            val intent = Intent(fragmentActivity, SegmentDetailActivity::class.java)
            intent.putExtra("pathPoints", ArrayList(record.pathPoints.map {
                LatLngEntity(it.latitude, it.longitude)
            }))
            intent.putExtra("segments", ArrayList(record.segments.map {
                SegmentDataEntity(
                    avgHeartRate = it.avgHeartRate,
                    minHeartRate = it.minHeartRate,
                    maxHeartRate = it.maxHeartRate,
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            }))
            if (record.segments.isNotEmpty()) {
                val avgHeartRate = record.segments.map { it.avgHeartRate }.average().toFloat()
                val minHeartRate = record.segments.map { it.minHeartRate }.minOrNull() ?: 0f
                val maxHeartRate = record.segments.map { it.maxHeartRate }.maxOrNull() ?: 0f
                intent.putExtra("avgHeartRate", avgHeartRate)
                intent.putExtra("minHeartRate", minHeartRate)
                intent.putExtra("maxHeartRate", maxHeartRate)
            }
            fragmentActivity.startActivity(intent)
        }
        holder.itemView.setOnLongClickListener {
            showDeleteConfirmationDialog(record)
            true
        }
    }

    override fun getItemCount(): Int {
        return records.size
    }

    // updateData 함수에서 email을 전달
    fun updateData(newRecords: List<ExerciseRecordEntity>) {
        val account = GoogleSignIn.getLastSignedInAccount(fragmentActivity)
        val email = account?.email ?: "unknown"
        records = newRecords.map { it.toExerciseRecord(email) } // 이메일 전달
        notifyDataSetChanged()
    }

    private fun sendRecordToServer(record: ExerciseRecord, email: String) {
        val call = RetrofitClient.recordsInstance.sendRecord(record.copy(email = email))
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(fragmentActivity, "기록이 서버로 전송되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(fragmentActivity, "서버 응답 오류: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(fragmentActivity, "서버 전송 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun showDeleteConfirmationDialog(record: ExerciseRecord) {
        AlertDialog.Builder(fragmentActivity).apply {
            setTitle("기록 삭제")
            setMessage("이 기록을 삭제하시겠습니까?")
            setPositiveButton("삭제") { _, _ ->
                viewModel.deleteExerciseRecord(record.toExerciseRecordEntity())
                Toast.makeText(fragmentActivity, "기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            setNegativeButton("취소", null)
        }.create().show()
    }

    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), OnMapReadyCallback {
        private val tvDistance: TextView = itemView.findViewById(R.id.tv_distance)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvCalories: TextView = itemView.findViewById(R.id.tv_calories)
        private val tvHeartRate: TextView = itemView.findViewById(R.id.tv_heart_rate)
        private val tvDetails: TextView = itemView.findViewById(R.id.tv_details)
        private val mapView: MapView = itemView.findViewById(R.id.map_view)
        private lateinit var googleMap: GoogleMap
        private val pathPoints: MutableList<LatLng> = mutableListOf()

        init {
            mapView.onCreate(null)
            mapView.getMapAsync(this)
        }

        @SuppressLint("SetTextI18n")
        fun bind(record: ExerciseRecord) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val startTime = dateFormat.format(record.date)

            tvDistance.text = "거리: ${"%.2f".format(record.distance / 1000)} km"
            tvTime.text = "시간: ${record.elapsedTime / 1000} 초"
            tvCalories.text = "칼로리: ${record.calories.roundToInt()}"
            val avgHeartRate = if (record.segments.isNotEmpty()) {
                record.segments.map { it.avgHeartRate }.average().roundToInt()
            } else {
                0
            }
            tvHeartRate.text = "심박수: $avgHeartRate BPM"
            tvDetails.text = "시작 시간: $startTime\n시간: ${record.elapsedTime / 1000} 초, 칼로리: ${record.calories.roundToInt()}"

            pathPoints.clear()
            pathPoints.addAll(record.pathPoints.map { LatLng(it.latitude, it.longitude) })
            updateMap()
        }

        override fun onMapReady(map: GoogleMap) {
            googleMap = map
            updateMap()
        }

        private fun updateMap() {
            if (::googleMap.isInitialized && pathPoints.isNotEmpty()) {
                googleMap.clear()
                val polylineOptions = PolylineOptions().addAll(pathPoints).width(30f).color(fragmentActivity.resources.getColor(R.color.colorAccent, null))
                googleMap.addPolyline(polylineOptions)

                val boundsBuilder = LatLngBounds.builder()
                for (point in pathPoints) {
                    boundsBuilder.include(point)
                }
                val bounds = boundsBuilder.build()
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

                val firstPoint = pathPoints.first()
                val lastPoint = pathPoints.last()
                googleMap.addMarker(MarkerOptions().position(firstPoint).title("Start").icon(bitmapDescriptorFromVector(fragmentActivity, R.drawable.flag_start)))
                googleMap.addMarker(MarkerOptions().position(lastPoint).title("End").icon(bitmapDescriptorFromVector(fragmentActivity, R.drawable.person_end)))
            }
        }

        fun onResume() {
            mapView.onResume()
        }

        fun onPause() {
            mapView.onPause()
        }

        fun onDestroy() {
            mapView.onDestroy()
        }
    }

    override fun onViewRecycled(holder: RecordViewHolder) {
        super.onViewRecycled(holder)
        holder.onDestroy()
    }

    fun onResume() {
        notifyDataSetChanged()
    }

    fun onPause() {
        notifyDataSetChanged()
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable?.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable!!.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
