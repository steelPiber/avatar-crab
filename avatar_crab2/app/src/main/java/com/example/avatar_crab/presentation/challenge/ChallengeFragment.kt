package com.example.avatar_crab.presentation.challenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.avatar_crab.data.AppDatabase
import com.example.avatar_crab.data.challenge.Challenge
import com.example.avatar_crab.data.challenge.ChallengeRepository
import com.example.avatar_crab.data.ActivityData
import com.example.avatar_crab.data.exercise.ExerciseRecordEntity
import com.example.avatar_crab.databinding.FragmentChallengeBinding
import kotlinx.coroutines.runBlocking
import java.util.Calendar

class ChallengeFragment : Fragment() {

    private lateinit var repository: ChallengeRepository
    private val viewModel: ChallengeViewModel by activityViewModels { ChallengeViewModelFactory(repository) }
    private var _binding: FragmentChallengeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChallengeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(requireContext())
        repository = ChallengeRepository(database.challengeDao(), database.exerciseRecordDao())
        adapter = ChallengeAdapter(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            viewModel.loadChallengesForDate(selectedDate)
        }

        viewModel.challenges.observe(viewLifecycleOwner, Observer<List<Challenge>> { challenges ->
            adapter.submitList(challenges)
        })

        viewModel.loadChallengesForToday()
        loadActivityDataAndUpdateChallenges()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadActivityDataAndUpdateChallenges() {
        val activityData = getActivityDataFromDatabase()
        val exerciseRecords = getExerciseRecordsFromDatabase()
        viewModel.updateChallengesWithActivityData(activityData, exerciseRecords)
    }

    private fun getActivityDataFromDatabase(): List<ActivityData> {
        val database = AppDatabase.getDatabase(requireContext())
        return runBlocking {
            database.activityDataDao().getAllActivityData()
        }
    }

    private fun getExerciseRecordsFromDatabase(): List<ExerciseRecordEntity> {
        val database = AppDatabase.getDatabase(requireContext())
        return runBlocking {
            database.exerciseRecordDao().getAllRecords()
        }
    }
}
