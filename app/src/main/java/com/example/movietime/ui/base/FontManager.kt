package com.example.movietime.ui.base

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.movietime.R

/**
 * Heading font picker (Settings → Font).
 *
 * How it works: every heading in layouts carries android:tag="font_heading"
 * (or "font_heading_bold"). [BaseActivity] walks each new window/fragment
 * view tree once and swaps the Typeface directly — no theme magic, so it
 * cannot silently break. Body text always stays readable.
 */
object FontManager {

    const val PREFS = "app_prefs"
    const val KEY_FONT = "pref_heading_font"

    const val MONTSERRAT = "montserrat"
    const val INTER_TIGHT = "inter_tight"
    const val PRESS_START = "press_start"
    const val RUBIK = "rubik"

    const val TAG_HEADING = "font_heading"
    const val TAG_HEADING_BOLD = "font_heading_bold"

    val OPTIONS = listOf(MONTSERRAT, INTER_TIGHT, PRESS_START, RUBIK)

    private val typefaceCache = mutableMapOf<Int, Typeface?>()

    fun getSavedFont(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FONT, MONTSERRAT) ?: MONTSERRAT
    }

    fun saveFont(context: Context, code: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_FONT, code).apply()
    }

    fun displayNameRes(code: String): Int {
        return when (code) {
            INTER_TIGHT -> R.string.font_inter_tight
            PRESS_START -> R.string.font_press_start
            RUBIK -> R.string.font_rubik
            else -> R.string.font_montserrat
        }
    }

    private fun fontRes(code: String, bold: Boolean): Int {
        return when (code) {
            INTER_TIGHT -> if (bold) R.font.inter_tight_bold else R.font.inter_tight_semibold
            PRESS_START -> R.font.press_start_2p
            RUBIK -> if (bold) R.font.rubik_bold else R.font.rubik_semibold
            else -> if (bold) R.font.montserrat_bold else R.font.montserrat_semibold
        }
    }

    /**
     * Typeface for headings, or null to keep whatever the view already has.
     * Never throws — a missing/broken font must not break screens.
     */
    fun getHeadingTypeface(context: Context, bold: Boolean): Typeface? {
        val resId = fontRes(getSavedFont(context), bold)
        return typefaceCache.getOrPut(resId) {
            try {
                ResourcesCompat.getFont(context, resId)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Walks a view tree once and swaps the typeface of tagged headings.
     * Safe to call repeatedly (idempotent) and cheap for unchanged trees.
     */
    fun applyToTree(root: View?) {
        if (root == null) return
        val context = root.context ?: return
        val regular = getHeadingTypeface(context, bold = false)
        val bold = getHeadingTypeface(context, bold = true)
        // If even the default font is missing, bail out entirely.
        if (regular == null && bold == null) return
        applyRecursive(root, regular, bold)
    }

    private fun applyRecursive(view: View, regular: Typeface?, bold: Typeface?) {
        if (view is TextView) {
            when (view.tag) {
                TAG_HEADING -> if (regular != null) view.typeface = regular
                TAG_HEADING_BOLD -> if (bold != null) view.typeface = bold
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyRecursive(view.getChildAt(i), regular, bold)
            }
        }
    }
}
