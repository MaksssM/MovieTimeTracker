package com.example.movietime.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.movietime.BuildConfig
import com.example.movietime.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.movietime.data.backup.BackupFile
import com.example.movietime.data.backup.BackupManager
import com.example.movietime.worker.NotificationHelper

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private lateinit var prefs: SharedPreferences
    private val viewModel: SettingsViewModel by viewModels()
    private val backupViewModel: BackupViewModel by viewModels()

    private val createBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val result = backupViewModel.createBackupToUri(it)
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), getString(R.string.backup_created_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val importBackupLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { showImportModeDialog(it) }
    }

    private lateinit var tvCurrentLanguage: TextView
    private lateinit var tvCurrentContentLanguage: TextView
    private lateinit var tvCurrentTheme: TextView
    private lateinit var tvCacheSize: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Initialize Views
        tvCurrentLanguage = view.findViewById(R.id.tvCurrentLanguage)
        tvCurrentContentLanguage = view.findViewById(R.id.tvCurrentContentLanguage)
        tvCurrentTheme = view.findViewById(R.id.tvCurrentTheme)
        tvCacheSize = view.findViewById(R.id.tvCacheSize)
        
        // Update Labels
        updateLanguageText()
        updateContentLanguageText()
        updateThemeText()
        updateCacheSize()
        
        val optLanguage = view.findViewById<LinearLayout>(R.id.optLanguage)
        val optContentLanguage = view.findViewById<LinearLayout>(R.id.optContentLanguage)
        val optTheme = view.findViewById<LinearLayout>(R.id.optTheme)
        val btnClearCache = view.findViewById<LinearLayout>(R.id.optClearCache)
        val btnCreateBackup = view.findViewById<LinearLayout>(R.id.optCreateBackup)
        val btnManageBackups = view.findViewById<LinearLayout>(R.id.optManageBackups)
        val btnImportBackup = view.findViewById<LinearLayout>(R.id.optImportBackup)
        
        optLanguage.setOnClickListener { showLanguageDialog() }
        optContentLanguage.setOnClickListener { showContentLanguageDialog() }
        optTheme.setOnClickListener { showThemeDialog() }
        btnClearCache.setOnClickListener { clearCache() }
        btnCreateBackup.setOnClickListener { createBackup() }
        btnManageBackups.setOnClickListener { showBackupManagerDialog() }
        btnImportBackup.setOnClickListener { importBackup() }

        
        val switchCompactMode = view.findViewById<SwitchMaterial>(R.id.switchCompactMode)
        val switchShowRatings = view.findViewById<SwitchMaterial>(R.id.switchShowRatings)
        val switchNewEpisodes = view.findViewById<SwitchMaterial>(R.id.switchNewEpisodes)
        val switchNotifyDigest = view.findViewById<SwitchMaterial>(R.id.switchNotifyDigest)

        // Initialize Switches
        switchCompactMode.isChecked = prefs.getBoolean("pref_compact_mode", false)
        switchShowRatings.isChecked = prefs.getBoolean("pref_show_ratings", true)
        switchNewEpisodes?.isChecked = prefs.getBoolean("pref_notify_episodes", true)
        switchNotifyDigest?.isChecked = prefs.getBoolean("pref_notify_digest", false)

        // Set Listeners
        switchCompactMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("pref_compact_mode", isChecked) }
            Toast.makeText(requireContext(), getString(R.string.theme_changed), Toast.LENGTH_SHORT).show()
        }

        switchShowRatings.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("pref_show_ratings", isChecked) }
        }
        
        switchNewEpisodes?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("pref_notify_episodes", isChecked) }
            NotificationHelper.scheduleEpisodeNotifications(requireContext(), isChecked)
            Toast.makeText(
                requireContext(),
                if (isChecked) getString(R.string.notify_new_episodes) + ": " + getString(R.string.on) else getString(R.string.notify_new_episodes) + ": " + getString(R.string.off),
                Toast.LENGTH_SHORT
            ).show()
        }

        switchNotifyDigest?.setOnCheckedChangeListener { _, isChecked ->
             prefs.edit { putBoolean("pref_notify_digest", isChecked) }
        }
    }

    private fun createBackup() {
        // Generate default filename with timestamp
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val fileName = "backup_$timestamp.json"
        
        // Launch file picker to choose save location
        createBackupLauncher.launch(fileName)
    }

    private fun importBackup() {
        // Launch file picker to choose backup file
        importBackupLauncher.launch(arrayOf("application/json", "*/*"))
    }

    private fun showBackupManagerDialog() {
        lifecycleScope.launch {
            val backups = backupViewModel.getBackupListDirect()
            if (!isAdded) return@launch
            if (backups.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.no_backups), Toast.LENGTH_SHORT).show()
            } else {
                showBackupListDialog(backups)
            }
        }
    }

    private fun showBackupListDialog(backups: List<BackupFile>) {
        val sdf = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
        val displayItems = backups.map { backup ->
            val autoLabel = if (backup.isAutoBackup) " [${getString(R.string.backup_auto_label)}]" else ""
            val cleanName = backup.name
                .removePrefix("backup_")
                .removePrefix(BackupManager.AUTO_BACKUP_PREFIX)
                .removeSuffix(".json")
                .replace("_", " ")
            val date = sdf.format(Date(backup.dateModified))
            val line2 = if (backup.watchedCount >= 0) {
                getString(R.string.backup_counts_format, backup.watchedCount, backup.plannedCount, backup.watchingCount) +
                    "  \u00b7  ${backup.sizeKb} " + getString(R.string.backup_kb_unit)
            } else {
                "$date  \u00b7  ${backup.sizeKb} " + getString(R.string.backup_kb_unit)
            }
            "$cleanName$autoLabel\n$line2"
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.backup_list))
            .setItems(displayItems) { _, which -> showBackupActionDialog(backups[which]) }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showBackupActionDialog(backup: BackupFile) {
        val actions = arrayOf(
            getString(R.string.backup_restore_replace),
            getString(R.string.backup_restore_merge),
            getString(R.string.delete_backup)
        )
        val cleanTitle = backup.name.removeSuffix(".json").replace("_", " ")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(cleanTitle)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> confirmAndRestore(backup, merge = false)
                    1 -> confirmAndRestore(backup, merge = true)
                    2 -> deleteBackup(backup)
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun confirmAndRestore(backup: BackupFile, merge: Boolean) {
        val message = getString(
            if (merge) R.string.restore_backup_merge_confirm else R.string.restore_backup_confirm
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.restore_backup))
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    // Always create a safety auto-backup before restore
                    backupViewModel.createAutoBackup()
                    val progressDialog = MaterialAlertDialogBuilder(requireContext())
                        .setMessage(getString(R.string.backup_restore_in_progress))
                        .setCancelable(false)
                        .show()
                    val result = backupViewModel.restoreBackup(backup.path, merge)
                    progressDialog.dismiss()
                    result.onSuccess {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.backup_restored_success),
                            Toast.LENGTH_LONG
                        ).show()
                        requireActivity().recreate()
                    }
                    result.onFailure { error ->
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.backup_restore_error, error.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showImportModeDialog(uri: android.net.Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.backup_restore_mode_title))
            .setMessage(getString(R.string.import_backup_mode_message))
            .setPositiveButton(getString(R.string.backup_restore_replace)) { _, _ ->
                performImportRestore(uri, merge = false)
            }
            .setNeutralButton(getString(R.string.backup_restore_merge)) { _, _ ->
                performImportRestore(uri, merge = true)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun performImportRestore(uri: android.net.Uri, merge: Boolean) {
        lifecycleScope.launch {
            val progressDialog = MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.backup_restore_in_progress))
                .setCancelable(false)
                .show()
            val result = backupViewModel.restoreBackupFromUri(uri, merge)
            progressDialog.dismiss()
            result.onSuccess {
                Toast.makeText(requireContext(), getString(R.string.backup_restored_success), Toast.LENGTH_SHORT).show()
                requireActivity().recreate()
            }
            result.onFailure { error ->
                Toast.makeText(requireContext(), getString(R.string.backup_restore_error, error.message), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteBackup(backup: BackupFile) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_backup))
            .setMessage(getString(R.string.delete_backup_confirm))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    val result = backupViewModel.deleteBackup(backup.path)
                    result.onSuccess {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.backup_deleted),
                            Toast.LENGTH_SHORT
                        ).show()
                        showBackupManagerDialog()
                    }
                    result.onFailure { error ->
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.backup_restore_error, error.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateLanguageText() {
        val langCode = prefs.getString("pref_lang", "uk") ?: "uk"
        tvCurrentLanguage.text = when (langCode) {
            "uk" -> getString(R.string.lang_uk)
            "ru" -> getString(R.string.lang_ru)
            "en" -> getString(R.string.lang_en)
            else -> getString(R.string.lang_uk)
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.lang_uk), getString(R.string.lang_ru), getString(R.string.lang_en))
        val codes = arrayOf("uk", "ru", "en")
        
        val currentCode = prefs.getString("pref_lang", "uk")
        val checkedItem = codes.indexOf(currentCode).takeIf { it != -1 } ?: 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.language_title))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val selectedCode = codes[which]
                if (selectedCode != currentCode) {
                    viewModel.onLanguageChanged()
                    // Use the modern per-app language API — it handles
                    // saving, applying, and recreating activities automatically
                    com.example.movietime.util.LocaleHelper.setLocale(requireContext(), selectedCode)
                    dialog.dismiss()
                    return@setSingleChoiceItems
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun updateContentLanguageText() {
        val langCode = com.example.movietime.util.LocaleHelper.getSavedLanguageCode(requireContext())
        tvCurrentContentLanguage.text = when (langCode) {
            "uk" -> getString(R.string.lang_uk)
            "ru" -> getString(R.string.lang_ru)
            "en" -> getString(R.string.lang_en)
            else -> getString(R.string.lang_uk)
        }
    }

    private fun showContentLanguageDialog() {
        // Язык контента теперь совпадает с основным языком приложения
        showLanguageDialog()
    }

    private fun updateThemeText() {
        val themeCode = prefs.getString("pref_theme", "system") ?: "system"
        tvCurrentTheme.text = when (themeCode) {
            "system" -> getString(R.string.theme_system)
            "light" -> getString(R.string.theme_light)
            "dark" -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_system)
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark))
        val codes = arrayOf("system", "light", "dark")
        
        val currentCode = prefs.getString("pref_theme", "system")
        val checkedItem = codes.indexOf(currentCode).takeIf { it != -1 } ?: 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.theme_title))
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val selectedCode = codes[which]
                if (selectedCode != currentCode) {
                    prefs.edit { putString("pref_theme", selectedCode) }
                    updateThemeText()
                    applyTheme(selectedCode)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun clearCache() {
        // Simple confirmation
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.clear_cache))
            .setMessage(getString(R.string.clear_cache) + "?")
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                viewModel.clearCache()
                updateCacheSize()
                Toast.makeText(requireContext(), getString(R.string.clear_cache_success), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateCacheSize() {
        try {
            val cacheDir = requireContext().cacheDir
            val size = getFolderSize(cacheDir)
            val formattedSize = formatFileSize(size)
            tvCacheSize.text = getString(R.string.cache_size_format, formattedSize)
        } catch (e: Exception) {
            tvCacheSize.text = getString(R.string.cache_size_format, "0 KB")
        }
    }

    private fun getFolderSize(folder: File): Long {
        var size: Long = 0
        if (folder.isDirectory) {
            folder.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getFolderSize(file)
                } else {
                    file.length()
                }
            }
        } else {
            size = folder.length()
        }
        return size
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    }

    private fun applyTheme(value: String) {
        when (value) {
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}