package com.example.avatar_crab.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.avatar_crab.R
import com.example.avatar_crab.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        setupObservers()

        return binding.root
    }

    private fun setupObservers() {
        binding.switchEcg.isChecked = viewModel.ecgEnabled.value ?: false
        binding.switchBradycardia.isChecked = viewModel.bradycardiaEnabled.value ?: false
        binding.switchTachycardia.isChecked = viewModel.tachycardiaEnabled.value ?: false

        binding.switchEcg.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setEcgEnabled(isChecked)
        }

        binding.switchBradycardia.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBradycardiaEnabled(isChecked)
        }

        binding.switchTachycardia.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTachycardiaEnabled(isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
