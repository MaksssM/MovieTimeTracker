package com.example.movietime.ui.tops

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.databinding.FragmentTopsBinding
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopsFragment : Fragment() {

    private var _binding: FragmentTopsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TopsViewModel by viewModels()
    private lateinit var rowAdapter: TopsRowAdapter
    private var lastNavTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rowAdapter = TopsRowAdapter { item, sharedView ->
            val now = System.currentTimeMillis()
            if (now - lastNavTime > 500L) {
                lastNavTime = now
                val target = if (item.mediaType == "tv") {
                    TvDetailsActivity::class.java
                } else {
                    DetailsActivity::class.java
                }
                val intent = Intent(requireContext(), target).apply {
                    putExtra("ITEM_ID", item.id)
                    putExtra("MEDIA_TYPE", item.mediaType)
                }
                val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(), sharedView, "poster_transition"
                )
                startActivity(intent, options.toBundle())
            }
        }
        binding.rvTops.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rowAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }

        binding.btnRetry.setOnClickListener { viewModel.loadTops() }

        viewModel.rows.observe(viewLifecycleOwner) { rows ->
            rowAdapter.submitList(rows)
            binding.rvTops.isVisible = rows.isNotEmpty()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.layoutLoading.isVisible = loading
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.layoutError.isVisible = error && (viewModel.rows.value?.isEmpty() != false)
        }

        if (viewModel.rows.value == null) {
            viewModel.loadTops()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
