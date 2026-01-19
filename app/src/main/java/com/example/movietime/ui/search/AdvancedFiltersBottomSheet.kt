package com.example.movietime.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movietime.R
import com.example.movietime.data.model.PersonRole
import com.example.movietime.data.model.SortOption
import com.example.movietime.databinding.BottomSheetAdvancedFiltersBinding
import com.example.movietime.ui.search.adapters.GenreAdapter
import com.example.movietime.ui.search.adapters.PersonAdapter
import com.example.movietime.ui.search.adapters.CompanyAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AdvancedFiltersBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAdvancedFiltersBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SearchViewModel by activityViewModels()
    
    private lateinit var genreAdapter: GenreAdapter
    private lateinit var personAdapter: PersonAdapter
    private lateinit var companyAdapter: CompanyAdapter
    
    private var searchJob: Job? = null
    private var companySearchJob: Job? = null

    var onApplyFilters: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAdvancedFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupGenresRecyclerView()
        setupPeopleRecyclerView()
        setupCompaniesRecyclerView()
        setupYearSpinner()
        setupSortSpinner()
        setupRoleChips()
        setupButtons()
        observeViewModel()
        
        // Load initial data
        viewModel.loadGenres()
        viewModel.loadPopularPeople()
    }
    
    override fun onStart() {
        super.onStart()
        // Expand bottom sheet fully
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    private fun setupGenresRecyclerView() {
        genreAdapter = GenreAdapter(
            onGenreClick = { genre ->
                viewModel.toggleGenre(genre)
                genreAdapter.notifyDataSetChanged()
            },
            isSelected = { genre ->
                viewModel.isGenreSelected(genre)
            }
        )
        
        binding.rvGenres.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = genreAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupPeopleRecyclerView() {
        personAdapter = PersonAdapter { person ->
            viewModel.selectPerson(person)
            updateSelectedPersonUI()
        }
        
        binding.rvPeople.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = personAdapter
            setHasFixedSize(true)
        }
        
        // Search people
        binding.etSearchPerson.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            searchJob?.cancel()
            
            if (query.length < 2) {
                // Show popular people when query is short
                viewModel.loadPopularPeople()
                return@addTextChangedListener
            }
            
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(400)
                viewModel.searchPeople(query)
            }
        }
    }

    private fun setupCompaniesRecyclerView() {
        companyAdapter = CompanyAdapter { company ->
            viewModel.selectCompany(company)
            updateSelectedCompanyUI()
        }

        binding.rvCompanies.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = companyAdapter
            setHasFixedSize(true)
        }

        binding.etSearchCompany.addTextChangedListener { editable ->
            val query = editable?.toString() ?: ""
            companySearchJob?.cancel()

            if (query.length < 2) {
                viewModel.searchCompanies("")
                _binding?.rvCompanies?.visibility = View.GONE
                return@addTextChangedListener
            }

            companySearchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                viewModel.searchCompanies(query)
            }
        }
    }

    private fun setupYearSpinner() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = mutableListOf<String>()
        years.add("Будь-який рік")
        for (year in currentYear downTo 1900) {
            years.add(year.toString())
        }
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = adapter
        
        // Set current selection
        viewModel.selectedYear.value?.let { year ->
            val position = years.indexOf(year.toString())
            if (position > 0) {
                binding.spinnerYear.setSelection(position)
            }
        }
        
        binding.spinnerYear.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val year = if (position == 0) null else years[position].toIntOrNull()
                viewModel.setSelectedYear(year)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupSortSpinner() {
        val sortOptions = SortOption.values().map { it.displayName }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSort.adapter = adapter
        
        // Set current selection
        viewModel.sortOption.value?.let { option ->
            binding.spinnerSort.setSelection(option.ordinal)
        }
        
        binding.spinnerSort.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setSortOption(SortOption.values()[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupRoleChips() {
        binding.chipRoleAny.setOnClickListener { viewModel.setPersonRole(PersonRole.ANY) }
        binding.chipRoleActor.setOnClickListener { viewModel.setPersonRole(PersonRole.ACTOR) }
        binding.chipRoleDirector.setOnClickListener { viewModel.setPersonRole(PersonRole.DIRECTOR) }
        binding.chipRoleWriter.setOnClickListener { viewModel.setPersonRole(PersonRole.WRITER) }
        binding.chipRoleProducer.setOnClickListener { viewModel.setPersonRole(PersonRole.PRODUCER) }
        
        // Update checked state
        viewModel.selectedPersonRole.observe(viewLifecycleOwner) { role ->
            binding.chipRoleAny.isChecked = role == PersonRole.ANY
            binding.chipRoleActor.isChecked = role == PersonRole.ACTOR
            binding.chipRoleDirector.isChecked = role == PersonRole.DIRECTOR
            binding.chipRoleWriter.isChecked = role == PersonRole.WRITER
            binding.chipRoleProducer.isChecked = role == PersonRole.PRODUCER
        }
    }

    private fun setupButtons() {
        binding.btnApplyFilters.setOnClickListener {
            onApplyFilters?.invoke()
            dismiss()
        }
        
        binding.btnResetFilters.setOnClickListener {
            viewModel.resetAdvancedFilters()
            binding.etSearchPerson.text?.clear()
            binding.etSearchCompany.text?.clear()
            binding.spinnerYear.setSelection(0)
            binding.spinnerSort.setSelection(0)
            genreAdapter.notifyDataSetChanged()
            updateSelectedPersonUI()
            updateSelectedCompanyUI()
        }
        
        binding.btnClearSelectedPerson.setOnClickListener {
            viewModel.selectPerson(null)
            binding.etSearchPerson.text?.clear()
            updateSelectedPersonUI()
        }

        binding.btnClearSelectedCompany.setOnClickListener {
            viewModel.selectCompany(null)
            binding.etSearchCompany.text?.clear()
            updateSelectedCompanyUI()
        }
        
        binding.ivClose.setOnClickListener {
            dismiss()
        }
    }

    private fun observeViewModel() {
        viewModel.availableGenres.observe(viewLifecycleOwner) { genres ->
            genreAdapter.submitList(genres)
        }
        
        viewModel.searchedPeople.observe(viewLifecycleOwner) { people ->
            personAdapter.submitList(people)
            binding.rvPeople.visibility = if (people.isNotEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.popularPeople.observe(viewLifecycleOwner) { people ->
            if (binding.etSearchPerson.text.isNullOrBlank()) {
                personAdapter.submitList(people)
                binding.rvPeople.visibility = if (people.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
        
        viewModel.selectedPerson.observe(viewLifecycleOwner) {
            updateSelectedPersonUI()
        }

        viewModel.searchedCompanies.observe(viewLifecycleOwner) { companies ->
            companyAdapter.submitList(companies)
            binding.rvCompanies.visibility = if (companies.isNotEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.selectedCompany.observe(viewLifecycleOwner) {
            updateSelectedCompanyUI()
        }
        
        viewModel.selectedGenres.observe(viewLifecycleOwner) { genres ->
            binding.tvSelectedGenresCount.text = if (genres.isEmpty()) {
                "Жанри не вибрані"
            } else {
                "Вибрано: ${genres.size}"
            }
        }
        
        viewModel.activeFiltersCount.observe(viewLifecycleOwner) { count ->
            binding.btnApplyFilters.text = if (count > 0) {
                "Застосувати ($count)"
            } else {
                "Застосувати"
            }
        }
    }

    private fun updateSelectedPersonUI() {
        val person = viewModel.selectedPerson.value
        if (person != null) {
            binding.layoutSelectedPerson.visibility = View.VISIBLE
            binding.tvSelectedPersonName.text = person.name
            binding.tvSelectedPersonRole.text = person.knownForDepartment ?: ""
            binding.chipGroupRoles.visibility = View.VISIBLE
        } else {
            binding.layoutSelectedPerson.visibility = View.GONE
            binding.chipGroupRoles.visibility = View.GONE
        }
    }

    private fun updateSelectedCompanyUI() {
        val company = viewModel.selectedCompany.value
        if (company != null) {
            binding.layoutSelectedCompany.visibility = View.VISIBLE
            binding.tvSelectedCompanyName.text = company.name ?: getString(R.string.no_title)
            binding.tvSelectedCompanyCountry.text = company.originCountry ?: ""
        } else {
            binding.layoutSelectedCompany.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        companySearchJob?.cancel()
        _binding = null
    }

    companion object {
        const val TAG = "AdvancedFiltersBottomSheet"
        
        fun newInstance(): AdvancedFiltersBottomSheet {
            return AdvancedFiltersBottomSheet()
        }
    }
}
