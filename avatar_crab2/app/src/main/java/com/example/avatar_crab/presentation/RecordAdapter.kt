package com.example.avatar_crab.presentation

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.avatar_crab.R
import com.example.avatar_crab.data.exercise.ExerciseRecordEntity
import com.example.avatar_crab.data.exercise.LatLngEntity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions

class RecordAdapter(
    private var records: List<ExerciseRecordEntity>,
    private val context: Context
) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = records[position]
        holder.bind(record)
    }

    override fun getItemCount(): Int {
        return records.size
    }

    fun updateData(newRecords: List<ExerciseRecordEntity>) {
        records = newRecords
        notifyDataSetChanged()
    }

    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), OnMapReadyCallback {
        private val tvDistance: TextView = itemView.findViewById(R.id.tv_distance)
        private val tvDetails: TextView = itemView.findViewById(R.id.tv_details)
        private lateinit var map: GoogleMap
        private val pathPoints: MutableList<LatLng> = mutableListOf()

        init {
            val mapFragment = (itemView.context as? SupportMapFragment)
            mapFragment?.getMapAsync(this)

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val record = records[position]
                    val intent = Intent(context, SegmentDetailActivity::class.java).apply {
                        putExtra("avgHeartRate", record.segments[0].avgHeartRate)
                        putExtra("minHeartRate", record.segments[0].minHeartRate)
                        putExtra("maxHeartRate", record.segments[0].maxHeartRate)
                        putParcelableArrayListExtra("pathPoints", ArrayList(record.pathPoints))
                    }
                    context.startActivity(intent)
                }
            }
        }

        fun bind(record: ExerciseRecordEntity) {
            tvDistance.text = "거리: ${record.distance} km"
            tvDetails.text = "시간: ${record.elapsedTime / 1000} 초, 칼로리: ${record.calories}, 평균 페이스: ${record.avgPace}"

            pathPoints.clear()
            record.pathPoints.forEach {
                val point = LatLng(it.latitude.toDouble(), it.longitude.toDouble())
                pathPoints.add(point)
            }
        }

        override fun onMapReady(googleMap: GoogleMap) {
            map = googleMap
            if (pathPoints.isNotEmpty()) {
                val polylineOptions = PolylineOptions().addAll(pathPoints).width(5f).color(R.color.colorAccent)
                map.addPolyline(polylineOptions)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(pathPoints.first(), 15f))
            }
        }
    }
}
