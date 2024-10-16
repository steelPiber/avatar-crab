package com.example.avatar_crab.presentation

import android.content.Context
import android.widget.TextView

import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import piber.avatar_crab.R

class CustomMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvActiveContent: TextView = findViewById(R.id.tvActiveContent)
    private val tvRestContent: TextView = findViewById(R.id.tvRestContent)
    private val tvExerciseContent: TextView = findViewById(R.id.tvExerciseContent)
    private val tvNormalContent: TextView = findViewById(R.id.tvNormalContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null && e.data is List<*>) {
            val values = e.data as List<Float>
            tvActiveContent.text = String.format("활동: %.2f", values[1])
            tvRestContent.text = String.format("휴식: %.2f", values[3])
            tvExerciseContent.text = String.format("운동: %.2f", values[2])
            tvNormalContent.text = String.format("보통: %.2f", values[0])
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width / 2).toFloat(), (-height).toFloat())
    }
}
