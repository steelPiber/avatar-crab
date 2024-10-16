package com.example.avatar_crab.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import piber.avatar_crab.R
import com.example.avatar_crab.data.exercise.SegmentData

class SegmentDetailAdapter(private val segmentList: List<SegmentData>) :
    RecyclerView.Adapter<SegmentDetailAdapter.SegmentViewHolder>() {

    class SegmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAvgHeartRate: TextView = itemView.findViewById(R.id.tv_avg_heart_rate)
        val tvMinHeartRate: TextView = itemView.findViewById(R.id.tv_min_heart_rate)
        val tvMaxHeartRate: TextView = itemView.findViewById(R.id.tv_max_heart_rate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SegmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_segment, parent, false)
        return SegmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: SegmentViewHolder, position: Int) {
        val segment = segmentList[position]
        holder.tvAvgHeartRate.text = "Avg: ${segment.avgHeartRate}"
        holder.tvMinHeartRate.text = "Min: ${segment.minHeartRate}"
        holder.tvMaxHeartRate.text = "Max: ${segment.maxHeartRate}"
    }

    override fun getItemCount(): Int {
        return segmentList.size
    }
}
