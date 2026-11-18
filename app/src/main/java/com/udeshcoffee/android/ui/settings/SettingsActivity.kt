package com.udeshcoffee.android.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.webkit.WebView
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.service.CollectionService
import com.udeshcoffee.android.extensions.setRoundColor
import com.udeshcoffee.android.ui.common.dialogs.WhatsNewDialog
import com.udeshcoffee.android.utils.PreferenceUtil

/**
 * Created by Udathari on 10/24/2017.
 */
class SettingsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.SettingsTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""

        val titleView = findViewById<TextView>(R.id.title_view)
        titleView.setRoundColor(R.color.settingsAccent)

        setSupportActionBar(toolbar)

        val ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)

        val preferenceFragment = PreferenceFragment()

        val fm = supportFragmentManager
        fm.beginTransaction()
                .replace(R.id.settings_content, preferenceFragment)
                .commit()
    }

    class PreferenceFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_main)

            val prefLibStart = findPreference<ListPreference>(PreferenceUtil.PREF_LIB_START)!!
            prefLibStart.summary = prefLibStart.entry

            val prefCollectContent = findPreference<CheckBoxPreference>(PreferenceUtil.PREF_COLLECT_CONTENT)!!
            findPreference<Preference>(PreferenceUtil.PREF_WIFI_ONLY)!!.isEnabled = prefCollectContent.isChecked

            findPreference<Preference>(PreferenceUtil.PREF_COLLECT_LYRICS)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent(context, CollectionService::class.java)
                intent.action = CollectionService.ACTION_COLLECT_LYRICS
                ContextCompat.startForegroundService(requireContext(), intent)
                true
            }

            findPreference<Preference>(PreferenceUtil.PREF_OPEN)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.dialog_licence, null) as WebView
                view.loadUrl("file:///android_asset/licences.html")
                AlertDialog.Builder(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert)
                        .setTitle("Open Source Licences")
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                true
            }

            findPreference<Preference>(PreferenceUtil.PREF_WHATS_NEW)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                WhatsNewDialog.create().show(parentFragmentManager, "WhatsNewDialog")
                true
            }

            findPreference<Preference>(PreferenceUtil.PREF_VERSION)!!.summary =
                    requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)

        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            when (key) {
                PreferenceUtil.PREF_LIB_START -> {
                    val pref = findPreference<ListPreference>(key)!!
                    pref.summary = pref.entry
                }
                PreferenceUtil.PREF_COLLECT_CONTENT -> {
                    val enabled = sharedPreferences.getBoolean(key, true)
                    findPreference<Preference>(PreferenceUtil.PREF_WIFI_ONLY)!!.isEnabled = enabled
                }
            }

        }
    }
}
