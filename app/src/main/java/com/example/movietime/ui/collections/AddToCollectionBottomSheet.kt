package com.example.movietime.ui.collections

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.movietime.R
import com.example.movietime.data.db.UserCollection
import com.example.movietime.data.repository.CollectionsRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bottom sheet to show list of collections to add an item to.
 */
@AndroidEntryPoint
class AddToCollectionBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var repository: CollectionsRepository

    private var itemId: Int = 0
    private var mediaType: String = ""
    private var title: String? = null
    private var posterPath: String? = null

    companion object {
        const val ARG_ITEM_ID = "item_id"
        const val ARG_MEDIA_TYPE = "media_type"
        const val ARG_TITLE = "title"
        const val ARG_POSTER_PATH = "poster_path"

        fun newInstance(itemId: Int, mediaType: String, title: String?, posterPath: String?): AddToCollectionBottomSheet {
            val fragment = AddToCollectionBottomSheet()
            val args = Bundle().apply {
                putInt(ARG_ITEM_ID, itemId)
                putString(ARG_MEDIA_TYPE, mediaType)
                putString(ARG_TITLE, title)
                putString(ARG_POSTER_PATH, posterPath)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = arguments?.getInt(ARG_ITEM_ID) ?: 0
        mediaType = arguments?.getString(ARG_MEDIA_TYPE) ?: ""
        title = arguments?.getString(ARG_TITLE)
        posterPath = arguments?.getString(ARG_POSTER_PATH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_to_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<android.widget.ListView>(R.id.lvCollections)
        val btnCreateNew = view.findViewById<View>(R.id.containerCreateNew)

        // Load collections and check which ones already contain the item
        lifecycleScope.launch {
            val allCollections = repository.getAllCollectionsSync()
            val containingCollections = repository.getCollectionsForItem(itemId, mediaType)
                .map { it.id }.toSet()

            val adapter = CollectionsListAdapter(requireContext(), allCollections, containingCollections)
            listView.adapter = adapter
            
            listView.setOnItemClickListener { _, _, position, _ ->
                val collection = allCollections[position]
                val isSelected = containingCollections.contains(collection.id)
                
                lifecycleScope.launch {
                    if (isSelected) {
                        repository.removeItemFromCollection(collection.id, itemId, mediaType)
                    } else {
                        repository.addItemToCollection(collection.id, itemId, mediaType, title, posterPath)
                    }
                    dismiss()
                }
            }
        }
        
        btnCreateNew.setOnClickListener {
            // Logic to create new collection (could open the dialog from here)
            // For now just dismiss, ideally would open CreateCollectionDialog
            dismiss()
        }
    }

    private class CollectionsListAdapter(
        context: Context,
        private val collections: List<UserCollection>,
        private val selectedCollectionIds: Set<Long>
    ) : BaseAdapter() {

        private val inflater = LayoutInflater.from(context)

        override fun getCount(): Int = collections.size
        override fun getItem(position: Int): Any = collections[position]
        override fun getItemId(position: Int): Long = collections[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: inflater.inflate(R.layout.item_add_to_collection, parent, false)
            val collection = collections[position]
            
            val tvName = view.findViewById<TextView>(R.id.tvCollectionName)
            val tvEmoji = view.findViewById<TextView>(R.id.tvEmoji)
            val ivCheck = view.findViewById<View>(R.id.ivCheck)

            tvName.text = collection.name
            tvEmoji.text = collection.emoji ?: "📂"
            ivCheck.isVisible = selectedCollectionIds.contains(collection.id)
            
            return view
        }
    }
}
