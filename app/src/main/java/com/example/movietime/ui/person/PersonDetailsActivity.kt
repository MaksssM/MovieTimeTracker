package com.example.movietime.ui.person

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.movietime.R
import com.example.movietime.databinding.ActivityPersonDetailsBinding
import android.content.Intent
import com.example.movietime.ui.details.DetailsActivity
import com.example.movietime.ui.details.TvDetailsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class PersonDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonDetailsBinding
    private val viewModel: PersonDetailsViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(applyLocale(newBase))
    }

    private fun applyLocale(context: Context): Context {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val langPref = prefs.getString("pref_lang", "uk") ?: "uk"
        val locale = when (langPref) {
            "uk" -> Locale("uk")
            "ru" -> Locale("ru")
            "en" -> Locale("en")
            else -> Locale("uk")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        val localeList = android.os.LocaleList(locale)
        config.setLocales(localeList)
        return context.createConfigurationContext(config)
    }

    private val actingAdapter by lazy {
        CombinedCreditsAdapter { id, mediaType ->
            navigateToDetails(id, mediaType)
        }
    }

    private val directingAdapter by lazy {
        CombinedCreditsAdapter { id, mediaType ->
            navigateToDetails(id, mediaType)
        }
    }

    private fun navigateToDetails(id: Int, mediaType: String) {
        when (mediaType) {
            "movie" -> {
                val intent = Intent(this, DetailsActivity::class.java).apply {
                    putExtra("ITEM_ID", id)
                    putExtra("MEDIA_TYPE", "movie")
                }
                startActivity(intent)
            }
            "tv" -> {
                val intent = Intent(this, TvDetailsActivity::class.java).apply {
                    putExtra("ITEM_ID", id)
                    putExtra("MEDIA_TYPE", "tv")
                }
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerViews()

        val personId = intent.getIntExtra("PERSON_ID", -1)
        if (personId != -1) {
            viewModel.loadPersonDetails(personId)
        }

        observeViewModel()
    }

    private fun setupRecyclerViews() {
        binding.rvActing.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@PersonDetailsActivity,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = actingAdapter
        }

        binding.rvDirecting.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@PersonDetailsActivity,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = directingAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.personDetails.collect { person ->
                person?.let {
                    supportActionBar?.title = it.name
                    binding.tvName.text = it.name

                    // Photo
                    val photoUrl = it.profilePath?.let { path -> "https://image.tmdb.org/t/p/w500$path" }
                    binding.ivPhoto.load(photoUrl) {
                        crossfade(400)
                        placeholder(R.drawable.ic_placeholder)
                        error(R.drawable.ic_placeholder)
                    }

                    // Department
                    binding.tvDepartment.text = when (it.knownForDepartment?.lowercase()) {
                        "acting" -> getString(R.string.department_acting)
                        "directing" -> getString(R.string.department_directing)
                        "writing" -> getString(R.string.department_writing)
                        "production" -> getString(R.string.department_production)
                        else -> it.knownForDepartment ?: ""
                    }

                    // Biography
                    binding.tvBiography.text = it.biography?.takeIf { bio -> bio.isNotBlank() }
                        ?: getString(R.string.no_description_available)

                    // Birthday
                    it.birthday?.let { birthday ->
                        binding.layoutBirthday.visibility = View.VISIBLE
                        binding.tvBirthday.text = birthday
                    }

                    // Birthplace
                    it.placeOfBirth?.let { place ->
                        binding.layoutBirthplace.visibility = View.VISIBLE
                        binding.tvBirthplace.text = place
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.actingCredits.collect { credits ->
                 if (credits.isNotEmpty()) {
                    binding.tvActingTitle.visibility = View.VISIBLE
                    binding.rvActing.visibility = View.VISIBLE
                    actingAdapter.submitList(credits)
                } else {
                    binding.tvActingTitle.visibility = View.GONE
                    binding.rvActing.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.directingCredits.collect { credits ->
                 if (credits.isNotEmpty()) {
                    binding.tvDirectingTitle.visibility = View.VISIBLE
                    binding.rvDirecting.visibility = View.VISIBLE
                    directingAdapter.submitList(credits)
                } else {
                    binding.tvDirectingTitle.visibility = View.GONE
                    binding.rvDirecting.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }
}
