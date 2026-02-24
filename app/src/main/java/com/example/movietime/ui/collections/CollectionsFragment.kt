package com.example.movietime.ui.collections

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.movietime.R
import com.example.movietime.databinding.FragmentCollectionsBinding
import com.example.movietime.utils.GridSpacingItemDecoration
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionsFragment : Fragment() {

    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectionsViewModel by viewModels()
    private lateinit var adapter: CollectionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = CollectionsAdapter(
            onCollectionClick = { collection ->
                val bundle = Bundle().apply {
                    putLong("collection_id", collection.collection.id)
                }
                findNavController().navigate(R.id.action_collections_to_detail, bundle)
            }
        )
        
        binding.rvCollections.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@CollectionsFragment.adapter
            addItemDecoration(GridSpacingItemDecoration(2, resources.getDimensionPixelSize(R.dimen.default_margin), true))
        }
    }

    private fun setupObservers() {
        viewModel.collections.observe(viewLifecycleOwner) { collections ->
            adapter.submitList(collections)
            binding.tvEmptyState.isVisible = collections.isEmpty()
            binding.rvCollections.isVisible = collections.isNotEmpty()
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        binding.fabCreateCollection.setOnClickListener {
            showCreateCollectionDialog()
        }
    }

    private fun showCreateCollectionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_collection, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCollectionName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etCollectionDescription)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.new_collection)
            .setView(dialogView)
            .setPositiveButton(R.string.create_collection) { _, _ ->
                val name = etName.text.toString().trim()
                val description = etDescription.text.toString().trim()
                
                if (name.isNotEmpty()) {
                    viewModel.createCollection(name, description)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
