package com.example.avatar_crab.presentation.challenge

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.avatar_crab.R
import com.example.avatar_crab.data.challenge.Challenge

class ChallengeAdapter(
    private val context: Context
) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {

    private var challenges: List<Challenge> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_challenge, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        holder.bind(challenge)
    }

    override fun getItemCount(): Int {
        return challenges.size
    }

    fun submitList(challengeList: List<Challenge>) {
        challenges = challengeList
        notifyDataSetChanged()
    }

    inner class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tvDescription)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val progressTextView: TextView = itemView.findViewById(R.id.tvProgress)
        private val targetTextView: TextView = itemView.findViewById(R.id.tvTarget)

        fun bind(challenge: Challenge) {
            descriptionTextView.text = challenge.description
            progressBar.progress = challenge.progress
            progressTextView.text = "${challenge.progress}%"
            targetTextView.text = "목표: ${challenge.target}"

            itemView.setOnClickListener {
                showPopup(challenge, itemView)
            }
        }

        private fun showPopup(challenge: Challenge, anchorView: View) {
            val inflater = LayoutInflater.from(context)
            val popupView = inflater.inflate(R.layout.popup_challenge, null)
            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val currentTextView: TextView = popupView.findViewById(R.id.tv_current)
            val targetTextView: TextView = popupView.findViewById(R.id.tv_target)

            currentTextView.text = "현재: ${challenge.progress}"
            targetTextView.text = "목표: ${challenge.target}"

            popupWindow.isFocusable = true
            popupWindow.update()
            popupWindow.showAsDropDown(anchorView, 0, -anchorView.height)
        }
    }
}
