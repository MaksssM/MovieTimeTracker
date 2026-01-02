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
import com.example.movietime.data.db.UserCollection
import com.example.movietime.databinding.FragmentCollectionDetailBinding
import com.example.movietime.utils.GridSpacingItemDecoration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollectionDetailFragment : Fragment() {

    private var _binding: FragmentCollectionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectionDetailViewModel by viewModels()
    private lateinit var adapter: CollectionItemsAdapter
    
    private var collectionId: Long = 0

    companion object {
        const val ARG_COLLECTION_ID = "collection_id"
        
        fun newInstance(collectionId: Long): CollectionDetailFragment {
            val fragment = CollectionDetailFragment()
            val args = Bundle().apply {
                putLong(ARG_COLLECTION_ID, collectionId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionId = arguments?.getLong(ARG_COLLECTION_ID) ?: 0
        viewModel.setCollectionId(collectionId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = CollectionItemsAdapter(
            onItemClick = { item ->
                val intent = if (item.mediaType == "tv") {
                    android.content.Intent(requireContext(), com.example.movietime.ui.details.TvDetailsActivity::class.java).apply {
                        putExtra("ITEM_ID", item.itemId)
                    }
                } else {
                    android.content.Intent(requireContext(), com.example.movietime.ui.details.DetailsActivity::class.java).apply {
                        putExtra("ITEM_ID", item.itemId)
                        putExtra("MEDIA_TYPE", "movie")
                    }
                }
                startActivity(intent)
            }
        )
        
        binding.rvCollectionItems.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = this@CollectionDetailFragment.adapter
            addItemDecoration(GridSpacingItemDecoration(3, resources.getDimensionPixelSize(R.dimen.small_margin), true))
        }
    }

    private fun setupObservers() {
        viewModel.collection.observe(viewLifecycleOwner) { collection ->
            if (collection != null) {
                updateHeader(collection)
            } else {
                // Collection deleted or not found
                requireActivity().onBackPressed()
            }
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            binding.tvItemCount.text = "${items.size} елементів"
            binding.tvEmptyState.isVisible = items.isEmpty()
            binding.rvCollectionItems.isVisible = items.isNotEmpty()
        }
    }

    private fun updateHeader(collection: UserCollection) {
        with(binding) {
            tvCollectionName.text = collection.name
            tvCollectionDescription.text = collection.description
            tvCollectionDescription.isVisible = !collection.description.isNullOrEmpty()
            tvCollectionEmoji.text = collection.emoji ?: "📂"
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
        
        binding.btnEdit.setOnClickListener {
            showEditDialog()
        }
    }
    
    private fun showEditDialog() {
        val current = viewModel.collection.value ?: return
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_collection, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCollectionName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etCollectionDescription)
        
        etName.setText(current.name)
        etDescription.setText(current.description)

        AlertDialog.Builder(requireContext())
            .setTitle("Редагувати колекцію")
            .setView(dialogView)
            .setPositiveButton("Зберегти") { _, _ ->
                val name = etName.text.toString().trim()
                val description = etDescription.text.toString().trim()
                
                if (name.isNotEmpty()) {
                    viewModel.updateCollection(name, description)
                }
            }
            .setNeutralButton("Видалити") { _, _ ->
                showDeleteConfirmation()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }
    
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Видалити колекцію?")
            .setMessage("Ви впевнені, що хочете видалити цю колекцію? Це дію неможливо скасувати.")
            .setPositiveButton("Видалити") { _, _ ->
                viewModel.deleteCollection()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
