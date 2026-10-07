package com.example.movietime.ui.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

/**
 * Common base for all activities: locale wrapping + heading-font application.
 *
 * Fonts are applied programmatically ([FontManager.applyToTree]) instead of
 * theme attributes, because android:fontFamily silently ignores ?attr
 * references and theme overlays get dropped by AppCompat — both failure
 * modes are invisible, so this path uses direct font resources only.
 */
abstract class BaseActivity : AppCompatActivity() {

    private val fragmentFontCallback = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(
            fm: FragmentManager,
            f: Fragment,
            v: View,
            savedInstanceState: Bundle?
        ) {
            FontManager.applyToTree(v)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.movietime.util.LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentFontCallback, true)
    }

    override fun onContentChanged() {
        super.onContentChanged()
        // Runs after every setContentView: covers activity-owned views.
        // Fragment views are covered by [fragmentFontCallback].
        window?.decorView?.findViewById<View>(android.R.id.content)?.let {
            FontManager.applyToTree(it)
        }
    }

    override fun onDestroy() {
        try {
            supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentFontCallback)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
