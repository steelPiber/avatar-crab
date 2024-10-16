package com.example.avatar_crab.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import piber.avatar_crab.R
import com.example.avatar_crab.data.exercise.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.avatar_crab.data.exercise.toExerciseRecord
import com.example.avatar_crab.data.exercise.toExerciseRecordEntity


class RecordListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var recordListAdapter: RecordListAdapter
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recordListAdapter = RecordListAdapter(emptyList(), requireActivity(), viewModel) { record ->
            Toast.makeText(requireContext(), "기록 자세히 보기: ${record.date}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = recordListAdapter

        viewModel.exerciseRecords.observe(viewLifecycleOwner) { records ->
            recordListAdapter.updateData(records)
            val account = GoogleSignIn.getLastSignedInAccount(requireContext())
            val email = account?.email ?: "unknown"
            sendAllRecordsToServer(records.map { it.toExerciseRecord(email) })
        }
    }

    private fun sendAllRecordsToServer(records: List<ExerciseRecord>) {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        val email = account?.email ?: "unknown"
        for (record in records) {
            sendRecordToServer(record.copy(email = email))
        }
    }

    private fun sendRecordToServer(record: ExerciseRecord) {
        lifecycleScope.launch {
            RetrofitClient.recordsInstance.sendRecord(record).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "기록이 서버로 전송되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "서버 응답 오류: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(requireContext(), "서버 전송 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
